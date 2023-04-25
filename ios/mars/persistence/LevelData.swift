//
//  LevelData.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation

struct LevelData : Codable {
    let levelId: Int
    let puzzle: String
    let categoryRewarded: Bool
    let progress: LevelProgress
    let score: Int
    let best_score: Int
    let timeModified: Int64
    let author: String
    let columnCount:Int
}
