//
//  BlueTheme.swift
//  mars
//
//  Created by Mina on 27.11.2023.
//

import Foundation
import UIKit


struct BlueTheme : Palette {
    
    var isLightMode: Bool = true
    
    init(isLightMode: Bool) {
        self.isLightMode = isLightMode
    }
    var primary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF006780) : UIColor(rgb: 0xFF4FD5FF)}
    var onPrimary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF003543)}
    var primaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFB7EAFF) : UIColor(rgb: 0xFF004E61)}
    var onPrimaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF001F28) : UIColor(rgb: 0xFFB7EAFF)}
    var secondary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF4C626B) : UIColor(rgb: 0xFFB3CAD4)}
    var onSecondary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF1E333B)}
    var secondaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFCFE6F1) : UIColor(rgb: 0xFF344A52)}
    var onSecondaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF071E26) : UIColor(rgb: 0xFFCFE6F1)}
    var tertiary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF5A5B7E) : UIColor(rgb: 0xFFC3C3EB)}
    var onTertiary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF2C2E4D)}
    var tertiaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFE1E0FF) : UIColor(rgb: 0xFF434465)}
    var onTertiaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF171837) : UIColor(rgb: 0xFFE1E0FF)}
    var error: UIColor { self.isLightMode ? UIColor(rgb: 0xFFBA1A1A) : UIColor(rgb: 0xFFFFB4AB)}
    var errorContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFDAD6) : UIColor(rgb: 0xFF93000A)}
    var onError: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF690005)}
    var onErrorContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF410002) : UIColor(rgb: 0xFFFFDAD6)}
    var outline: UIColor { self.isLightMode ? UIColor(rgb: 0xFF70787C) : UIColor(rgb: 0xFF8A9296)}
    
    var background: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFBFCFE) : UIColor(rgb: 0xFF191C1D)}
    var onBackground: UIColor { self.isLightMode ? UIColor(rgb: 0xFF191C1D) : UIColor(rgb: 0xFFE1E3E4)}
    var surface: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFBFCFE) : UIColor(rgb: 0xFF191C1D)}
    var onSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFF191C1D) : UIColor(rgb: 0xFFE1E3E4)}
    var surfaceVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFFDBE4E8) : UIColor(rgb: 0xFF40484C)}
    var onSurfaceVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFF40484C) : UIColor(rgb: 0xFFBFC8CC)}
    
    var inverseSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFF2E3132) : UIColor(rgb: 0xFFE1E3E4)}
    var inverseOnSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFFEFF1F2) : UIColor(rgb: 0xFF191C1D)}
    var inversePrimary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF4FD5FF) : UIColor(rgb: 0xFF006780)}
    var shadow: UIColor { self.isLightMode ? UIColor(rgb: 0xFF000000) : UIColor(rgb: 0xFF000000)}
    var surfaceTint: UIColor { self.isLightMode ? UIColor(rgb: 0xFF006780) : UIColor(rgb: 0xFF4FD5FF)}
    var outlineVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFFBFC8CC) : UIColor(rgb: 0xFF40484C)}
    var scrim: UIColor { self.isLightMode ? UIColor(rgb: 0xFF000000) : UIColor(rgb: 0xFF000000)}
    
}
