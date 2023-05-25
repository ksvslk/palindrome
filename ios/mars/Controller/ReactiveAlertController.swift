//
//  AdAwareAlertController.swift
//  mars
//
//  Created by Mina on 18.05.2023.
//

import Foundation
import UIKit
import StoreKit

class ReactiveAlertController: UIAlertController, AdAvailabilityDelegate {

    var premiumHint: Bool = false
    var premiumLetter: Bool = false

    
    func adAvailabilityDidChange(isAvailable: Bool) {
        // Update the actions of the UIAlertController here
        updateAdsActions(adAvailable: isAvailable)
    }
    
    
    func updateAdsActions(adAvailable: Bool) {
        // Implement your logic to update the actions based on ad availability
        // For example, enable/disable specific actions
        print("Update actions for ad aware alert controller. Ad available: \(adAvailable)")
        print("Premium hint: \(premiumHint)")
        print("Premium letter: \(premiumLetter)")
               
        // Example:
        if let letterAdsAction = actions.first(where: { $0.accessibilityIdentifier == "watch_letter_ad" }), !premiumLetter {
            letterAdsAction.setValue(adAvailable ? "watch_letter_ad"~ : "ad_not_available"~, forKey: "title")
            letterAdsAction.isEnabled = adAvailable
        }
    
        if let hintAdsAction = actions.first(where: { $0.accessibilityIdentifier == "watch_hint_ad" }), !premiumHint {
            hintAdsAction.setValue(adAvailable ? "watch_hint_ad"~ : "ad_not_available"~, forKey: "title")
            hintAdsAction.isEnabled = adAvailable
        }
        
        if premiumHint {
            updateHintsToPremium()
        }
        
        if premiumLetter {
            updateLettersToPremium()
        }
    }
    
    
    func updateHintsToPremium()
    {
        if let hintAdsAction = actions.first(where: { $0.accessibilityIdentifier == "watch_hint_ad" }) {
            hintAdsAction.setValue("reveal_hint"~, forKey: "title")
            hintAdsAction.setValue(nil, forKey: "image")
            hintAdsAction.isEnabled = true
        }
    }
    
    
    func updateLettersToPremium()
    {
        if let letterAdsAction = actions.first(where: { $0.accessibilityIdentifier == "watch_letter_ad" }) {
            letterAdsAction.setValue("place_letter"~, forKey: "title")
            letterAdsAction.setValue(nil, forKey: "image")
            letterAdsAction.isEnabled = true
        }
    }
 
    
}
