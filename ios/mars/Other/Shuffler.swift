//
//  Shuffler.swift
//  mars
//
//  Created by Mina on 29.04.2023.
//

import Foundation


public class Shuffler {
    
    // MARK: Shuffled string
    static func getShuffledSolution(solutionsWithWhiteSpace: [String]) -> String {
        let solutions = solutionsWithWhiteSpace.map { str in
            str.replacingOccurrences(of: " ", with: "")
        }
        var shuffledChars = Array(solutions.first!)
        var shuffledSolution = solutions.first! // Start with not shuffled solution
        var similarities: [Double] = []
        
        while (
            solutions.contains(where: { $0 == shuffledSolution }) ||
            similarities.contains(where: { $0 > 0.5 })) {
            similarities.removeAll()
            shuffledChars.shuffle()
            shuffledSolution = String(shuffledChars)
            solutions.forEach({ s in
                similarities.append(similarity(s1: s, s2: shuffledSolution))
            })
            
        }
        return String(shuffledChars)
    }
    
    /**
     * Calculates the similarity (a number within 0 and 1) between two strings.
     */
    // MARK: Similarity
    private static func similarity(s1: String, s2: String) -> Double {
        var longer = s1
        var shorter = s2
        if (s1.count < s2.count) { // longer should always have greater length
            longer = s2
            shorter = s1
        }
        let longerLength = Double(longer.count)
        return (longerLength == 0) ? 1.0 :
        (longerLength - Double(editDistance(a: longer, b: shorter))) / longerLength
    }
    
    
    // MARK: Edit distance
    private static func editDistance(a: String, b: String) -> Int {
        let s1 = a.lowercased()
        let s2 = b.lowercased()
        var costs = [Int](repeating: 0, count: s2.count + 1)
        for i in 0...s1.count {
            var lastValue = i
            for j in 0...s2.count {
                if (i == 0) { costs[j] = j }
                else {
                    if (j > 0) {
                        var newValue = costs[j - 1]
                        if (s1[s1.index(s1.startIndex, offsetBy: i-1)] != s2[s2.index(s2.startIndex, offsetBy: j-1)]) {
                            newValue = min(min(newValue, lastValue), costs[j]) + 1
                        }
                        costs[j - 1] = lastValue
                        lastValue = newValue
                    }
                }
            }
            if (i > 0) { costs[s2.count] = lastValue }
        }
        return costs[s2.count]
    }
    
}
