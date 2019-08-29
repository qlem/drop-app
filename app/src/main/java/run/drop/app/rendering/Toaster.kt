package run.drop.app.rendering

import android.graphics.Color
import android.view.Gravity
import android.view.View
import androidx.annotation.ColorInt
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

class Toaster(layout: View) {

    fun show(message: CharSequence) {
        snackBar.setText(message)
                .withColor(0xFF1EB980.toInt())
                .show()
    }

    fun error(message: CharSequence) {
        snackBar.setText(message)
                .withColor(Color.RED)
                .show()
    }

    private fun Snackbar.withColor(@ColorInt colorInt: Int): Snackbar {
        this.view.setBackgroundColor(colorInt)
        return this
    }

    private val snackBar: Snackbar = Snackbar.make(layout, "", Snackbar.LENGTH_LONG)

    init {
        val view = snackBar.view
        val params = view.layoutParams as (CoordinatorLayout.LayoutParams)
        params.gravity = Gravity.TOP
        view.layoutParams = params
    }
}