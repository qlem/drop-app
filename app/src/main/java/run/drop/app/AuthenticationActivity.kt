package run.drop.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import run.drop.app.utils.setStatusBarColor

class AuthenticationActivity : AppCompatActivity(), SignInFragment.OnClickFragmentListener {

    private lateinit var signInFragment: SignInFragment
    private lateinit var signUpFragment: SignUpFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        setStatusBarColor(window, this)

        initFragments()
        showSignInFragment()
    }

    private fun initFragments() {
        signInFragment = SignInFragment().apply { listener = this@AuthenticationActivity }
        signUpFragment = SignUpFragment()
    }

    private fun showSignInFragment() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.main_layout,signInFragment, SignInFragment::class.java.name)
                .commit()
    }

    private fun showSignUpFragment(){
        supportFragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.main_layout,signUpFragment, SignUpFragment::class.java.name)
                .commit()
    }

    override fun signUp() {
        showSignUpFragment()
    }
}
