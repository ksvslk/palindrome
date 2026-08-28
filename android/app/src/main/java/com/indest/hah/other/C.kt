package com.indest.hah.other

object C {
    const val SAVE_GAME = "SAVE_GAME"
    const val TOP_LAYER = 3f
    const val MIDDLE_LAYER = 2f
    const val NEUTRAL_LAYER = 1f
    const val BOTTOM_LAYER = -1f
    const val REWARDED_ANIM_MUL = 3
    const val REWARDED_ANIM_MUL_DEFAULT = 1

    const val FIREBASE_PICS_STORAGE_PATH = "gs://hah-35889753.appspot.com/level_hint/"
    const val PLAYER_LEVELS_COVERED_LOCALLY = 1155

    // Player levels synced from Firestore live above the bundled ceiling and
    // below the language band. They are always unlocked and intentionally
    // excluded from Play Games saves, so cloud restores and the live catalog
    // can never fight over them.
    const val CLOUD_LEVELS_FROM = PLAYER_LEVELS_COVERED_LOCALLY + 1
    const val CLOUD_LEVELS_TO = 9999
    const val PLAYER_LEVELS_TAG = "isFeatured_v3"
    val LANGUAGE_LEVELS_RANGE = 10001..10540

    // do not change order here - affects how language packs are added
    val languages = mapOf(
        "cn" to "Chinese",
        "de" to "German",
        "es" to "Spanish",
        "et" to "Estonian",
        "fi" to "Finnish",
        "fr" to "French",
        "id" to "Indonesian",
        "it" to "Italian",
        "ja" to "Japanese",
        "ko" to "Korean",
        "nl" to "Dutch",
        "pl" to "Polish",
        "pt" to "Portuguese",
        "ro" to "Romanian",
        "sv" to "Swedish",
        "tr" to "Turkish",
        "uk" to "Ukrainian"
    )

}

