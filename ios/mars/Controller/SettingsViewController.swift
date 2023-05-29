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
            if (result)
            {
                self.view.makeToast("Successfully restored!", position: .center)
            }
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
    
    weak var purchaseManager: PurchaseManager?
    
    init(purchaseManager:PurchaseManager?) {
        self.purchaseManager = purchaseManager
        super.init(nibName: nil, bundle: nil)
  
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
            MenuItemInfo(name: C.soundButton.0, comment: "", systemImage: C.soundButton.1, closure: {menuItem in
                print("Tapped \(C.soundButton.0)")
                self.soundSwitch.setOn(!self.soundSwitch.isOn, animated: true)
                self.soundSwitchStateChanged(self.soundSwitch)
            }),
            MenuItemInfo(name: C.vibrationButton.0, comment: "", systemImage: C.vibrationButton.1, closure: {menuItem in
                print("Tapped \(C.vibrationButton.0)")
                self.vibrationSwitch.setOn(!self.vibrationSwitch.isOn, animated: true)
                self.vibrationSwitchStateChanged(self.vibrationSwitch)
            }),
            MenuItemInfo(name: C.tutorialButton.0, comment: "", systemImage: C.tutorialButton.1, closure: {menuItem in
                print("Tapped \(C.tutorialButton.0)")
                self.view?.makeToastActivity(.center)
                self.view?.isUserInteractionEnabled = false
            }),
            MenuItemInfo(name: C.feedbackButton.0, comment: "", systemImage: C.feedbackButton.1, closure: {menuItem in
                print("Tapped \(C.feedbackButton.0)")
                guard let writeReviewURL = URL(string: C.APP_FEEDBACK_URL)
                else { fatalError("Expected a valid URL") }
                UIApplication.shared.open(writeReviewURL, options: [:], completionHandler: nil)
            }),
            MenuItemInfo(name: C.shareButton.0, comment: "", systemImage: C.shareButton.1, closure: {menuItem in
                print("Tapped \(C.shareButton.0)")
                let items: [Any] = ["share_description"~, URL(string: C.APP_URL)!]
                let share = UIActivityViewController(activityItems: items, applicationActivities: nil)
                if let popoverController = share.popoverPresentationController {
                    popoverController.sourceView = menuItem
                    popoverController.sourceRect = menuItem.bounds
                }
                self.present(share, animated: true)
            }),
            MenuItemInfo(name: C.restoreButton.0, comment: "", systemImage: C.restoreButton.1, closure: {menuItem in
                print("Tapped \(C.restoreButton.0)")
      
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
            soundSwitch.onTintColor = Theme.colorSecondary
        }
        else if (item.name == C.vibrationButton.0)
        {
            let userDefaults = UserDefaults.standard
            let vibrationOn = userDefaults.bool(forKey: C.VIBRATION_ON)
            cell.stackView.addArrangedSubview(vibrationSwitch)
            vibrationSwitch.addTarget(self, action: #selector(vibrationSwitchStateChanged(_:)), for: .valueChanged)
            vibrationSwitch.isOn = vibrationOn
            vibrationSwitch.onTintColor = Theme.colorSecondary
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
        print("toggleSettingInUserDefaults")
        let userDefaults = UserDefaults.standard
        if isOn {
            userDefaults.set(true, forKey: setting)
        } else {
            userDefaults.set(false, forKey: setting)
        }
    }
    
    
    @objc private func soundSwitchStateChanged(_ sender: UISwitch) {
        toggleSettingInUserDefaults(isOn: sender.isOn, setting: C.SOUND_ON)
    }
    
    
    @objc private func vibrationSwitchStateChanged(_ sender: UISwitch) {
        toggleSettingInUserDefaults(isOn: sender.isOn, setting: C.VIBRATION_ON)
    }
    
}
