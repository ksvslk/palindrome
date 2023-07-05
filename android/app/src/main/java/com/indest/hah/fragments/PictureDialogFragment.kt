package com.indest.hah.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LightingColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.indest.hah.R


class PictureDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog  {
        val layout = layoutInflater.inflate(
            R.layout.picture_dialog_fragment,null
        )
        return MaterialAlertDialogBuilder(requireContext(), R.style.HehMaterialAlertDialogTheme)
            .setView(layout)
            .create()
    }

    companion object {
        const val TAG = "PictureDialogFragment"
    }

}