//
//  Solution+CoreDataProperties.swift
//  mars
//
//  Created by Mina on 29.04.2023.
//
//

import Foundation
import CoreData


extension Solution {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<Solution> {
        return NSFetchRequest<Solution>(entityName: "Solution")
    }

    @NSManaged public var hint: String
    @NSManaged public var hint_cn: String
    @NSManaged public var hint_de: String
    @NSManaged public var hint_es: String
    @NSManaged public var hint_et: String
    @NSManaged public var hint_fi: String
    @NSManaged public var hint_fr: String
    @NSManaged public var hint_id: String
    @NSManaged public var hint_it: String
    @NSManaged public var hint_ja: String
    @NSManaged public var hint_ko: String
    @NSManaged public var hint_nl: String
    @NSManaged public var hint_pl: String
    @NSManaged public var hint_pt: String
    @NSManaged public var hint_ro: String
    @NSManaged public var hint_sv: String
    @NSManaged public var hint_tr: String
    @NSManaged public var hint_uk: String
    
    @NSManaged public var palindrome: String
    @NSManaged public var solved: Bool
    @NSManaged public var level: Level
    
    public override var debugDescription: String {
        return "\(palindrome) - hint: \(hint)"
    }

}
