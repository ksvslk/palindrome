//
//  Solution+CoreDataProperties.swift
//  mars
//
//  Created by ksvslk on 27.04.2023.
//
//

import Foundation
import CoreData


extension Solution {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<Solution> {
        return NSFetchRequest<Solution>(entityName: "Solution")
    }

    @NSManaged public var hint: String
    @NSManaged public var palindrome: String
    @NSManaged public var solved: Bool
    @NSManaged public var level: Level
    
    public override var debugDescription: String {
        return "\(palindrome) - hint: \(hint)"
    }

}
