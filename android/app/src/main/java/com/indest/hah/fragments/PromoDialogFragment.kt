package com.indest.hah.fragments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.indest.hah.R
import com.indest.hah.persistance.dto.PromoDialogData

class PromoDialogFragment(private val promoData: PromoDialogData) : DialogFragment() {

    private var customDrawable: Drawable? = null
    private var bitmap: Bitmap? = null

    fun setDrawable(localContext: Context) {
        try {
            val directory = localContext.filesDir
            val fileName = "promo.jpg"
            val bm = BitmapFactory.decodeFile(directory.path.toString() + "/" + fileName)
            val size = (1024f * localContext.resources.displayMetrics.density).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bm, size, size, true)
            bitmap = scaledBitmap
        } catch (e: Exception) {
            val hintResourceId = R.drawable.baseline_new_releases_36
            customDrawable = ContextCompat.getDrawable(localContext, hintResourceId)
            Log.e(TAG, "Was not able to load promo.")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = layoutInflater.inflate(
            R.layout.promo_dialog_fragment, null
        )
        val imageView = layout.findViewById<ImageView>(R.id.promo_picture)
        val title = layout.findViewById<AppCompatTextView>(R.id.promo_title)
        val subtitle = layout.findViewById<AppCompatTextView>(R.id.promo_subtitle)
        subtitle.text = promoData.text
        title.text = promoData.title
        if (customDrawable != null) imageView.setImageDrawable(customDrawable)
        else if (bitmap != null) imageView.setImageBitmap(bitmap)

        layout.setOnClickListener {
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(promoData.link)
                setPackage(getString(R.string.vending_package))
                if (context?.let { it1 -> this.resolveActivity(it1.packageManager) } != null) {
                    startActivity(this)
                } else {
                    val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(promoData.link)
                    }
                    startActivity(browserIntent)
                }
            }
        }

        return MaterialAlertDialogBuilder(requireContext(), R.style.HehMaterialAlertPromoTheme)
            .setView(layout)
            .create()
    }

    companion object {
        const val TAG = "PromoDialogFragment"
    }

}