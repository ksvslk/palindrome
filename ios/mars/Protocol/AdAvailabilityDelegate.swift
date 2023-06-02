//
//  AdAvailabilityDelegate.swift
//  mars
//
//  Created by Mina on 18.05.2023.
//

import Foundation

protocol AdAvailabilityDelegate: AnyObject {
    func adAvailabilityDidChange(isAvailable: Bool)
}
