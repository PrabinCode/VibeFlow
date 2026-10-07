package com.maxrave.data.di.loader

import com.maxrave.data.loginsync.LoginSyncSenderRepositoryImpl
import com.maxrave.data.loginsync.LoginSyncStore
import com.maxrave.domain.repository.LoginSyncSenderRepository
import com.maxrave.media3.di.loadMediaService
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.simpmusic.loginsync.LoginSyncClient

actual fun loadMediaService() {
    loadMediaService()
}

actual fun loadLoginSyncModule() {
    loadKoinModules(
        module {
            single { LoginSyncStore(get(), get(), get()) }
            single<LoginSyncSenderRepository> { LoginSyncSenderRepositoryImpl(get(), LoginSyncClient()) }
        },
    )
}