package com.indest.hah.persistance.dto

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Data class for hint translations
@Serializable
data class HintTranslations(
    @Required
    @SerialName("hint") val hint: String,
    @Required
    @SerialName("hint_cn") val hintCn: String,
    @Required
    @SerialName("hint_de") val hintDe: String,
    @Required
    @SerialName("hint_es") val hintEs: String,
    @Required
    @SerialName("hint_et") val hintEt: String,
    @Required
    @SerialName("hint_fi") val hintFi: String,
    @Required
    @SerialName("hint_fr") val hintFr: String,
    @Required
    @SerialName("hint_id") val hintId: String,
    @Required
    @SerialName("hint_it") val hintIt: String,
    @Required
    @SerialName("hint_ja") val hintJa: String,
    @Required
    @SerialName("hint_ko") val hintKo: String,
    @Required
    @SerialName("hint_nl") val hintNl: String,
    @Required
    @SerialName("hint_pl") val hintPl: String,
    @Required
    @SerialName("hint_pt") val hintPt: String,
    @Required
    @SerialName("hint_ro") val hintRo: String,
    @Required
    @SerialName("hint_sv") val hintSv: String,
    @Required
    @SerialName("hint_tr") val hintTr: String,
    @Required
    @SerialName("hint_uk") val hintUk: String
)