package run.drop.app.apollo

import android.content.Context
import java.io.File
import java.io.FileNotFoundException

object TokenHandler {
    var token: String? = null

    fun init(context: Context) {
        getToken(context)
    }

    fun setToken(token: String, context: Context) {
        context.openFileOutput("token", Context.MODE_PRIVATE).use {
            it.write(token.toByteArray())
        }
        TokenHandler.token = token
    }

    private fun getToken(context: Context): String? {
        return try {
            val fs = context.openFileInput("token")
            val token = fs.bufferedReader().use { it.readText() }
            TokenHandler.token = token
            token
        } catch (e: FileNotFoundException) {
            null
        }
    }

    fun clearToken(context: Context) {
        val file = File(context.filesDir, "token")
        file.delete()
        token = null
    }
}