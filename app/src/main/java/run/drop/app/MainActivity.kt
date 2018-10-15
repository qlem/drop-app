package run.drop.app

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.apollographql.apollo.ApolloClient
import okhttp3.OkHttpClient
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException

private const val BASE_URL = "http://10.0.2.2:4000"
private lateinit var apolloClient: ApolloClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val okHttpClient = OkHttpClient.Builder().build()

        apolloClient = ApolloClient
                .builder()
                .serverUrl(BASE_URL)
                .okHttpClient(okHttpClient)
                .build()

        val droppedQuery = DroppedQuery.builder().build()

        apolloClient.query(droppedQuery).enqueue(object : ApolloCall.Callback<DroppedQuery.Data>() {

            override fun onResponse(dataResponse: Response<DroppedQuery.Data>) {
                Log.d("APOLLO", "OK !!!")
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message.toString())
                e.stackTrace.forEach { Log.e("APOLLO", it.toString()) }
            }
        })
    }
}
