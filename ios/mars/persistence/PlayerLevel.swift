//
//  PlayerLevel.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation

struct PlayerLevel: Codable {
    let levelId: Int64
    let player: String
    let solutionToCategory: [String: String]
}
