//
//  SettingsViewController.swift
//  mars
//
//  Created by Mina on 26.05.2023.
//

import Foundation
import UIKit

class SettingsViewController: BaseMenuController {
    
    private let soundSwitch:UISwitch = UISwitch()
    private let vibrationSwitch:UISwitch = UISwitch()
    
    override var menuItem: [MenuItemInfo] {
        return [
            MenuItemInfo(name: C.soundButton.0, comment: "", systemImage: C.soundButton.1, closure: {view in
            print("Tapped \(C.soundButton.0)")
                self.soundSwitch.setOn(!self.soundSwitch.isOn, animated: true)
                self.soundSwitchStateChanged(self.soundSwitch)
        }),
            MenuItemInfo(name: C.vibrationButton.0, comment: "", systemImage: C.vibrationButton.1, closure: {view in
            print("Tapped \(C.vibrationButton.0)")
                self.vibrationSwitch.setOn(!self.vibrationSwitch.isOn, animated: true)
                self.vibrationSwitchStateChanged(self.vibrationSwitch)
        }),
            MenuItemInfo(name: C.tutorialButton.0, comment: "", systemImage: C.tutorialButton.1, closure: {view in
            print("Tapped \(C.tutorialButton.0)")
        }),
                MenuItemInfo(name: C.feedbackButton.0, comment: "", systemImage: C.feedbackButton.1, closure: {view in 
            print("Tapped \(C.feedbackButton.0)")
        }),
                MenuItemInfo(name: C.shareButton.0, comment: "", systemImage: C.shareButton.1, closure: {view in 
            print("Tapped \(C.shareButton.0)")
        }),
                MenuItemInfo(name: C.restoreButton.0, comment: "", systemImage: C.restoreButton.1, closure: {view in 
            print("Tapped \(C.restoreButton.0)")
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
