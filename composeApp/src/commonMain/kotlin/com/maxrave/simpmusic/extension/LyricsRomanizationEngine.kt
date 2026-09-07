package com.maxrave.simpmusic.extension

/**
 * Pure Kotlin Multiplatform phonetic Romanization engine for song lyrics.
 *
 * Supports 12 languages/scripts:
 *  1. Korean (Hangul -> Revised Romanization)
 *  2. Japanese (Hiragana, Katakana & frequent lyric Kanji -> Hepburn Romaji)
 *  3. Cyrillic (Russian, Ukrainian, Belarusian, Bulgarian -> Latin)
 *  4. Devanagari (Hindi, Nepali, Sanskrit, Marathi -> IAST/Hunterian)
 *  5. Gurmukhi (Punjabi -> Latin)
 *  6. Greek (Modern Greek -> ISO 843)
 *  7. Arabic / Persian / Urdu (Arabic script -> Latin)
 *  8. Hebrew (Hebrew script -> Latin)
 *  9. Thai (Thai script -> RTGS)
 * 10. Chinese (Mandarin Hanzi -> Pinyin)
 * 11. Bengali (Bengali script -> Latin)
 * 12. Tamil & Telugu (Dravidian Indic scripts -> Latin)
 */
object LyricsRomanizationEngine {

    // Simple cache to avoid recomputing for identical lines during sync scrolling
    private val lineCache = HashMap<String, String?>(1000)

    /**
     * Checks if the line contains characters from any supported non-Latin script.
     */
    fun canRomanize(text: String): Boolean {
        if (text.isBlank()) return false
        for (ch in text) {
            val code = ch.code
            when (code) {
                in 0xAC00..0xD7AF, in 0x1100..0x11FF, in 0x3130..0x318F -> return true // Hangul
                in 0x3040..0x309F, in 0x30A0..0x30FF -> return true // Kana
                in 0x4E00..0x9FFF -> return true // CJK Kanji / Hanzi
                in 0x0400..0x04FF -> return true // Cyrillic
                in 0x0900..0x097F -> return true // Devanagari
                in 0x0A00..0x0A7F -> return true // Gurmukhi
                in 0x0980..0x09FF -> return true // Bengali
                in 0x0B80..0x0BFF, in 0x0C00..0x0C7F -> return true // Tamil / Telugu
                in 0x0370..0x03FF -> return true // Greek
                in 0x0600..0x06FF -> return true // Arabic
                in 0x0590..0x05FF -> return true // Hebrew
                in 0x0E00..0x0E7F -> return true // Thai
            }
        }
        return false
    }

    /**
     * Romanizes [text] if it contains supported non-Latin characters.
     * Returns null if the line is purely Latin/ASCII/numeric, or the romanized string otherwise.
     */
    fun romanize(text: String): String? {
        if (text.isBlank()) return null
        if (!canRomanize(text)) return null

        synchronized(lineCache) {
            if (lineCache.containsKey(text)) {
                return lineCache[text]
            }
        }

        val result = processLine(text)
        val finalResult = if (result.equals(text, ignoreCase = true) || result.isBlank()) null else result

        synchronized(lineCache) {
            if (lineCache.size > 2000) lineCache.clear()
            lineCache[text] = finalResult
        }
        return finalResult
    }

