package com.indest.hah.fragments

import android.R.attr
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.graphics.drawable.DrawableContainerCompat
import androidx.appcompat.resources.Compatibility.Api21Impl
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.set
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.indest.hah.R
import com.indest.hah.other.C
import java.io.File


class PictureDialogFragment : DialogFragment() {

    private var customDrawable:Drawable? = null
    private var bitmap:Bitmap? = null

    fun setDrawable(localContext: Context, levelNr:Int) {
        if (levelNr <= C.LEVEL_HINTS_COVERED_LOCALLY) {
            val hintResourceId = getDrawableFor(levelNr = levelNr)
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

    private fun getDrawableFor(levelNr:Int):Int
    {
        return when (levelNr) {
            1 -> R.drawable.level1
            2 -> R.drawable.level2
            3 -> R.drawable.level3
            4 -> R.drawable.level4
            5 -> R.drawable.level5
            6 -> R.drawable.level6
            7 -> R.drawable.level7
            8 -> R.drawable.level8
            9 -> R.drawable.level9
            10 -> R.drawable.level10
            /*
          11 -> R.drawable.level11
          12 -> R.drawable.level12
          13 -> R.drawable.level13
          14 -> R.drawable.level14
          15 -> R.drawable.level15
          16 -> R.drawable.level16
          17 -> R.drawable.level17
          18 -> R.drawable.level18
          19 -> R.drawable.level19
          20 -> R.drawable.level20
          21 -> R.drawable.level21
          22 -> R.drawable.level22
          23 -> R.drawable.level23
          24 -> R.drawable.level24
          25 -> R.drawable.level25
          26 -> R.drawable.level26
          27 -> R.drawable.level27
          28 -> R.drawable.level28
          29 -> R.drawable.level29
          30 -> R.drawable.level30
          31 -> R.drawable.level31
          32 -> R.drawable.level32
          33 -> R.drawable.level33
          34 -> R.drawable.level34
          35 -> R.drawable.level35
          36 -> R.drawable.level36
          37 -> R.drawable.level37
          38 -> R.drawable.level38
          39 -> R.drawable.level39
          40 -> R.drawable.level40
          41 -> R.drawable.level41
          42 -> R.drawable.level42
          43 -> R.drawable.level43
          44 -> R.drawable.level44
          45 -> R.drawable.level45
          46 -> R.drawable.level46
          47 -> R.drawable.level47
          48 -> R.drawable.level48
          49 -> R.drawable.level49
          50 -> R.drawable.level50
          51 -> R.drawable.level51
          52 -> R.drawable.level52
          53 -> R.drawable.level53
          54 -> R.drawable.level54
          55 -> R.drawable.level55
          56 -> R.drawable.level56
          57 -> R.drawable.level57
          58 -> R.drawable.level58
          59 -> R.drawable.level59
          60 -> R.drawable.level60
          61 -> R.drawable.level61
          62 -> R.drawable.level62
          63 -> R.drawable.level63
          64 -> R.drawable.level64
          65 -> R.drawable.level65
          66 -> R.drawable.level66
          67 -> R.drawable.level67
          68 -> R.drawable.level68
          69 -> R.drawable.level69
          70 -> R.drawable.level70
          71 -> R.drawable.level71
          72 -> R.drawable.level72
          73 -> R.drawable.level73
          74 -> R.drawable.level74
          75 -> R.drawable.level75
          76 -> R.drawable.level76
          77 -> R.drawable.level77
          78 -> R.drawable.level78
          79 -> R.drawable.level79
          80 -> R.drawable.level80
          81 -> R.drawable.level81
          82 -> R.drawable.level82
          83 -> R.drawable.level83
          84 -> R.drawable.level84
          85 -> R.drawable.level85
          86 -> R.drawable.level86
          87 -> R.drawable.level87
          88 -> R.drawable.level88
          89 -> R.drawable.level89
          90 -> R.drawable.level90
          91 -> R.drawable.level91
          92 -> R.drawable.level92
          93 -> R.drawable.level93
          94 -> R.drawable.level94
          95 -> R.drawable.level95
          96 -> R.drawable.level96
          97 -> R.drawable.level97
          98 -> R.drawable.level98
          99 -> R.drawable.level99
          100 -> R.drawable.level100
          101 -> R.drawable.level101
          102 -> R.drawable.level102
          103 -> R.drawable.level103
          104 -> R.drawable.level104
          105 -> R.drawable.level105
          106 -> R.drawable.level106
          107 -> R.drawable.level107
          108 -> R.drawable.level108
          109 -> R.drawable.level109
          110 -> R.drawable.level110
          111 -> R.drawable.level111
          112 -> R.drawable.level112
          113 -> R.drawable.level113
          114 -> R.drawable.level114
          115 -> R.drawable.level115
          116 -> R.drawable.level116
          117 -> R.drawable.level117
          118 -> R.drawable.level118
          119 -> R.drawable.level119
          120 -> R.drawable.level120
          121 -> R.drawable.level121
          122 -> R.drawable.level122
          123 -> R.drawable.level123
          124 -> R.drawable.level124
          125 -> R.drawable.level125
          126 -> R.drawable.level126
          127 -> R.drawable.level127
          128 -> R.drawable.level128
          129 -> R.drawable.level129
          130 -> R.drawable.level130
          131 -> R.drawable.level131
          132 -> R.drawable.level132
          133 -> R.drawable.level133
          134 -> R.drawable.level134
          135 -> R.drawable.level135
          136 -> R.drawable.level136
          137 -> R.drawable.level137
          138 -> R.drawable.level138
          139 -> R.drawable.level139
          140 -> R.drawable.level140
          141 -> R.drawable.level141
          142 -> R.drawable.level142
          143 -> R.drawable.level143
          144 -> R.drawable.level144
          145 -> R.drawable.level145
          146 -> R.drawable.level146
          147 -> R.drawable.level147
          148 -> R.drawable.level148
          149 -> R.drawable.level149
          150 -> R.drawable.level150
          151 -> R.drawable.level151
          152 -> R.drawable.level152
          153 -> R.drawable.level153
          154 -> R.drawable.level154
          155 -> R.drawable.level155
          156 -> R.drawable.level156
          157 -> R.drawable.level157
          158 -> R.drawable.level158
          159 -> R.drawable.level159
          160 -> R.drawable.level160
          161 -> R.drawable.level161
          162 -> R.drawable.level162
          163 -> R.drawable.level163
          164 -> R.drawable.level164
          165 -> R.drawable.level165
          166 -> R.drawable.level166
            //TODO: to be completed
          */
            else -> R.drawable.ic_baseline_error_24
        }
    }

    companion object {
        const val TAG = "PictureDialogFragment"
    }

}