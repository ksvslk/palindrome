package com.indest.hah.persistance.dto

import androidx.annotation.Keep

@Keep
data class PromoMenuButtonData(
    @field:JvmField
    val activated: Boolean? = null,
    val link: String? = null,
    val title: String? = null
)