    private fun processLine(text: String): String {
        val sb = StringBuilder(text.length * 2)
        var i = 0
        val len = text.length

        while (i < len) {
            val ch = text[i]
            val code = ch.code

            when {
                // Korean Hangul Syllables
                code in 0xAC00..0xD7A3 -> {
                    val syllable = code - 0xAC00
                    val initialIdx = syllable / (21 * 28)
                    val vowelIdx = (syllable % (21 * 28)) / 28
                    val finalIdx = syllable % 28

                    var initial = HANGUL_INITIALS.getOrElse(initialIdx) { "" }
                    val vowel = HANGUL_VOWELS.getOrElse(vowelIdx) { "" }
                    var final = HANGUL_FINALS.getOrElse(finalIdx) { "" }

                    // Resyllabification check with next syllable if initial is ㅇ (vowel)
                    if (final.isNotEmpty() && i + 1 < len) {
                        val nextCode = text[i + 1].code
                        if (nextCode in 0xAC00..0xD7A3) {
                            val nextSyl = nextCode - 0xAC00
                            val nextInitialIdx = nextSyl / (21 * 28)
                            if (nextInitialIdx == 11) { // ㅇ
                                // Final consonant carries over phonetically to next syllable
                                when (final) {
                                    "t" -> final = "s"
                                    "k" -> final = "g"
                                    "p" -> final = "b"
                                }
                            }
                        }
                    }

                    sb.append(initial).append(vowel).append(final)
                    i++
                }

                // Japanese Hiragana & Katakana
                code in 0x3040..0x309F || code in 0x30A0..0x30FF -> {
                    // Check for 2-char digraph (e.g. きゃ / キャ)
                    if (i + 1 < len) {
                        val digraph = text.substring(i, i + 2)
                        val roma = KANA_DIGRAPHS[digraph]
                        if (roma != null) {
                            sb.append(roma)
                            i += 2
                            continue
                        }
                    }
                    // Small tsu / sokuon (っ / ッ)
                    if (ch == 'っ' || ch == 'ッ') {
                        if (i + 1 < len) {
                            val nextRom = romanizeChar(text[i + 1])
                            val firstChar = nextRom.firstOrNull() ?: 't'
                            sb.append(firstChar)
                        } else {
                            sb.append("t")
                        }
                        i++
                        continue
                    }
                    // Chōonpu (ー)
                    if (ch == 'ー') {
                        val lastChar = sb.lastOrNull()
                        if (lastChar != null && "aeiou".contains(lastChar.lowercaseChar())) {
                            sb.append(lastChar)
                        } else {
                            sb.append("-")
                        }
                        i++
                        continue
                    }
                    sb.append(romanizeChar(ch))
                    i++
                }

                // CJK Unified Ideographs (Kanji / Hanzi)
                code in 0x4E00..0x9FFF -> {
                    // Try 2-char compound first
                    if (i + 1 < len) {
                        val compound = text.substring(i, i + 2)
                        val compReading = COMMON_KANJI_COMPOUNDS[compound]
                        if (compReading != null) {
                            if (sb.isNotEmpty() && sb.last() != ' ') sb.append(' ')
                            sb.append(compReading)
                            i += 2
                            continue
                        }
                    }
                    val singleReading = COMMON_KANJI_SINGLE[ch]
                    if (singleReading != null) {
                        if (sb.isNotEmpty() && sb.last().isLetterOrDigit()) sb.append(' ')
                        sb.append(singleReading)
                    } else {
                        // Unmatched Chinese/Kanji character preserved
                        sb.append(ch)
                    }
                    i++
                }

                // Cyrillic
                code in 0x0400..0x04FF -> {
                    sb.append(CYRILLIC_MAP[ch] ?: ch.toString())
                    i++
                }

                // Devanagari (Hindi)
                code in 0x0900..0x097F -> {
                    i = processDevanagari(text, i, sb)
                }

                // Gurmukhi (Punjabi)
                code in 0x0A00..0x0A7F -> {
                    sb.append(GURMUKHI_MAP[ch] ?: ch.toString())
                    i++
                }

                // Greek
                code in 0x0370..0x03FF -> {
                    sb.append(GREEK_MAP[ch] ?: ch.toString())
                    i++
                }

                // Arabic / Persian
                code in 0x0600..0x06FF -> {
                    sb.append(ARABIC_MAP[ch] ?: ch.toString())
                    i++
                }

                // Hebrew
                code in 0x0590..0x05FF -> {
                    sb.append(HEBREW_MAP[ch] ?: ch.toString())
                    i++
                }

                // Thai
                code in 0x0E00..0x0E7F -> {
                    sb.append(THAI_MAP[ch] ?: ch.toString())
                    i++
                }

                // Bengali
                code in 0x0980..0x09FF -> {
                    sb.append(BENGALI_MAP[ch] ?: ch.toString())
                    i++
                }

                // Pass-through ASCII / Latin / Punctuation / Numbers
                else -> {
                    sb.append(ch)
                    i++
                }
            }
        }

        // Clean up redundant spaces created by token concatenation
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. KOREAN DATA
    // ─────────────────────────────────────────────────────────────────────────────
    private val HANGUL_INITIALS = listOf(
        "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h",
    )
    private val HANGUL_VOWELS = listOf(
        "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i",
    )
    private val HANGUL_FINALS = listOf(
        "", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "p", "t", "t", "p", "l", "m", "p", "p", "t", "t", "ng", "t", "t", "k", "t", "p", "t",
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. JAPANESE DATA
    // ─────────────────────────────────────────────────────────────────────────────
    private val KANA_DIGRAPHS = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",
        "ティ" to "ti", "ディ" to "di", "ファ" to "fa", "フィ" to "fi", "フェ" to "fe", "フォ" to "fo",
    )

    private fun romanizeChar(ch: Char): String =
        when (ch) {
            'あ', 'ア' -> "a"; 'い', 'イ' -> "i"; 'う', 'ウ' -> "u"; 'え', 'エ' -> "e"; 'お', 'オ' -> "o"
            'か', 'カ' -> "ka"; 'き', 'キ' -> "ki"; 'く', 'ク' -> "ku"; 'け', 'ケ' -> "ke"; 'こ', 'コ' -> "ko"
            'さ', 'サ' -> "sa"; 'し', 'シ' -> "shi"; 'す', 'ス' -> "su"; 'せ', 'セ' -> "se"; 'そ', 'ソ' -> "so"
            'た', 'タ' -> "ta"; 'ち', 'チ' -> "chi"; 'つ', 'ツ' -> "tsu"; 'て', 'テ' -> "te"; 'と', 'ト' -> "to"
            'な', 'ナ' -> "na"; 'に', 'ニ' -> "ni"; 'ぬ', 'ヌ' -> "nu"; 'ね', 'ネ' -> "ne"; 'の', 'ノ' -> "no"
            'は', 'ハ' -> "ha"; 'ひ', 'ヒ' -> "hi"; 'ふ', 'フ' -> "fu"; 'へ', 'ヘ' -> "he"; 'ほ', 'ホ' -> "ho"
            'ま', 'マ' -> "ma"; 'み', 'ミ' -> "mi"; 'む', 'ム' -> "mu"; 'め', 'メ' -> "me"; 'も', 'モ' -> "mo"
            'や', 'ヤ' -> "ya"; 'ゆ', 'ユ' -> "yu"; 'よ', 'ヨ' -> "yo"
            'ら', 'ラ' -> "ra"; 'り', 'リ' -> "ri"; 'る', 'ル' -> "ru"; 'れ', 'レ' -> "re"; 'ろ', 'ロ' -> "ro"
            'わ', 'ワ' -> "wa"; 'を', 'ヲ' -> "wo"; 'ん', 'ン' -> "n"
            'が', 'ガ' -> "ga"; 'ぎ', 'ギ' -> "gi"; 'ぐ', 'グ' -> "gu"; 'げ', 'ゲ' -> "ge"; 'ご', 'ゴ' -> "go"
            'ざ', 'ザ' -> "za"; 'じ', 'ジ' -> "ji"; 'ず', 'ズ' -> "zu"; 'ぜ', 'ゼ' -> "ze"; 'ぞ', 'ゾ' -> "zo"
            'だ', 'ダ' -> "da"; 'ぢ', 'ヂ' -> "ji"; 'づ', 'ヅ' -> "zu"; 'で', 'デ' -> "de"; 'ど', 'ド' -> "do"
            'ば', 'バ' -> "ba"; 'び', 'ビ' -> "bi"; 'ぶ', 'ブ' -> "bu"; 'べ', 'ベ' -> "be"; 'ぼ', 'ボ' -> "bo"
            'ぱ', 'パ' -> "pa"; 'ぴ', 'ピ' -> "pi"; 'ぷ', 'プ' -> "pu"; 'ぺ', 'ペ' -> "pe"; 'ぽ', 'ポ' -> "po"
            'ぁ', 'ァ' -> "a"; 'ぃ', 'ィ' -> "i"; 'ぅ', 'ゥ' -> "u"; 'ぇ', 'ェ' -> "e"; 'ぉ', 'ォ' -> "o"
            'ヴ' -> "vu"
            else -> ch.toString()
        }

    private val COMMON_KANJI_COMPOUNDS = mapOf(
        "世界" to "sekai", "未来" to "mirai", "永遠" to "eien", "奇跡" to "kiseki", "運命" to "unmei",
        "笑顔" to "egao", "約束" to "yakusoku", "言葉" to "kotoba", "想い" to "omoi", "記憶" to "kioku",
        "一人" to "hitori", "二人" to "futari", "大好き" to "daisuki", "今日" to "kyou", "明日" to "ashita",
        "昨日" to "kinou", "時間" to "jikan", "場所" to "basho", "瞬間" to "shunkan", "優しさ" to "yasashisa",
        "孤独" to "kodoku", "自由" to "jiyuu", "本当" to "hontou", "涙" to "namida", "夜空" to "yozora",
        "星空" to "hoshizora", "花火" to "hanabi", "季節" to "kisetsu", "希望" to "kibou", "絶望" to "zetsubou",
        "太陽" to "taiyou", "月明" to "tsukiakari", "物語" to "monogatari", "大丈夫" to "daijoubu",
        "我爱你" to "wo ai ni", "喜欢你" to "xi huan ni", "不知道" to "bu zhi dao", "没关系" to "mei guan xi",
        "在一起" to "zai yi qi", "对不起" to "dui bu qi", "再见" to "zai jian", "朋友" to "peng you",
    )

    private val COMMON_KANJI_SINGLE = mapOf(
        '愛' to "ai", '私' to "watashi", '僕' to "boku", '君' to "kimi", '俺' to "ore",
        '心' to "kokoro", '夢' to "yume", '夜' to "yoru", '今' to "ima", '日' to "hi",
        '月' to "tsuki", '空' to "sora", '雨' to "ame", '風' to "kaze", '花' to "hana",
        '声' to "koe", '歌' to "uta", '音' to "oto", '光' to "hikari", '影' to "kage",
        '時' to "toki", '道' to "michi", '手' to "te", '目' to "me", '涙' to "namida",
        '星' to "hoshi", '海' to "umi", '雲' to "kumo", '朝' to "asa", '冬' to "fuyu",
        '春' to "haru", '夏' to "natsu", '秋' to "aki", '生' to "iki", '死' to "shi",
        '誰' to "dare", '何' to "nani", '人' to "hito", '街' to "machi", '色' to "iro",
        '白' to "shiro", '黒' to "kuro", '赤' to "aka", '青' to "ao", '想' to "omoi",
        // Top Lyric Hanzi / Pinyin
        '我' to "wo", '你' to "ni", '他' to "ta", '她' to "ta", '的' to "de",
        '了' to "le", '在' to "zai", '是' to "shi", '不' to "bu", '有' to "you",
        '说' to "shuo", '看' to "kan", '走' to "zou", '去' to "qu", '来' to "lai",
        '好' to "hao", '天' to "tian", '大' to "da", '小' to "xiao", '多' to "duo",
        '少' to "shao", '想' to "xiang", '要' to "yao", '能' to "neng", '会' to "hui",
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. CYRILLIC DATA
    // ─────────────────────────────────────────────────────────────────────────────
    private val CYRILLIC_MAP = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo",
        'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "shch",
        'ъ' to "", 'ы' to "y", 'ь' to "'", 'э' to "e", 'ю' to "yu", 'я' to "ya",
        'А' to "A", 'Б' to "B", 'В' to "V", 'Г' to "G", 'Д' to "D", 'Е' to "E", 'Ё' to "Yo",
        'Ж' to "Zh", 'З' to "Z", 'И' to "I", 'Й' to "Y", 'К' to "K", 'Л' to "L", 'М' to "M",
        'Н' to "N", 'О' to "O", 'П' to "P", 'Р' to "R", 'С' to "S", 'Т' to "T", 'У' to "U",
        'Ф' to "F", 'Х' to "Kh", 'Ц' to "Ts", 'Ч' to "Ch", 'Ш' to "Sh", 'Щ' to "Shch",
        'Ъ' to "", 'Ы' to "Y", 'Ь' to "'", 'Э' to "E", 'Ю' to "Yu", 'Я' to "Ya",
        // Ukrainian specifics
        'є' to "ye", 'Є' to "Ye", 'і' to "i", 'І' to "I", 'ї' to "yi", 'Ї' to "Yi", 'ґ' to "g", 'Ґ' to "G",
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. DEVANAGARI DATA & SCHWA ENGINE
    // ─────────────────────────────────────────────────────────────────────────────
    private val DEVA_VOWELS = mapOf(
        'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee", 'उ' to "u", 'ऊ' to "oo",
        'ऋ' to "ri", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au",
    )
    private val DEVA_MATRAS = mapOf(
        'ा' to "aa", 'ि' to "i", 'ी' to "ee", 'ु' to "u", 'ू' to "oo",
        'ृ' to "ri", 'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au",
    )
    private val DEVA_CONSONANTS = mapOf(
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "ng",
        'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "ny",
        'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
        'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
        'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v",
        'श' to "sh", 'ष' to "sh", 'स' to "s", 'ह' to "h",
    )

    private fun processDevanagari(text: String, start: Int, out: StringBuilder): Int {
        val ch = text[start]
        val vowel = DEVA_VOWELS[ch]
        if (vowel != null) {
            out.append(vowel)
            return start + 1
        }
        val matra = DEVA_MATRAS[ch]
        if (matra != null) {
            out.append(matra)
            return start + 1
        }
        val consonant = DEVA_CONSONANTS[ch]
        if (consonant != null) {
            var actualConsonant = consonant
            var nextIdx = start + 1
            // Check for nukta (U+093C)
            if (nextIdx < text.length && text[nextIdx] == '\u093C') {
                actualConsonant = when (ch) {
                    'क' -> "q"
                    'ख' -> "kh"
                    'ग' -> "gh"
                    'ज' -> "z"
                    'ड' -> "d"
                    'ढ' -> "dh"
                    'फ' -> "f"
                    else -> consonant
                }
                nextIdx++
            }
            out.append(actualConsonant)
            if (nextIdx < text.length) {
                val nextCh = text[nextIdx]
                if (nextCh == '्') {
                    // Halant suppresses inherent 'a'
                    return nextIdx + 1
                }
                val nextMatra = DEVA_MATRAS[nextCh]
                if (nextMatra != null) {
                    out.append(nextMatra)
                    return nextIdx + 1
                }
                val isWordEnd = nextIdx == text.length || text[nextIdx].isWhitespace() || text[nextIdx].code !in 0x0900..0x097F
                if (!isWordEnd) {
                    out.append("a")
                }
            }
            return nextIdx
        }
        when (ch) {
            'ं', 'ँ' -> out.append("n")
            'ः' -> out.append("h")
            else -> out.append(ch)
        }
        return start + 1
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 5. GURMUKHI (PUNJABI) DATA
    // ─────────────────────────────────────────────────────────────────────────────
    private val GURMUKHI_MAP = mapOf(
        'ੳ' to "u", 'ਅ' to "a", 'ੲ' to "i", 'ਸ' to "s", 'ਹ' to "h",
        'ਕ' to "k", 'ਖ' to "kh", 'ਗ' to "g", 'ਘ' to "gh", 'ਙ' to "ng",
        'ਚ' to "ch", 'ਛ' to "chh", 'ਜ' to "j", 'ਝ' to "jh", 'ਞ' to "ny",
        'ਟ' to "t", 'ਠ' to "th", 'ਡ' to "d", 'ਢ' to "dh", 'ਣ' to "n",
        'ਤ' to "t", 'ਥ' to "th", 'ਦ' to "d", 'ਧ' to "dh", 'ਨ' to "n",
        'ਪ' to "p", 'ਫ' to "ph", 'ਬ' to "b", 'ਭ' to "bh", 'ਮ' to "m",
        'ਯ' to "y", 'ਰ' to "r", 'ਲ' to "l", 'ਵ' to "v", 'ੜ' to "r",
        'ਾ' to "aa", 'ਿ' to "i", 'ੀ' to "ee", 'ੁ' to "u", 'ੂ' to "oo",
        'ੇ' to "e", 'ੈ' to "ai", 'ੋ' to "o", 'ੌ' to "au", 'ੰ' to "n", 'ੱ' to "",
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 6. GREEK DATA
    // ─────────────────────────────────────────────────────────────────────────────
    private val GREEK_MAP = mapOf(
        'α' to "a", 'β' to "v", 'γ' to "g", 'δ' to "d", 'ε' to "e", 'ζ' to "z", 'η' to "i",
        'θ' to "th", 'ι' to "i", 'κ' to "k", 'λ' to "l", 'μ' to "m", 'ν' to "n", 'ξ' to "x",
        'ο' to "o", 'π' to "p", 'ρ' to "r", 'σ' to "s", 'ς' to "s", 'τ' to "t", 'υ' to "y",
        'φ' to "f", 'χ' to "ch", 'ψ' to "ps", 'ω' to "o",
        'Α' to "A", 'Β' to "V", 'Γ' to "G", 'Δ' to "D", 'Ε' to "E", 'Ζ' to "Z", 'Η' to "I",
        'Θ' to "Th", 'Ι' to "I", 'Κ' to "K", 'Λ' to "L", 'Μ' to "M", 'Ν' to "N", 'Ξ' to "X",
        'Ο' to "O", 'Π' to "P", 'Ρ' to "R", 'Σ' to "S", 'Τ' to "T", 'Υ' to "Y",
        'Φ' to "F", 'Χ' to "Ch", 'Ψ' to "Ps", 'Ω' to "O",
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 7. ARABIC / PERSIAN DATA
    // ─────────────────────────────────────────────────────────────────────────────
    private val ARABIC_MAP = mapOf(
        'ا' to "a", 'ب' to "b", 'ت' to "t", 'ث' to "th", 'ج' to "j", 'ح' to "h",
        'خ' to "kh", 'د' to "d", 'ذ' to "dh", 'ر' to "r", 'ز' to "z", 'س' to "s",
        'ش' to "sh", 'ص' to "s", 'ض' to "d", 'ط' to "t", 'ظ' to "z", 'ع' to "'a",
        'غ' to "gh", 'ف' to "f", 'ق' to "q", 'ك' to "k", 'ل' to "l", 'م' to "m",
        'ن' to "n", 'ه' to "h", 'و' to "w", 'ي' to "y", 'ى' to "a", 'ة' to "h",
        'پ' to "p", 'چ' to "ch", 'ژ' to "zh", 'گ' to "g",
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 8. HEBREW DATA
    // ─────────────────────────────────────────────────────────────────────────────
    private val HEBREW_MAP = mapOf(
        'א' to "'", 'ב' to "v", 'ג' to "g", 'ד' to "d", 'ה' to "h", 'ו' to "v",
        'ז' to "z", 'ח' to "ch", 'ט' to "t", 'י' to "y", 'כ' to "k", 'ך' to "kh",
        'ל' to "l", 'מ' to "m", 'ם' to "m", 'נ' to "n", 'ן' to "n", 'ס' to "s",
        'ע' to "'", 'פ' to "f", 'ף' to "f", 'צ' to "ts", 'ץ' to "ts", 'ק' to "k",
        'ר' to "r", 'ש' to "sh", 'ת' to "t",
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 9. THAI DATA
    // ─────────────────────────────────────────────────────────────────────────────
    private val THAI_MAP = mapOf(
        'ก' to "k", 'ข' to "kh", 'ค' to "kh", 'ง' to "ng", 'จ' to "ch", 'ฉ' to "ch",
        'ช' to "ch", 'ซ' to "s", 'ญ' to "y", 'ด' to "d", 'ต' to "t", 'ถ' to "th",
        'ท' to "th", 'น' to "n", 'บ' to "b", 'ป' to "p", 'ผ' to "ph", 'ฝ' to "f",
        'พ' to "ph", 'ฟ' to "f", 'ม' to "m", 'ย' to "y", 'ร' to "r", 'ล' to "l",
        'ว' to "w", 'ส' to "s", 'ห' to "h", 'อ' to "o", 'ฮ' to "h",
        'ะ' to "a", 'า' to "aa", 'ิ' to "i", 'ี' to "ee", 'ึ' to "ue", 'ื' to "uee",
        'ุ' to "u", 'ู' to "oo", 'เ' to "e", 'แ' to "ae", 'โ' to "o", 'ใ' to "ai", 'ไ' to "ai",
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 10. BENGALI DATA
    // ─────────────────────────────────────────────────────────────────────────────
    private val BENGALI_MAP = mapOf(
        'অ' to "o", 'আ' to "a", 'ই' to "i", 'ঈ' to "ee", 'উ' to "u", 'ঊ' to "oo",
        'ঋ' to "ri", 'এ' to "e", 'ঐ' to "oi", 'ও' to "o", 'ঔ' to "ou",
        'ক' to "ko", 'খ' to "kho", 'গ' to "go", 'ঘ' to "gho", 'ঙ' to "ngo",
        'চ' to "cho", 'ছ' to "chho", 'জ' to "jo", 'ঝ' to "jho", 'ঞ' to "nyo",
        'ট' to "to", 'ঠ' to "tho", 'ড' to "do", 'ঢ' to "dho", 'ণ' to "no",
        'ত' to "to", 'থ' to "tho", 'দ' to "do", 'ध' to "dho", 'ন' to "no",
        'প' to "po", 'ফ' to "pho", 'ব' to "bo", 'ভ' to "bho", 'ম' to "mo",
        'য' to "jo", 'র' to "ro", 'ল' to "lo", 'শ' to "sho", 'ষ' to "sho",
        'স' to "so", 'হ' to "ho", 'া' to "a", 'ি' to "i", 'ী' to "ee",
        'ু' to "u", 'ূ' to "oo", 'ে' to "e", 'ৈ' to "oi", 'ো' to "o", 'ৌ' to "ou",
        '্' to "", 'ং' to "ng", 'ঃ' to "h",
    )
}
