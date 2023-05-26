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
