package com.github.aakumykov.sync_dir_to_cloud.di.modules

import android.content.Context
import android.content.res.Resources
import com.github.aakumykov.sync_dir_to_cloud.di.annotations.AppContext
import com.github.aakumykov.sync_dir_to_cloud.di.annotations.AppScope
import dagger.Module
import dagger.Provides

@Module
class ContextModule(private val appContext: Context) {

    @AppScope
    @Provides
    @AppContext
    fun provideAppContext(): Context {
        return appContext
    }

    @AppScope
    @Provides
    fun provideResource(): Resources {
        return appContext.resources
    }
}