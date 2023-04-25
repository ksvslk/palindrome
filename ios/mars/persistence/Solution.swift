//
//  Solution.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation

struct Solution: Codable {
    let solutionId: Int
    let parentId: Int
    let palindrome: String
    let hint: String
    let solved: Bool
}
