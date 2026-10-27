//
//  MessageDelegate.swift
//  mars
//
//  Created by Mina on 02.06.2023.
//

import Foundation

protocol MessageDelegate: AnyObject {
    func didSendNewMessage(withMessage message: String)
}
