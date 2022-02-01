package com.indest.hah.util

object VaryingProbability {
    fun isAnimationPlayed(level:Int):Boolean{
        val randomNumber = (1..100).random()
        val randomPercentage = getRange(level).random()
        return randomNumber <= randomPercentage
    }

    private fun getRange(level: Int):IntRange
    {
        return when (level) {
            in 1..10 -> 40..45
            in 11..20 -> 30..40
            in 21..30 -> 25..35
            in 31..40 -> 20..30
            in 41..50 -> 15..25
            in 51..60 -> 10..20
            in 61..70 -> 10..15
            in 71..80 -> 5..10
            in 81..90 -> 10..15
            in 91..100 -> 10..20
            else -> 5..10
        }
    }
}