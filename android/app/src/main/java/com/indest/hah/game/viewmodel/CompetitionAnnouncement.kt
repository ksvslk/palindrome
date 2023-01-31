package com.indest.hah.game.viewmodel

data class CompetitionAnnouncement(
    val show:Boolean = false,
    val prizesLeft:Long = 0,
    val accessedFromMenu:Boolean = false,
    val competitionEnded:Boolean=false,
    val triedToParticipate:Boolean = false,
    val triedToOfferPrize:Boolean = false
)