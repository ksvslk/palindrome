//
//  HintsViewController.swift
//  mars
//
//  Created by Mina on 27.05.2023.
//

import Foundation
import UIKit
import SpriteKit

class HintsViewController: BaseMenuController, AdAvailabilityDelegate, ProductsUpdateDelegate, TransactionProcessDelegate {
 
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
        //Swift.debugPrint("init hints")
        self.gameViewController = gameViewController
        self.adManager = adManager
        self.purchaseManager = purchaseManager
        super.init(nibName: nil, bundle: nil)
    }
    
    
    //MARK: View did appear
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        //Swift.debugPrint("viewDidAppear")
        self.purchaseManager?.delegate = self
        self.purchaseManager?.processDelegate = self
        self.purchaseManager?.sendLatestInfo()
        self.adManager?.delegate = self
        self.adManager?.sendLatestInfo()
        
        if (Database.shared.currentLevel?.categoryRewarded == true)
        {
            updateHintsToPremium()
        }

        if (self.purchaseManager?.loading == true)
        {
            view.makeToastActivity(.center)
            view.isUserInteractionEnabled = false
        }
    }
    
    // MARK: View did disappear
    override func viewDidDisappear(_ animated: Bool) {
        //Swift.debugPrint("viewDidDisappear")
        self.purchaseManager?.delegate = nil
        self.purchaseManager?.processDelegate = nil
        self.adManager?.delegate = nil
    }
    
    
    func didFinishProcessing(result: Bool) {
        DispatchQueue.main.async {
            self.view.hideToastActivity()
            self.view.isUserInteractionEnabled = true
        }
    }
    
    func didStartProcessing() {
        DispatchQueue.main.async {
            self.view.makeToastActivity(.center)
            self.view.isUserInteractionEnabled = false
        }
    }
    
    
    // MARK: Set show hint closure
    private func setShowHintClosure()
    {
        self.revealClueClosure = { _ in
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
    
    // MARK: Set give letter closure
    private func setGiveLetterClosure()
    {
        self.placeLetterClosure = { _ in
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
    
    
    //MARK: Hints ads -> premium
    private func updateHintsToPremium()
    {
        Swift.debugPrint(self, "updateHintsToPremium")
        updateCell(
            of: .reveal_clue_btn,
            withName: C.revealClueButton.0,
            withComment: "instantly_comment"~,
            withCommentColors: (Theme.shared.colors.tertiaryContainer, Theme.shared.colors.onTertiaryContainer),
            withTouchEnabled: true)
        
        setShowHintClosure()
    }
    
    
    //MARK: Letters ads -> premium
    private func updateLettersToPremium()
    {
        Swift.debugPrint(self, "updateLettersToPremium")
        updateCell(
            of: .place_letter_btn,
            withName: C.placeLetterButton.0,
            withComment: "instantly_comment"~,
            withCommentColors: (Theme.shared.colors.tertiaryContainer, Theme.shared.colors.onTertiaryContainer),
            withTouchEnabled: true)
        
        setGiveLetterClosure()
    }
    
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    
    //MARK: Ad availability callback
    func adAvailabilityDidChange(isAvailable: Bool) {
        //Swift.debugPrint("Latest info for ads or just updated: \(isAvailable)")
        
        let hintsPurchased = purchaseManager?.isHintsPurchasedInAnyWay() ?? false
        let lettersPurchased = purchaseManager?.isLettersPurchasedInAnyWay() ?? false
        
        
        if isAvailable {
            if Database.shared.currentLevel?.categoryRewarded == false && !hintsPurchased {
                updateCell(
                    of: .reveal_clue_btn,
                    withName: C.revealClueButton.0,
                    withComment: "watch_ad_comment"~,
                    withCommentColors: (Theme.shared.colors.primaryContainer, Theme.shared.colors.onPrimaryContainer),
                    withTouchEnabled: true
                )
                setAdShowClosure(for: .reveal_clue_btn)
            }
            
            if !lettersPurchased {
                updateCell(
                    of: .place_letter_btn,
                    withName: C.placeLetterButton.0,
                    withComment: "watch_ad_comment"~,
                    withCommentColors: (Theme.shared.colors.secondary, Theme.shared.colors.onSecondary),
                    withTouchEnabled: true
                )
                setAdShowClosure(for: .place_letter_btn)
            }
        } else {
            if !hintsPurchased {
                updateCell(
                    of: .reveal_clue_btn,
                    withName: C.revealClueButton.0,
                    withComment: "no_ad_comment"~,
                    withCommentColors: (Theme.shared.colors.error, Theme.shared.colors.onError),
                    withTouchEnabled: false
                )
            }
            
            if !lettersPurchased {
                updateCell(
                    of: .place_letter_btn,
                    withName: C.placeLetterButton.0,
                    withComment: "no_ad_comment"~,
                    withCommentColors: (Theme.shared.colors.error, Theme.shared.colors.onError),
                    withTouchEnabled: false
                )
            }
        }
    }
    
    
    // MARK: Set ad show closure
    private func setAdShowClosure(for button:Button)
    {
        if (button == .place_letter_btn)
        {
            placeLetterClosure = { _ in
                self.dismiss(animated: false) {
                    self.adManager?.show(viewController: self.gameViewController!, rewardType: Reward.letter)
                }
            }
        }
        else if (button == .reveal_clue_btn)
        {
            revealClueClosure = { _ in
                self.dismiss(animated: false) {
                    self.adManager?.show(viewController: self.gameViewController!, rewardType: Reward.hint)
                }
            }
        }
    }
    
    
    //MARK: Update products callback
    func didUpdateProducts(withProducts products: [String : ProductInfo]) {
        DispatchQueue.main.async {
            self.updatePurchasesUiInfo(purchases: products)
        }
    }
    
    
    // MARK: Update all in case
    private func allInPurchasedUiUpdate(products:[String : ProductInfo])
    {
        // give hints and letters
        updateHintsToPremium()
        updateLettersToPremium()
        
       
        // hide all purchases
        [Button.purchase_all_btn, Button.purchase_clues_btn, Button.purchase_letters_btn, Button.purchase_no_ads_btn].forEach { buttonCase in
            
            var name = ""
            
            if (buttonCase == Button.purchase_all_btn)
            {
                name = products[C.PURCHASE_ALL_ID]?.product.localizedTitle ?? name
            }
            else if (buttonCase == Button.purchase_clues_btn)
            {
                name = products[C.PURCHASE_HINTS_ID]?.product.localizedTitle  ?? name
            }

            else if (buttonCase == Button.purchase_letters_btn)
            {
                name = products[C.PURCHASE_LETTERS_ID]?.product.localizedTitle ?? name
            }

            else if (buttonCase == Button.purchase_no_ads_btn)
            {
                name = products[C.PURCHASE_NO_ADS_ID]?.product.localizedTitle ?? name
            }

            updateCell(
                of: buttonCase,
                withName: name,
                withTouchEnabled: false,
                toOwned: true
            )
        }
        
    }
    
    
    //MARK: Update buttons Ui
    private func updatePurchasesUiInfo(purchases products:[String : ProductInfo])
    {
        //Swift.debugPrint("updatePurchasesUiInfo")
       
        if products[C.PURCHASE_ALL_ID]?.isPurchased == true {
            allInPurchasedUiUpdate(products: products)
            return
        }
        
        
        products.forEach { (productId: String, info: ProductInfo) in
            if (productId == C.PURCHASE_HINTS_ID)
            {
                // give hints if purchased
                if (info.isPurchased) { updateHintsToPremium() }
                
                // update button
                updateCell(
                    of: Button.purchase_clues_btn,
                    withName: info.product.localizedTitle,
                    withComment: info.localizedPrice,
                    withCommentColors: (Theme.shared.colors.primaryContainer, Theme.shared.colors.onPrimaryContainer),
                    withTouchEnabled: !info.isPurchased,
                    toOwned: info.isPurchased
                )
                
                // add tap closure
                purchaseCluesClosure = { menuItem in
                    if (!info.isPurchased) {
                        self.purchaseManager?.purchaseProduct(productId)
                    }
                }
            }
            else if (productId == C.PURCHASE_LETTERS_ID)
            {
                // give letters if purchased
                if (info.isPurchased) { updateLettersToPremium() }
                
                // update button
                updateCell(
                    of: Button.purchase_letters_btn,
                    withName: info.product.localizedTitle,
                    withComment: info.localizedPrice,
                    withCommentColors: (Theme.shared.colors.primaryContainer, Theme.shared.colors.onPrimaryContainer),
                    withTouchEnabled: !info.isPurchased,
                    toOwned: info.isPurchased
                )
                
                // add tap closure
                purchaseLettersClosure = { _ in
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
                    withCommentColors: (Theme.shared.colors.primaryContainer, Theme.shared.colors.onPrimaryContainer),
                    withTouchEnabled: !info.isPurchased,
                    toOwned: info.isPurchased
                )
                
                // add tap closure
                purchaseNoAdsClosure = { _ in
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
                    withCommentColors: (Theme.shared.colors.primaryContainer, Theme.shared.colors.onPrimaryContainer),
                    withTouchEnabled: !info.isPurchased,
                    toOwned: info.isPurchased
                )
                
                // add tap closure
                purchaseAllClosure = { _ in
                    if (!info.isPurchased) {
                        self.purchaseManager?.purchaseProduct(productId)
                    }
                }
            }
        }
        
        if (isAllInPriceHigherThatOthersCombined(products:products))
        {
            updateCell(
                of: Button.purchase_all_btn,
                toThanks: true
            )
        }
        
    }
    
    
    private func isAllInPriceHigherThatOthersCombined(products:[String : ProductInfo]) -> Bool
    {
        let allInPrice = products[C.PURCHASE_ALL_ID]?.product.price ?? 0
        let sumWithoutAllIn = products.reduce(NSDecimalNumber.zero) { (result, element) -> NSDecimalNumber in
            guard element.key != C.PURCHASE_ALL_ID && !element.value.isPurchased else {
                return result
            }
            
            let price = element.value.product.price
            return result.adding(price)
        }
        //Swift.debugPrint("sumWithoutAllIn: \(sumWithoutAllIn)")
        //Swift.debugPrint("allinprice: \(allInPrice)")
        return allInPrice.compare(sumWithoutAllIn) == ComparisonResult.orderedDescending
    }
    
    
    
    //MARK: Update cell
    private func updateCell(
        of button:Button,
        withName name:String = "",
        withComment comment:String = "",
        withCommentColors commentLabelColors:(UIColor, UIColor) = (UIColor.clear, UIColor.clear),
        withTouchEnabled enabled:Bool = true,
        toOwned purchased:Bool = false,
        toThanks thankYou:Bool = false,
        withTouchAction closure:@escaping (UIView) -> Void = { _ in }
    ) {
        let indexPath = IndexPath(row: button.rawValue, section: 0)
        
        if let cell = tableView.cellForRow(at: indexPath) as? MenuItemCell {
            if (purchased) {
                let checkMark = UIImage(systemName: "checkmark.shield.fill")!
                cell.removeImageFromIconView()
                cell.addImageToIconView(image: checkMark)
                cell.iconView.backgroundColor = Theme.shared.colors.tertiaryContainer
                cell.nameLabel.text = "\(name) ✦"
                cell.commentLabel.text = ""
                cell.commentLabel.backgroundColor = .clear
                cell.isUserInteractionEnabled = false
                return
            }
            if (thankYou)
            {
                let checkMark = UIImage(systemName: "heart")!
                cell.removeImageFromIconView()
                cell.addImageToIconView(image: checkMark)
                cell.iconView.backgroundColor = UIColor.clear
                cell.iconView.tintColor = Theme.shared.colors.primary
                cell.nameLabel.text = "Thank you!"
                cell.commentLabel.text = ""
                cell.commentLabel.backgroundColor = .clear
                cell.isUserInteractionEnabled = false
                return
            }
            cell.presentable.closure = closure
            cell.nameLabel.text = name
            cell.commentLabel.text = comment
            cell.commentLabel.backgroundColor = commentLabelColors.0
            cell.commentLabel.textColor = commentLabelColors.1
            if (enabled){
                cell.isUserInteractionEnabled = true
                cell.backgroundView?.backgroundColor = #colorLiteral(red: 0.8196078431, green: 0.8235294118, blue: 0.8274509804, alpha: 1).withAlphaComponent(0.11)
            } else {
                cell.isUserInteractionEnabled = false
                cell.backgroundView?.backgroundColor = UIColor.clear
            }
        }
        
    }
    
    //MARK: Closures
    var revealClueClosure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.revealClueButton.0)")
    }
    
    var placeLetterClosure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.placeLetterButton.0)")
    }
    
    var purchaseCluesClosure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseCluesButton.0)")
    }
    
    var purchaseLettersClosure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseLettersButton.0)")
    }
    
    var purchaseNoAdsClosure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseNoAdsButton.0)")
    }
    
    var purchaseAllClosure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseAllButton.0)")
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
        ]}
    
}

