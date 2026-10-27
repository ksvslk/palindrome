//
//  SolutionChecker.swift
//  mars
//
//  Created by Mina on 13.05.2023.
//

import Foundation

// TESTED: "BOOB", "WOW", "LIVE EVIL" (2 solutions), "DOG! NO POOP ON GOD!" (4 solutions)
class SolutionChecker {
    
    static let charsToIgnoreRegex = "[\\s^([?!.,’':-])]+"
    
    /**
     Compare current guess to all solutions and bogus solutions
     - Returns: When no match with correct palindrome or bogus (reversed) palindrome`.wrong`.
     - Returns: Any correct palindrome from solutions`.correct`.
     - Returns: Any bogus palindrome (reversed) from solutions`.bogus_palindrome`.
     */
    static func getGuessResult(guess: [LetterData], solutions:[Solution]) -> SolutionCheckResult
    {
        if (guess.allSatisfy({ letter in
            letter.friendIndex != -1
        })) {
            let currentGuess = guess.map({ l in charsToIgnoreRegex.contains(l.letter) ? "" : l.letter }).joined()
            for solution in solutions {
                let correctSolution = getSolutionWithoutExtras(str: solution.palindrome)
                if (correctSolution == currentGuess) {
                    return SolutionCheckResult.correct
                }
            }
            if (currentGuess.isPalindrome()) {
                    return SolutionCheckResult.bogus_palindrome
            }
            return SolutionCheckResult.wrong
        }
        return SolutionCheckResult.invalid_guess
    }
    
    
    private static func getSolutionWithoutExtras(str: String) -> String {
        return str.replacingOccurrences(of: charsToIgnoreRegex, with: "", options: .regularExpression)
    }
    
}
