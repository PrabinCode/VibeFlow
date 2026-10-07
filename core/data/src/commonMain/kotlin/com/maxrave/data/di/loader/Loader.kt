package com.maxrave.data.di.loader

import com.maxrave.data.di.databaseModule
import com.maxrave.data.di.mediaHandlerModule
import com.maxrave.data.di.repositoryModule
import org.koin.core.context.loadKoinModules

fun loadAllModules() {
    loadKoinModules(
        listOf(
            databaseModule,
            repositoryModule,
        ),
    )
    loadKoinModules(mediaHandlerModule)
    loadMediaService()
    loadLoginSyncModule()
}

expect fun loadMediaService()

/** Login sync is one-sided per platform: Desktop hosts, Android sends, iOS takes no part. */
expect fun loadLoginSyncModule()