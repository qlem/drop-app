package run.drop.app

import android.app.Activity
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
        setContentView(R.layout.sign_in)
        setStatusBarColor(window, this)

        val signinEmail : EditText = findViewById(R.id.signinEmail)
        val signinPassword : EditText = findViewById(R.id.signinPassword)
        val signupText : TextView = findViewById(R.id.signupText)
        val signinButton : Button = findViewById(R.id.loginButton)

        signupText.setOnClickListener{
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        signinButton.setOnClickListener{
            if (signinEmail.text.isBlank() || signinPassword.text.isBlank()) {
                Toast.makeText(applicationContext, "Please enter an email and a password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            logIn(signinEmail.text.toString(), signinPassword.text.toString())
        }
    }

    private fun logIn(email : String, password : String){
        val intent = Intent(this, DropActivity::class.java)
        Apollo.client.mutate(
                LoginMutation.builder()
                        .email(email)
                        .password(password)
                        .build())?.enqueue(object : ApolloCall.Callback<LoginMutation.Data>() {

            override fun onResponse(dataResponse: Response<LoginMutation.Data>) {
                if (dataResponse.data()?.login()?.token() != null) {
                    TokenStore.setToken(dataResponse.data()?.login()?.token().toString(), this@SignInActivity)
                    startActivity(intent)
                    finish()
                }
                else{
                    this@SignInActivity.runOnUiThread {
                        Toast.makeText(applicationContext, "Incorrect password or email.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                e.stackTrace.forEach { Log.e("APOLLO", it.toString()) }
            }
        })
    }
}
