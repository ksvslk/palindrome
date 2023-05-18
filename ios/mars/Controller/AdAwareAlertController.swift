//
//  AdAwareAlertController.swift
//  mars
//
//  Created by Mina on 18.05.2023.
//

import Foundation
import UIKit

class AdAwareAlertController: UIAlertController, AdAvailabilityDelegate, PurchasesDelegate {
    
    func purchasesChanged(ownedProducts: [String]) {
        print("purchasesChanged: \(ownedProducts)")
    }
    
    
    func adAvailabilityDidChange(isAvailable: Bool) {
        // Update the actions of the UIAlertController here
        updateAdsActions(adAvailable: isAvailable)
    }
    
    func updateAdsActions(adAvailable: Bool) {
        // Implement your logic to update the actions based on ad availability
        // For example, enable/disable specific actions
        print("Update actions for ad aware alert controller. Ad available: \(adAvailable)")
               
        // Example:
        if let letterAdsAction = actions.first(where: { $0.accessibilityIdentifier == "watch_letter_ad" }) {
            letterAdsAction.setValue(adAvailable ? "watch_letter_ad"~ : "ad_not_available"~, forKey: "title")
            letterAdsAction.isEnabled = adAvailable
        }
        
        if let hintAdsAction = actions.first(where: { $0.accessibilityIdentifier == "watch_hint_ad" }) {
            hintAdsAction.setValue(adAvailable ? "watch_hint_ad"~ : "ad_not_available"~, forKey: "title")
            hintAdsAction.isEnabled = adAvailable
        }
    }
    
}
