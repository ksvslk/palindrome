package com.indest.heh.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.indest.heh.R

class ExitGameDialogFragment : DialogFragment() {

    // Use this instance of the interface to deliver action events
    private lateinit var listener: ExitGameDialogListener

    /* The activity that creates an instance of this dialog fragment must
    * implement this interface in order to receive event callbacks.
    * Each method passes the DialogFragment in case the host needs to query it. */
    interface ExitGameDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    // Override the Fragment.onAttach() method to instantiate the ExitGameDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the ExitGameDialogListener so we can send events to the host
            listener = context as ExitGameDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(("$context must implement NoticeDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            builder
                .setBackground(ContextCompat.getDrawable(requireActivity(), R.drawable.round_rectangle))
                .setTitle(R.string.exit_game_title)
                .setMessage(R.string.exit_game_message)
                .setPositiveButton(R.string.yes)
                { _, _ ->listener.onDialogPositiveClick(this) }
                .setNegativeButton(R.string.cancel)
                { _, _ -> listener.onDialogNegativeClick(this)}
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}