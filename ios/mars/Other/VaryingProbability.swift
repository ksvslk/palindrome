//
//  VaryingProbability.swift
//  mars
//
//  Created by Mina on 31.05.2023.
//

import Foundation

struct VaryingProbability {
    static func isAnimationPlayed(level: Int) -> Bool {
        let randomNumber = Int.random(in: 1...100)
        let randomPercentage = getRange(level: level).randomElement() ?? 0
        return randomNumber <= randomPercentage
    }

    private static func getRange(level: Int) -> ClosedRange<Int> {
        switch level {
        case 1...10:
            return 40...50
        case 11...20:
            return 30...50
        default:
            return 25...45
        }
    }
}
