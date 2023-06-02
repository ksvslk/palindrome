//
//  ScoreItemInfo.swift
//  mars
//
//  Created by Mina on 31.05.2023.
//

import Foundation
import UIKit

struct ScoreItemInfo: Equatable {
 
    let displayName: String
    let photo: UIImage
    let score: Int
    let rank: Int
    let isCurrentPlayer: Bool
   
    static func == (lhs: ScoreItemInfo, rhs: ScoreItemInfo) -> Bool {
        return lhs.displayName == rhs.displayName
    }
}
