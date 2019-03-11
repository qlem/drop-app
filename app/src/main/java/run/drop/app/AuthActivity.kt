package run.drop.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import run.drop.app.authFragments.OnClickSignInFragmentListener
import run.drop.app.authFragments.SignInFragment
import run.drop.app.authFragments.SignUpFragment
import run.drop.app.utils.setStatusBarColor

class AuthActivity : AppCompatActivity(), OnClickSignInFragmentListener {

    private lateinit var signInFragment: SignInFragment
    private lateinit var signUpFragment: SignUpFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        setStatusBarColor(window, this)

        signInFragment = SignInFragment().apply { listener = this@AuthActivity }
        signUpFragment = SignUpFragment()

        supportFragmentManager.beginTransaction()
                .replace(R.id.main_layout,signInFragment, SignInFragment::class.java.name)
                .commit()
    }

    override fun showSignUpFragment() {
        supportFragmentManager.beginTransaction()
                .addToBackStack(null)
                .replace(R.id.main_layout,signUpFragment, SignUpFragment::class.java.name)
                .commit()
    }
}
