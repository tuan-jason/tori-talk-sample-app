package com.torilab.socket.di

import com.castalk.socket.extensions.JsonHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ParserModule {

    @Singleton
    @Provides
    fun provideMoshi(
    ): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()


    @Singleton
    @Provides
    fun provideJsonHelper(
        moshi: Moshi
    ): JsonHelper = JsonHelper(moshi)
}