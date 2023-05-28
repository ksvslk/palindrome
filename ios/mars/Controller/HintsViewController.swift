//
//  HintsViewController.swift
//  mars
//
//  Created by Mina on 27.05.2023.
//

import Foundation
import UIKit
import SpriteKit

class HintsViewController: BaseMenuController, AdAvailabilityDelegate, ProductsUpdateDelegate {
    
    weak var adManager: AdManager?
    weak var purchaseManager: PurchaseManager?
    weak var gameViewController: GameViewController?
    
    // helper for members array to get location of some button in table. Manually matched to the members array!
    private enum Button: Int, CaseIterable {
        case reveal_clue_btn = 0
        case place_letter_btn = 1
        case purchase_clues_btn = 2
        case purchase_letters_btn = 3
        case purchase_no_ads_btn = 4
        case purchase_all_btn = 5
    }
    
    init(adManager:AdManager?, purchaseManager:PurchaseManager?, gameViewController:GameViewController?) {
        print("init hints")
        self.gameViewController = gameViewController
        self.adManager = adManager
        self.purchaseManager = purchaseManager
        super.init(nibName: nil, bundle: nil)
    }
    
    
    //MARK: View did appear
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        self.purchaseManager?.delegate = self
        self.purchaseManager?.sendLatestInfo()
        self.adManager?.delegate = self
        self.adManager?.sendLatestInfo()
        
