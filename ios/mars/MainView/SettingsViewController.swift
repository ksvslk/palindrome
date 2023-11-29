//
//  SettingsViewController.swift
//  mars
//
//  Created by Mina on 26.05.2023.
//

import Foundation
import UIKit
import Toast

class SettingsViewController: BaseMenuController, TransactionProcessDelegate {
    
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
    
    private let soundSwitch:UISwitch = UISwitch()
    private let vibrationSwitch:UISwitch = UISwitch()
    
    
    private let colorClosure = { (action: UIAction) in
        Theme.shared.changeTheme(name: ThemeName(rawValue: action.title.lowercased())!)
    }
    private let colorSelectionButton = UIButton(primaryAction: nil)
    
    weak var purchaseManager: PurchaseManager?
    var menuPromoData: PromoMenuButtonData?
    
    init(purchaseManager:PurchaseManager?, promoMenudata:PromoMenuButtonData?) {
        self.purchaseManager = purchaseManager
        super.init(nibName: nil, bundle: nil)
        
        if let promo = promoMenudata { menuPromoData = promo}
        
        Theme.shared.addListener {
            self.changeSettingsMenuColors()
        }
        
        colorSelectionButton.layer.cornerRadius = 10
        colorSelectionButton.contentEdgeInsets = UIEdgeInsets(top: 10, left: 10, bottom: 10, right: 10)
        colorSelectionButton.menu = UIMenu(children: [
            UIAction(title: ThemeName.purple.rawValue.capitalized, handler: colorClosure),
            UIAction(title: ThemeName.yellow.rawValue.capitalized, handler: colorClosure),
            UIAction(title: ThemeName.green.rawValue.capitalized, handler: colorClosure),
            UIAction(title: ThemeName.blue.rawValue.capitalized, handler: colorClosure),
            UIAction(title: ThemeName.red.rawValue.capitalized, handler: colorClosure),
        ])
        colorSelectionButton.showsMenuAsPrimaryAction = true
        changeColorSelectionButtonColor()
    }
    
    
    private func populatePromoButton(cell:MenuItemCell)
    {
        if let menuPromoData = menuPromoData {
            if (menuPromoData.activated == true)
            {
                    promoButtonClosure = { _ in
                        guard let promoUrl = URL(string: menuPromoData.link ?? C.MORE_GAMES_URL)
                        else { fatalError("Expected a valid URL") }
                        UIApplication.shared.open(promoUrl, options: [:], completionHandler: nil)
                    }
                    cell.nameLabel.text = menuPromoData.title
                    cell.commentLabel.text = menuPromoData.subtitle
                    cell.commentLabel.text = menuPromoData.subtitle
                    cell.commentLabel.backgroundColor = Theme.shared.colors.tertiaryContainer
                    cell.commentLabel.textColor = Theme.shared.colors.onTertiaryContainer
            }
        }
   
    }
    
    
    private func changeSettingsMenuColors()
    {
        if let selectedIndexPath = self.tableView.indexPathForSelectedRow {
            self.tableView.deselectRow(at: selectedIndexPath, animated: false)
        }
        self.tableView.setNeedsLayout()
        self.tableView.layoutIfNeeded()
        self.tableView.reloadData()
        self.changeColorSelectionButtonColor()
    }
    

    private func changeColorSelectionButtonColor()
    {
        colorSelectionButton.setTitle("OOO", for: .normal)
        let color = Theme.shared.colors.primary
        switch Theme.shared.currentTheme {
        case .purple:
            changeOtherColorsAsNotSelected(selectedNumber: 0)
            colorSelectionButton.tintColor = color
            colorSelectionButton.backgroundColor = color
        case .yellow:
            changeOtherColorsAsNotSelected(selectedNumber: 1)
            colorSelectionButton.tintColor = color
            colorSelectionButton.backgroundColor = color
        case .green:
            changeOtherColorsAsNotSelected(selectedNumber: 2)
            colorSelectionButton.tintColor = color
            colorSelectionButton.backgroundColor = color
        case .blue:
            changeOtherColorsAsNotSelected(selectedNumber: 3)
            colorSelectionButton.tintColor = color
            colorSelectionButton.backgroundColor = color
        case .red:
            changeOtherColorsAsNotSelected(selectedNumber: 4)
            colorSelectionButton.tintColor = color
            colorSelectionButton.backgroundColor = color
        }
    }
    
    private func changeOtherColorsAsNotSelected(selectedNumber:Int)
    {
        if let colorItems = colorSelectionButton.menu?.children {
            for (index, element) in colorItems.enumerated() {
                if (index == selectedNumber)
                {
                    (element as? UIAction)?.state = .on
                }
                else {
                    (element as? UIAction)?.state = .off
                }
            }
        }
    }

    
    override func viewDidAppear(_ animated: Bool) {
        self.purchaseManager?.processDelegate = self
        if (self.purchaseManager?.loading == true)
        {
            view.makeToastActivity(.center)
            view.isUserInteractionEnabled = false
        }
    }
    
    
    override func viewDidDisappear(_ animated: Bool) {
        self.purchaseManager?.processDelegate = nil
    }
    
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    var promoButtonClosure: ((MenuItemCell) -> Void) = { menuItem in
        guard let promoUrl = URL(string: C.MORE_GAMES_URL)
        else { fatalError("Expected a valid URL") }
        UIApplication.shared.open(promoUrl, options: [:], completionHandler: nil)
    }
    
