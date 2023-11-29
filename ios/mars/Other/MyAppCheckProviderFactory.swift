//
//  AppCheckProviderFactory.swift
//  mars
//
//  Created by Mina on 29.11.2023.
//
import FirebaseAppCheck

import Foundation
import FirebaseCore

class MyAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
  func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
    return AppAttestProvider(app: app)
  }
}
