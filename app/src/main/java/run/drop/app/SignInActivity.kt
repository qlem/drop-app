package run.drop.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import run.drop.app.apollo.Apollo
import run.drop.app.apollo.TokenHandler
import run.drop.app.utils.setStatusBarColor

class SignInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        setStatusBarColor(window, this)

        val signInEmail: EditText = findViewById(R.id.email)
        val signInPassword: EditText = findViewById(R.id.password)
        val signInButton: Button = findViewById(R.id.sign_in_button)
        val signUpButton: TextView = findViewById(R.id.sign_up_button)

        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        signInButton.setOnClickListener {
            if (checkEmptyFields(signInEmail, signInPassword)) {
                logIn(signInEmail, signInPassword)
            }
        }
    }

    private fun logIn(email : EditText, password : EditText) {
        Apollo.client.mutate(
                LoginMutation.builder()
                        .email(email.text.toString())
                        .password(password.text.toString())
                        .build())?.enqueue(object : ApolloCall.Callback<LoginMutation.Data>() {

            override fun onResponse(dataResponse: Response<LoginMutation.Data>) {
                when {
                    dataResponse.data()?.login()?.token() != null -> {
                        TokenHandler.setToken(dataResponse.data()?.login()?.token().toString(),
                                this@SignInActivity)
                        startActivity(Intent(this@SignInActivity, DropActivity::class.java))
                        finish()
                    }
                    dataResponse.errors()[0].message() == "Invalid email" -> this@SignInActivity.runOnUiThread {
                        email.error = "Wrong email"
                    }
                    else -> this@SignInActivity.runOnUiThread {
                        password.error = "Wrong password"
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message)
                e.printStackTrace()
            }
        })
    }

    private fun checkEmptyFields(email: EditText, password: EditText): Boolean {
        if (email.text.isBlank()) {
            email.error = "Can not be empty"
            return false
        }
        if (password.text.isBlank()) {
            password.error = "Can not be empty"
            return false
        }
        return true
    }
}