    override var menuItem: [MenuItemInfo] {
        return [
            MenuItemInfo(name: C.promoButton.0, comment: "", systemImage: C.promoButton.1, closure: promoButtonClosure),
            MenuItemInfo(name: C.colorButton.0, comment: "", systemImage: C.colorButton.1, closure: {menuItem in
              
            }),
            MenuItemInfo(name: C.soundButton.0, comment: "", systemImage: C.soundButton.1, closure: {menuItem in
                //Swift.debugPrint("Tapped \(C.soundButton.0)")
                self.soundSwitch.setOn(!self.soundSwitch.isOn, animated: true)
                self.soundSwitchStateChanged(self.soundSwitch)
            }),
            MenuItemInfo(name: C.vibrationButton.0, comment: "", systemImage: C.vibrationButton.1, closure: {menuItem in
                //Swift.debugPrint("Tapped \(C.vibrationButton.0)")
                self.vibrationSwitch.setOn(!self.vibrationSwitch.isOn, animated: true)
                self.vibrationSwitchStateChanged(self.vibrationSwitch)
            }),
            MenuItemInfo(name: C.feedbackButton.0, comment: "", systemImage: C.feedbackButton.1, closure: {menuItem in
                //Swift.debugPrint("Tapped \(C.feedbackButton.0)")
                guard let writeReviewURL = URL(string: C.APP_FEEDBACK_URL)
                else { fatalError("Expected a valid URL") }
                UIApplication.shared.open(writeReviewURL, options: [:], completionHandler: nil)
            }),
            MenuItemInfo(name: C.shareButton.0, comment: "", systemImage: C.shareButton.1, closure: {menuItem in
                //Swift.debugPrint("Tapped \(C.shareButton.0)")
                let items: [Any] = ["share_description"~, URL(string: C.APP_URL)!]
                let share = UIActivityViewController(activityItems: items, applicationActivities: nil)
                if let popoverController = share.popoverPresentationController {
                    popoverController.sourceView = menuItem
                    popoverController.sourceRect = menuItem.bounds
                }
                self.present(share, animated: true)
            }),
            MenuItemInfo(name: C.restoreButton.0, comment: "", systemImage: C.restoreButton.1, closure: {menuItem in
                //Swift.debugPrint("Tapped \(C.restoreButton.0)")
                self.purchaseManager?.restorePurchases()
                self.view.makeToastActivity(.center)
                self.view.isUserInteractionEnabled = false
            })
        ]
    }
    
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as? MenuItemCell
        else {Swift.debugPrint("Returning UITableViewCell");  return UITableViewCell() }
        
        let item = menuItem[indexPath.row]
        cell.configure(with: item)
        
        Swift.debugPrint("Cell: \(cell.nameLabel)")
        
        if (item.name == C.soundButton.0)
        {
            let userDefaults = UserDefaults.standard
            let soundOn = userDefaults.bool(forKey: C.SOUND_ON)
            cell.stackView.addArrangedSubview(soundSwitch)
            soundSwitch.addTarget(self, action: #selector(soundSwitchStateChanged(_:)), for: .valueChanged)
            soundSwitch.isOn = soundOn
      
            soundSwitch.tintColor = Theme.shared.colors.surfaceVariant
            soundSwitch.onTintColor = Theme.shared.colors.primary
            soundSwitch.thumbTintColor = Theme.shared.colors.onPrimary
         
        }
        else if (item.name == C.vibrationButton.0)
        {
            let userDefaults = UserDefaults.standard
            let vibrationOn = userDefaults.bool(forKey: C.VIBRATION_ON)
            cell.stackView.addArrangedSubview(vibrationSwitch)
            vibrationSwitch.addTarget(self, action: #selector(vibrationSwitchStateChanged(_:)), for: .valueChanged)
            vibrationSwitch.isOn = vibrationOn
            vibrationSwitch.tintColor = Theme.shared.colors.surfaceVariant
            vibrationSwitch.onTintColor = Theme.shared.colors.primary
            vibrationSwitch.thumbTintColor = Theme.shared.colors.onPrimary
        }
        else if (item.name == C.colorButton.0)
        {
            cell.selectionStyle = UITableViewCell.SelectionStyle.none
            cell.stackView.addArrangedSubview(colorSelectionButton)
        }
        else if (item.name == C.promoButton.0)
        {
            populatePromoButton(cell: cell)
        }
        
        return cell
    }
    
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        soundSwitch.removeTarget(self, action: #selector(soundSwitchStateChanged(_:)), for: .valueChanged)
        vibrationSwitch.removeTarget(self, action: #selector(vibrationSwitchStateChanged(_:)), for: .valueChanged)
    }
    
    
    private func toggleSettingInUserDefaults(isOn:Bool, setting:String)
    {
        //Swift.debugPrint("toggleSettingInUserDefaults")
        let userDefaults = UserDefaults.standard
        if isOn {
            userDefaults.set(true, forKey: setting)
            NSUbiquitousKeyValueStore.default.set(true, forKey: setting)
        } else {
            userDefaults.set(false, forKey: setting)
            NSUbiquitousKeyValueStore.default.set(false, forKey: setting)
        }
    }
    
    
    @objc private func soundSwitchStateChanged(_ sender: UISwitch) {
        toggleSettingInUserDefaults(isOn: sender.isOn, setting: C.SOUND_ON)
    }
    
    
    @objc private func vibrationSwitchStateChanged(_ sender: UISwitch) {
        toggleSettingInUserDefaults(isOn: sender.isOn, setting: C.VIBRATION_ON)
    }
    
}
