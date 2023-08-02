package com.indest.hah.fragments

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.indest.hah.R
import com.indest.hah.other.C
import com.indest.hah.other.C.LANGUAGE_LEVELS_RANGE


class PictureDialogFragment : DialogFragment() {

    private var customDrawable:Drawable? = null
    private var bitmap:Bitmap? = null

    fun setDrawable(localContext: Context, levelNr:Int, countryCode:String = "gb") {
        if (levelNr <= C.PLAYER_LEVELS_COVERED_LOCALLY || levelNr in LANGUAGE_LEVELS_RANGE) {
            val hintResourceId = getDrawableFor(levelNr = levelNr, cc = countryCode, context = localContext)
            customDrawable = ContextCompat.getDrawable(localContext, hintResourceId)
        }
        else {
            try {
                val directory = localContext.filesDir
                val fileName = "level$levelNr.jpg"
                val bm = BitmapFactory.decodeFile(directory.path.toString()+"/"+fileName)
                val size = (200f * localContext.resources.displayMetrics.density).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(bm, size, size, true)
                bitmap = scaledBitmap
            } catch (e:Exception)
            {
                Log.e(TAG, "Was not able to load drawable for level: $levelNr")
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog  {
        val layout = layoutInflater.inflate(
            R.layout.picture_dialog_fragment,null
        )
        val imageView = layout.findViewById<ImageView>(R.id.imageView)
        if (customDrawable != null) imageView.setImageDrawable(customDrawable)
        else if (bitmap != null) imageView.setImageBitmap(bitmap)

        return MaterialAlertDialogBuilder(requireContext(), R.style.HehMaterialAlertDialogTheme)
            .setView(layout)
            .create()
    }


    private fun getDrawableFor(levelNr:Int, cc:String, context: Context):Int
    {
        val middlePart = "_level"
        val name = if (cc == "gb") "level$levelNr" else "$cc$middlePart$levelNr"
        var drawableId = R.drawable.ic_baseline_error_24
        try {
            drawableId = context.applicationContext.resources.getIdentifier(
                name,
                "drawable",
                context.applicationContext.packageName
            )
        } catch (ex:java.lang.Exception)
        {
            Log.e(TAG, "Could not get drawable id for hint picture.", ex)
        }
        return drawableId
    }


    companion object {
        const val TAG = "PictureDialogFragment"
    }

}