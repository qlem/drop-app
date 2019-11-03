package run.drop.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import run.drop.app.apollo.TokenHandler
import run.drop.app.utils.setStatusBarColor
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.UserBuilder
import run.drop.app.apollo.Apollo
import run.drop.app.apollo.IsAuth


class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setStatusBarColor(window, this)

        TokenHandler.init(this)
        checkAuthentication()

        val ctx = this.applicationContext
        val sentryDsn = "https://e7375363417e4426a77e53d872ecd282@sentry.io/1482813"
        Sentry.init(sentryDsn, AndroidSentryClientFactory(ctx))
    }

    private fun checkAuthentication() {
        Apollo.client.query(
                AmIAuthQuery.builder().build()).enqueue(object : ApolloCall.Callback<AmIAuthQuery.Data>() {

            override fun onResponse(response: Response<AmIAuthQuery.Data>) {
                IsAuth.state = response.data()!!.amIAuth().isAuth()
                startActivity(Intent(this@LauncherActivity, DropActivity::class.java))
                finish()
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message ?: "apollo error: AmIAuthQuery")

                Sentry.getContext().recordBreadcrumb(
                        BreadcrumbBuilder().setMessage("Failed to Check identification APOLLO").build()
                )

                val email = getSharedPreferences("Drop", Context.MODE_PRIVATE).getString("email", "")
                Sentry.getContext().user = UserBuilder().setEmail(email).build()

                Sentry.capture(e)
                Sentry.getContext().clear()

                e.printStackTrace()
            }
        })
    }
}
