package run.drop.app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException

class SignInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)
        setStatusBarColor(window, this)

        val droppedQuery = DroppedQuery.builder().build()
        Apollo.client.query(droppedQuery).enqueue(object : ApolloCall.Callback<DroppedQuery.Data>() {

            override fun onResponse(dataResponse: Response<DroppedQuery.Data>) {
                Log.d("APOLLO", dataResponse.data()?.dropped()?.first()?.id())
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message.toString())
                e.stackTrace.forEach { Log.e("APOLLO", it.toString()) }
            }
        })
    }
}
