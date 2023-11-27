//
//  GreenTheme.swift
//  mars
//
//  Created by Mina on 27.11.2023.
//

import Foundation
import UIKit

struct GreenTheme : Palette {
    
    var isLightMode: Bool = true
    
    init(isLightMode: Bool) {
        self.isLightMode = isLightMode
    }
    
    var primary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF006C47) : UIColor(rgb: 0xFF3DE09C)}
    var onPrimary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF003823)}
    var primaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF62FDB7) : UIColor(rgb: 0xFF005234)}
    var onPrimaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF002112) : UIColor(rgb: 0xFF62FDB7)}
    var secondary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF4D6356) : UIColor(rgb: 0xFFB4CCBC)}
    var onSecondary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF203529)}
    var secondaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFD0E8D7) : UIColor(rgb: 0xFF364B3F)}
    var onSecondaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF0B1F15) : UIColor(rgb: 0xFFD0E8D7)}
    var tertiary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF3C6472) : UIColor(rgb: 0xFFA4CDDD)}
    var onTertiary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF053542)}
    var tertiaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFC0E9FA) : UIColor(rgb: 0xFF234C59)}
    var onTertiaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF001F28) : UIColor(rgb: 0xFFC0E9FA)}
    var error: UIColor { self.isLightMode ? UIColor(rgb: 0xFFBA1A1A) : UIColor(rgb: 0xFFFFB4AB)}
    var errorContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFDAD6) : UIColor(rgb: 0xFF93000A)}
    var onError: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF690005)}
    var onErrorContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF410002) : UIColor(rgb: 0xFFFFDAD6)}
    var outline: UIColor { self.isLightMode ? UIColor(rgb: 0xFF707972) : UIColor(rgb: 0xFF8A938C)}

    var background: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFBFDF8) : UIColor(rgb: 0xFF191C1A)}
    var onBackground: UIColor { self.isLightMode ? UIColor(rgb: 0xFF191C1A) : UIColor(rgb: 0xFFE1E3DF)}
    var surface: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFBFDF8) : UIColor(rgb: 0xFF191C1A)}
    var onSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFF191C1A) : UIColor(rgb: 0xFFE1E3DF)}
    var surfaceVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFFDCE5DC) : UIColor(rgb: 0xFF404943)}
    var onSurfaceVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFF404943) : UIColor(rgb: 0xFFC0C9C1)}

    var inverseSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFF2E312E) : UIColor(rgb: 0xFFE1E3DF)}
    var inverseOnSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFFEFF1ED) : UIColor(rgb: 0xFF191C1A)}
    var inversePrimary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF3DE09C) : UIColor(rgb: 0xFF006C47)}
    var shadow: UIColor { self.isLightMode ? UIColor(rgb: 0xFF000000) : UIColor(rgb: 0xFF000000)}
    var surfaceTint: UIColor { self.isLightMode ? UIColor(rgb: 0xFF006C47) : UIColor(rgb: 0xFF3DE09C)}
    var outlineVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFFC0C9C1) : UIColor(rgb: 0xFF404943)}
    var scrim: UIColor { self.isLightMode ? UIColor(rgb: 0xFF000000) : UIColor(rgb: 0xFF000000)}


}
