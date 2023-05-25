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
    
}

