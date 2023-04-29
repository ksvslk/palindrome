//
//  LetterData+CoreDataProperties.swift
//  mars
//
//  Created by Mina on 29.04.2023.
//
//

import Foundation
import CoreData


extension LetterData {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<LetterData> {
        return NSFetchRequest<LetterData>(entityName: "LetterData")
    }

    @NSManaged public var friendIndex: Int64
    @NSManaged public var homeIndex: Int64
    @NSManaged public var letter: String
    @NSManaged public var locked: Bool
    @NSManaged public var level: Level

    public override var debugDescription: String {
        return "\(letter) - (Home: \(homeIndex), Friend: \(friendIndex))"
    }
    
}
