//
//  DefaultTheme.swift
//  mars
//
//  Created by Mina on 07.09.2023.
//

import Foundation
import UIKit

struct DefaultTheme : Palette {
    
    var isLightMode: Bool = true
    
    init(isLightMode: Bool) {
        self.isLightMode = isLightMode
    }
    
    // light mode
    var primary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF676000) : UIColor(rgb: 0xFFD7CA18)}
    var onPrimary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF353100)}
    var primaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFF4E63B) : UIColor(rgb: 0xFF4D4800)}
    var onPrimaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF1F1C00) : UIColor(rgb: 0xFFF4E63B)}
    var secondary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF635F41) : UIColor(rgb: 0xFFCDC7A3)}
    var onSecondary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF343117)}
    var secondaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFE9E3BE) : UIColor(rgb: 0xFF4B482C)}
    var onSecondaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF1E1C05) : UIColor(rgb: 0xFFE9E3BE)}
    var tertiary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF3F6653) : UIColor(rgb: 0xFFA6D0B9)}
    var onTertiary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF0F3727)}
    var tertiaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFC1ECD4) : UIColor(rgb: 0xFF284E3D)}
    var onTertiaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF002114) : UIColor(rgb: 0xFFC1ECD4)}
    var error: UIColor { self.isLightMode ? UIColor(rgb: 0xFFBA1A1A) : UIColor(rgb: 0xFFFFB4AB)}
    var errorContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFDAD6) : UIColor(rgb: 0xFF93000A)}
    var onError: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF690005)}
    var onErrorContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF410002) : UIColor(rgb: 0xFFFFDAD6)}
    var background: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFBFF) : UIColor(rgb: 0xFF1D1C16)}
    var onBackground: UIColor { self.isLightMode ? UIColor(rgb: 0xFF1D1C16) : UIColor(rgb: 0xFFE6E2D9)}
    var surface: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFBFF) : UIColor(rgb: 0xFF1D1C16)}
    var onSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFF1D1C16) : UIColor(rgb: 0xFFE6E2D9)}
    var surfaceVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFFE7E3D0) : UIColor(rgb: 0xFF49473A)}
    var onSurfaceVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFF49473A) : UIColor(rgb: 0xFFCBC7B5)}
    var outline: UIColor { self.isLightMode ? UIColor(rgb: 0xFF7A7768) : UIColor(rgb: 0xFF949181)}
    var inverseOnSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFFF5F0E7) : UIColor(rgb: 0xFF1D1C16)}
    var inverseSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFF32302B) : UIColor(rgb: 0xFFE6E2D9)}
    var inversePrimary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFD7CA18) : UIColor(rgb: 0xFF676000)}
    var shadow: UIColor { self.isLightMode ? UIColor(rgb: 0xFF000000) : UIColor(rgb: 0xFF000000)}
    var surfaceTint: UIColor { self.isLightMode ? UIColor(rgb: 0xFF676000) : UIColor(rgb: 0xFFD7CA18)}
    var outlineVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFFCBC7B5) : UIColor(rgb: 0xFF49473A)}
    var scrim: UIColor { self.isLightMode ? UIColor(rgb: 0xFF000000) : UIColor(rgb: 0xFF000000)}

}
