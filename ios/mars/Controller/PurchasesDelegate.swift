//
//  PurchasesDelegate.swift
//  mars
//
//  Created by Mina on 18.05.2023.
//

import Foundation

protocol PurchasesDelegate: AnyObject {
    func purchasesChanged(ownedProducts: [String])
}
