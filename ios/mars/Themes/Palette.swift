//
//  Theme.swift
//  test
//
//  Created by ksvslk on 24.04.2023.
//

import Foundation
import UIKit


protocol Palette {
    
    var isLightMode:Bool { get }
    
    init(isLightMode: Bool)
 
    var primary: UIColor { get }
    var onPrimary: UIColor { get }
    var primaryContainer: UIColor { get }
    var onPrimaryContainer: UIColor { get }
    var secondary: UIColor { get }
    var onSecondary: UIColor { get }
    var secondaryContainer: UIColor { get }
    var onSecondaryContainer: UIColor { get }
    var tertiary: UIColor { get }
    var onTertiary: UIColor { get }
    var tertiaryContainer: UIColor { get }
    var onTertiaryContainer: UIColor { get }
    var error: UIColor { get }
    var errorContainer: UIColor { get }
    var onError: UIColor { get }
    var onErrorContainer: UIColor { get }
    var background: UIColor { get }
    var onBackground: UIColor { get }
    var surface: UIColor { get }
    var onSurface: UIColor { get }
    var surfaceVariant: UIColor { get }
    var onSurfaceVariant: UIColor { get }
    var outline: UIColor { get }
    var inverseOnSurface: UIColor { get }
    var inverseSurface: UIColor { get }
    var inversePrimary: UIColor { get }
    var shadow: UIColor { get }
    var surfaceTint: UIColor { get }
    var outlineVariant: UIColor { get }
    var scrim: UIColor { get }

}
