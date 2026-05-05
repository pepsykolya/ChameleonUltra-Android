package com.chameleonultra.android.di

import com.chameleonultra.android.data.BleRepositoryImpl
import com.chameleonultra.android.domain.usecase.BleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBleRepository(
        impl: BleRepositoryImpl
    ): BleRepository
}
