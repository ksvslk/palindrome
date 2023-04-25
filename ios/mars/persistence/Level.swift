//
//  Level.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation

struct Level: Codable {
    let levelInfo: LevelData
    let lettersProperties: [LetterProperties]
    let solutions: [Solution]
}
