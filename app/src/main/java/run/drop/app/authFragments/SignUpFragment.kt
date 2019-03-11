package run.drop.app.authFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import run.drop.app.R
import run.drop.app.SignupMutation
import run.drop.app.apollo.Apollo
import run.drop.app.apollo.TokenHandler

class SignUpFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sign_up_fragment,container,false)
        val username: EditText = view.findViewById(R.id.username)
        val email: EditText = view.findViewById(R.id.email)
        val password: EditText = view.findViewById(R.id.password)
        val confirmedPassword: EditText = view.findViewById(R.id.confirmed_password)
        val signUpButton: Button = view.findViewById(R.id.sign_up_button)

        signUpButton.setOnClickListener {
            if (checkAllFields(username, email, password, confirmedPassword)) {
                createAccount(username, email, password)
            }
        }

        return view
    }

    private fun createAccount(username : EditText, email : EditText, password : EditText) {
        val fragmentTransaction = fragmentManager!!.beginTransaction()
        val signInFragment = SignInFragment()
        val bundle = Bundle()

        Apollo.client.mutate(
                SignupMutation.builder()
                        .username(username.text.toString())
                        .email(email.text.toString())
                        .password(password.text.toString())
                        .build())?.enqueue(object : ApolloCall.Callback<SignupMutation.Data>() {

            override fun onResponse(dataResponse: Response<SignupMutation.Data>) {
                if (dataResponse.data()?.signup()?.token() != null) {
                    TokenHandler.setToken(dataResponse.data()?.signup()?.token().toString(), context!!)
                    bundle.putString("email", email.text.toString())
                    bundle.putString("password", password.text.toString())
                    signInFragment.arguments = bundle
                    fragmentTransaction.replace(R.id.main_layout, signInFragment)
                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()
                } else {
                    activity?.runOnUiThread {
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