package run.drop.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException

class SignInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        setStatusBarColor(window, this)

        val signInEmail: EditText = findViewById(R.id.email)
        val signInPassword: EditText = findViewById(R.id.password)
        val signInButton: Button = findViewById(R.id.sign_in_button)
        val signUpButton: TextView = findViewById(R.id.sign_up_button)

        signUpButton.setOnClickListener{
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        signInButton.setOnClickListener{
            if (signInEmail.text.isBlank() || signInPassword.text.isBlank()) {
                Toast.makeText(applicationContext, "Please enter an email and a password",
                        Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            logIn(signInEmail.text.toString(), signInPassword.text.toString())
        }
    }

    private fun logIn(email : String, password : String) {
        Apollo.client.mutate(
                LoginMutation.builder()
                        .email(email)
                        .password(password)
                        .build())?.enqueue(object : ApolloCall.Callback<LoginMutation.Data>() {

            override fun onResponse(dataResponse: Response<LoginMutation.Data>) {
                if (dataResponse.data()?.login()?.token() != null) {
                    TokenStore.setToken(dataResponse.data()?.login()?.token().toString(),
                            this@SignInActivity)
                    startActivity(Intent(this@SignInActivity, DropActivity::class.java))
                    finish()
                } else {
                    this@SignInActivity.runOnUiThread {
                        Toast.makeText(applicationContext, "Incorrect password or email.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                e.stackTrace.forEach { Log.e("APOLLO", it.toString()) }
            }
        })
    }
}
