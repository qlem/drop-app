package run.drop.app

import okhttp3.OkHttpClient

object HttpClient {
    var instance: OkHttpClient

    init {
        instance = OkHttpClient.Builder().addInterceptor {
            val token = TokenStore.token
            val request = if (token != null) {
                it.request().newBuilder().header("Authorization", "Bearer $token").build()
            } else {
                it.request()
            }
            it.proceed(request)
        }
                .build()
    }
}