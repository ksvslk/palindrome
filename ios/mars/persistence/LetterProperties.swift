//
//  LetterProperties.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation


struct LetterProperties : Codable {
    let infoId: Int
    let ownerId: Int
    let homeIndex: Int
    let friendIndex: Int
    let letter: String
    let locked: Bool
}
