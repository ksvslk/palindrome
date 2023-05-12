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
    
    // User defaults
    static let VIBRATION_ON = "VIBRATION_ON"
    static let SOUND_ON = "SOUND_ON"
    static let LOGGED_IN = "LOGGED_IN"
    
}
