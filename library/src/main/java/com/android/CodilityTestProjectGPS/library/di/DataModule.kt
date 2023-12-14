package com.android.CodilityTestProjectGPS.di

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.android.CodilityTestProjectGPS.library.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.android.CodilityTestProjectGPS.library.util.Constants.Companion.BASE_URL
import kotlinx.coroutines.GlobalScope
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Configuration for DI on the repository and shared location manager
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {


    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context
    ):SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @Singleton
    fun provideSharedLocationManager(
        @ApplicationContext context: Context,
        prefs: SharedPreferences
    ): SharedLocationManager =
        SharedLocationManager(context, GlobalScope, prefs)

    @Singleton
    @Provides
    fun provideHttpClient(): OkHttpClient {
        return OkHttpClient
            .Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Singleton
    @Provides
    fun provideConverterFactory(): GsonConverterFactory =
        GsonConverterFactory.create()

    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gsonConverterFactory: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(gsonConverterFactory)
            .build()
    }

    @Singleton
    @Provides
    fun provideGeocodingService(retrofit: Retrofit): ApiInterface? =
        retrofit.create(ApiInterface::class.java)

    @Provides
    fun provideContext(
        @ApplicationContext context: Context,
    ): Context {
        return context
    }


}