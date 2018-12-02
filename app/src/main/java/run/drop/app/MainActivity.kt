package run.drop.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.android.material.appbar.CollapsingToolbarLayout
import androidx.core.content.ContextCompat
import android.view.WindowManager


private const val BASE_URL = "http://10.0.2.2:4000"
private lateinit var apolloClient: ApolloClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        TokenStore.initStore(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatusBar)

        val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.ppcollapsingToolbarLayout)
        collapsingToolbarLayout.title = getString(R.string.app_name)

        apolloClient = ApolloClient
                .builder()
                .serverUrl(BASE_URL)
                .okHttpClient(HttpClient.instance)
                .build()

        val droppedQuery = DroppedQuery.builder().build()
        apolloClient.query(droppedQuery).enqueue(object : ApolloCall.Callback<DroppedQuery.Data>() {

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
