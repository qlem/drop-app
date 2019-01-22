package run.drop.app.utils

import android.util.Log
import android.content.Context
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import run.drop.app.R

fun setStatusBarColor (window: Window, context: Context) {
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.statusBarColor = ContextCompat.getColor(context, R.color.colorStatusBar)
}

fun log(message: String) {
    Log.d("Drop", message)
}

fun colorIntToHexString(colorInt: Int): String {
    return "#${Integer.toHexString(colorInt).drop(2)}"
}

fun colorHexStringToInt(colorHexString: String): Int {
    return Integer.parseUnsignedInt("ff${colorHexString.drop(1)}", 16)
}