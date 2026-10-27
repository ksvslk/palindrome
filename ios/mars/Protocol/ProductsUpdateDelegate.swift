//
//  ProductsUpdateDelegate.swift
//  mars
//
//  Created by Mina on 25.05.2023.
//

import Foundation

protocol ProductsUpdateDelegate: AnyObject {
    func didUpdateProducts(withProducts products: [String:ProductInfo])
}
