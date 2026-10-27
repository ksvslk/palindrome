//
//  DefaultTheme.swift
//  mars
//
//  Created by Mina on 07.09.2023.
//

import Foundation
import UIKit

struct PurpleTheme : Palette {
    
    var isLightMode: Bool = true
    
    init(isLightMode: Bool) {
        self.isLightMode = isLightMode
    }
    
    var primary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF6750A4) : UIColor(rgb: 0xFFD0BCFF)}
    var onPrimary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF381E72)}
    var primaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFEADDFF) : UIColor(rgb: 0xFF4F378B)}
    var onPrimaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF21005D) : UIColor(rgb: 0xFFEADDFF)}
    var secondary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF625B71) : UIColor(rgb: 0xFFCCC2DC)}
    var onSecondary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF332D41)}
    var secondaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFE8DEF8) : UIColor(rgb: 0xFF4A4458)}
    var onSecondaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF1D192B) : UIColor(rgb: 0xFFE8DEF8)}
    var tertiary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF7D5260) : UIColor(rgb: 0xFFEFB8C8)}
    var onTertiary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF492532)}
    var tertiaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFD8E4) : UIColor(rgb: 0xFF633B48)}
    var onTertiaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF31111D) : UIColor(rgb: 0xFFFFD8E4)}
    var error: UIColor { self.isLightMode ? UIColor(rgb: 0xFFB3261E) : UIColor(rgb: 0xFFF2B8B5)}
    var errorContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFF9DEDC) : UIColor(rgb: 0xFFF9DEDC)}
    var onError: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF601410)}
    var onErrorContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF410E0B) : UIColor(rgb: 0xFFF9DEDC)}
    var outline: UIColor { self.isLightMode ? UIColor(rgb: 0xFF79747E) : UIColor(rgb: 0xFF938F99)}
    
    var background: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFBFE) : UIColor(rgb: 0xFF1C1B1F)}
    var onBackground: UIColor { self.isLightMode ? UIColor(rgb: 0xFF1C1B1F) : UIColor(rgb: 0xFFE6E1E5)}
    var surface: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFBFE) : UIColor(rgb: 0xFF1C1B1F)}
    var onSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFF1C1B1F) : UIColor(rgb: 0xFFE6E1E5)}
    var surfaceVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFFE7E0EC) : UIColor(rgb: 0xFF49454F)}
    var onSurfaceVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFF49454F) : UIColor(rgb: 0xFFCAC4D0)}
    
    var inverseSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFF313033) : UIColor(rgb: 0xFFE6E1E5)}
    var inverseOnSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFFF4EFF4) : UIColor(rgb: 0xFF313033)}
    var inversePrimary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFD0BCFF) : UIColor(rgb: 0xFF6750A4)}
    var shadow: UIColor { self.isLightMode ? UIColor(rgb: 0xFF000000) : UIColor(rgb: 0xFF000000)}
    var surfaceTint: UIColor { self.isLightMode ? UIColor(rgb: 0xFF6750A4) : UIColor(rgb: 0xFFD0BCFF)}
    var outlineVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFFCAC4D0) : UIColor(rgb: 0xFF49454F)}
    var scrim: UIColor { self.isLightMode ? UIColor(rgb: 0xFF000000) : UIColor(rgb: 0xFF000000)}

}
