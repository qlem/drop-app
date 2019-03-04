package run.drop.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import run.drop.app.apollo.Apollo
import run.drop.app.apollo.TokenHandler
import run.drop.app.utils.setStatusBarColor

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        setStatusBarColor(window, this)

        val username: EditText = findViewById(R.id.username)
        val email: EditText = findViewById(R.id.email)
        val password: EditText = findViewById(R.id.password)
        val confirmedPassword: EditText = findViewById(R.id.confirmed_password)
        val signUpButton: Button = findViewById(R.id.new_account_button)

        signUpButton.setOnClickListener {
            if (checkAllFields(username, email, password, confirmedPassword)) {
                createAccount(username, email, password)
            }
        }
    }

    private fun createAccount(username : EditText, email : EditText, password : EditText) {
        Apollo.client.mutate(
                SignupMutation.builder()
                        .username(username.text.toString())
                        .email(email.text.toString())
                        .password(password.text.toString())
                        .build())?.enqueue(object : ApolloCall.Callback<SignupMutation.Data>() {

            override fun onResponse(dataResponse: Response<SignupMutation.Data>) {
                if (dataResponse.data()?.signup()?.token() != null) {
                    TokenHandler.setToken(dataResponse.data()?.signup()?.token.toString(), this@SignUpActivity)
                    startActivity(Intent(this@SignUpActivity, DropActivity::class.java))
                    finish()
                } else {
                    this@SignUpActivity.runOnUiThread {
                        username.error = "Email address or username already exists"
                        email.error = "Email address or username already exists"
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message)
                e.printStackTrace()
            }
        })
    }

    private fun checkAllFields(username: EditText, email: EditText, password: EditText, confirmPassword: EditText): Boolean {
        if (username.text.isBlank()) {
            username.error = "Can not be empty"
            return false
        }
        if (email.text.isBlank()) {
            email.error = "Can not be empty"
            return false
        }
        if (password.text.isBlank()) {
            password.error = "Can not be empty"
            return false
        }
        if (password.text.toString() != confirmPassword.text.toString()) {
            confirmPassword.error = "Must be the same as the password"
            return false
        }
        return true
    }
}
