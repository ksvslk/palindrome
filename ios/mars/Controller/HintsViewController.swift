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
    private enum Button: Int {
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
    
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        self.adManager?.delegate = self
        self.adManager?.sendLatestInfo()
        
        if (Database.shared.currentLevel?.categoryRewarded == true)
        {
            updateHintsToPremium()
        }
    }
    
    
    private func updateHintsToPremium()
    {
        updateCell(
            of: .reveal_clue_btn,
            withName: C.revealClueButton.0,
            withComment: "instantly_comment"~,
            withCommentColors: (Theme.successBoxColor,Theme.successTextColor),
            withTouchEnabled: true) { _ in
                
            }
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
    
    
    private func updateLettersToPremium()
    {
        updateCell(
            of: .place_letter_btn,
            withName: C.placeLetterButton.0,
            withComment: "instantly_comment"~,
            withCommentColors: (Theme.successBoxColor,Theme.successTextColor),
            withTouchEnabled: true) { _ in
                
            }
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
                withCommentColors: (Theme.colorPrimary, Theme.colorOnPrimary),
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
    
    
    func didUpdateProducts(withProducts products: [String : ProductInfo]) {
        //TODO: Ads buttons + purchase buttons UI needs to be updated
    }
    
    
    private func updateCell(
        of button:Button,
        withName name:String = "",
        withComment comment:String = "",
        withCommentColors commentLabelColors:(UIColor, UIColor) = (UIColor.clear, UIColor.clear),
        withTouchEnabled enabled:Bool = true,
        withTouchAction closure:@escaping (UIView) -> Void = { view in }
    ) {
        let indexPath = IndexPath(row: button.rawValue, section: 0)
        if let cell = tableView.cellForRow(at: indexPath) as? MenuItemCell {
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
    
    override var members: [MenuItemInfo] {
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