        if (Database.shared.currentLevel?.categoryRewarded == true)
        {
            updateHintsToPremium()
        }
    }
    
    
    //MARK: Hints ads -> premium
    private func updateHintsToPremium()
    {
        updateCell(
            of: .reveal_clue_btn,
            withName: C.revealClueButton.0,
            withComment: "instantly_comment"~,
            withCommentColors: (Theme.successBoxColor,Theme.successTextColor),
            withTouchEnabled: true)
        
        self.revealClueClosure = { view in
            if UIDevice.current.userInterfaceIdiom == .pad {
                self.dismiss(animated: true) { [self] in
                    gameViewController?.rewardWithHint()
                }
            }
            else {
                self.adManager?.rewardToBeGiven = Reward.hint
                self.dismiss(animated: true)
            }
        }
    }
    
    
    //MARK: Letters ads -> premium
    private func updateLettersToPremium()
    {
        updateCell(
            of: .place_letter_btn,
            withName: C.placeLetterButton.0,
            withComment: "instantly_comment"~,
            withCommentColors: (Theme.successBoxColor,Theme.successTextColor),
            withTouchEnabled: true)
        
        self.placeLetterClosure = { view in
            if UIDevice.current.userInterfaceIdiom == .pad {
                self.dismiss(animated: true) { [self] in
                    gameViewController?.rewardWithLetter()
                }
            }
            else {
                self.adManager?.rewardToBeGiven = Reward.letter
                self.dismiss(animated: true)
            }
        }
    }
  
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    

    //MARK: Ad availability callback
    func adAvailabilityDidChange(isAvailable: Bool) {
        print("Latest info for ads or just updated: \(isAvailable)")
        if (isAvailable)
        {
            if (Database.shared.currentLevel?.categoryRewarded == false) {
                updateCell(
                    of: .reveal_clue_btn,
                    withName: C.revealClueButton.0,
                    withComment: "watch_ad_comment"~,
                    withCommentColors: (Theme.colorPrimary, Theme.colorOnPrimary),
                    withTouchEnabled: true
                )
                
                revealClueClosure = { _ in
                    self.dismiss(animated: false) {
                        let appDelegate = UIApplication.shared.delegate as! AppDelegate
                        guard let gvc = appDelegate.window?.rootViewController else { return }
                        self.adManager?.show(viewController: gvc, rewardType: Reward.hint)
                    }
                }
            }
            
            updateCell(
                of: .place_letter_btn,
                withName: C.placeLetterButton.0,
                withComment: "watch_ad_comment"~,
                withCommentColors: (Theme.colorSecondary, Theme.colorOnPrimary),
                withTouchEnabled: true
            )
            placeLetterClosure = { _ in
                self.dismiss(animated: false) {
                    let appDelegate = UIApplication.shared.delegate as! AppDelegate
                    guard let gvc = appDelegate.window?.rootViewController else { return }
                    self.adManager?.show(viewController: gvc, rewardType: Reward.letter)
                }
            }
            
        }
        else {
            updateCell(
                of: .reveal_clue_btn,
                withName: C.revealClueButton.0,
                withComment: "no_ad_comment"~,
                withCommentColors: (Theme.hintsNotificationColor, UIColor.white),
                withTouchEnabled: false
            )
            
            updateCell(
                of: .place_letter_btn,
                withName: C.placeLetterButton.0,
                withComment: "no_ad_comment"~,
                withCommentColors: (Theme.hintsNotificationColor, UIColor.white),
                withTouchEnabled: false
            )
        }
        
    }
    
    
    //MARK: Update products callback
    func didUpdateProducts(withProducts products: [String : ProductInfo]) {
        updatePurchasesUiInfo(purchases: products)
    }
    

    private func allInPurchasedUiUpdate()
    {
        // give hints and letters
        updateHintsToPremium()
        updateLettersToPremium()
        
        // hide all purchases
        Button.allCases.forEach { buttonCase in
            updateCell(
                of: buttonCase,
                withTouchEnabled: false,
                toHidden: true
            )
        }
        
    }
    
    
    //MARK: Update buttons Ui
    private func updatePurchasesUiInfo(purchases products:[String : ProductInfo])
    {
        guard let allInPurchased = products[C.PURCHASE_ALL_ID]?.isPurchased else {
            allInPurchasedUiUpdate()
            return
        }
      
        products.forEach { (productId: String, info: ProductInfo) in
            if (productId == C.PURCHASE_HINTS_ID)
            {
                // give hints
                updateHintsToPremium()
                
                // update button
                updateCell(
                    of: Button.purchase_clues_btn,
                    withName: info.product.localizedTitle,
                    withComment: info.localizedPrice,
                    withCommentColors: (Theme.letterRewardedColor, Theme.colorOnPrimary),
                    withTouchEnabled: !info.isPurchased,
                    toHidden: info.isPurchased
                )
                
                // add tap closure
                purchaseCluesClosure = { view in
                    if (!info.isPurchased) {
                        self.purchaseManager?.purchaseProduct(productId)
                    }
                }
            }
            else if (productId == C.PURCHASE_LETTERS_ID)
            {
                // give letters
                updateLettersToPremium()
                
                // update button
                updateCell(
                    of: Button.purchase_letters_btn,
                    withName: info.product.localizedTitle,
                    withComment: info.localizedPrice,
                    withCommentColors: (Theme.letterRewardedColor, Theme.colorOnPrimary),
                    withTouchEnabled: !info.isPurchased,
                    toHidden: info.isPurchased
                )
                
                // add tap closure
                purchaseLettersClosure = { view in
                    if (!info.isPurchased) {
                        self.purchaseManager?.purchaseProduct(productId)
                    }
                }
            }
            else if (productId == C.PURCHASE_NO_ADS_ID)
            {
                // no content given, read from PurchaseManager / UserDefaults on demand
                updateCell(
                    of: Button.purchase_no_ads_btn,
                    withName: info.product.localizedTitle,
                    withComment: info.localizedPrice,
                    withCommentColors: (Theme.letterRewardedColor, Theme.colorOnPrimary),
                    withTouchEnabled: !info.isPurchased,
                    toHidden: info.isPurchased
                )
                
                // add tap closure
                purchaseNoAdsClosure = { view in
                    if (!info.isPurchased) {
                        self.purchaseManager?.purchaseProduct(productId)
                    }
                }
            }
            else if (productId == C.PURCHASE_ALL_ID)
            {
                updateCell(
                    of: Button.purchase_all_btn,
                    withName: info.product.localizedTitle,
                    withComment: info.localizedPrice,
                    withCommentColors: (Theme.letterRewardedColor, Theme.colorOnPrimary),
                    withTouchEnabled: !info.isPurchased,
                    toHidden: info.isPurchased
                )
                
                // add tap closure
                purchaseAllClosure = { view in
                    if (!info.isPurchased) {
                        self.purchaseManager?.purchaseProduct(productId)
                    }
                }
            }
        }
        
        let allInPrice = products[C.PURCHASE_ALL_ID]?.product.price ?? 0
        let sumWithoutAllIn = products.reduce(NSDecimalNumber.zero) { (result, element) -> NSDecimalNumber in
            guard element.key != C.PURCHASE_ALL_ID && !element.value.isPurchased else {
                return result
            }
            
            let price = element.value.product.price
            return result.adding(price)
        }
        
        if (allInPrice.compare(sumWithoutAllIn) == ComparisonResult.orderedDescending)
        {
            updateCell(
                of: Button.purchase_all_btn,
                withTouchEnabled: false,
                toHidden: true
            )
        }
    }
    
    
    //MARK: Update cell
    private func updateCell(
        of button:Button,
        withName name:String = "",
        withComment comment:String = "",
        withCommentColors commentLabelColors:(UIColor, UIColor) = (UIColor.clear, UIColor.clear),
        withTouchEnabled enabled:Bool = true,
        toHidden hidden:Bool = false,
        withTouchAction closure:@escaping (UIView) -> Void = { view in }
    ) {
        let indexPath = IndexPath(row: button.rawValue, section: 0)
        if let cell = tableView.cellForRow(at: indexPath) as? MenuItemCell {
            if (hidden) {
                cell.isHidden = true
                return
            }
            cell.presentable.closure = closure
            cell.nameLabel.text = name
            cell.commentLabel.text = comment
            cell.commentLabel.backgroundColor = commentLabelColors.0
            cell.commentLabel.textColor = commentLabelColors.1
            if (enabled){
                cell.alpha = 1.0
                cell.isUserInteractionEnabled = true
                cell.backgroundView?.backgroundColor = #colorLiteral(red: 0.8196078431, green: 0.8235294118, blue: 0.8274509804, alpha: 1).withAlphaComponent(0.11)
            } else {
                cell.alpha = 0.8
                cell.isUserInteractionEnabled = false
                cell.backgroundView?.backgroundColor = UIColor.clear
            }
        }
    }
    
    //MARK: Closures
    var revealClueClosure: ((UIView) -> Void) = { view in
        print("Tapped \(C.revealClueButton.0)")
    }
    
    var placeLetterClosure: ((UIView) -> Void) = { view in
        print("Tapped \(C.placeLetterButton.0)")
    }
    
    var purchaseCluesClosure: ((UIView) -> Void) = { view in
        print("Tapped \(C.purchaseCluesButton.0)")
    }
    
    var purchaseLettersClosure: ((UIView) -> Void) = { view in
        print("Tapped \(C.purchaseLettersButton.0)")
    }
    
    var purchaseNoAdsClosure: ((UIView) -> Void) = { view in
        print("Tapped \(C.purchaseNoAdsButton.0)")
    }
    
    var purchaseAllClosure: ((UIView) -> Void) = { view in
        print("Tapped \(C.purchaseAllButton.0)")
    }
    
    
    //MARK: Menu items
    override var menuItem: [MenuItemInfo] {
        return [
            MenuItemInfo(name: C.revealClueButton.0, comment: "no_ad_comment"~, systemImage: C.revealClueButton.1, closure: revealClueClosure),
            MenuItemInfo(name: C.placeLetterButton.0, comment: "no_ad_comment"~, systemImage: C.placeLetterButton.1, closure: placeLetterClosure),
            MenuItemInfo(name: C.purchaseCluesButton.0, comment: "", systemImage: C.purchaseCluesButton.1, closure: purchaseCluesClosure),
            MenuItemInfo(name: C.purchaseLettersButton.0, comment: "", systemImage: C.purchaseLettersButton.1, closure: purchaseLettersClosure),
            MenuItemInfo(name: C.purchaseNoAdsButton.0, comment: "", systemImage: C.purchaseNoAdsButton.1, closure: purchaseNoAdsClosure),
            MenuItemInfo(name: C.purchaseAllButton.0, comment: "", systemImage: C.purchaseAllButton.1, closure: purchaseAllClosure)
        ]
    }
    
}
