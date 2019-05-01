package run.drop.app

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.thebluealliance.spectrum.SpectrumPalette

class DropDialog(ctx: Context) : Dialog(ctx),  SpectrumPalette.OnColorSelectedListener {

    var color: Int = 0

    fun validateForm() {
        val submitButton = this.findViewById<Button>(R.id.dropSubmit)
        val textInput = this.findViewById<EditText>(R.id.dropTextInput)
        submitButton.isEnabled = textInput.length() > 0 && color != 0
    }

    override fun onColorSelected(newColor: Int) {
        color = newColor
        validateForm()
        Log.e("COLOR", Integer.toHexString(color).toUpperCase())
    }
}
