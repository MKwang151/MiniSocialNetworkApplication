package com.example.minisocialnetworkapplication.core.di

import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * ============================================================================
 * NETWORK MODULE - REST API Configuration
 * ============================================================================
 *
 * This module provides Retrofit and OkHttpClient for REST API connections.
 *
 * NOTE: This module is ACTIVE but does NOT affect the current app flow.
 * The app still uses Firebase SDK directly (see FirebaseModule.kt).
 * No API interfaces are provided, so no part of the app uses these instances.
 *
 * To use REST API in the future:
 * 1. Create API interfaces in core/data/remote/api/
 * 2. Add @Provides methods for each API interface below
 * 3. Inject the API interfaces into your repositories
 *
 * ============================================================================
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Base URL Configuration:
     *
     * - Emulator:     "http://10.0.2.2:8080/api/"     (maps to localhost)
     * - Real Device:  "http://192.168.x.x:8080/api/" (your PC's IP address)
     * - Production:   "https://your-server.com/api/"
     */
    private const val BASE_URL = "http://10.0.2.2:8080/api/"

    // Connection timeouts (in seconds)
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L

    /**
     * Provides HttpLoggingInterceptor for debugging network requests.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * Provides OkHttpClient with logging and authentication interceptors.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                // Add Firebase Auth token to request headers
                val token = try {
                    FirebaseAuth.getInstance()
                        .currentUser
                        ?.getIdToken(false)
                        ?.result
                        ?.token
                } catch (e: Exception) {
                    null
                }

                val request = chain.request().newBuilder()
                    .apply {
                        token?.let { addHeader("Authorization", "Bearer $it") }
                    }
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()

                chain.proceed(request)
            }
            .build()
    }

    /**
     * Provides Retrofit instance configured with base URL and Gson converter.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * ========================================================================
     * API INTERFACE PROVIDERS (Add when needed)
     * ========================================================================
     *
     * Currently no API interfaces are provided, so the app uses Firebase directly.
     * To connect to Spring Boot backend, add providers like:
     *
     * @Provides
     * @Singleton
     * fun provideUserApi(retrofit: Retrofit): UserApi {
     *     return retrofit.create(UserApi::class.java)
     * }
     *
     * @Provides
     * @Singleton
     * fun providePostApi(retrofit: Retrofit): PostApi {
     *     return retrofit.create(PostApi::class.java)
     * }
     */
}

/**
 * ============================================================================
 * EXAMPLE API INTERFACES (Create in core/data/remote/api/ when needed)
 * ============================================================================
 *
 * // UserApi.kt
 * interface UserApi {
 *     @GET("users/{id}")
 *     suspend fun getUser(@Path("id") id: String): Response<UserDto>
 *
 *     @PUT("users/{id}")
 *     suspend fun updateUser(@Path("id") id: String, @Body user: UserDto): Response<UserDto>
 * }
 *
 * // PostApi.kt
 * interface PostApi {
 *     @GET("posts")
 *     suspend fun getPosts(): Response<List<PostDto>>
 *
 *     @POST("posts")
 *     suspend fun createPost(@Body post: PostDto): Response<PostDto>
 * }
 */
