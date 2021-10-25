package com.indest.heh.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.indest.heh.R

class MessageDialogFragment(val title:String, val message:String) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            builder
                .setBackground(ContextCompat.getDrawable(requireActivity(), R.drawable.round_rectangle))
                .setPositiveButton(getString(R.string.i_can_do_better)) { _,_ -> }
                .setMessage(message)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")

    companion object {
        const val TAG = "MessageDialogFragment"
    }
}