package run.drop.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import run.drop.app.utils.setStatusBarColor

class ProfileActivity : AppCompatActivity() {

    private lateinit var usernameView: TextView
    private lateinit var emailView: TextView
    private lateinit var dropView: TextView
    private lateinit var likeView: TextView
    private lateinit var reportView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_layout)
        setStatusBarColor(window, this)
        setTheme(R.style.AppTheme)

        usernameView = findViewById(R.id.username)
        emailView = findViewById(R.id.email)
        dropView = findViewById(R.id.nb_drop)
        likeView = findViewById(R.id.nb_like)
        reportView = findViewById(R.id.nb_report)
    }
}