//
//  Level+CoreDataProperties.swift
//  mars
//
//  Created by ksvslk on 27.04.2023.
//
//

import Foundation
import CoreData


extension Level {
    
    enum Progress: Int {
        case todo = 1
        case in_progress = 2
        case replay = 3
        case intermediate = 4
    }
    
    @nonobjc public class func fetchRequest() -> NSFetchRequest<Level> {
        return NSFetchRequest<Level>(entityName: "Level")
    }
    
    @NSManaged public var author: String
    @NSManaged public var bestScore: Int64
    @NSManaged public var categoryRewarded: Bool
    @NSManaged public var columnCount: Int64
    @NSManaged public var levelNumber: Int64
    @NSManaged public var progress: Int64
    @NSManaged public var score: Int64
    @NSManaged public var timeModified: Date?
    @NSManaged public var letters: NSSet
    @NSManaged public var solutions: NSSet
    
    public var lettersArr: [LetterData] {
        letters.allObjects as? [LetterData] ?? []
    }
    
    public var solutionsArr: [Solution] {
        solutions.allObjects as? [Solution] ?? []
    }
    
    var levelProgress: Progress {
        return Progress(rawValue: Int(progress)) ?? Progress.todo
    }

    public override var debugDescription: String {
        return "Level: \(levelNumber) \n Letters: \(lettersArr) \n Solutions: \(solutionsArr) \n Progress: \(String(describing: Level.Progress(rawValue: Int(progress)))) \n Columns: \(columnCount) \n Score: \(score) \n Best score: \(bestScore) \n Modified: \(String(describing: timeModified)) \n Category rewarded: \(categoryRewarded) \n Author: \(author)"
    }

}

// MARK: Generated accessors for letters
extension Level {

    @objc(addLettersObject:)
    @NSManaged public func addToLetters(_ value: LetterData)

    @objc(removeLettersObject:)
    @NSManaged public func removeFromLetters(_ value: LetterData)

    @objc(addLetters:)
    @NSManaged public func addToLetters(_ values: NSSet)

    @objc(removeLetters:)
    @NSManaged public func removeFromLetters(_ values: NSSet)

}

// MARK: Generated accessors for solutions
extension Level {

    @objc(addSolutionsObject:)
    @NSManaged public func addToSolutions(_ value: Solution)

    @objc(removeSolutionsObject:)
    @NSManaged public func removeFromSolutions(_ value: Solution)

    @objc(addSolutions:)
    @NSManaged public func addToSolutions(_ values: NSSet)

    @objc(removeSolutions:)
    @NSManaged public func removeFromSolutions(_ values: NSSet)

}
