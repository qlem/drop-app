package run.drop.app.apollo

import com.apollographql.apollo.ApolloClient

object Apollo {

    // TODO replace by https://api.drop.run for prod (http://10.0.2.2:4000 for local)
    private const val BASE_URL = "https://api.drop.run"

    val client: ApolloClient = ApolloClient
            .builder()
            .serverUrl(BASE_URL)
            .okHttpClient(HttpClient.instance)
            .build()
}