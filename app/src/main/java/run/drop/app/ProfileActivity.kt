package run.drop.app

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import run.drop.app.apollo.Apollo
import run.drop.app.apollo.IsAuth
import run.drop.app.utils.setStatusBarColor

class ProfileActivity : AppCompatActivity() {

    private lateinit var usernameView: TextView
    private lateinit var emailView: TextView
    private lateinit var dropsView: TextView
    private lateinit var likesView: TextView
    private lateinit var reportsView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_layout)
        setStatusBarColor(window, this)
        setTheme(R.style.AppTheme)

        usernameView = findViewById(R.id.username)
        emailView = findViewById(R.id.email)
        dropsView = findViewById(R.id.nb_drop)
        likesView = findViewById(R.id.nb_like)
        reportsView = findViewById(R.id.nb_report)

        if (IsAuth.state) {
            fetchProfile()
        }
    }

    private fun fetchProfile() {
        Apollo.client.query(
                ProfileQuery.builder().build()).enqueue(object : ApolloCall.Callback<ProfileQuery.Data>() {

            override fun onResponse(response: Response<ProfileQuery.Data>) {
                val profile = response.data()!!.profile
                runOnUiThread {
                    usernameView.text = profile.me.username
                    emailView.text = profile.me.email
                    dropsView.text = profile.drops.toString()
                    likesView.text = profile.likes.toString()
                    reportsView.text = profile.reports.toString()
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message ?: "apollo error: ProfileQuery")
            }
        })
    }
}