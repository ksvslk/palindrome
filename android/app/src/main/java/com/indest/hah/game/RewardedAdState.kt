package com.indest.hah.game

data class RewardedAdState(
    val adAvailable: Boolean = false,
    val userEarnedReward: Boolean = false,
    val rewardType: RewardType = RewardType.NONE,
    val shouldLoadNewAd:Boolean = false
)