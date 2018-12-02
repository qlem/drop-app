package run.drop.app

import com.apollographql.apollo.ApolloClient

object ApolloClient {
    private const val BASE_URL = "http://10.0.2.2:4000"
    var instance: ApolloClient

    init {
        instance = ApolloClient
                .builder()
                .serverUrl(BASE_URL)
                .okHttpClient(HttpClient.instance)
                .build()
    }
}