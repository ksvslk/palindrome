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
        case couldNotInitializeGame(error: String)
        case couldNotFindJson(error: String)
    }
    
    static let shared = Database()
    
    private let persistentContainer: NSPersistentContainer
    private let context: NSManagedObjectContext
    
    var currentLevel: Level?
    
    // MARK: Closures for UI layer
    var levelChangedClosure: (() -> Void)?
    var solutionCheckResultClosure: ((_ solutionCheckResult:SolutionCheckResult) -> Void)?
    var hintsSuggestionClosure: (() -> Void)?
    
    
    private init() {
        if #available(iOS 13.0, *) {
            persistentContainer = NSPersistentContainer(name: "Model") // TODO: Change to CloudContainer..
        } else {
            persistentContainer = NSPersistentContainer(name: "Model")
        }
        persistentContainer.loadPersistentStores { _, error in
            if let error = error {
                fatalError("Failed to load Core Data stack: \(error)")
            }
        }
        
        context = persistentContainer.viewContext
        
    }
    
    
    // MARK: Add game levels (1 time)
    func initializeDatabaseWithGameLevels()
    {
        
        if (maxLevelNumber() >= 100) { return }
        
        let start = CFAbsoluteTimeGetCurrent()
        
        struct JsonLevel: Codable {
            var nr:Int
            var solutionToCategory: Dictionary<String, String>
        }
        var levels = [JsonLevel]()
        let resource = "game_levels"
        
        if let path = Bundle.main.path(forResource: resource, ofType: "json") {
            do {
                let data = try Data(contentsOf: URL(fileURLWithPath: path), options: .alwaysMapped)
                let list = try JSONDecoder().decode([JsonLevel].self, from: data)
                levels = list
            } catch let error {
                fatalError(error.localizedDescription)
            }
        } else {
            fatalError("Json was not found from path: \(resource)")
        }
        
        levels.forEach { jsonLevel in
            addNewLevel(
                levelNr: jsonLevel.nr,
                palindrome: jsonLevel.solutionToCategory.first!.key,
                solutionsToHints: jsonLevel.solutionToCategory,
                //progress: jsonLevel.nr == 1 ? Level.Progress.in_progress : Level.Progress.locked
                progress: Level.Progress.in_progress
            )
        }
        addInterval(name: C.HINT_INTERVAL, limitCount: 5)
        
        let diff = CFAbsoluteTimeGetCurrent() - start
        print("Inserting all levels to database took \(diff) seconds")
        
    }
    
    
    // MARK: Add interval
    private func addInterval(name: String, limitCount: Int64) {
        // Create a fetch request to check if an interval with the same name already exists
        let fetchRequest: NSFetchRequest<Interval> = Interval.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "name == %@", name)
        
        do {
            // Execute the fetch request
            let matchingIntervals = try context.fetch(fetchRequest)
            
            if matchingIntervals.isEmpty {
                // No matching interval found, create and save a new interval
                let interval = Interval(context: context)
                interval.currentCount = 0
                interval.limitCount = limitCount
                interval.name = name
                
                try context.save()
            } else {
                // A matching interval already exists, handle the duplicate case
                print("Interval with name '\(name)' already exists.")
            }
        } catch {
            print("Error fetching intervals: \(error)")
        }
    }
    
    
    // MARK: Add new level
    func addNewLevel(
        levelNr: Int,
        palindrome: String,
        solutionsToHints: [String:String],
        author: String = "",
        progress: Level.Progress = Level.Progress.in_progress
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
            letter.homeIndex = Int64(index) + 1
            return letter
        })
        level.progress = Int64(progress.rawValue)
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
    
    
    func resetLevel()
    {
        currentLevel!.progress = Int64(Level.Progress.in_progress.rawValue)
        currentLevel!.letters = NSSet(array: currentLevel!.shuffledPuzzle.enumerated().map { (index, char) in
            let letter = LetterData(context: context)
            letter.letter = String(char)
            letter.homeIndex = Int64(index) + 1
            return letter
        })
        
        do {
            try context.save()
        } catch {
            print("Error saving item: \(error)")
        }
    }
    
    
    func changeToNextLevel()
    {
        resetLevel()
        let nextLevel = currentLevel!.levelNumber + 1
        changeGameLevel(nr: Int(nextLevel))
    }
    
    
    // MARK: Increase interval -> result
    private func updateCurrentCount(forName name: String) -> Bool {
        var result = false
        
        // Get the managed object context
        let context = persistentContainer.viewContext
        
        // Perform the transaction in a block
        context.performAndWait {
            // Create a fetch request to retrieve the object with the specified name
            let fetchRequest: NSFetchRequest<Interval> = Interval.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "name == %@", name)
            
            do {
                // Fetch the object
                let objects = try context.fetch(fetchRequest)
                
                if let object = objects.first {
                    // Increase the currentCount
                    object.currentCount += 1
                    
                    // Compare with the limitCount
                    if object.currentCount == object.limitCount {
                        // Reset currentCount to 0
                        object.currentCount = 0
                        
                        // Set the result to true
                        result = true
                    }
                }
                
                // Save the changes
                try context.save()
            } catch {
                // Handle the error
                print("Error: \(error)")
            }
        }
        
        return result
    }
    
    
    // MARK: Change level
    func changeGameLevel(nr: Int)
    {
        let request: NSFetchRequest<Level> = Level.fetchRequest()
        request.predicate = NSPredicate(format: "levelNumber = %@", String(nr))
        print("CHANGE LEVEL TO: \(nr)")
        var fetchedLevels: [Level] = []
        do {
            fetchedLevels = try context.fetch(request)
        } catch {
            print("Error fetching level")
        }
        
        if !fetchedLevels.isEmpty {
            currentLevel = fetchedLevels.first
            switch currentLevel!.levelProgress {
            case .in_progress:
                StateManager.shared.currentState = .play
            case .completed:
                StateManager.shared.currentState = .completed
            case .locked:
                fatalError("Level progress should not be .locked here.")
            }
            levelChangedClosure?()
            checkSolutionCorrectness()
        }
        
        // TODO: When level has -1 columns (brand new level), we should also set the columns in the same transaction.
        
    }
    
    
    // MARK: Get max level number
    func maxLevelNumber() -> Int {
        let request: NSFetchRequest<Level> = Level.fetchRequest()
        request.resultType = .dictionaryResultType
        request.propertiesToFetch = ["levelNumber"]
        request.sortDescriptors = [NSSortDescriptor(key: "levelNumber", ascending: false)]
        request.fetchLimit = 1
        
        do {
            let result = try context.fetch(request) as! [NSDictionary]
            if let maxLevelNumber = result.first?["levelNumber"] as? Int {
                return maxLevelNumber
            }
        } catch {
            print("Error getting maximum level from database.")
        }
        return -1
    }
    
    
    // MARK: Refresh current level
    func refreshCurrentLevel()
    {
        let request: NSFetchRequest<Level> = Level.fetchRequest()
        request.predicate = NSPredicate(format: "levelNumber = %@", String(currentLevel!.levelNumber))
        var fetchedLevels: [Level] = []
        do {
            fetchedLevels = try context.fetch(request)
        } catch {
            print("Error fetching level")
        }
        
        if !fetchedLevels.isEmpty {
            currentLevel = fetchedLevels.first
        }
    }
    
    
    // MARK: Get all levels with progress
    func getAllGameLevels() -> [Level.Progress]
    {
        let request: NSFetchRequest<Level> = Level.fetchRequest()
        request.predicate = NSPredicate(format: "levelNumber <= %@", argumentArray: [100])
        let sortDescriptor = NSSortDescriptor(key: "levelNumber", ascending: true)
        request.sortDescriptors = [sortDescriptor]
        
        var levels:[Level.Progress] = []
        
        do {
            let results = try context.fetch(request) as [Level]
            results.forEach { level in
                levels.append(level.levelProgress)
            }
        } catch {
            print("Error fetching results: \(error.localizedDescription)")
        }
        return levels
    }
    
    
    // MARK: Change friend index
    func changeFriendIndex(letter: LetterData, newFriendIndex: Int64)
    {
        letter.friendIndex = newFriendIndex
        do {
            try context.save()
        } catch {
            print("Error changing friend index: \(error)")
        }
        //refreshCurrentLevel()
        checkSolutionCorrectness(letter: letter)
#if DEBUG
        assertFriendIndexesAreUniqueForEachLetter()
#endif
    }
    
    
    // MARK: Switch friend indexes
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
        //refreshCurrentLevel()
        checkSolutionCorrectness()
