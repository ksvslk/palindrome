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
        Theme.shared.changeTheme(name: ThemeName(rawValue: action.title)!)
        
    }
    private let colorSelectionButton = UIButton(primaryAction: nil)
    
    weak var purchaseManager: PurchaseManager?

    
    init(purchaseManager:PurchaseManager?) {
        self.purchaseManager = purchaseManager
        super.init(nibName: nil, bundle: nil)

        Theme.shared.addListener {
            if let selectedIndexPath = self.tableView.indexPathForSelectedRow {
                self.tableView.deselectRow(at: selectedIndexPath, animated: false)
            }
            self.tableView.setNeedsLayout()
            self.tableView.layoutIfNeeded()
            self.tableView.reloadData()
            self.changeColorSelectionButtonColor()
        }
        

        colorSelectionButton.layer.cornerRadius = 10
        colorSelectionButton.contentEdgeInsets = UIEdgeInsets(top: 10, left: 10, bottom: 10, right: 10)
      

        colorSelectionButton.menu = UIMenu(children: [
            UIAction(title: ThemeName.original.rawValue, handler: colorClosure),
            UIAction(title: ThemeName.yellow.rawValue, handler: colorClosure)
        ])
        colorSelectionButton.showsMenuAsPrimaryAction = true
        
        changeColorSelectionButtonColor()
    }
    
    
    private func changeColorSelectionButtonColor()
    {
        colorSelectionButton.setTitle("OO", for: .normal)
        switch Theme.shared.currentTheme {
        case .original:
            (colorSelectionButton.menu?.children[0] as? UIAction)?.state = .on
            (colorSelectionButton.menu?.children[1] as? UIAction)?.state = .off
            colorSelectionButton.tintColor = .purple
            colorSelectionButton.backgroundColor = .purple
        case .yellow:
            (colorSelectionButton.menu?.children[1] as? UIAction)?.state = .on
            (colorSelectionButton.menu?.children[0] as? UIAction)?.state = .off
            colorSelectionButton.tintColor = .yellow
            colorSelectionButton.backgroundColor = .yellow
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
    
    
    override var menuItem: [MenuItemInfo] {
        return [
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
             
            })]
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as? MenuItemCell
        else { return UITableViewCell() }
        
        let item = menuItem[indexPath.row]
        cell.configure(with: item)
        
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
            cell.stackView.addArrangedSubview(colorSelectionButton)
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
