//
//  SettingsViewController.swift
//  mars
//
//  Created by Mina on 26.05.2023.
//

import Foundation

class SettingsViewController: BaseMenuController {
    
    override var members: [MenuItemInfo] {
        return [MenuItemInfo(name: C.tutorialButton.0, comment: "", systemImage: C.tutorialButton.1, closure: {view in
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
   
}

// Some helpful code maybe
/*
 let userDefaults = UserDefaults.standard

 let vibrationOn = userDefaults.bool(forKey: C.VIBRATION_ON)
 let soundOn = userDefaults.bool(forKey: C.SOUND_ON)
 
 // Vibration button
 let vibrationAction = UIAlertAction(title: vibrationOn ? "Vibration On" : "Vibration Off", style: .default) { (action:UIAlertAction) in
     userDefaults.set(!vibrationOn, forKey: C.VIBRATION_ON)
 }
 vibrationAction.setValue(UIImage(systemName: vibrationOn ? "waveform" : "minus.circle"), forKey: "image")
 
 // Sound button
 let soundAction = UIAlertAction(title: soundOn ? "Sound On" : "Sound Off", style: .default) { (action:UIAlertAction) in
     userDefaults.set(!soundOn, forKey: C.SOUND_ON)
 }
 soundAction.setValue(UIImage(systemName: soundOn ? "speaker.3" : "minus.circle"), forKey: "image")
 */
