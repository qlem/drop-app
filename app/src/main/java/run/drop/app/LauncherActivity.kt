package run.drop.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setStatusBarColor(window, this)

        val token = TokenStore.getToken(this)
        if (token == null) {
            startActivity(Intent(this, SignInActivity::class.java))
        } else {
            testTokenAuth()
        }

        finish()
    }

    private fun testTokenAuth() {
        Apollo.client.query(
                AmIAuthQuery.builder().build()).enqueue(object : ApolloCall.Callback<AmIAuthQuery.Data>() {

            override fun onResponse(response: Response<AmIAuthQuery.Data>) {
                if (!response.data()!!.amIAuth().isAuth()) {
                    startActivity(Intent(this@LauncherActivity, SignInActivity::class.java))
                } else {
                    startActivity(Intent(this@LauncherActivity, DropActivity::class.java))
                }
            }

            override fun onFailure(e: ApolloException) {}
        })
    }
}
