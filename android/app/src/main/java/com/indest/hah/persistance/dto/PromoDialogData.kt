package com.indest.hah.persistance.dto

import androidx.annotation.Keep

@Keep
data class PromoDialogData(
    @field:JvmField
    val activated: Boolean? = null,
    val levelIds: List<Int>? = null,
    val link: String? = null,
    val picturePath: String? = null,
    val text: String? = null,
    val title: String? = null
)
