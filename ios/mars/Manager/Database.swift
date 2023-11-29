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


struct JsonLevel: Codable {
    var nr:Int
    var solutionToCategory: Dictionary<String, String>
}

struct LevelPackRanges {
    var ranges: [(Int, Int)]
    init(_ ranges: KeyValuePairs<Int, Int>) {
        self.ranges = Array(ranges)
    }
}

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
    
    
    private init() {
        persistentContainer = NSPersistentContainer(name: "Model")
        persistentContainer.loadPersistentStores { _, error in
            if let error = error {
                fatalError("Failed to load Core Data stack: \(error)")
            }
        }
        context = persistentContainer.viewContext
    }
    
    let levelPackRanges = LevelPackRanges(
        [
            1 : 100,
            101 : 125,
            126 : 150,
            151 : 175,
            176 : 200,
            201 : 225,
            226 : 250
        ]
    )
    
    private func getRangeForLevelPack(pack:LevelPack) -> (Int, Int)
    {
        switch pack {
        case LevelPack.game_pack:
            return levelPackRanges.ranges[0]
        case LevelPack.level_pack_1:
            return levelPackRanges.ranges[1]
        case LevelPack.level_pack_2:
            return levelPackRanges.ranges[2]
        case LevelPack.level_pack_3:
            return levelPackRanges.ranges[3]
        case LevelPack.level_pack_4:
            return levelPackRanges.ranges[4]
        case LevelPack.level_pack_5:
            return levelPackRanges.ranges[5]
        case LevelPack.level_pack_6:
            return levelPackRanges.ranges[6]
        }
    }
    

    func getLevelPackNr(for level:Int) -> Int
    {
        switch level {
        case levelPackRanges.ranges[0].0...levelPackRanges.ranges[0].1:
            return 0
        case levelPackRanges.ranges[1].0...levelPackRanges.ranges[1].1:
            return 1
        case levelPackRanges.ranges[2].0...levelPackRanges.ranges[2].1:
            return 2
        case levelPackRanges.ranges[3].0...levelPackRanges.ranges[3].1:
            return 3
        case levelPackRanges.ranges[4].0...levelPackRanges.ranges[4].1:
            return 4
        case levelPackRanges.ranges[5].0...levelPackRanges.ranges[5].1:
            return 5
        case levelPackRanges.ranges[6].0...levelPackRanges.ranges[6].1:
            return 6
        default:
            fatalError("No such level: \(level), that matches any level pack.")
        }
    }
    
    
    func getLevelPack(for level:Int) -> LevelPack
    {
        switch level {
        case levelPackRanges.ranges[0].0...levelPackRanges.ranges[0].1:
            return LevelPack.game_pack
        case levelPackRanges.ranges[1].0...levelPackRanges.ranges[1].1:
            return LevelPack.level_pack_1
        case levelPackRanges.ranges[2].0...levelPackRanges.ranges[2].1:
            return LevelPack.level_pack_2
        case levelPackRanges.ranges[3].0...levelPackRanges.ranges[3].1:
            return LevelPack.level_pack_3
        case levelPackRanges.ranges[4].0...levelPackRanges.ranges[4].1:
            return LevelPack.level_pack_4
        case levelPackRanges.ranges[5].0...levelPackRanges.ranges[5].1:
            return LevelPack.level_pack_5
        case levelPackRanges.ranges[6].0...levelPackRanges.ranges[6].1:
            return LevelPack.level_pack_6
        default:
            fatalError("No such level: \(level), that matches any level pack.")
        }
    }
    
    
    func getNormalizedLevelNumber(levelNr:Int? = nil) -> Int
    {
       let currentLevelNumber = levelNr != nil ? levelNr! : Int(currentLevel!.levelNumber)
       let levelPack = getLevelPack(for: currentLevelNumber)
       switch levelPack {
        case LevelPack.game_pack:
            return currentLevelNumber
        case LevelPack.level_pack_1:
           return currentLevelNumber - 100
        case LevelPack.level_pack_2:
            return currentLevelNumber - levelPackRanges.ranges[1].1
        case LevelPack.level_pack_3:
           return currentLevelNumber - levelPackRanges.ranges[2].1
        case LevelPack.level_pack_4:
           return currentLevelNumber - levelPackRanges.ranges[3].1
        case LevelPack.level_pack_5:
           return currentLevelNumber - levelPackRanges.ranges[4].1
        case LevelPack.level_pack_6:
           return currentLevelNumber - levelPackRanges.ranges[5].1
        }
    }
    
    
    func getLevelNrAndPackName(levelNr:Int? = nil, longPackName:Bool = false) -> String
    {
       let currentLevelNumber = levelNr == nil ? Int(currentLevel!.levelNumber) : levelNr!
       let levelPack = getLevelPack(for: currentLevelNumber)
       switch levelPack {
        case LevelPack.game_pack:
           return longPackName ? "Main Levels" : String("\(currentLevelNumber)")
        case LevelPack.level_pack_1:
           return longPackName ? "Symmetric Secrets" : String("\(currentLevelNumber - 100) - SS Pack")
        case LevelPack.level_pack_2:
           return longPackName ? "Inverted Illusions" : String("\(currentLevelNumber - levelPackRanges.ranges[1].1) - II Pack")
        case LevelPack.level_pack_3:
           return longPackName ? "Reversal Riddles" : String("\(currentLevelNumber - levelPackRanges.ranges[2].1) - RR Pack")
        case LevelPack.level_pack_4:
           return longPackName ? "Echoed Elegance" : String("\(currentLevelNumber - levelPackRanges.ranges[3].1) - EE Pack")
        case LevelPack.level_pack_5:
           return longPackName ? "Twisted Texts" : String("\(currentLevelNumber - levelPackRanges.ranges[4].1) - TT Pack")
        case LevelPack.level_pack_6:
           return longPackName ? "Looped Lexicon" : String("\(currentLevelNumber - levelPackRanges.ranges[5].1) - LL Pack")
        }
    }
    
    
    func getActualLevelNumber(normalizedLevelNr levelNr:Int, levelPack:LevelPack) -> Int
    {
       switch levelPack {
        case LevelPack.game_pack:
            return levelNr
        case LevelPack.level_pack_1:
           return levelNr + 100
        case LevelPack.level_pack_2:
            return levelNr + levelPackRanges.ranges[1].1
        case LevelPack.level_pack_3:
           return levelNr + levelPackRanges.ranges[2].1
        case LevelPack.level_pack_4:
           return levelNr + levelPackRanges.ranges[3].1
        case LevelPack.level_pack_5:
           return levelNr + levelPackRanges.ranges[4].1
        case LevelPack.level_pack_6:
           return levelNr + levelPackRanges.ranges[5].1
        }
    }
    
    
    // MARK: Add game levels (1 time)
    func initializeDatabaseWithGameLevels()
    {
        if (maxLevelNumber() >= 100) {
            return
        }
        
        let start = CFAbsoluteTimeGetCurrent()
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
        ////Swift.debugPrint("Inserting all levels to database took \(diff) seconds")
        
    }
    
    // MARK: Add level packs
    func initializeLevelPacks()
    {
        if (maxLevelNumber() >= 250) {
            return
        }
        
        let levelPackResources = ["level_pack_1","level_pack_2","level_pack_3","level_pack_4","level_pack_5","level_pack_6"]
        
        levelPackResources.forEach { resource in
            var levels = [JsonLevel]()
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
            
            let firstLevel: Int = {
                if resource == "level_pack_1" {
                    return levelPackRanges.ranges[1].0
                } else if resource == "level_pack_2" {
                    return levelPackRanges.ranges[2].0
                } else if resource == "level_pack_3" {
                    return levelPackRanges.ranges[3].0
                } else if resource == "level_pack_4" {
                    return levelPackRanges.ranges[4].0
                } else if resource == "level_pack_5" {
                    return levelPackRanges.ranges[5].0
                } else if resource == "level_pack_6" {
                    return levelPackRanges.ranges[6].0
                } else {
                    return -1
                }
            }()
           
            levels.forEach { jsonLevel in
                addNewLevel(
                    levelNr: jsonLevel.nr,
                    palindrome: jsonLevel.solutionToCategory.first!.key,
                    solutionsToHints: jsonLevel.solutionToCategory,
                    progress: jsonLevel.nr == firstLevel ? Level.Progress.in_progress : Level.Progress.locked
                )
            }
        }
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
                ////Swift.debugPrint("Interval with name '\(name)' already exists.")
            }
        } catch {
            ////Swift.debugPrint("Error fetching intervals: \(error)")
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
            ////Swift.debugPrint("Error fetching level")
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
            ////Swift.debugPrint("Error saving item: \(error)")
        }
        
    }
    
    
    // MARK: Change to next level
    func changeToNextLevel()
    {
        let currentLevel = Int(currentLevel!.levelNumber)
        let nextLevel =  getNormalizedLevelNumber() + 1
        let levelPack = getLevelPack(for: currentLevel)
        let range = getRangeForLevelPack(pack: getLevelPack(for: currentLevel))

        if (getActualLevelNumber(normalizedLevelNr: nextLevel, levelPack: levelPack) <= range.1)
        {
            changeLevel(to: Int(nextLevel), withProgressReset: true, levelPack: levelPack)
        }
        else {
            let normalizedFirstLevelNr = getNormalizedLevelNumber(levelNr: range.0)
            changeLevel(to: normalizedFirstLevelNr, withProgressReset: true, levelPack: levelPack)
        }
        
    }
    
    
    //MARK: Get level pack progress
    func getLevelPackProgress(levelPack:LevelPack, completion: @escaping (Decimal) -> Void)
    {
            let levelPackProgresses = self.getAllLevelsProgresses(currentLevel: -1, pack: levelPack)
            let unlockedLevelsCount = levelPackProgresses.filter({progress in progress != Level.Progress.locked }).count
            let percentage = unlockedLevelsCount > 1 ? Decimal(unlockedLevelsCount) / Decimal(levelPackProgresses.count) * 100.0 : -1.0
            completion(percentage)
    
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
                ////Swift.debugPrint("Error: \(error)")
            }
        }
        return result
    }
    
    
    private func isLocalProgressBetter(localLevels:[Level], cloudProgresses:[Int64]) -> Bool
    {
        let localLevelsUnlockedCount = localLevels.filter({ level in
            level.progress != Level.Progress.locked.rawValue
        }).count
        let cloudLevelsUnlockedCount = cloudProgresses.filter({ progress in
            progress != Level.Progress.locked.rawValue
        }).count
        return localLevelsUnlockedCount > cloudLevelsUnlockedCount || localLevels.count > cloudProgresses.count
    }
    
    
    func loadFromCloudKeyValueStore() {
        let store = NSUbiquitousKeyValueStore.default
        let cloudScores:[Int64] = store.array(forKey: C.CLOUD_BEST_SCORES_KEY) as? [Int64] ?? []
        let cloudProgresses:[Int64] = store.array(forKey: C.CLOUD_PROGRESS_KEY) as? [Int64] ?? []
        let cloudCurrentLevelNr:Int64 = store.longLong(forKey: C.CLOUD_CURRENT_LEVEL_KEY)
        
        ////Swift.debugPrint("loadFromCloudKeyValueStore: \(scores)")
        ////Swift.debugPrint("loadFromCloudKeyValueStore: \(progresses)")
        ////Swift.debugPrint("loadFromCloudKeyValueStore: \(currentLevelNumber)")
        
        let localLevels = getAllLevels(currentLevel: -1)
        guard !isLocalProgressBetter(localLevels: localLevels, cloudProgresses: cloudProgresses) else { return }
      
        if !cloudScores.isEmpty && !cloudProgresses.isEmpty {
            let localLevels:[Level] = getAllLevels(currentLevel: -1)
            let areCountsEqual = (localLevels.count == cloudScores.count) && (cloudScores.count == cloudProgresses.count)
            if (areCountsEqual) {
                for level in localLevels {
                    let levelNr = Int(level.levelNumber) - 1
                    level.bestScore = cloudScores[levelNr]
                    level.progress = cloudProgresses[levelNr]
                    if (cloudProgresses[levelNr] == Level.Progress.completed.rawValue)
                    {
                        level.progress = Int64(Level.Progress.in_progress.rawValue)
                    }
                    if (level.levelNumber == cloudCurrentLevelNr)
                    {
                        level.timeModified = Date.init(timeIntervalSinceNow: .zero)
                    }
                    else {
                        level.timeModified = nil
                    }
                }
              
                ////Swift.debugPrint("Progresses from cloud: \(progresses)")
                ////Swift.debugPrint("Scores from cloud: \(scores)")
                do {
                    try context.save()
                } catch {
                    ////Swift.debugPrint("Error saving items from cloud: \(error)")
                }
            }
        }
    }
    
    
    func saveToCloudKeyValueStore() {
  
        let localLevels:[Level] = getAllLevels(currentLevel: -1)
        
        let store = NSUbiquitousKeyValueStore.default
        var cloudScores:[Int64] = store.array(forKey: C.CLOUD_BEST_SCORES_KEY) as? [Int64] ?? []
        var cloudProgresses:[Int64] = store.array(forKey: C.CLOUD_PROGRESS_KEY) as? [Int64] ?? []
        
        ////Swift.debugPrint("saveToCloudKeyValueStore: \(scores)")
        ////Swift.debugPrint("saveToCloudKeyValueStore: \(progresses)")
        
        // local progress should be better to save to cloud
        guard isLocalProgressBetter(localLevels: localLevels, cloudProgresses: cloudProgresses) else { return }
        
        cloudScores.removeAll()
        cloudProgresses.removeAll()
        
        if (!localLevels.isEmpty) {
            localLevels.forEach { level in
                cloudScores.append(level.bestScore)
                cloudProgresses.append(level.progress)
            }
            
            let store = NSUbiquitousKeyValueStore.default
            store.set(cloudProgresses, forKey: C.CLOUD_PROGRESS_KEY)
            store.set(cloudScores, forKey: C.CLOUD_BEST_SCORES_KEY)
            if let currentLevel {
                store.set(currentLevel.levelNumber, forKey: C.CLOUD_CURRENT_LEVEL_KEY)
            }
            
            ////Swift.debugPrint("Saving progresses: \(progresses)")
            ////Swift.debugPrint("Saving scores: \(scores)")
        }
        
    }
    
    
    // MARK: Change level
    func changeLevel(to nextLevel:Int = -1, toLatest loadLatestLevel:Bool = false, withProgressReset reset:Bool = false, levelPack:LevelPack)
    {
        var intervals:[Interval] = []
        var previousLevel = Database.shared.currentLevel?.levelNumber
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
                        let letter = LetterData(context: self.context)
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
                    let actualLevelNr = getActualLevelNumber(normalizedLevelNr: nextLevel, levelPack: levelPack)
                    let nextLevelRequest: NSFetchRequest<Level> = Level.fetchRequest()
                    nextLevelRequest.predicate = NSPredicate(format: "levelNumber = %@", String(actualLevelNr))
                    nextLevelRequest.fetchLimit = 1
                    let results = try context.fetch(nextLevelRequest)
                    if let chosenLevel = results.first {
                        currentLevel = chosenLevel
                        if (currentLevel?.progress == Int64(Level.Progress.locked.rawValue))
                        {
                           currentLevel?.progress = Int64(Level.Progress.in_progress.rawValue)
                        }
                    } else {
                        fatalError("Could not set next level nr \(nextLevel)")
                    }
                }
                
                
                // change the max time to the current level
                currentLevel?.timeModified = Date.init(timeIntervalSinceNow: .zero)
                
                // set game state based on progress
                switch Level.Progress(rawValue: Int(currentLevel!.progress)) {
                case .in_progress:
                    StateManager.shared.currentState = .play
                case .completed:
                    StateManager.shared.currentState = .completed
                case .locked:
                    fatalError("Level progress should not be .locked here.")
                case .none:
                    Swift.debugPrint("None progress?")
                }
                
                try context.save()
                
                // execute closures
                if (previousLevel != currentLevel?.levelNumber)
                {
                    levelChangedClosure?(intervals)
                }
                
                if (!loadLatestLevel)
                {
                    saveToCloudKeyValueStore()
                }
                
                checkSolutionCorrectness()
                
            } catch {
                // Handle the error
                ////Swift.debugPrint("Error: \(error)")
            }
        }
    }
    
    
    //MARK: Get total score
    private func getTotalScore(for currentLevel:Int) -> Int {
        var score = 0
        
        let levelPack = getLevelPack(for:currentLevel)
        let levelFirstAndLast = getRangeForLevelPack(pack: levelPack)
        let predicate = NSPredicate(format: "levelNumber >= %@ AND levelNumber <= %@", argumentArray: [levelFirstAndLast.0, levelFirstAndLast.1])
        
        let request: NSFetchRequest<Level> = Level.fetchRequest()
        request.predicate = predicate
        
        do {
            let results = try context.fetch(request) as [Level]
            results.forEach { level in
                score += Int(level.bestScore)
            }
        } catch {
            ////Swift.debugPrint("Error fetching results: \(error.localizedDescription)")
        }
        return score
    }
    
    
    //MARK: Submit score
    private func submitScoreToLeaderboard(score: Int, levelNr:Int) async throws {
        
        let leaderBoard = levelNr > 100 ? C.ANTIMATTER_LEADERBOARD : C.GAME_LEADERBOARD
        
        if GKLocalPlayer.local.isAuthenticated {
            GKLeaderboard.submitScore(
                score,
                context: 0,
                player: GKLocalPlayer.local,
                leaderboardIDs: [leaderBoard],
                completionHandler: { error in
                    if (error != nil) {
                        ////Swift.debugPrint("Could not submit score: \(String(describing: error))")
                    }
                }
            )
        } else {
            ////Swift.debugPrint("Player not authenticated to submit score.")
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
            ////Swift.debugPrint("Error getting maximum level from database.")
        }
        return -1
    }
    
    
    // MARK: Get all levels with progress
    func getAllLevelsProgresses(currentLevel:Int, pack:LevelPack? = nil) -> [Level.Progress]
    {
        let levelPack = pack == nil ? getLevelPack(for: currentLevel) : pack!
        let predicateFormat = "levelNumber >= %@ AND levelNumber <= %@"
        let levelFirstAndLast = getRangeForLevelPack(pack: levelPack)
        
        let request: NSFetchRequest<Level> = Level.fetchRequest()
        request.predicate = NSPredicate(format: predicateFormat, argumentArray: [levelFirstAndLast.0, levelFirstAndLast.1])
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
    

    
    // MARK: Get all levels (or level pack all levels)
    func getAllLevels(currentLevel:Int) -> [Level]
    {
        let predicate = currentLevel == -1 ? NSPredicate(format: "levelNumber <= %@", argumentArray: [250]) : NSPredicate(format: "levelNumber >= %@ AND levelNumber <= %@", argumentArray: [getRangeForLevelPack(pack: getLevelPack(for: currentLevel)).0, getRangeForLevelPack(pack: getLevelPack(for: currentLevel)).1])
        
        let request: NSFetchRequest<Level> = Level.fetchRequest()
        request.predicate = predicate
        let sortDescriptor = NSSortDescriptor(key: "levelNumber", ascending: true)
        request.sortDescriptors = [sortDescriptor]
        
        var levels:[Level] = []
        
        do {
            let results = try context.fetch(request) as [Level]
            results.forEach { level in
                levels.append(level)
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
            ////Swift.debugPrint("Error changing friend index: \(error)")
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
            ////Swift.debugPrint("Error switching friend indexes: \(error)")
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
            ////Swift.debugPrint("Could not fetch. \(error), \(error.userInfo)")
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
        ////Swift.debugPrint("handleWrongSolutionAchievementProgress")
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
        ////Swift.debugPrint("handleLevelCompleteAchievements")
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
        // Level pack 1
        else if (levelNr == 110)
        {
            unlockAchievement(identifier: C.symmetric_scout)
        }
        else if (levelNr == 125)
        {
            unlockAchievement(identifier: C.symmetric_sage)
        }
        // Level pack 2
        else if (levelNr == 135)
        {
            unlockAchievement(identifier: C.inverted_innocence)
        }
        else if (levelNr == 150)
        {
            unlockAchievement(identifier: C.inverted_intellectual)
        }
        // Level pack 3
        else if (levelNr == 160)
        {
            unlockAchievement(identifier: C.reversal_rookie)
        }
        else if (levelNr == 175)
        {
            unlockAchievement(identifier: C.reversal_renegade)
        }
        // Level pack 4
        else if (levelNr == 185)
        {
            unlockAchievement(identifier: C.echoed_enthusiast)
        }
        else if (levelNr == 200)
        {
            unlockAchievement(identifier: C.echoed_expert)
        }
        // Level pack 5
        else if (levelNr == 210)
        {
            unlockAchievement(identifier: C.twisted_trailblazer)
        }
        else if (levelNr == 225)
        {
            unlockAchievement(identifier: C.twisted_titan)
        }
        // Level pack 6
        else if (levelNr == 235)
        {
            unlockAchievement(identifier: C.looped_learner)
        }
        else if (levelNr == 250)
        {
            unlockAchievement(identifier: C.looped_luminary)
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
                ////Swift.debugPrint("Achievement unlock error: \(error.localizedDescription)")
            } else {
                // Achievement unlocked successfully
                ////Swift.debugPrint("Achievement unlocked: \(identifier)")
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
            ////Swift.debugPrint("Error changing score: \(error)")
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
            ////Swift.debugPrint("Error changing columnd count: \(error)")
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
            ////Swift.debugPrint("Error changing category rewarded: \(error)")
        }
    }
    
    
    // MARK: Update letter locked status
    func lockLetter(letter:LetterData)
    {
        letter.locked = true
        
        do {
            try context.save()
        } catch {
            ////Swift.debugPrint("Error locking letter: \(error)")
        }
        
    }
    
    
    // MARK: Set level as completed
    func setLevelCompletedImmediately()
    {
        let levelNr = Int(currentLevel!.levelNumber)
        currentLevel!.bestScore = max(currentLevel!.score, currentLevel!.bestScore)
        currentLevel!.progress = Int64(Level.Progress.completed.rawValue)
       
        handleLevelCompleteAchievements(
                isMaxScore: currentLevel!.score == currentLevel!.shuffledPuzzle.maxScore(),
                levelNr:levelNr
            )
        
        do {
            try context.save()
        } catch {
            ////Swift.debugPrint("Error setting level completed: \(error)")
        }
     
        let totalScore = getTotalScore(for: levelNr)
        
        Task {
            do {
                try await submitScoreToLeaderboard(score: totalScore, levelNr: levelNr)
            } catch {
                ////Swift.debugPrint("Error:", error.localizedDescription)
            }
        }
        
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
            ////Swift.debugPrint("Error while trying to count friendIndexes")
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
