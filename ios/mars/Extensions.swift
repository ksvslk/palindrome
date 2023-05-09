//
//  Extensions.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation
import SpriteKit


// MARK: Distance between CGPoint's
extension CGPoint {
    func distance(point: CGPoint) -> CGFloat {
        return CGFloat(hypotf(Float(point.x - self.x), Float(point.y - self.y)))
    }
}


// MARK: Slice String to equal parts
extension String {
    func chunked(into size: Int) -> [String] {
        var result = [String]()
        for i in stride(from: 0, to: self.count, by: size) {
            let startIndex = self.index(self.startIndex, offsetBy: i)
            let endIndex = self.index(startIndex, offsetBy: size, limitedBy: self.endIndex) ?? self.endIndex
            var chunk = String(self[startIndex..<endIndex])
            if chunk.count < size {
                chunk = chunk.padding(toLength: size, withPad: " ", startingAt: 0)
            }
            result.append(chunk)
        }
        return result
    }
}


extension UIAlertController {
    
    // MARK: Game center menu
    static func gameCenterSheet(
        onGameLeaderBoardTap: @escaping () -> Void,
        onPlayerLeaderBoardTap: @escaping () -> Void,
        onAchievementsTap: @escaping () -> Void
    ) -> UIAlertController {
        let alertController =
        UIAlertController(title: "Game Center", message: "Compete and complete!", preferredStyle: .actionSheet)
        alertController.view.tintColor = Theme.colorOnBackground
        
        let gameLeaderboardAction = UIAlertAction(title: "Game Leaderboard", style: .default) { (action:UIAlertAction) in
            onGameLeaderBoardTap()
        }
        
        let playerLeaderboardAction = UIAlertAction(title: "Player Leaderboard", style: .default) { (action:UIAlertAction) in
            onPlayerLeaderBoardTap()
        }
        
        let achievementsAction = UIAlertAction(title: "Achievements", style: .default) { (action:UIAlertAction) in
            onAchievementsTap()
        }
        
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) { (action:UIAlertAction) in
            // Handle Cancel
        }
        alertController.addAction(gameLeaderboardAction)
        alertController.addAction(playerLeaderboardAction)
        alertController.addAction(achievementsAction)
        alertController.addAction(cancelAction)
        return alertController
    }
    
    
    // MARK: Hints menu
    static func hintsSheet(
        onLetterRewardedAdsTap: @escaping () -> Void,
        onHintRewardedAdsTap: @escaping () -> Void,
        onUnlimitedLettersPurchaseTap: @escaping () -> Void,
        onUnlimitedHintsPurchaseTap: @escaping () -> Void,
        onRemoveIntermediateAdsPurchaseTap: @escaping () -> Void,
        onAllInPackPurchaseTap: @escaping () -> Void
    ) -> UIAlertController {
        let alertController =
        UIAlertController(title: "Hints", message: "Sometimes lose, always win!", preferredStyle: .actionSheet)
        alertController.view.tintColor = Theme.colorOnBackground
        
        let letterRewardedAdsAction = UIAlertAction(title: "LETTER: Watch Ad ▶️", style: .default) { (action:UIAlertAction) in
            onLetterRewardedAdsTap()
        }
        
        let hintRewardedAdsAction = UIAlertAction(title: "HINT: Watch Ad ▶️", style: .default) { (action:UIAlertAction) in
            onHintRewardedAdsTap()
        }
        
        let unlimitedLettersPurchaseAction = UIAlertAction(title: "Unlimited Letters 💰", style: .default) { (action:UIAlertAction) in
            onUnlimitedLettersPurchaseTap()
        }
        
        let unlimitedHintsPurchaseAction = UIAlertAction(title: "Unlimited Hints 💰", style: .default) { (action:UIAlertAction) in
            onUnlimitedHintsPurchaseTap()
        }
        
        let removeIntermediateAdsPurchaseAction = UIAlertAction(title: "Remove Intermediate Ads 💰", style: .default) { (action:UIAlertAction) in
            onRemoveIntermediateAdsPurchaseTap()
        }
        
        let allInPackPurchaseAction = UIAlertAction(title: "All-In Pack 💰", style: .default) { (action:UIAlertAction) in
            onAllInPackPurchaseTap()
        }
        
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) { (action:UIAlertAction) in
            // Handle Cancel
        }
        
        alertController.addAction(letterRewardedAdsAction)
        alertController.addAction(hintRewardedAdsAction)
        alertController.addAction(unlimitedLettersPurchaseAction)
        alertController.addAction(unlimitedHintsPurchaseAction)
        alertController.addAction(removeIntermediateAdsPurchaseAction)
        alertController.addAction(allInPackPurchaseAction)
        alertController.addAction(cancelAction)
        return alertController
    }
    
    
    // MARK: Level select menu
    static func levelSelectSheet(
        onSubmitLevelTap: @escaping () -> Void,
        onPlayerLevelsTap: @escaping () -> Void,
        onGameLevelsTap: @escaping () -> Void
    ) -> UIAlertController {
        let alertController =
        UIAlertController(title: "Levels", message: "Play community levels or submit yours!", preferredStyle: .actionSheet)
        alertController.view.tintColor = Theme.colorOnBackground
        
        let submitLevelAction = UIAlertAction(title: "Submit Your Level", style: .default) { (action:UIAlertAction) in
            onSubmitLevelTap()
        }
        
        let playerLevelsAction = UIAlertAction(title: "Player Levels", style: .default) { (action:UIAlertAction) in
            onPlayerLevelsTap()
        }
        
        let mainGameLevelsAction = UIAlertAction(title: "Main Game Levels", style: .default) { (action:UIAlertAction) in
            onGameLevelsTap()
        }
        
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) { (action:UIAlertAction) in
            // Handle Cancel
        }
        alertController.addAction(submitLevelAction)
        alertController.addAction(playerLevelsAction)
        alertController.addAction(mainGameLevelsAction)
        alertController.addAction(cancelAction)
        return alertController
    }
    
    
    // MARK: Settings menu
    static func settingsSheet(
        onTutorialTap: @escaping () -> Void,
        onFeedbackTap: @escaping () -> Void,
        onShareTap: @escaping () -> Void
    ) -> UIAlertController {
        let alertController =
        UIAlertController(title: "Settings", message: "Customize, learn and share!", preferredStyle: .actionSheet)
        alertController.view.tintColor = Theme.colorOnBackground
        
        let userDefaults = UserDefaults.standard

        let vibrationOn = userDefaults.bool(forKey: C.VIBRATION_ON)
        let soundOn = userDefaults.bool(forKey: C.SOUND_ON)
        
        let vibrationAction = UIAlertAction(title: vibrationOn ? "Vibration On" : "Vibration Off", style: .default) { (action:UIAlertAction) in
            userDefaults.set(!vibrationOn, forKey: C.VIBRATION_ON)
        }
        
        let soundAction = UIAlertAction(title: soundOn ? "Sound On" : "Sound Off", style: .default) { (action:UIAlertAction) in
            userDefaults.set(!soundOn, forKey: C.SOUND_ON)
        }
        
        let tutorialAction = UIAlertAction(title: "Tutorial", style: .default) { (action:UIAlertAction) in
            onTutorialTap()
        }
        
        let feedbackAction = UIAlertAction(title: "Give Feedback", style: .default) { (action:UIAlertAction) in
            onFeedbackTap()
        }
        
        let shareAction = UIAlertAction(title: "Share", style: .default) { (action:UIAlertAction) in
            onShareTap()
        }
        
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) { (action:UIAlertAction) in
            // Handle Cancel
        }

        alertController.addAction(vibrationAction)
        alertController.addAction(soundAction)
        alertController.addAction(tutorialAction)
        alertController.addAction(feedbackAction)
        alertController.addAction(shareAction)
        
        alertController.addAction(cancelAction)
        return alertController
    }
    
}

