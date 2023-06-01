//
//  LevelSelectController.swift
//  mars
//
//  Created by Mina on 26.05.2023.
//

import Foundation

class LevelSelectController: BaseMenuController {
    
    override var menuItem: [MenuItemInfo] {
        return [
          MenuItemInfo(name: C.gameLevelsButton.0, comment: "", systemImage: C.gameLevelsButton.1, closure: {menuItem in
            print("Tapped \(C.gameLevelsButton.0)")
            self.present(GameLevelSelectController(dismissLevelSelectMenu: {
                self.dismiss(animated: true)
            }), animated: true, completion: nil)
           
        }),
          MenuItemInfo(name: C.tutorialButton.0, comment: "", systemImage: C.tutorialButton.1, closure: {menuItem in
              print("Tapped \(C.tutorialButton.0)")
              let tutorialView = TutorialViewController()
              self.present(tutorialView, animated: true)
          })
        ]
    }
   
}


