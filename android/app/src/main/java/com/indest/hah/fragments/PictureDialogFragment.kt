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
        if (levelNr <= C.PLAYER_LEVELS_COVERED_LOCALLY) {
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
            10001 -> R.drawable.level10001
            10002 -> R.drawable.level10002
            10003 -> R.drawable.level10003
            10004 -> R.drawable.level10004
            10005 -> R.drawable.level10005
            10006 -> R.drawable.level10006
            10007 -> R.drawable.level10007
            10008 -> R.drawable.level10008
            10009 -> R.drawable.level10009
            10010 -> R.drawable.level10010
            10011 -> R.drawable.level10011
            10012 -> R.drawable.level10012
            10013 -> R.drawable.level10013
            10014 -> R.drawable.level10014
            10015 -> R.drawable.level10015
            10016 -> R.drawable.level10016
            10017 -> R.drawable.level10017
            10018 -> R.drawable.level10018
            10019 -> R.drawable.level10019
            10020 -> R.drawable.level10020
            10021 -> R.drawable.level10021
            10022 -> R.drawable.level10022
            10023 -> R.drawable.level10023
            10024 -> R.drawable.level10024
            10025 -> R.drawable.level10025
            10026 -> R.drawable.level10026
            10027 -> R.drawable.level10027
            10028 -> R.drawable.level10028
            10029 -> R.drawable.level10029
            10030 -> R.drawable.level10030
            10031 -> R.drawable.level10031
            10032 -> R.drawable.level10032
            10033 -> R.drawable.level10033
            10034 -> R.drawable.level10034
            10035 -> R.drawable.level10035
            10036 -> R.drawable.level10036
            10037 -> R.drawable.level10037
            10038 -> R.drawable.level10038
            10039 -> R.drawable.level10039
            10040 -> R.drawable.level10040
            10041 -> R.drawable.level10041
            10042 -> R.drawable.level10042
            10043 -> R.drawable.level10043
            10044 -> R.drawable.level10044
            10045 -> R.drawable.level10045
            10046 -> R.drawable.level10046
            10047 -> R.drawable.level10047
            10048 -> R.drawable.level10048
            10049 -> R.drawable.level10049
            10050 -> R.drawable.level10050
            10051 -> R.drawable.level10051
            10052 -> R.drawable.level10052
            10053 -> R.drawable.level10053
            10054 -> R.drawable.level10054
            10055 -> R.drawable.level10055
            10056 -> R.drawable.level10056
            10057 -> R.drawable.level10057
            10058 -> R.drawable.level10058
            10059 -> R.drawable.level10059
            10060 -> R.drawable.level10060
            10061 -> R.drawable.level10061
            10062 -> R.drawable.level10062
            10063 -> R.drawable.level10063
            10064 -> R.drawable.level10064
            10065 -> R.drawable.level10065
            10066 -> R.drawable.level10066
            10067 -> R.drawable.level10067
            10068 -> R.drawable.level10068
            10069 -> R.drawable.level10069
            10070 -> R.drawable.level10070
            10071 -> R.drawable.level10071
            10072 -> R.drawable.level10072
            10073 -> R.drawable.level10073
            10074 -> R.drawable.level10074
            10075 -> R.drawable.level10075
            10076 -> R.drawable.level10076
            10077 -> R.drawable.level10077
            10078 -> R.drawable.level10078
            10079 -> R.drawable.level10079
            10080 -> R.drawable.level10080
            10081 -> R.drawable.level10081
            10082 -> R.drawable.level10082
            10083 -> R.drawable.level10083
            10084 -> R.drawable.level10084
            10085 -> R.drawable.level10085
            10086 -> R.drawable.level10086
            10087 -> R.drawable.level10087
            10088 -> R.drawable.level10088
            10089 -> R.drawable.level10089
            10090 -> R.drawable.level10090
            10091 -> R.drawable.level10091
            10092 -> R.drawable.level10092
            10093 -> R.drawable.level10093
            10094 -> R.drawable.level10094
            10095 -> R.drawable.level10095
            10096 -> R.drawable.level10096
            10097 -> R.drawable.level10097
            10098 -> R.drawable.level10098
            10099 -> R.drawable.level10099
            10100 -> R.drawable.level10100
            10101 -> R.drawable.level10101
            10102 -> R.drawable.level10102
            10103 -> R.drawable.level10103
            10104 -> R.drawable.level10104
            10105 -> R.drawable.level10105
            10106 -> R.drawable.level10106
            10107 -> R.drawable.level10107
            10108 -> R.drawable.level10108
            10109 -> R.drawable.level10109
            10110 -> R.drawable.level10110
            10111 -> R.drawable.level10111
            10112 -> R.drawable.level10112
            10113 -> R.drawable.level10113
            10114 -> R.drawable.level10114
            10115 -> R.drawable.level10115
            10116 -> R.drawable.level10116
            10117 -> R.drawable.level10117
            10118 -> R.drawable.level10118
            10119 -> R.drawable.level10119
            10120 -> R.drawable.level10120
            10121 -> R.drawable.level10121
            10122 -> R.drawable.level10122
            10123 -> R.drawable.level10123
            10124 -> R.drawable.level10124
            10125 -> R.drawable.level10125
            10126 -> R.drawable.level10126
            10127 -> R.drawable.level10127
            10128 -> R.drawable.level10128
            10129 -> R.drawable.level10129
            10130 -> R.drawable.level10130
            10131 -> R.drawable.level10131
            10132 -> R.drawable.level10132
            10133 -> R.drawable.level10133
            10134 -> R.drawable.level10134
            10135 -> R.drawable.level10135
            10136 -> R.drawable.level10136
            10137 -> R.drawable.level10137
            10138 -> R.drawable.level10138
            10139 -> R.drawable.level10139
            10140 -> R.drawable.level10140
            10141 -> R.drawable.level10141
            10142 -> R.drawable.level10142
            10143 -> R.drawable.level10143
            10144 -> R.drawable.level10144
            10145 -> R.drawable.level10145
            10146 -> R.drawable.level10146
            10147 -> R.drawable.level10147
            10148 -> R.drawable.level10148
            10149 -> R.drawable.level10149
            10150 -> R.drawable.level10150
            10151 -> R.drawable.level10151
            10152 -> R.drawable.level10152
            10153 -> R.drawable.level10153
            10154 -> R.drawable.level10154
            10155 -> R.drawable.level10155
            10156 -> R.drawable.level10156
            10157 -> R.drawable.level10157
            10158 -> R.drawable.level10158
            10159 -> R.drawable.level10159
            10160 -> R.drawable.level10160
            10161 -> R.drawable.level10161
            10162 -> R.drawable.level10162
            10163 -> R.drawable.level10163
            10164 -> R.drawable.level10164
            10165 -> R.drawable.level10165
            10166 -> R.drawable.level10166
            10167 -> R.drawable.level10167
            10168 -> R.drawable.level10168
            10169 -> R.drawable.level10169
            10170 -> R.drawable.level10170
            10171 -> R.drawable.level10171
            10172 -> R.drawable.level10172
            10173 -> R.drawable.level10173
            10174 -> R.drawable.level10174
            10175 -> R.drawable.level10175
            10176 -> R.drawable.level10176
            10177 -> R.drawable.level10177
            10178 -> R.drawable.level10178
            10179 -> R.drawable.level10179
            10180 -> R.drawable.level10180
            10181 -> R.drawable.level10181
            10182 -> R.drawable.level10182
            10183 -> R.drawable.level10183
            10184 -> R.drawable.level10184
            10185 -> R.drawable.level10185
            10186 -> R.drawable.level10186
            10187 -> R.drawable.level10187
            10188 -> R.drawable.level10188
            10189 -> R.drawable.level10189
            10190 -> R.drawable.level10190
            10191 -> R.drawable.level10191
            10192 -> R.drawable.level10192
            10193 -> R.drawable.level10193
            10194 -> R.drawable.level10194
            10195 -> R.drawable.level10195
            10196 -> R.drawable.level10196
            10197 -> R.drawable.level10197
            10198 -> R.drawable.level10198
            10199 -> R.drawable.level10199
            10200 -> R.drawable.level10200
            10201 -> R.drawable.level10201
            10202 -> R.drawable.level10202
            10203 -> R.drawable.level10203
            10204 -> R.drawable.level10204
            10205 -> R.drawable.level10205
            10206 -> R.drawable.level10206
            10207 -> R.drawable.level10207
            10208 -> R.drawable.level10208
            10209 -> R.drawable.level10209
            10210 -> R.drawable.level10210
            10211 -> R.drawable.level10211
            10212 -> R.drawable.level10212
            10213 -> R.drawable.level10213
            10214 -> R.drawable.level10214
            10215 -> R.drawable.level10215
            10216 -> R.drawable.level10216
            10217 -> R.drawable.level10217
            10218 -> R.drawable.level10218
            10219 -> R.drawable.level10219
            10220 -> R.drawable.level10220
            10221 -> R.drawable.level10221
            10222 -> R.drawable.level10222
            10223 -> R.drawable.level10223
            10224 -> R.drawable.level10224
            10225 -> R.drawable.level10225
            10226 -> R.drawable.level10226
            10227 -> R.drawable.level10227
            10228 -> R.drawable.level10228
            10229 -> R.drawable.level10229
            10230 -> R.drawable.level10230
            10231 -> R.drawable.level10231
            10232 -> R.drawable.level10232
            10233 -> R.drawable.level10233
            10234 -> R.drawable.level10234
            10235 -> R.drawable.level10235
            10236 -> R.drawable.level10236
            10237 -> R.drawable.level10237
            10238 -> R.drawable.level10238
            10239 -> R.drawable.level10239
            10240 -> R.drawable.level10240
            10241 -> R.drawable.level10241
            10242 -> R.drawable.level10242
            10243 -> R.drawable.level10243
            10244 -> R.drawable.level10244
            10245 -> R.drawable.level10245
            10246 -> R.drawable.level10246
            10247 -> R.drawable.level10247
            10248 -> R.drawable.level10248
            10249 -> R.drawable.level10249
            10250 -> R.drawable.level10250
            10251 -> R.drawable.level10251
            10252 -> R.drawable.level10252
            10253 -> R.drawable.level10253
            10254 -> R.drawable.level10254
            10255 -> R.drawable.level10255
            10256 -> R.drawable.level10256
            10257 -> R.drawable.level10257
            10258 -> R.drawable.level10258
            10259 -> R.drawable.level10259
            10260 -> R.drawable.level10260
            10261 -> R.drawable.level10261
            10262 -> R.drawable.level10262
            10263 -> R.drawable.level10263
            10264 -> R.drawable.level10264
            10265 -> R.drawable.level10265
            10266 -> R.drawable.level10266
            10267 -> R.drawable.level10267
            10268 -> R.drawable.level10268
            10269 -> R.drawable.level10269
            10270 -> R.drawable.level10270
            10271 -> R.drawable.level10271
            10272 -> R.drawable.level10272
            10273 -> R.drawable.level10273
            10274 -> R.drawable.level10274
            10275 -> R.drawable.level10275
            10276 -> R.drawable.level10276
            10277 -> R.drawable.level10277
            10278 -> R.drawable.level10278
            10279 -> R.drawable.level10279
            10280 -> R.drawable.level10280
            10281 -> R.drawable.level10281
            10282 -> R.drawable.level10282
            10283 -> R.drawable.level10283
            10284 -> R.drawable.level10284
            10285 -> R.drawable.level10285
            10286 -> R.drawable.level10286
            10287 -> R.drawable.level10287
            10288 -> R.drawable.level10288
            10289 -> R.drawable.level10289
            10290 -> R.drawable.level10290
            10291 -> R.drawable.level10291
            10292 -> R.drawable.level10292
            10293 -> R.drawable.level10293
            10294 -> R.drawable.level10294
            10295 -> R.drawable.level10295
            10296 -> R.drawable.level10296
            10297 -> R.drawable.level10297
            10298 -> R.drawable.level10298
            10299 -> R.drawable.level10299
            10300 -> R.drawable.level10300
            10301 -> R.drawable.level10301
            10302 -> R.drawable.level10302
            10303 -> R.drawable.level10303
            10304 -> R.drawable.level10304
            10305 -> R.drawable.level10305
            10306 -> R.drawable.level10306
            10307 -> R.drawable.level10307
            10308 -> R.drawable.level10308
            10309 -> R.drawable.level10309
            10310 -> R.drawable.level10310
            10311 -> R.drawable.level10311
            10312 -> R.drawable.level10312
            10313 -> R.drawable.level10313
            10314 -> R.drawable.level10314
            10315 -> R.drawable.level10315
            10316 -> R.drawable.level10316
            10317 -> R.drawable.level10317
            10318 -> R.drawable.level10318
            10319 -> R.drawable.level10319
            10320 -> R.drawable.level10320
            10321 -> R.drawable.level10321
            10322 -> R.drawable.level10322
            10323 -> R.drawable.level10323
            10324 -> R.drawable.level10324
            10325 -> R.drawable.level10325
            10326 -> R.drawable.level10326
            10327 -> R.drawable.level10327
            10328 -> R.drawable.level10328
            10329 -> R.drawable.level10329
            10330 -> R.drawable.level10330
            10331 -> R.drawable.level10331
            10332 -> R.drawable.level10332
            10333 -> R.drawable.level10333
            10334 -> R.drawable.level10334
            10335 -> R.drawable.level10335
            10336 -> R.drawable.level10336
            10337 -> R.drawable.level10337
            10338 -> R.drawable.level10338
            10339 -> R.drawable.level10339
            10340 -> R.drawable.level10340
            10341 -> R.drawable.level10341
            10342 -> R.drawable.level10342
            10343 -> R.drawable.level10343
            10344 -> R.drawable.level10344
            10345 -> R.drawable.level10345
            10346 -> R.drawable.level10346
            10347 -> R.drawable.level10347
            10348 -> R.drawable.level10348
            10349 -> R.drawable.level10349
            10350 -> R.drawable.level10350
            10351 -> R.drawable.level10351
            10352 -> R.drawable.level10352
            10353 -> R.drawable.level10353
            10354 -> R.drawable.level10354
            10355 -> R.drawable.level10355
            10356 -> R.drawable.level10356
            10357 -> R.drawable.level10357
            10358 -> R.drawable.level10358
            10359 -> R.drawable.level10359
            10360 -> R.drawable.level10360
            10361 -> R.drawable.level10361
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