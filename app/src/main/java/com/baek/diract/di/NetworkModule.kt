package com.baek.diract.di

import com.baek.diract.BuildConfig
import com.baek.diract.data.local.TokenManager
import com.baek.diract.data.remote.api.AuthApi
import com.baek.diract.data.remote.api.RefreshRequest
import com.baek.diract.data.remote.api.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val refreshMutex = Mutex()

    // 요청마다 Access Token을 Authorization 헤더에 첨부 (인메모리 캐시 — 블로킹 없음)
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()

            val token = tokenManager.cachedAccessToken
            if (token == null || original.header("Authorization") != null) {
                return@Interceptor chain.proceed(original)
            }
            val request = original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()

            chain.proceed(request)
        }
    }

    // 401 응답 시 Refresh Token으로 Access Token 재발급 (Mutex로 동시 refresh 방지)
    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenManager: TokenManager,
        @AuthRetrofit authRetrofit: Retrofit
    ): Authenticator {
        return Authenticator { _, response ->
            if (response.request.header("X-Retry") != null) return@Authenticator null

            // 이 요청이 사용한 토큰
            val failedToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")

            runBlocking {
                refreshMutex.withLock {
                    // 다른 스레드가 이미 refresh 완료했는지 확인
                    val currentToken = tokenManager.cachedAccessToken
                    if (currentToken != null && currentToken != failedToken) {
                        // 이미 갱신됨 → 새 토큰으로 재시도
                        return@runBlocking response.request.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .header("X-Retry", "true")
                            .build()
                    }

                    val refreshToken = tokenManager.cachedRefreshToken
                        ?: return@runBlocking null

                    val authApi = authRetrofit.create(AuthApi::class.java)
                    val refreshResponse = try {
                        authApi.refreshToken(RefreshRequest(refreshToken))
                    } catch (e: Exception) {
                        return@runBlocking null
                    }

                    if (refreshResponse.success && refreshResponse.data != null) {
                        tokenManager.saveTokens(
                            refreshResponse.data.accessToken,
                            refreshResponse.data.refreshToken
                        )
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${refreshResponse.data.accessToken}")
                            .header("X-Retry", "true")
                            .build()
                    } else {
                        null
                    }
                }
            }
        }
    }

    // refresh 요청 전용 Retrofit (Authenticator 없음 — 무한 루프 방지)
    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        authenticator: Authenticator
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }
}
