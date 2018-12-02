package run.drop.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        TokenStore.initStore(this)
        setStatusBarColor(window, this)
    }

    override fun onStart() {
        super.onStart()
        val token = TokenStore.getToken(this)
        if (token == null) {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }
}
