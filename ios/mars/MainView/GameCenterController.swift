//
//  GameCenterController.swift
//  mars
//
//  Created by Mina on 26.05.2023.
//

import Foundation
import GameKit
import PanModal

class GameCenterController: BaseMenuController, GKGameCenterControllerDelegate {

    override var menuItem: [MenuItemInfo] {
        return [MenuItemInfo(name: C.gameLeaderButton.0, comment: "", systemImage: C.gameLeaderButton.1, closure: {menuItem in
            //Swift.debugPrint("Tapped \(C.gameLeaderButton.0)")
            var viewController = GKGameCenterViewController(
                leaderboardID: C.GAME_LEADERBOARD,
                playerScope: .global,
                timeScope: .allTime)
            self.setDefaultSettings(for: &viewController)
            self.presentPanModal(viewController)
        }),
            MenuItemInfo(name: C.achievementsButton.0, comment: "", systemImage: C.achievementsButton.1, closure: {menuItem in
            //Swift.debugPrint("Tapped \(C.achievementsButton.0)")
            var viewController = GKGameCenterViewController(state: .achievements)
          
            self.setDefaultSettings(for: &viewController)
            self.presentPanModal(viewController)
        })]
    }
    
    
    private func setDefaultSettings(`for` viewController: inout GKGameCenterViewController)
    {
        viewController.gameCenterDelegate = self
        if UIDevice.current.userInterfaceIdiom == .pad {
            viewController.modalPresentationStyle = .popover
            let largerSize = CGSize(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height)
            viewController.preferredContentSize = largerSize
            viewController.popoverPresentationController?.sourceView = view
            viewController.popoverPresentationController?.sourceRect = CGRect(x: view.bounds.midX, y: view.bounds.midY, width: 0, height: 0)
        }
    }
    
    
    func gameCenterViewControllerDidFinish(_ gameCenterViewController: GKGameCenterViewController) {
        // Dismiss the view controller.
        gameCenterViewController.dismiss(animated:true)
    }
    
}

extension GKGameCenterViewController: PanModalPresentable {
    public var panScrollable: UIScrollView? {
        return nil
    }
}
