package com.indest.hah.game.viewmodel

object FirebaseCustom {
    object Events {
        const val REWARD_GIVEN = "reward_given"
        const val PREMIUM_HINT_USAGE = "premium_hint_usage"
        const val SHARE_GAME_CLICKED = "share_game_clicked"
        const val COMPETITION_25_ANNOUNCED = "competition_25_announced"
        const val COMPETITION_25_PRIZE = "competition_25_prize"
        const val COMPETITION_25_FINISHED = "competition_25_finished"
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