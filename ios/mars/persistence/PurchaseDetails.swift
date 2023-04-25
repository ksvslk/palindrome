//
//  PurchaseDetails.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation

struct PurchaseDetails: Codable {
    let sku: String
    let title: String
    let price: String
    let canPurchase: Bool
    let isPurchased: Bool
}
