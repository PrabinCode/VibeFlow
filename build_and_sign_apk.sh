#!/bin/bash
set -e

BUILD_TYPE="release"
BUILD_VARIANT="full"

while [[ "$#" -gt 0 ]]; do
  case $1 in
    --full) BUILD_VARIANT="full" ;;
    --foss) BUILD_VARIANT="foss" ;;
    --release) BUILD_TYPE="release" ;;
    --debug) BUILD_TYPE="debug" ;;
    -h|--help) exit 0 ;;
    *) shift ;;
  esac
  shift
done

echo "===================="
echo "Building VibeFlow APK ($BUILD_VARIANT - $BUILD_TYPE)"
echo "===================="

# Step 1: Clean and build with Gradle (Gradle handles native signing, zipalign & optimization)
./gradlew clean --no-configuration-cache
./gradlew :androidApp:assembleRelease --no-configuration-cache

OUTPUT_DIR="./androidApp/build/outputs/apk/$BUILD_TYPE"
echo "Renaming APK outputs in $OUTPUT_DIR..."

cd "$OUTPUT_DIR"
for apk in *.apk; do
  [ -f "$apk" ] || continue
  case "$apk" in
    VibeFlow-*) continue ;;
    *)
      clean_name="${apk/androidApp-/}"
      clean_name="${clean_name/app-/}"
      new_name="VibeFlow-${BUILD_VARIANT}-${clean_name}"
      cp -f "$apk" "$new_name"
      echo "Prepared release asset: $new_name"
      ;;
  esac
done

echo "===================="
echo "VibeFlow APK build completed successfully!"
echo "Outputs:"
ls -lh VibeFlow-*.apk || ls -lh *.apk
echo "===================="