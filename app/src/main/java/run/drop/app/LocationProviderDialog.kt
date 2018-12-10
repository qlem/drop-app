package run.drop.app

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class LocationProviderDialog : DialogFragment() {

    private lateinit var listener: OpenSettingsListener

    interface OpenSettingsListener {
        fun onOpenSettingsClick(dialog: DialogFragment)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.location_provider_dialog_title)
            builder.setMessage(R.string.location_provider_dialog_message)
                    .setPositiveButton(R.string.location_provider_dialog_button) { _, _ ->
                        listener.onOpenSettingsClick(this)
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OpenSettingsListener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() + " must implement NoticeDialogListener"))
        }
    }
}