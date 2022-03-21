package com.indest.hah.game.viewmodel

/**
 * Enum representing various states of a container
 * that holds the letters.
 */
enum class ContainerState {
    /**
     * Container is in this state when starting the game.
     * To get to this state again the game has to be restarted.
     * Loading progress is shown in UI.
     */
    LOADING,

    /**
     * Normal state when the letters can be moved around.
     * No restrictions apply.
     */
    PLAY,

    /**
     * Immediate state that is set when the correct solution
     * has been achieved. Container letters are not not clickable.
     */
    LEVEL_COMPLETE,

    /**
     * State set after the game is loaded and the level is completed.
     * That means the level progress in database is INTERMEDIATE.
     * Container letters are not not clickable.
     */
    INTERMEDIATE,

    /**
     * This state is set when we are going to watch a rewarded
     * video to receive a letter as a reward. We must wait until
     * the animation is ended to proceed further from this state.
     * We don't need to refresh the container contents when returning
     * from this state. Container is not clickable.
     */
    BUSY,

    /**
     * State for doing configuration changes - we need to refresh
     * the container contents when returning from this state
     * back to PLAY.
     */
    PAUSED,

    /**
     * When the menu action is starting another activity like
     * leaderboard, achievements or billing flow we don't need
     * to refresh the container contents.
     */
    MENU_ACTION,
}