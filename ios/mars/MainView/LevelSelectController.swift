//
//  LevelSelectController.swift
//  mars
//
//  Created by Mina on 26.05.2023.
//

import Foundation
import UIKit

class LevelSelectController: BaseMenuController,ProductsUpdateDelegate, TransactionProcessDelegate {
    
    weak var purchaseManager: PurchaseManager?
    
    // helper for members array to get location of some button in table. Manually matched to the members array!
    private enum Button: Int, CaseIterable {
        case all_level_packs_btn = 0
        case level_pack_1_btn = 1
        case level_pack_2_btn = 2
        case level_pack_3_btn = 3
        case level_pack_4_btn = 4
        case level_pack_5_btn = 5
        case level_pack_6_btn = 6
        case level_pack_game_btn = 7
    }
    
    init(purchaseManager:PurchaseManager?) {
        self.purchaseManager = purchaseManager
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    

    // MARK: View will appear
    override func viewWillAppear(_ animated: Bool) {
        self.purchaseManager?.delegate = self
        self.purchaseManager?.processDelegate = self
        self.purchaseManager?.sendLatestInfo()
        
        if (self.purchaseManager?.loading == true)
        {
            view.makeToastActivity(.center)
            view.isUserInteractionEnabled = false
        }
    }
    
    // MARK: View did disappear
    override func viewDidDisappear(_ animated: Bool) {
        self.purchaseManager?.delegate = nil
        self.purchaseManager?.processDelegate = nil
    }
    
    override func viewDidAppear(_ animated: Bool) {
        Database.shared.getLevelPackProgress(levelPack: LevelPack.game_pack) { percentageComplete in
            DispatchQueue.main.async {

                    self.updateCell(
                        of: Button.level_pack_game_btn,
                        withComment: "\(percentageComplete)%",
                        withCommentColors: (Theme.shared.colors.tertiaryContainer, Theme.shared.colors.onTertiaryContainer),
                        withTouchEnabled: true,
                        onlyCommentUpdate: true
                    )
           
            }
        }
    }
    
    
    //MARK: Update products callback
    func didUpdateProducts(withProducts products: [String : ProductInfo]) {
        DispatchQueue.main.async {
            Swift.debugPrint("products \(products)")
            self.updatePurchasesUiInfo(purchases: products)
        }
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
    
    //MARK: Update cell
    private func updateCell(
        of button:Button,
        withName name:String = "",
        withComment comment:String = "",
        withCommentColors commentLabelColors:(UIColor, UIColor) = (UIColor.clear, UIColor.clear),
        withTouchEnabled enabled:Bool = true,
        toOwned purchased:Bool = false,
        toThanks thankYou:Bool = false,
        onlyCommentUpdate commentUpdate:Bool = false,
        withTouchAction closure:@escaping (UIView) -> Void = { _ in }
    ) {
        let indexPath = IndexPath(row: button.rawValue, section: 0)
        if let cell = tableView.cellForRow(at: indexPath) as? MenuItemCell {
            
            if (commentUpdate)
            {
                cell.commentLabel.backgroundColor = commentLabelColors.0
                cell.commentLabel.textColor = commentLabelColors.1
                cell.commentLabel.text = "\(comment)"
                return
            }
            
            if (purchased) {
                let checkMark = UIImage(systemName: "play.circle.fill")!
                cell.removeImageFromIconView()
                cell.addImageToIconView(image: checkMark)
                cell.iconView.backgroundColor = Theme.shared.colors.primaryContainer
                cell.iconView.tintColor = Theme.shared.colors.onPrimaryContainer
                cell.nameLabel.text = "\(name)"
                
                cell.commentLabel.backgroundColor = commentLabelColors.0
                cell.commentLabel.textColor = commentLabelColors.1
                cell.commentLabel.text = "\(comment)"
                //cell.isUserInteractionEnabled = false
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
    
    var purchaseLevelPack1Closure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseCluesButton.0)")
    }
    
    var purchaseLevelPack2Closure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseLettersButton.0)")
    }
    
    var purchaseLevelPack3Closure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseNoAdsButton.0)")
    }
    
    var purchaseLevelPack4Closure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseAllButton.0)")
    }
    
    var purchaseLevelPack5Closure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseAllButton.0)")
    }
    
    var purchaseLevelPack6Closure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseAllButton.0)")
    }
    
    var purchaseAllLevelPacksClosure: ((MenuItemCell) -> Void) = { menuItem in
        //Swift.debugPrint("Tapped \(C.purchaseAllButton.0)")
    }
    
    
    //MARK: Set premium closure
    private func setLevelPackTapClosure(info:ProductInfo) -> Void
    {
        if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_1_ID)
        {
            purchaseLevelPack1Closure = { menuItem in
                self.present(GameLevelSelectController(levelPack: LevelPack.level_pack_1, dismissLevelSelectMenu: {
                    self.dismiss(animated: true)
                }), animated: true, completion: nil)
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_2_ID)
        {
            purchaseLevelPack2Closure = { menuItem in
                self.present(GameLevelSelectController(levelPack: LevelPack.level_pack_2, dismissLevelSelectMenu: {
                    self.dismiss(animated: true)
                }), animated: true, completion: nil)
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_3_ID)
        {
            purchaseLevelPack3Closure = { menuItem in
                self.present(GameLevelSelectController(levelPack: LevelPack.level_pack_3, dismissLevelSelectMenu: {
                    self.dismiss(animated: true)
                }), animated: true, completion: nil)
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_4_ID)
        {
            purchaseLevelPack4Closure = { menuItem in
                self.present(GameLevelSelectController(levelPack: LevelPack.level_pack_4, dismissLevelSelectMenu: {
                    self.dismiss(animated: true)
                }), animated: true, completion: nil)
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_5_ID)
        {
            purchaseLevelPack5Closure = { menuItem in
                self.present(GameLevelSelectController(levelPack: LevelPack.level_pack_5, dismissLevelSelectMenu: {
                    self.dismiss(animated: true)
                }), animated: true, completion: nil)
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_6_ID)
        {
            purchaseLevelPack6Closure = { menuItem in
                self.present(GameLevelSelectController(levelPack: LevelPack.level_pack_6, dismissLevelSelectMenu: {
                    self.dismiss(animated: true)
                }), animated: true, completion: nil)
            }
        }
        
    }
    
    
    //MARK: Set purchasable closure
    private func setLevelPackTapClosurePurchasable(info:ProductInfo) -> Void
    {
        if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_1_ID)
        {
            purchaseLevelPack1Closure = { _ in
                if (!info.isPurchased) {
                    self.purchaseManager?.purchaseProduct(info.product.productIdentifier)
                }
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_2_ID)
        {
            purchaseLevelPack2Closure = {_ in
                if (!info.isPurchased) {
                    self.purchaseManager?.purchaseProduct(info.product.productIdentifier)
                }
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_3_ID)
        {
            purchaseLevelPack3Closure = {_ in
                if (!info.isPurchased) {
                    self.purchaseManager?.purchaseProduct(info.product.productIdentifier)
                }
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_4_ID)
        {
            purchaseLevelPack4Closure = {_ in
                if (!info.isPurchased) {
                    self.purchaseManager?.purchaseProduct(info.product.productIdentifier)
                }
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_5_ID)
        {
            purchaseLevelPack5Closure = {_ in
                if (!info.isPurchased) {
                    self.purchaseManager?.purchaseProduct(info.product.productIdentifier)
                }
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_6_ID)
        {
            purchaseLevelPack6Closure = {_ in
                if (!info.isPurchased) {
                    self.purchaseManager?.purchaseProduct(info.product.productIdentifier)
                }
            }
        }
        else if (info.product.productIdentifier == C.PURCHASE_ALL_LEVELS_ID)
        {
            purchaseAllLevelPacksClosure = {_ in
                if (!info.isPurchased) {
                    self.purchaseManager?.purchaseProduct(info.product.productIdentifier)
                }
            }
        }
        
    }
    
    
    //MARK: Update buttons Ui
    private func updatePurchasesUiInfo(purchases products:[String : ProductInfo])
    {
        let allPacksPurchased = products[C.PURCHASE_ALL_LEVELS_ID]?.isPurchased == true
        let showAllInPack = products.filter {
            ($1.product.productIdentifier == C.PURCHASE_LEVELS_PACK_1_ID ||
            $1.product.productIdentifier == C.PURCHASE_LEVELS_PACK_2_ID ||
            $1.product.productIdentifier == C.PURCHASE_LEVELS_PACK_3_ID ||
            $1.product.productIdentifier == C.PURCHASE_LEVELS_PACK_4_ID ||
            $1.product.productIdentifier == C.PURCHASE_LEVELS_PACK_5_ID ||
            $1.product.productIdentifier == C.PURCHASE_LEVELS_PACK_6_ID) &&
            $1.isPurchased
        }.count > 2 || allPacksPurchased
        
        products.forEach { (productId: String, info: ProductInfo) in
            if (productId == C.PURCHASE_LEVELS_PACK_1_ID ||
                productId == C.PURCHASE_LEVELS_PACK_2_ID ||
                productId == C.PURCHASE_LEVELS_PACK_3_ID ||
                productId == C.PURCHASE_LEVELS_PACK_4_ID ||
                productId == C.PURCHASE_LEVELS_PACK_5_ID ||
                productId == C.PURCHASE_LEVELS_PACK_6_ID)
            {
                if (info.isPurchased || allPacksPurchased || info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_3_ID) {
                    updateLevelPackToPremium(info: info)
                    setLevelPackTapClosure(info: info)
                }
                else {
                    updateLevelPackToPurchasable(info: info)
                    setLevelPackTapClosurePurchasable(info: info)
                }
            }
            else if (productId == C.PURCHASE_ALL_LEVELS_ID)
            {
                if (showAllInPack || allPacksPurchased) {
                    updateCell(of: Button.all_level_packs_btn, toThanks: true)
                    purchaseAllLevelPacksClosure = { v in }
                }
                else {
                    updateLevelPackToPurchasable(info: info)
                    setLevelPackTapClosurePurchasable(info: info)
                }
            }
        }
    }
    
    
    //MARK: Change level pack to purchasable
    private func updateLevelPackToPurchasable(info:ProductInfo)
    {
        let button:Button? = if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_1_ID)
        {
            Button.level_pack_1_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_2_ID)
        {
            Button.level_pack_2_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_3_ID)
        {
            Button.level_pack_3_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_4_ID)
        {
            Button.level_pack_4_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_5_ID)
        {
            Button.level_pack_5_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_6_ID)
        {
            Button.level_pack_6_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_ALL_LEVELS_ID) {
            Button.all_level_packs_btn
        }
        else {
            nil
        }
  
        if let button = button {
                updateCell(
                    of: button,
                    withName: info.product.localizedTitle,
                    withComment: info.localizedPrice,
                    withCommentColors: (Theme.shared.colors.primaryContainer, Theme.shared.colors.onPrimaryContainer),
                    withTouchEnabled: !info.isPurchased,
                    toOwned: info.isPurchased
                )
        }
    }
    
    
    //MARK: Change level pack to premium
    private func updateLevelPackToPremium(info:ProductInfo)
    {
        var levelPack:LevelPack?
        var button:Button?
        
        if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_1_ID)
        {
            levelPack = LevelPack.level_pack_1
            button = Button.level_pack_1_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_2_ID)
        {
            levelPack = LevelPack.level_pack_2
            button = Button.level_pack_2_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_3_ID)
        {
            levelPack = LevelPack.level_pack_3
            button = Button.level_pack_3_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_4_ID)
        {
            levelPack = LevelPack.level_pack_4
            button = Button.level_pack_4_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_5_ID)
        {
            levelPack = LevelPack.level_pack_5
            button = Button.level_pack_5_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_LEVELS_PACK_6_ID)
        {
            levelPack = LevelPack.level_pack_6
            button = Button.level_pack_6_btn
        }
        else if (info.product.productIdentifier == C.PURCHASE_ALL_LEVELS_ID){
            levelPack = LevelPack.game_pack
            button = Button.all_level_packs_btn
        }
        else {
            levelPack = nil
            button = nil
        }
        
        if let levelPack = levelPack {
            Database.shared.getLevelPackProgress(levelPack: levelPack) { percentageComplete in
                Swift.debugPrint("Progress for \(levelPack.rawValue) is \(percentageComplete)")
                DispatchQueue.main.async {
                    if let button = button {
                        let title = info.product.localizedTitle.getStringOutsideBrackets()
                        self.updateCell(
                            of: button,
                            withName: title,
                            withComment: "\(percentageComplete)%",
                            withCommentColors: (Theme.shared.colors.tertiaryContainer, Theme.shared.colors.onTertiaryContainer),
                            withTouchEnabled: true,
                            toOwned: true
                        )
                    }
                }
            }
        }
        else {
            DispatchQueue.main.async {
                if let button = button {
                    let title = info.product.localizedTitle.getStringOutsideBrackets()
                    self.updateCell(
                        of: button,
                        withName: title,
                        withCommentColors: (Theme.shared.colors.tertiaryContainer, Theme.shared.colors.onTertiaryContainer),
                        withTouchEnabled: true,
                        toOwned: true
                    )
                }
            }
       
        }
     
    }
    
    
    override var menuItem: [MenuItemInfo] {
        return [
            MenuItemInfo(name: C.purchaseAllLevelPacksButton.0, comment: "", systemImage: C.purchaseAllLevelPacksButton.1, closure: purchaseAllLevelPacksClosure),
            MenuItemInfo(name: C.purchaseLevelPack1Button.0, comment: "", systemImage: C.purchaseLevelPack1Button.1, closure: purchaseLevelPack1Closure),
            MenuItemInfo(name: C.purchaseLevelPack2Button.0, comment: "", systemImage: C.purchaseLevelPack2Button.1, closure: purchaseLevelPack2Closure),
            MenuItemInfo(name: C.purchaseLevelPack3Button.0, comment: "", systemImage: C.purchaseLevelPack3Button.1, closure: purchaseLevelPack3Closure),
            MenuItemInfo(name: C.purchaseLevelPack4Button.0, comment: "", systemImage: C.purchaseLevelPack4Button.1, closure: purchaseLevelPack4Closure),
            MenuItemInfo(name: C.purchaseLevelPack5Button.0, comment: "", systemImage: C.purchaseLevelPack5Button.1, closure: purchaseLevelPack5Closure),
            MenuItemInfo(name: C.purchaseLevelPack6Button.0, comment: "", systemImage: C.purchaseLevelPack6Button.1, closure: purchaseLevelPack6Closure),
            MenuItemInfo(name: C.gameLevelsButton.0, comment: "", systemImage: C.gameLevelsButton.1, closure: {menuItem in
                self.present(GameLevelSelectController(levelPack: LevelPack.game_pack, dismissLevelSelectMenu: {
                    self.dismiss(animated: true)
                }), animated: true, completion: nil)
                
            }),
            MenuItemInfo(name: C.tutorialButton.0, comment: "", systemImage: C.tutorialButton.1, closure: {menuItem in
                let tutorialView = TutorialViewController()
                self.present(tutorialView, animated: true)
            }),
            
            
        ]
    }
    
}


