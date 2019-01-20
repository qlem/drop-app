package run.drop.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import run.drop.app.apollo.Apollo
import run.drop.app.token.TokenHandler
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
        val button: Button = findViewById(R.id.new_account_button)

        button.setOnClickListener{
            if (checkAllFields(username.text.toString(), email.text.toString(), password.text.toString(),
                            confirmedPassword.text.toString())) {
                createAccount(username.text.toString(), email.text.toString(), password.text.toString())
            }
        }
    }

    private fun createAccount(username : String, email : String, password : String){
        Apollo.client.mutate(
                SignupMutation.builder()
                        .username(username)
                        .email(email)
                        .password(password)
                        .build())?.enqueue(object : ApolloCall.Callback<SignupMutation.Data>() {

            override fun onResponse(dataResponse: Response<SignupMutation.Data>) {
                if (dataResponse.data()?.signup()?.token() != null) {
                    startActivity(Intent(this@SignUpActivity, DropActivity::class.java))
                    TokenHandler.setToken(dataResponse.data()?.signup()?.token.toString(), this@SignUpActivity)
                    finish()
                } else {
                    this@SignUpActivity.runOnUiThread {
                        Toast.makeText(applicationContext, "Email address or username already exist.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message.toString())
                e.stackTrace.forEach { Log.e("APOLLO", it.toString()) }
            }
        })
    }

    private fun checkAllFields(username : String, email : String, password : String, confirmPassword: String): Boolean {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(applicationContext, "Please, complete all fields.", Toast.LENGTH_SHORT).show()
            return false
        } else {
            if (password == confirmPassword) {
                return true
            } else {
                Toast.makeText(applicationContext, "Please, verify your passwords.",
                        Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }
}
