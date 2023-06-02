//
//  ViewModel.swift
//  mars
//
//  Created by ksvslk on 26.04.2023.
//

import Foundation
import CoreData
import CloudKit
import GameKit

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
    var levelChangedClosure: ((_ intervals:[Interval]) -> Void)?
    var solutionCheckResultClosure: ((_ solutionCheckResult:SolutionCheckResult, _ scoreAndBest:(Int,Int)) -> Void)?
    var hintsSuggestionClosure: (() -> Void)?
    var cloudChangesClosure:((_ info:String, _ resultOfQuestion : @escaping (Bool) -> Void) -> Void)?
    
    
    private init() {
        persistentContainer = NSPersistentContainer(name: "Model")
        persistentContainer.loadPersistentStores { _, error in
            if let error = error {
                fatalError("Failed to load Core Data stack: \(error)")
            }
        }
        
        context = persistentContainer.viewContext
        observeKeyValueChangesInCloud()
    }
    
    func pp()
    {
        cloudChangesClosure?("Do you want to load from cloud?") { result in
            print("Player chose: \(result)")
        }
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
                progress: jsonLevel.nr == 1 ? Level.Progress.in_progress : Level.Progress.locked
            )
        }
        addInterval(name: C.HINT_INTERVAL, limitCount: 5)
        addInterval(name: C.SCORES_INTERVAL, limitCount: 4)
        addInterval(name: C.ADS_INTERVAL, limitCount: 5)
        addInterval(name: C.FLAWLESS_5_INTERVAL, limitCount: 5)
        addInterval(name: C.FLAWLESS_15_INTERVAL, limitCount: 15)
        
        let diff = CFAbsoluteTimeGetCurrent() - start
        Swift.debugPrint("Inserting all levels to database took \(diff) seconds")
        
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
                Swift.debugPrint("Interval with name '\(name)' already exists.")
            }
        } catch {
            Swift.debugPrint("Error fetching intervals: \(error)")
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
            Swift.debugPrint("Error fetching level")
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
        level.score = level.shuffledPuzzle.maxScore()
        level.bestScore = 0
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
            Swift.debugPrint("Error saving item: \(error)")
        }
        
    }
    
    
    // MARK: Change to next level
    func changeToNextLevel()
    {
        let nextLevel = Int(currentLevel!.levelNumber + 1)
        if (nextLevel <= getAllGameLevels().count)
        {
            changeLevel(to: Int(nextLevel), withProgressReset: true)
        }
        else {
            changeLevel(to: 1, withProgressReset: true)
        }
        
    }
    
    
    //MARK: Observe key-value changes
    func observeKeyValueChangesInCloud() {
        
        NotificationCenter.default.addObserver(self,
            selector: #selector(ubiquitousKeyValueStoreDidChange(_:)),
            name: NSUbiquitousKeyValueStore.didChangeExternallyNotification,
            object: NSUbiquitousKeyValueStore.default)
   
        if NSUbiquitousKeyValueStore.default.synchronize() == false {
            fatalError("This app was not built with the proper entitlement requests.")
        }
    }
    
    /** This notification is sent only upon a change received from iCloud; it is not sent when your app
        sets a value. So this is called when the key-value store in the cloud has changed externally.
    */
    @objc
    func ubiquitousKeyValueStoreDidChange(_ notification: Notification) {
        print("ubiquitousKeyValueStoreDidChange: \(notification.debugDescription)")
        guard let userInfo = notification.userInfo else { return }
        
        // Get the reason for the notification (initial download, external change or quota violation change).
        guard let reasonForChange = userInfo[NSUbiquitousKeyValueStoreChangeReasonKey] as? Int else { return }

        // Check if any of the keys we care about were updated, and if so use the new value stored under that key.
        guard let keys =
            userInfo[NSUbiquitousKeyValueStoreChangedKeysKey] as? [String] else { return }
        
        print(keys.debugDescription)
        
        switch reasonForChange {
        case NSUbiquitousKeyValueStoreServerChange: // Value(s) were changed externally from other users/devices.
            print("Initiate the change.")
        case NSUbiquitousKeyValueStoreInitialSyncChange: // Inital download and when user switches primary iCloud account
            print("Add default values")
        case NSUbiquitousKeyValueStoreAccountChange: // User changed primary iCloud account
            print("Add default values")
        case NSUbiquitousKeyValueStoreQuotaViolationChange: // Quota exceeded
            fatalError("Key-value quota exceeded. This should not happen.")
        default:
            print(":)")
        }
    }
    
    
    // MARK: Sync UserDefaults info
    private func updateBasedOnCloudKitUserDefaults()
    {
        let defaults = UserDefaults.standard
        let maximumUnlockedLevel = defaults.integer(forKey: C.MAX_UNLOCKED_LEVEL)
        let latestPlayedLevel = defaults.integer(forKey: C.LATEST_PLAYED_LEVEL)
        let scores = defaults.integer(forKey: C.PLAYER_SCORES)
        
        // Should do it in a transaction style
        
        // Perform the transaction in a block
        context.performAndWait {
            
        }
        
        //TODO: Check if maximum unlocked level is bigger than local database maximum level and unlock until that level if YES
        
        //TODO: If maximum was change then change also latest played, otherwise skip
        
        //TODO: Check if local total score is smaller than scores combined. If YES then loop and update levels array 0 is level 1 and so on.
        
        
        
    }
    
    
    // MARK: Increase interval -> result
    private func updateCurrentCount(forName name: String, by addition: Int64 = 1, reset: Bool = false) -> Bool {
        var result = false
        
        // Perform the transaction in a block
        context.performAndWait {
            // Create a fetch request to retrieve the object with the specified name
            let fetchRequest: NSFetchRequest<Interval> = Interval.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "name == %@", name)
            
            do {
                // Fetch the object
                let objects = try context.fetch(fetchRequest)
                
                if let object = objects.first {
                    
                    if reset {
                        object.currentCount = 0
                        return
                    }
                    
                    // Increase the currentCount
                    object.currentCount += addition
                    
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
                Swift.debugPrint("Error: \(error)")
            }
        }
        return result
    }
    
    
    // MARK: Change level
    func changeLevel(to nextLevel:Int = -1, toLatest loadLatestLevel:Bool = false, withProgressReset reset:Bool = false)
    {
        var intervals:[Interval] = []
        
        context.performAndWait {
            let intervalsRequest: NSFetchRequest<Interval> = Interval.fetchRequest()
            intervalsRequest.fetchLimit = 0
            
            do {
                
                // reset hint intervals, and add +1 for other intervals
                intervals = try context.fetch(intervalsRequest)
                for interval in intervals {
                    if (interval.name == C.HINT_INTERVAL) {
                        interval.currentCount = 0
                    }
                    else if (interval.name != C.FLAWLESS_5_INTERVAL && interval.name != C.FLAWLESS_15_INTERVAL) {
                        if (interval.currentCount >= interval.limitCount)
                        {
                            interval.currentCount = 0
                        }
                        else {
                            interval.currentCount += 1
                        }
                    }
                }
                
                
                // reset letters info and set progress to .in_progress
                if (reset)
                {
                    currentLevel!.score = currentLevel!.shuffledPuzzle.maxScore()
                    currentLevel!.progress = Int64(Level.Progress.in_progress.rawValue)
                    currentLevel!.shuffledPuzzle = Shuffler.getShuffledSolution(
                        solutionsWithWhiteSpace: currentLevel!.solutionsArr.map({ solution in
                            solution.palindrome
                        })
                    )
                    currentLevel!.letters = NSSet(array: currentLevel!.shuffledPuzzle.enumerated().map { (index, char) in
                        let letter = LetterData(context: context)
                        letter.letter = String(char)
                        letter.homeIndex = Int64(index) + 1
                        return letter
                    })
                    
                }
                
                // load latest level OR the level specified
                if (loadLatestLevel)
                {
                    let latestLevelRequest: NSFetchRequest<Level> = Level.fetchRequest()
                    latestLevelRequest.predicate = NSPredicate(format: "timeModified == max(timeModified)")
                    let results = try context.fetch(latestLevelRequest)
                    if let latestLevel = results.first {
                        currentLevel = latestLevel
                    } else {
                        latestLevelRequest.predicate = NSPredicate(format: "levelNumber = %@", String(1))
                        latestLevelRequest.fetchLimit = 1
                        let firstLevelResults = try context.fetch(latestLevelRequest)
                        if let firstLevel = firstLevelResults.first {
                            currentLevel = firstLevel
                        }
                        else {
                            fatalError("Failing to fetch even first level.")
                        }
                    }
                }
                else {
                    let nextLevelRequest: NSFetchRequest<Level> = Level.fetchRequest()
                    nextLevelRequest.predicate = NSPredicate(format: "levelNumber = %@", String(nextLevel))
                    nextLevelRequest.fetchLimit = 1
                    let results = try context.fetch(nextLevelRequest)
                    if let chosenLevel = results.first {
                        currentLevel = chosenLevel
                        currentLevel?.progress = Int64(Level.Progress.in_progress.rawValue)
                    } else {
                        fatalError("Could not set next level nr \(nextLevel)")
                    }
                }
                
                
                // change the max time to the current level
                currentLevel?.timeModified = Date.init(timeIntervalSinceNow: .zero)
                
                // set game state based on progress
                switch currentLevel!.levelProgress {
                case .in_progress:
                    StateManager.shared.currentState = .play
                case .completed:
                    StateManager.shared.currentState = .completed
                case .locked:
                    fatalError("Level progress should not be .locked here.")
                }
                
                try context.save()
                
                // execute closures
                levelChangedClosure?(intervals)
                checkSolutionCorrectness()
                
            } catch {
                // Handle the error
                Swift.debugPrint("Error: \(error)")
            }
        }
    }
    
    
    //MARK: Get total score
    private func getTotalScore() -> Int {
        var score = 0
        
        let request: NSFetchRequest<Level> = Level.fetchRequest()
        request.predicate = NSPredicate(format: "levelNumber <= %@", argumentArray: [100])
        
        do {
            let results = try context.fetch(request) as [Level]
            results.forEach { level in
                score += Int(level.bestScore)
            }
        } catch {
            Swift.debugPrint("Error fetching results: \(error.localizedDescription)")
        }
        return score
    }
    
    
    //MARK: Submit score
    private func submitScoreToLeaderboard(score: Int) async throws {
        
        if GKLocalPlayer.local.isAuthenticated {
            GKLeaderboard.submitScore(
                score,
                context: 0,
                player: GKLocalPlayer.local,
                leaderboardIDs: [C.GAME_LEADERBOARD],
                completionHandler: { error in
                    if (error != nil) {
                        Swift.debugPrint("Could not submit score: \(String(describing: error))")
                    }
                }
            )
        } else {
            Swift.debugPrint("Player not authenticated to submit score.")
        }
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
            Swift.debugPrint("Error getting maximum level from database.")
        }
        return -1
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
            Swift.debugPrint("Error fetching results: \(error.localizedDescription)")
        }
        return levels
    }
    
    
    // MARK: Change friend index
    func changeFriendIndex(letter: LetterData, newFriendIndex: Int64)
    {
        letter.friendIndex = newFriendIndex
        currentLevel?.timeModified = Date.init(timeIntervalSinceNow: .zero)
        do {
            try context.save()
        } catch {
            Swift.debugPrint("Error changing friend index: \(error)")
        }
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
        currentLevel?.timeModified = Date.init(timeIntervalSinceNow: .zero)
        
        do {
            try context.save()
        } catch {
            Swift.debugPrint("Error switching friend indexes: \(error)")
        }
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
            Swift.debugPrint("Could not fetch. \(error), \(error.userInfo)")
        }
        
        let result = SolutionChecker.getGuessResult(guess: letters, solutions: currentLevel!.solutionsArr)
        solutionCheckResultClosure?(result, (Int(currentLevel!.score), Int(currentLevel!.shuffledPuzzle.maxScore())))
        
        if (result == SolutionCheckResult.bogus_palindrome || result == SolutionCheckResult.wrong)
        {
            handleWrongSolutionAchievementProgress(solution: result)
            let hintsIntervalReached = updateCurrentCount(forName: C.HINT_INTERVAL)
            if (hintsIntervalReached) {
                hintsSuggestionClosure?()
            }
        }
      
    }
 
    
    //MARK: Handle achievments (wrong)
    private func handleWrongSolutionAchievementProgress(solution: SolutionCheckResult)
    {
        Swift.debugPrint("handleWrongSolutionAchievementProgress")
        updateCurrentCount(forName: C.FLAWLESS_5_INTERVAL, reset: true)
        updateCurrentCount(forName: C.FLAWLESS_15_INTERVAL, reset: true)
        if (solution == SolutionCheckResult.bogus_palindrome)
        {
            unlockAchievement(identifier: C.achievement_wrong_palindrome)
        }
    }
    
    
    //MARK: Handle achievments (correct)
    private func handleLevelCompleteAchievements(isMaxScore:Bool, levelNr:Int)
    {
        Swift.debugPrint("handleLevelCompleteAchievements")
        if (isMaxScore)
        {
            unlockAchievement(identifier: C.achievement_zero_mistakes)
            let reachedInterval5 = updateCurrentCount(forName: C.FLAWLESS_5_INTERVAL)
            let reachedInterval15 = updateCurrentCount(forName: C.FLAWLESS_15_INTERVAL)
            if reachedInterval5 {
                unlockAchievement(identifier: C.achievement_flawless_5)
            }
            if reachedInterval15 {
                unlockAchievement(identifier: C.achievement_flawless_15)
            }
        }
        
        if (levelNr == 9)
        {
            unlockAchievement(identifier: C.achievement_complete_level)
        }
        else if (levelNr == 20)
        {
            unlockAchievement(identifier: C.achievement_greenhorn)
        }
        else if (levelNr == 40)
        {
            unlockAchievement(identifier: C.achievement_proficient)
        }
        else if (levelNr == 60)
        {
            unlockAchievement(identifier: C.achievement_crackerjack)
        }
        else if (levelNr == 80)
        {
            unlockAchievement(identifier: C.achievement_chief)
        }
        else if (levelNr == 100)
        {
            unlockAchievement(identifier: C.achievement_authority)
        }
    }
    
    
    //MARK: Unlock achievement
    private func unlockAchievement(identifier: String, percentComplete: Double = 100.0) {
        let achievement = GKAchievement(identifier: identifier)
        achievement.percentComplete = percentComplete
        achievement.showsCompletionBanner = true
        
        GKAchievement.report([achievement]) { (error) in
            if let error = error {
                // Handle the error
                Swift.debugPrint("Achievement unlock error: \(error.localizedDescription)")
            } else {
                // Achievement unlocked successfully
                Swift.debugPrint("Achievement unlocked: \(identifier)")
            }
        }
    }
    
    
    // MARK: Decrease score
    func decreaseScore()
    {
        let currentScore = currentLevel!.score
        let pointsToSubtract = currentLevel!.shuffledPuzzle.maxScore() / 10
        let newScore = (currentScore - pointsToSubtract < 0) ? 0 : currentScore - Int64(pointsToSubtract)
        currentLevel!.score = newScore
        
        do {
            try context.save()
        } catch {
            Swift.debugPrint("Error changing score: \(error)")
        }
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
            Swift.debugPrint("Error changing columnd count: \(error)")
        }
        
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
            Swift.debugPrint("Error changing category rewarded: \(error)")
        }
    }
    
    
    // MARK: Update letter locked status
    func lockLetter(letter:LetterData)
    {
        letter.locked = true
        
        do {
            try context.save()
        } catch {
            Swift.debugPrint("Error locking letter: \(error)")
        }
        
    }
    
    
    // MARK: Set level as completed
    func setLevelCompletedImmediately()
    {
        currentLevel!.bestScore = max(currentLevel!.score, currentLevel!.bestScore)
        currentLevel!.progress = Int64(Level.Progress.completed.rawValue)
     
        handleLevelCompleteAchievements(
                isMaxScore: currentLevel!.score == currentLevel!.shuffledPuzzle.maxScore(),
                levelNr:Int(currentLevel!.levelNumber)
            )
        
        do {
            try context.save()
        } catch {
            Swift.debugPrint("Error setting level completed: \(error)")
        }
        
        Task {
            let totalScore = getTotalScore()
            do {
                try await submitScoreToLeaderboard(score: totalScore)
            } catch {
                Swift.debugPrint("Error:", error.localizedDescription)
            }
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
            Swift.debugPrint("Error while trying to count friendIndexes")
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



