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
    static let LOGGED_IN = "LOGGED_IN"
        
    // IDs
    static let GAME_LEADERBOARD = "game_leaderboard"
    static let PLAYER_LEADERBOARD = "player_leaderboard"
    
    // Product IDs
    static let PURCHASE_LETTERS_ID = "purchase_letters" // 2.99
    static let PURCHASE_HINTS_ID = "purchase_hints" // 1.99
    static let PURCHASE_NO_ADS_ID = "purchase_no_ads" // 1.99
    static let PURCHASE_ALL_ID = "purchase_all" // 4.99
    
    static let PURCHASED_PRODUCTS_KEY = "user_defaults_purchases"
    
    // URLs
    static let APP_FEEDBACK_URL = "https://apps.apple.com/app/id6444929937?action=write-review" // TODO: Change id to hah
    static let APP_URL = "https://apps.apple.com/app/penalty-2d/id6444929937" // TODO: Change it
    static let SS = "005ee9f1fdd24a50b712ea8734f109f2"
    
    
    // Intervals
    static let HINT_INTERVAL = "hint_interval"
    static let ADS_INTERVAL = "ads_interval"
    static let SCORES_INTERVAL = "scores_interval"
    
    
    // Settings menu
    static let tutorialButton = ("tutorial"~, UIImage(systemName: "questionmark.app")!)
    static let feedbackButton = ("feedback"~, UIImage(systemName: "square.and.pencil")!)
    static let shareButton = ("share"~, UIImage(systemName: "square.and.arrow.up")!)
    static let restoreButton = ("restore"~, UIImage(systemName: "repeat")!)

    // Game Center menu
    static let playerLeaderButton = ("player_leader"~, UIImage(systemName: "increase.indent")!)
    static let gameLeaderButton = ("game_leader"~, UIImage(systemName: "decrease.indent")!)
    static let achievementsButton = ("achievements"~, UIImage(systemName: "rosette")!)

    
    // Level select menu
    static let submitLevelButton = ("submit_level"~, UIImage(systemName: "paperplane")!)
    static let playerLevelsButton = ("player_levels"~, UIImage(systemName: "person.2")!)
    static let gameLevelsButton = ("game_levels"~, UIImage(systemName: "square.stack.3d.up")!)
    
    
    // Hints menu
    static let revealClueButton = ("reveal_clue"~, UIImage(systemName: "eye")!)
    static let placeLetterButton = ("place_letter"~, UIImage(systemName: "bolt.badge.a")!)
    static let purchaseCluesButton = ("...", UIImage(systemName: "dollarsign.square.fill")!)
    static let purchaseLettersButton = ("...", UIImage(systemName: "dollarsign.square.fill")!)
    static let purchaseNoAdsButton = ("...", UIImage(systemName: "dollarsign.square.fill")!)
    static let purchaseAllButton = ("...", UIImage(systemName: "dollarsign.square.fill")!)
}
