package com.chameleonultra.android.di

import android.content.Context
import com.chameleonultra.android.data.ble.BleManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BleModule {

    @Provides
    @Singleton
    fun provideBleManager(
        @ApplicationContext context: Context
    ): BleManager = BleManager(context)
}
