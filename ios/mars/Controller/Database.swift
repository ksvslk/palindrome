//
//  ViewModel.swift
//  mars
//
//  Created by ksvslk on 26.04.2023.
//

import Foundation
import CoreData

class Database {
    
    static let shared = Database()
    
    private let persistentContainer: NSPersistentContainer
    private let context: NSManagedObjectContext
    
    var currentLevel: Level?
    
    private init() {
        persistentContainer = NSPersistentContainer(name: "Model")
        persistentContainer.loadPersistentStores { _, error in
            if let error = error {
                fatalError("Failed to load Core Data stack: \(error)")
            }
        }
        context = persistentContainer.viewContext
        
    }
    
    func saveInitialData() {
        let palindrome = "MMO"
        let level = Level(context: context)
        level.levelNumber = 1
        level.score = 300
     
        let lettersArray = palindrome.map { char in
            let letter = LetterData(context: context)
            letter.letter = String(char)
            return letter
        }
        level.letters = NSSet(array: lettersArray)
        
        let solution = Solution(context: context)
        solution.palindrome = "MOM"
        solution.hint = "Parent"
        level.solutions = NSSet(array: [solution])
        
        do {
            try context.save()
        } catch {
            print("Error saving item: \(error)")
        }
    }
    
    
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
        print(currentLevel)
        
    }
    
    func resetLevel(level:Int, progress: Level.Progress = Level.Progress.in_progress)
    {
        // delete letters info for level
        let request: NSFetchRequest<Level> = Level.fetchRequest()
     
    }
    
    
}
