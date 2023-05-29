//
//  PurchaseProcessDelegate.swift
//  mars
//
//  Created by Mina on 29.05.2023.
//

import Foundation

protocol TransactionProcessDelegate: AnyObject {
    func didFinishProcessing(result:Bool)
    func didStartProcessing()
}
