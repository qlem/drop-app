package run.drop.app.apollo

import com.apollographql.apollo.ApolloClient

object Apollo {

    // TODO replace by https://api.drop.run for prod
    private const val BASE_URL = "http://localhost:4000"

    val client: ApolloClient = ApolloClient
            .builder()
            .serverUrl(BASE_URL)
            .okHttpClient(HttpClient.instance)
            .build()
}