package run.drop.app

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException

class DropActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drop)
        val deconnectButton : Button = findViewById(R.id.disconnection)

        testTokenAuth()

        deconnectButton.setOnClickListener {
            TokenStore.clearToken(this)
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun testTokenAuth() {
        Apollo.client.query(
                AmIAuthQuery.builder().build()).enqueue(object : ApolloCall.Callback<AmIAuthQuery.Data>() {
            override fun onResponse(response: Response<AmIAuthQuery.Data>) {
                if (!response.data()!!.amIAuth().isAuth()) {
                    startActivity(Intent(this@DropActivity, SignInActivity::class.java))
                    finish()
                }
            }
            override fun onFailure(e: ApolloException) {
            }
        })
    }
}
