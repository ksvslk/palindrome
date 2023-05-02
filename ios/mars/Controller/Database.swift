//
//  ViewModel.swift
//  mars
//
//  Created by ksvslk on 26.04.2023.
//

import Foundation
import CoreData

class Database {
    
    enum DatabaseError: Error {
        case couldNotInsertLevel
        case insufficientFunds(coinsNeeded: Int)
        case outOfStock
    }
    
    static let shared = Database()
    
    private let persistentContainer: NSPersistentContainer
    private let context: NSManagedObjectContext
    
    var currentLevel: Level?
    
    
    // MARK: Initialize
    private init() {
        persistentContainer = NSPersistentContainer(name: "Model")
        persistentContainer.loadPersistentStores { _, error in
            if let error = error {
                fatalError("Failed to load Core Data stack: \(error)")
            }
        }
        context = persistentContainer.viewContext
        
    }
    
    
    // MARK: Add new level
    func addNewLevel(
        levelNr: Int,
        palindrome: String,
        solutionsToHints: [String:String],
        author: String = ""
    )
    {
        
        let request: NSFetchRequest<Level> = Level.fetchRequest()
        request.predicate = NSPredicate(format: "levelNumber = %@", String(levelNr))
        
        var fetchedLevels: [Level] = []
        do {
            fetchedLevels = try context.fetch(request)
        } catch {
            print("Error fetching level")
        }
        if !fetchedLevels.isEmpty { return }
        
        guard !palindrome.isEmpty || !solutionsToHints.isEmpty else { return }
        let level = Level(context: context)
        level.levelNumber = Int64(levelNr)
        level.shuffledPuzzle = Shuffler.getShuffledSolution(
            solutionsWithWhiteSpace: Array(solutionsToHints.keys)
        )
        level.author = author
        level.categoryRewarded = false
        level.bestScore = Int64(level.shuffledPuzzle.count * C.SCORE_LETTER_MULTIPLIER)
        level.letters = NSSet(array: level.shuffledPuzzle.enumerated().map { (index, char) in
            let letter = LetterData(context: context)
            letter.letter = String(char)
            letter.homeIndex = Int64(index)
            return letter
        })
        level.solutions = NSSet(array: solutionsToHints.map({ (key: String, value: String) in
            let solution = Solution(context: context)
            solution.palindrome = key
            solution.hint = value
            return solution
        }))
        
        do {
            try context.save()
        } catch {
            print("Error saving item: \(error)")
        }
        
    }
    
    
    // MARK: Change level
    func changeGameLevel(nr: Int)
    {
        let request: NSFetchRequest<Level> = Level.fetchRequest()
        request.predicate = NSPredicate(format: "levelNumber = %@", String(nr))
        
        var fetchedLevels: [Level] = []
        do {
            fetchedLevels = try context.fetch(request)
        } catch {
            print("Error fetching level")
        }
        
        
        
        if !fetchedLevels.isEmpty { currentLevel = fetchedLevels.first }
        
        // TODO: When level has -1 columns (brand new level), we should also set the columns in the same transaction.
        
    }
    
    
    // MARK: Reset Level
    func resetLevel(level:Int, progress: Level.Progress = Level.Progress.in_progress)
    {
        // TODO: Implement!
    }
    
    
    func changeFriendIndex(letter: LetterData, newFriendIndex: Int64)
    {
        letter.friendIndex = newFriendIndex
        do {
            try context.save()
        } catch {
            print("Error changing friend index: \(error)")
        }
    }
    
    
    func switchFriendIndexes(top: LetterData, bottom: LetterData)
    {
        let tempTopFriendIndex = top.friendIndex
        let tempTopHomeIndex = top.homeIndex
        top.friendIndex = bottom.friendIndex
        bottom.friendIndex = tempTopFriendIndex
        top.homeIndex = bottom.homeIndex
        bottom.homeIndex = tempTopHomeIndex

        do {
            try context.save()
        } catch {
            print("Error switching friend indexes: \(error)")
        }
    }
    
    
}