#if DEBUG
        assertFriendIndexesAreUniqueForEachLetter()
#endif
    }
    
    
    // MARK: Check solution correctness
    func checkSolutionCorrectness(letter: LetterData? = nil)
    {
        var letters: [LetterData] = []
        let request: NSFetchRequest<LetterData> = LetterData.fetchRequest()
        request.predicate = NSPredicate(format: "level.levelNumber = %@", currentLevel!.objectID)
        let sortDescriptor = NSSortDescriptor(key: "friendIndex", ascending: true)
        request.sortDescriptors = [sortDescriptor]
        
        do {
            letters = try context.fetch(request)
        } catch let error as NSError {
            print("Could not fetch. \(error), \(error.userInfo)")
        }
        
        let result = SolutionChecker.getGuessResult(guess: letters, solutions: currentLevel!.solutionsArr)
      
        if (result == SolutionCheckResult.correct)
        {
            //TODO: setCurrentLevelCompleted and other stuff that needs to be done when complete
        }
       
        solutionCheckResultClosure?(result)
        if (result == SolutionCheckResult.bogus_palindrome || result == SolutionCheckResult.wrong)
        {
            let hintsIntervalReached = updateCurrentCount(forName: C.HINT_INTERVAL)
            if (hintsIntervalReached) { hintsSuggestionClosure?() }
        }
        
    }
    
    
    private func setCurrentLevelCompleted()
    {
        //TODO: Object change and save context or query like save?
    }
    
    
    // MARK: Update column;letters
    func updateColumnCount(newColumntCount: Int64, oldFriendToNewFriendIndex:[Int:Int])
    {
        if (!oldFriendToNewFriendIndex.isEmpty)
        {
            currentLevel?.lettersArr.forEach({ letterData in
                
                if (letterData.friendIndex != -1)
                {
                    let newFriendIndex = oldFriendToNewFriendIndex[Int(letterData.friendIndex)]!
                    letterData.friendIndex = Int64(newFriendIndex)
                    
                }
                
            })
        }
        currentLevel?.columnCount = newColumntCount
        do {
            try context.save()
        } catch {
            print("Error changing columnd count: \(error)")
        }
        //refreshCurrentLevel()
        
    }
    
    func isPremiumLetters() -> Bool {
        return false
    }
    
    
    // MARK: Update hint status
    func setCategoryAsRewarded()
    {
        currentLevel?.categoryRewarded = true
        
        do {
            try context.save()
        } catch {
            print("Error changing category rewarded: \(error)")
        }
    }
    
    
    // MARK: Update letter locked status
    func lockLetter(letter:LetterData)
    {
        letter.locked = true
        
        do {
            try context.save()
        } catch {
            print("Error locking letter: \(error)")
        }
        
    }
    
    
    // MARK: Set level as completed
    func setLevelCompleted()
    {
        currentLevel!.progress = Int64(Level.Progress.completed.rawValue)
        
        do {
            try context.save()
        } catch {
            print("Error setting level completed: \(error)")
        }
        
    }
    
    
    // MARK: Is category rewarded
    func isCategoryRewarded() -> Bool
    {
        return currentLevel!.categoryRewarded
    }
    
    
    
#if DEBUG
    private func assertFriendIndexesAreUniqueForEachLetter()
    {
        let request: NSFetchRequest<LetterData> = LetterData.fetchRequest()
        request.predicate = NSPredicate(format: "level.levelNumber = %@", currentLevel!.objectID)
        let attributeName = "friendIndex"
        
        var fetchedLetters: [LetterData] = []
        do {
            fetchedLetters = try context.fetch(request)
        } catch {
            print("Error while trying to count friendIndexes")
        }
        let uniqueValues = Set(fetchedLetters.compactMap({
            let friendIndex = $0.value(forKey: attributeName) as? Int64
            if (friendIndex != -1) {
                return friendIndex
            }
            else {
                return nil
            }
        }))
        assert(uniqueValues.count == fetchedLetters.filter({ data in
            data.friendIndex != -1
        }).count)
    }
#endif
    
}
