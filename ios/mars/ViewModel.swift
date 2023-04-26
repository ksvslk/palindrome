//
//  ViewModel.swift
//  mars
//
//  Created by ksvslk on 26.04.2023.
//

import Foundation
import CoreData

class ViewModel {
    
    static let shared = ViewModel()
    
    private let persistentContainer: NSPersistentContainer
    private let context: NSManagedObjectContext
    
    var currentLevel: GameLevel?
    
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
        let gameLevel = GameLevel(context: context)
        gameLevel.levelNumber = 1
        gameLevel.level = Level(context: context)
        gameLevel.level?.score = 300
        gameLevel.level?.letters = NSSet(array: getArrayOfLetters(palindrome: palindrome))
        let solution = Solution(context: context)
        solution.palindrome = "MOM"
        gameLevel.level?.solutions = NSSet(array: [solution])
        
        do {
            try context.save()
        } catch {
            print("Error saving item: \(error)")
        }
    }
    
    func changeGameLevel(nr: Int)
    {
        let request: NSFetchRequest<GameLevel> = GameLevel.fetchRequest()
        request.predicate = NSPredicate(format: "levelNumber = %@", String(nr))
        
        var fetchedLevels: [GameLevel] = []
        do {
            fetchedLevels = try context.fetch(request)
        } catch {
            print("Error fetching level")
        }
        if !fetchedLevels.isEmpty { currentLevel = fetchedLevels.first }
        print("Loaded level:", (currentLevel!.level?.letters))
    }
    
    
    func getArrayOfLetters(palindrome:String) -> [LetterData]
    {
        palindrome.map { char in
            let letter = LetterData(context: context)
            letter.letter = String(char)
            return letter
        }
    }

    
}
