//
//  LevelSelectController.swift
//  mars
//
//  Created by Mina on 26.05.2023.
//

import Foundation

class LevelSelectController: BaseMenuController {
    
    override var menuItem: [MenuItemInfo] {
        return [MenuItemInfo(name: C.submitLevelButton.0, comment: "", systemImage: C.submitLevelButton.1, closure: {menuItem in
            print("Tapped \(C.submitLevelButton.0)")
        }),
                MenuItemInfo(name: C.playerLevelsButton.0, comment: "", systemImage: C.playerLevelsButton.1, closure: {menuItem in
            print("Tapped \(C.playerLevelsButton.0)")
        }),
                MenuItemInfo(name: C.gameLevelsButton.0, comment: "", systemImage: C.gameLevelsButton.1, closure: {menuItem in
            print("Tapped \(C.gameLevelsButton.0)")
            self.present(GameLevelSelectController(dismissLevelSelectMenu: {
                self.dismiss(animated: true)
            }), animated: true, completion: nil)
            
        })]
    }
   
}


