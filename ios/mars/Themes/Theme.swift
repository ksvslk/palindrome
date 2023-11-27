//
//  ThemeManager.swift
//  mars
//
//  Created by Mina on 06.09.2023.
//

import Foundation
import UIKit

class Theme {

    var listeners: [() -> Void] = []
    
    var colors:Palette = PurpleTheme(isLightMode: UIScreen.main.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.light)
    var currentTheme: ThemeName = .purple
    
    var yantraBold : String  = "Yantramanav-Bold"
    var robotoMedium : String  = "Roboto-Medium"
    var yantraMedium : String = "Yantramanav-Medium"
    
    static let shared: Theme = {
        let instance = Theme()
        return instance
    }()
    
    
    func changeTheme(name:ThemeName?)
    {
        currentTheme = name ?? currentTheme
        let isLightMode = UIScreen.main.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.light
        switch currentTheme {
        case .purple:
            colors = PurpleTheme(isLightMode: isLightMode)
        case .yellow:
            colors = YellowTheme(isLightMode: isLightMode)
        case .green:
            colors = GreenTheme(isLightMode: isLightMode)
        case .blue:
            colors = BlueTheme(isLightMode: isLightMode)
        case .red:
            colors = RedTheme(isLightMode: isLightMode)
        }
        notifyListeners()
    }
    

    func addListener(_ listener: @escaping () -> Void) {
           listeners.append(listener)
    }
    
    private func notifyListeners() {
          for listener in listeners {
              listener()
          }
    }
    
}
