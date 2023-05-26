//
//  AdAwareAlertController.swift
//  mars
//
//  Created by Mina on 18.05.2023.
//

import Foundation
import UIKit
import StoreKit

class ReactiveAlertController: UIAlertController, AdAvailabilityDelegate, ProductsUpdateDelegate {
    
    var premiumHint: Bool = false
    var premiumLetter: Bool = false
    
    func didUpdateProducts(withProducts products: [String : ProductInfo]) {

        let totalNumberOfProductsPurchased = products.filter { dictElement in
            dictElement.value.isPurchased
        }.count
        // gather all cases for the purchases
        let allInPurchased = products.keys.contains(C.PURCHASE_ALL_ID)
        let onlyHintsPurchased = products.keys.contains(C.PURCHASE_HINTS_ID) && totalNumberOfProductsPurchased == 1
        let onlyLettersPurchased = products.keys.contains(C.PURCHASE_LETTERS_ID) && totalNumberOfProductsPurchased == 1
        let onlyAdsPurchased = products.keys.contains(C.PURCHASE_NO_ADS_ID) && totalNumberOfProductsPurchased == 1
        let hintsAndLettersAndAdsPurchased = [C.PURCHASE_HINTS_ID, C.PURCHASE_LETTERS_ID, C.PURCHASE_NO_ADS_ID].allSatisfy { products.keys.contains($0) }
        let hintsAndLettersPurchased = [C.PURCHASE_HINTS_ID, C.PURCHASE_LETTERS_ID].allSatisfy { products.keys.contains($0) }
        let hintsAndAds = [C.PURCHASE_HINTS_ID, C.PURCHASE_NO_ADS_ID].allSatisfy { products.keys.contains($0) }
        let lettersAndAds = [C.PURCHASE_LETTERS_ID, C.PURCHASE_NO_ADS_ID].allSatisfy { products.keys.contains($0) }
        
        // update buttons UI (with title and price)
        products.forEach { (productId: String, info: ProductInfo) in
            if let purchaseButton = actions.first(where: { $0.accessibilityIdentifier == productId }) {
                
                purchaseButton.setValue(info.localizedTitleWithPrice, forKey: "title")
                purchaseButton.isEnabled = true
            }
        }
    
    }
    
    
    private func updateAllInPurchased()
    {
        premiumHint = true
        premiumLetter = true
        
    }
    
    
    func adAvailabilityDidChange(isAvailable: Bool) {
        updateAdsActions(adAvailable: isAvailable)
    }
    
    
    func updateAdsActions(adAvailable: Bool) {
        
        // Implement your logic to update the actions based on ad availability
        // For example, enable/disable specific actions
        print("Update actions for ad aware alert controller. Ad available: \(adAvailable)")
        print("Premium hint: \(premiumHint)")
        print("Premium letter: \(premiumLetter)")
        
        // Update ads buttons only if no premium
        if let letterAdsAction = actions.first(where: { $0.accessibilityIdentifier == "watch_letter_ad" }), !premiumLetter {
            letterAdsAction.setValue(adAvailable ? "watch_letter_ad"~ : "ad_not_available"~, forKey: "title")
            letterAdsAction.isEnabled = adAvailable
        }
        
        if let hintAdsAction = actions.first(where: { $0.accessibilityIdentifier == "watch_hint_ad" }), !premiumHint {
            hintAdsAction.setValue(adAvailable ? "watch_hint_ad"~ : "ad_not_available"~, forKey: "title")
            hintAdsAction.isEnabled = adAvailable
        }
        
        
        // purchased OR unlocked with ad
        if premiumHint {
            updateHintsToPremium()
        }
        
        
        // uunlocked by purchase
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
