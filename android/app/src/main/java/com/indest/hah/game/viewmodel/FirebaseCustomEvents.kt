package com.indest.hah.game.viewmodel

object FirebaseCustom {
    object Events {
        const val REWARD_GIVEN = "reward_given"
        const val PREMIUM_HINT_USAGE = "premium_hint_usage"
        const val SHARE_GAME_CLICKED = "share_game_clicked"
        const val DIALOG_PROMO_SHOWN = "dialog_promo_shown"
        const val MENU_PROMO_CLICKED = "menu_promo_clicked"
        const val COMPETITION_20_ANNOUNCED = "competition_20_announced"
        const val COMPETITION_20_PRIZE = "competition_20_prize"
        const val COMPETITION_20_FINISHED = "competition_20_finished"
    }
    object Params {
        const val REWARD_AD_TYPE = "reward_ad_type"
        const val HINT_TYPE = "hint_type"
        const val CURRENT_SOLUTION = "current_solution"
        const val LEVEL_SOLUTION = "level_solution"
        const val ORIGIN_SCREEN = "origin_screen"
        const val SCORE_TO_MAX_RATIO = "score_to_max_ratio"
    }
}