//
//  ShufflerTests.swift
//  marsTests
//
//  Created by Mina on 30.04.2023.
//

import XCTest
@testable import mars

final class ShufflerTests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }
    
    
    func testOneSolutionIsShuffled()
    {
        let solutions = ["BOOB"]
        for _ in 1...100 {
            let result = Shuffler.getShuffledSolution(solutionsWithWhiteSpace: solutions)
            XCTAssertFalse(result == solutions.first, "Solution should be shuffled.")
        }
        
    }
    
    
    func testMultipleSolutionsIsShuffled()
    {
        let solutions = ["ANNA", "NAAN"]
        for _ in 1...100 {
            let result = Shuffler.getShuffledSolution(solutionsWithWhiteSpace: solutions)
            XCTAssertFalse(result == solutions.first, "Solution should be shuffled.")
        }
    }
    
    
    func testShufflerExecutionTimeWithLongPalindrome()
    {
        let solutions = ["Retool llamas amidst old NASA men - icy men of fur. Go, gruff one! My cinema: sandlots dim as a mall looter."]
        measure {
            _ = Shuffler.getShuffledSolution(solutionsWithWhiteSpace: solutions)
        }
    }
    
    
    func testShufflerExecutionTimeWithTwoSolutions()
    {
        let solutions = ["ELBON NOBLE", "NOBLE ELBON"]
        measure {
            _ = Shuffler.getShuffledSolution(solutionsWithWhiteSpace: solutions)
        }
    }

}
