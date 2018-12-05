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

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up)

        val usernameSignup : EditText = findViewById(R.id.signupUsername)
        val emailSignup : EditText = findViewById(R.id.signupEmail)
        val passwordSignup : EditText = findViewById(R.id.signupPassword)
        val passwordConfirmSignup : EditText = findViewById(R.id.signupConfirmPassword)
        val buttonSignup : Button = findViewById(R.id.signupButton)

        buttonSignup.setOnClickListener{
            if (checkAllFields(usernameSignup.text.toString(), emailSignup.text.toString(), passwordSignup.text.toString(), passwordConfirmSignup.text.toString())){
                createAccount(usernameSignup.text.toString(), emailSignup.text.toString(), passwordSignup.text.toString())
            }
        }
    }

    private fun createAccount(username : String, email : String, password : String){
        val intent = Intent(this, LauncherActivity::class.java)
        Apollo.client.mutate(
                SignupMutation.builder()
                        .username(username)
                        .email(email)
                        .password(password)
                        .build())?.enqueue(object : ApolloCall.Callback<SignupMutation.Data>() {

            override fun onResponse(dataResponse: Response<SignupMutation.Data>) {
                if (dataResponse.data()?.signup()?.token() != null) {
                    startActivity(intent)
                    TokenStore.setToken(dataResponse.data()?.signup()?.token.toString(), this@SignUpActivity)
                    Log.d("APOLLO", "Création OK : " + dataResponse.data()?.signup()?.token.toString())
                    finish()
                }
                else {
                    this@SignUpActivity.runOnUiThread {
                        Toast.makeText(applicationContext, "Email address or username already exist.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message.toString())
                e.stackTrace.forEach { Log.e("APOLLO", it.toString()) }
            }
        })
    }

    private fun checkAllFields(username : String, email : String, password : String, confirmPassword: String) : Boolean{
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
            Toast.makeText(applicationContext, "Please, complete all fields.", Toast.LENGTH_SHORT).show()
            return false
        }
        else{
            if (password.equals(confirmPassword, true)){
                return true
            }
            else{
                Toast.makeText(applicationContext, "Please, verify your passwords.", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }
}
