//
//  Interval+CoreDataProperties.swift
//  mars
//
//  Created by ksvslk on 27.04.2023.
//
//

import Foundation
import CoreData


extension Interval {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<Interval> {
        return NSFetchRequest<Interval>(entityName: "Interval")
    }

    @NSManaged public var currentCount: Int64
    @NSManaged public var limitCount: Int64
    @NSManaged public var name: String
    @NSManaged public var timeStamp: Date?

}
