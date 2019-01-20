package run.drop.app.apollo

import okhttp3.OkHttpClient
import run.drop.app.token.TokenHandler

object HttpClient {
    var instance: OkHttpClient

    init {
        instance = OkHttpClient.Builder().addInterceptor {
            val token = TokenHandler.token
            val request = if (token != null) {
                it.request().newBuilder().header("Authorization", "Bearer $token").build()
            } else {
                it.request()
            }
            it.proceed(request)
        }.build()
    }
}