//
//  C.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation
import SpriteKit

struct C {
    
    // Layers
    static let TOP_LAYER = CGFloat(3)
    static let MIDDLE_LAYER = CGFloat(2)
    static let NEUTRAL_LAYER = CGFloat(1)
    static let BOTTOM_LAYER = CGFloat(-1)
    
    // Animation modifiers
    static let REWARDED_ANIMATION_MODIFIER = CGFloat(3)
    static let ANIMATION_MODIFIER = CGFloat(1)
    static let ON_CLICK_COLOR_CHANGE_ANIMATION = CGFloat(0.2)
    static let ON_RELEASE_COLOR_CHANGE_ANIMATION = CGFloat(0.2)
    static let HOME_ANIMATION_MULTIPLIER = CGFloat(0.05)
    
    
    // Score
    static let SCORE_LETTER_MULTIPLIER = 100 // Each letter in level will give 100 points
    static let SCORE_TAG = "SCORE_TAG"
    
    
    // User defaults
    static let VIBRATION_ON = "VIBRATION_ON"
    static let SOUND_ON = "SOUND_ON"
    static let MAX_UNLOCKED_LEVEL = "MAX_UNLOCKED_LEVEL"
    static let LATEST_PLAYED_LEVEL = "LATEST_PLAYED_LEVEL"
    static let PLAYER_SCORES = "PLAYER_SCORES"
    static let TUTORIAL_SHOWN = "TUTORIAL_SHOWN"
    
    static let CLOUD_PROGRESS_KEY = "CLOUD_PROGRESS"
    static let CLOUD_BEST_SCORES_KEY = "CLOUD_BEST_SCORES"
    static let CLOUD_CURRENT_LEVEL_KEY = "CLOUD_CURRENT_LEVEL"
    
    // IDs
    static let GAME_LEADERBOARD = "game_leaderboard"
    static let ANTIMATTER_LEADERBOARD = "antimatter_league"
    static let PLAYER_LEADERBOARD = "player_leaderboard"
    
    
    // Product IDs
    static let PURCHASE_LETTERS_ID = "purchase_letters" // 2.99
    static let PURCHASE_HINTS_ID = "purchase_hints" // 1.99
    static let PURCHASE_NO_ADS_ID = "purchase_no_ads" // 1.99
    static let PURCHASE_ALL_ID = "purchase_all" // 4.99
    
    static let PURCHASE_ALL_LEVELS_ID = "purchase_all_level_packs"
    static let PURCHASE_LEVELS_PACK_1_ID = "purchase_level_pack_1"
    static let PURCHASE_LEVELS_PACK_2_ID = "purchase_level_pack_2"
    static let PURCHASE_LEVELS_PACK_3_ID = "purchase_level_pack_3"
    static let PURCHASE_LEVELS_PACK_4_ID = "purchase_level_pack_4"
    static let PURCHASE_LEVELS_PACK_5_ID = "purchase_level_pack_5"
    static let PURCHASE_LEVELS_PACK_6_ID = "purchase_level_pack_6"
    
    static let PURCHASED_PRODUCTS_KEY = "user_defaults_purchases"
    
    
    // Ads
    static let DEBUG_REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/1712485313"
    static let RELEASE_REWARDED_AD_UNIT = "ca-app-pub-5386053272091445/5924268495"

    static let DEBUG_INTERSTITIAL_AD_UNIT = "ca-app-pub-3940256099942544/4411468910"
    static let RELEASE_INTERSTITIAL_AD_UNIT = "ca-app-pub-5386053272091445/2725093773"
    
    // Achievements
    static let achievement_greenhorn = "greenhorn"
    static let achievement_proficient = "proficient"
    static let achievement_crackerjack = "crackerjack"
    static let achievement_chief = "chief"
    static let achievement_authority = "authority"
    static let achievement_zero_mistakes = "zero_mistakes"
    static let achievement_complete_level = "complete_level"
    static let achievement_wrong_palindrome = "wrong_palindrome"
    static let achievement_flawless_5 = "flawless_5"
    static let achievement_flawless_15 = "flawless_15"
    
    // URLs
    static let APP_FEEDBACK_URL = "https://apps.apple.com/app/id1673006365?action=write-review"
    static let APP_URL = "https://apps.apple.com/app/hah-palindrome-puzzles/id1673006365"
    
    
    // Intervals
    static let HINT_INTERVAL = "hint_interval"
    static let ADS_INTERVAL = "ads_interval"
    static let SCORES_INTERVAL = "scores_interval"
    static let FLAWLESS_5_INTERVAL = "flawless_5_interval"
    static let FLAWLESS_15_INTERVAL = "flawless_15_interval"
    
    // Settings menu
    static let colorButton = ("color"~, UIImage(systemName: "music.note")!)
    static let soundButton = ("sound"~, UIImage(systemName: "music.note")!)
    static let vibrationButton = ("vibration"~, UIImage(systemName: "waveform")!)
    static let tutorialButton = ("tutorial"~, UIImage(systemName: "questionmark.app")!)
    static let feedbackButton = ("feedback"~, UIImage(systemName: "square.and.pencil")!)
    static let shareButton = ("share"~, UIImage(systemName: "square.and.arrow.up")!)
    static let restoreButton = ("restore"~, UIImage(systemName: "repeat")!)

    // Game Center menu
    static let playerLeaderButton = ("player_leader"~, UIImage(systemName: "increase.indent")!)
    static let gameLeaderButton = ("game_leader"~, UIImage(systemName: "decrease.indent")!)
    static let antimatterLeaderButton = ("antimatter_leader"~, UIImage(systemName: "decrease.indent")!)
    static let achievementsButton = ("achievements"~, UIImage(systemName: "rosette")!)

    
    // Level select menu
    static let submitLevelButton = ("submit_level"~, UIImage(systemName: "paperplane")!)
    static let playerLevelsButton = ("player_levels"~, UIImage(systemName: "person.2")!)
    static let gameLevelsButton = ("game_levels"~, UIImage(systemName: "square.stack.3d.up")!)
    static let purchaseAllLevelPacksButton = ("all_level_packs"~, UIImage(systemName: "dollarsign.circle.fill")!)
    static let purchaseLevelPack1Button = ("level_pack_1"~, UIImage(systemName: "dollarsign.circle.fill")!)
    static let purchaseLevelPack2Button = ("level_pack_2"~, UIImage(systemName: "dollarsign.circle.fill")!)
    static let purchaseLevelPack3Button = ("level_pack_3"~, UIImage(systemName: "dollarsign.circle.fill")!)
    static let purchaseLevelPack4Button = ("level_pack_4"~, UIImage(systemName: "dollarsign.circle.fill")!)
    static let purchaseLevelPack5Button = ("level_pack_5"~, UIImage(systemName: "dollarsign.circle.fill")!)
    static let purchaseLevelPack6Button = ("level_pack_6"~, UIImage(systemName: "dollarsign.circle.fill")!)
    
    // Hints menu
    static let revealClueButton = ("reveal_clue"~, UIImage(systemName: "eye")!)
    static let placeLetterButton = ("place_letter"~, UIImage(systemName: "bolt.badge.a")!)
    static let purchaseCluesButton = ("unlimited_clues"~, UIImage(systemName: "dollarsign.circle.fill")!)
    static let purchaseLettersButton = ("unlimited_letters"~, UIImage(systemName: "dollarsign.circle.fill")!)
    static let purchaseNoAdsButton = ("no_level_ads"~, UIImage(systemName: "dollarsign.circle.fill")!)
    static let purchaseAllButton = ("all_in_pack"~, UIImage(systemName: "dollarsign.circle.fill")!)

}
