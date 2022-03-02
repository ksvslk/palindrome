package com.indest.hah.fx

data class Sounds(
    val scoreSound:Int,
    val correctSound:Int,
    val hintNotification:Int,
    val levelTransition:Int,
    val switchLettersSoundId:Int,
    val placeLetterSoundId:Int,
    val letterRewardedSoundId:Int,
    val noteSounds:ArrayList<Int>
)
