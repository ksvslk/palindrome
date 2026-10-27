//
//  RedTheme.swift
//  mars
//
//  Created by Mina on 27.11.2023.
//

import Foundation
import UIKit
struct RedTheme : Palette {
    
    var isLightMode: Bool = true
    
    init(isLightMode: Bool) {
        self.isLightMode = isLightMode
    }

    var primary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFBD0040) : UIColor(rgb: 0xFFFFB2B9)}
    var onPrimary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF67001F)}
    var primaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFDADC) : UIColor(rgb: 0xFF91002F)}
    var onPrimaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF400010) : UIColor(rgb: 0xFFFFDADC)}
    var secondary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF765659) : UIColor(rgb: 0xFFE5BDBF)}
    var onSecondary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF43292C)}
    var secondaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFDADC) : UIColor(rgb: 0xFF5C3F42)}
    var onSecondaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF2C1517) : UIColor(rgb: 0xFFFFDADC)}
    var tertiary: UIColor { self.isLightMode ? UIColor(rgb: 0xFF775930) : UIColor(rgb: 0xFFE8C08E)}
    var onTertiary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF442B06)}
    var tertiaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFDDB6) : UIColor(rgb: 0xFF5D411B)}
    var onTertiaryContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF2A1800) : UIColor(rgb: 0xFFFFDDB6)}
    var error: UIColor { self.isLightMode ? UIColor(rgb: 0xFFBA1A1A) : UIColor(rgb: 0xFFFFB4AB)}
    var errorContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFDAD6) : UIColor(rgb: 0xFF93000A)}
    var onError: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFFFF) : UIColor(rgb: 0xFF690005)}
    var onErrorContainer: UIColor { self.isLightMode ? UIColor(rgb: 0xFF410002) : UIColor(rgb: 0xFFFFDAD6)}
    var outline: UIColor { self.isLightMode ? UIColor(rgb: 0xFF857374) : UIColor(rgb: 0xFF9F8C8D)}

    var background: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFBFF) : UIColor(rgb: 0xFF201A1A)}
    var onBackground: UIColor { self.isLightMode ? UIColor(rgb: 0xFF201A1A) : UIColor(rgb: 0xFFECE0E0)}
    var surface: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFFBFF) : UIColor(rgb: 0xFF201A1A)}
    var onSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFF201A1A) : UIColor(rgb: 0xFFECE0E0)}
    var surfaceVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFFF4DDDE) : UIColor(rgb: 0xFF524344)}
    var onSurfaceVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFF524344) : UIColor(rgb: 0xFFD7C1C2)}

    var inverseSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFF362F2F) : UIColor(rgb: 0xFFECE0E0)}
    var inverseOnSurface: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFBEEEE) : UIColor(rgb: 0xFF201A1A)}
    var inversePrimary: UIColor { self.isLightMode ? UIColor(rgb: 0xFFFFB2B9) : UIColor(rgb: 0xFFBD0040)}
    var shadow: UIColor { self.isLightMode ? UIColor(rgb: 0xFF000000) : UIColor(rgb: 0xFF000000)}
    var surfaceTint: UIColor { self.isLightMode ? UIColor(rgb: 0xFFBD0040) : UIColor(rgb: 0xFFFFB2B9)}
    var outlineVariant: UIColor { self.isLightMode ? UIColor(rgb: 0xFFD7C1C2) : UIColor(rgb: 0xFF524344)}
    var scrim: UIColor { self.isLightMode ? UIColor(rgb: 0xFF000000) : UIColor(rgb: 0xFF000000)}

}
