package run.drop.app.apollo

import com.apollographql.apollo.ApolloClient

object Apollo {
    private const val BASE_URL = "http://10.0.2.2:4000"

    val client: ApolloClient = ApolloClient
            .builder()
            .serverUrl(BASE_URL)
            .okHttpClient(HttpClient.instance)
            .build()
}