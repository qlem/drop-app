package run.drop.app.authFragments

import android.content.Context
import android.content.Intent
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
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.UserBuilder
import run.drop.app.apollo.Apollo
import run.drop.app.apollo.TokenHandler
import run.drop.app.DropActivity
import run.drop.app.LoginMutation
import run.drop.app.R
import run.drop.app.apollo.IsAuth

class SignInFragment : Fragment() {

    var listener: OnClickSignInFragmentListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sign_in_fragment,container,false)
        val email: EditText = view.findViewById(R.id.email)
        val password: EditText = view.findViewById(R.id.password)
        val signUpButton: Button = view.findViewById(R.id.sign_up_button)
        val signInButton: Button = view.findViewById(R.id.sign_in_button)

        if (arguments?.get("email") != null && arguments?.get("password") != null) {
            email.setText(arguments?.get("email").toString())
            password.setText(arguments?.get("password").toString())
        }

        signUpButton.setOnClickListener {
            listener?.showSignUpFragment()
        }

        signInButton.setOnClickListener {
            if (checkEmptyFields(email, password)) {
                logIn(email, password)
            }
        }

        return view
    }

    private fun logIn(email : EditText, password : EditText) {
        val sharedPreferences = this.context?.getSharedPreferences("Drop", Context.MODE_PRIVATE)

        sharedPreferences?.edit()?.putString("email", email.text.toString())?.apply()

        Apollo.client.mutate(
                LoginMutation.builder()
                        .email(email.text.toString())
                        .password(password.text.toString())
                        .build())?.enqueue(object : ApolloCall.Callback<LoginMutation.Data>() {

            override fun onResponse(dataResponse: Response<LoginMutation.Data>) {
                when {
                    dataResponse.data()?.login()?.token() != null -> {
                        TokenHandler.setToken(dataResponse.data()?.login()?.token().toString(), context!!)
                        IsAuth.state = true
                        startActivity(Intent(context, DropActivity::class.java))
                        activity!!.finish()
                    }
                    dataResponse.errors()[0].message() == "Invalid email" -> activity?.runOnUiThread {
                        email.error = "Wrong email"
                    }
                    else -> activity?.runOnUiThread {
                        password.error = "Wrong password"
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message ?: "apollo error: LoginMutation")
                Sentry.getContext().recordBreadcrumb(
                        BreadcrumbBuilder().setMessage("Failed to Login Apollo").build()
                )

                Sentry.getContext().user = UserBuilder().setEmail(email.text.toString()).build()

                Sentry.getContext().addExtra("exception", e)

                Sentry.capture(e)
                Sentry.getContext().clear()
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