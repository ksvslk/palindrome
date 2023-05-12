//
//  MenuSheetExtensions.swift
//  mars
//
//  Created by Mina on 12.05.2023.
//

import Foundation
import UIKit

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
        gameLeaderboardAction.setValue(UIImage(systemName: "decrease.indent"), forKey: "image")
        
        let playerLeaderboardAction = UIAlertAction(title: "Player Leaderboard", style: .default) { (action:UIAlertAction) in
            onPlayerLeaderBoardTap()
        }
        playerLeaderboardAction.setValue(UIImage(systemName: "increase.indent"), forKey: "image")
        
        let achievementsAction = UIAlertAction(title: "Achievements", style: .default) { (action:UIAlertAction) in
            onAchievementsTap()
        }
        achievementsAction.setValue(UIImage(systemName: "rosette"), forKey: "image")
        
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
        
        let letterRewardedAdsAction = UIAlertAction(title: "Watch Ad for Letter", style: .default) { (action:UIAlertAction) in
            onLetterRewardedAdsTap()
        }
        letterRewardedAdsAction.setValue(UIImage(systemName: "video.circle"), forKey: "image")
        
        let hintRewardedAdsAction = UIAlertAction(title: "Watch Ad for Hint", style: .default) { (action:UIAlertAction) in
            onHintRewardedAdsTap()
        }
        hintRewardedAdsAction.setValue(UIImage(systemName: "video.circle"), forKey: "image")
        
        let unlimitedLettersPurchaseAction = UIAlertAction(title: "Unlimited Letters", style: .default) { (action:UIAlertAction) in
            onUnlimitedLettersPurchaseTap()
        }
        unlimitedLettersPurchaseAction.setValue(UIImage(systemName: "dollarsign.circle"), forKey: "image")
        
        let unlimitedHintsPurchaseAction = UIAlertAction(title: "Unlimited Hints", style: .default) { (action:UIAlertAction) in
            onUnlimitedHintsPurchaseTap()
        }
        unlimitedHintsPurchaseAction.setValue(UIImage(systemName: "dollarsign.circle"), forKey: "image")
        
        let removeIntermediateAdsPurchaseAction = UIAlertAction(title: "Remove Intermediate Ads", style: .default) { (action:UIAlertAction) in
            onRemoveIntermediateAdsPurchaseTap()
        }
        removeIntermediateAdsPurchaseAction.setValue(UIImage(systemName: "dollarsign.circle"), forKey: "image")
        
        let allInPackPurchaseAction = UIAlertAction(title: "All-In Pack", style: .default) { (action:UIAlertAction) in
            onAllInPackPurchaseTap()
        }
        allInPackPurchaseAction.setValue(UIImage(systemName: "dollarsign.circle"), forKey: "image")
        
        
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
        UIAlertController(title: "Levels", message: "Play all levels and send yours!", preferredStyle: .actionSheet)
        alertController.view.tintColor = Theme.colorOnBackground
        
        let submitLevelAction = UIAlertAction(title: "Submit Your Level", style: .default) { (action:UIAlertAction) in
            onSubmitLevelTap()
        }
        submitLevelAction.setValue(UIImage(systemName: "paperplane"), forKey: "image")
   
        
        let playerLevelsAction = UIAlertAction(title: "Player Levels", style: .default) { (action:UIAlertAction) in
            onPlayerLevelsTap()
        }
        playerLevelsAction.setValue(UIImage(systemName: "person.2"), forKey: "image")
        
        
        let mainGameLevelsAction = UIAlertAction(title: "Game Levels", style: .default) { (action:UIAlertAction) in
            onGameLevelsTap()
        }
        mainGameLevelsAction.setValue(UIImage(systemName: "square.stack.3d.up"), forKey: "image")
        
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
        vibrationAction.setValue(UIImage(systemName: vibrationOn ? "waveform" : "minus.circle"), forKey: "image")
        
        
        let soundAction = UIAlertAction(title: soundOn ? "Sound On" : "Sound Off", style: .default) { (action:UIAlertAction) in
            userDefaults.set(!soundOn, forKey: C.SOUND_ON)
        }
        soundAction.setValue(UIImage(systemName: soundOn ? "speaker.3" : "minus.circle"), forKey: "image")
        
        let tutorialAction = UIAlertAction(title: "Tutorial", style: .default) { (action:UIAlertAction) in
            onTutorialTap()
        }
        
       
        
        tutorialAction.setValue(UIImage(systemName: "questionmark.app"), forKey: "image")
        
        let feedbackAction = UIAlertAction(title: "Give Feedback", style: .default) { (action:UIAlertAction) in
            onFeedbackTap()
        }
        feedbackAction.setValue(UIImage(systemName: "square.and.pencil"), forKey: "image")
        
        let shareAction = UIAlertAction(title: "Share", style: .default) { (action:UIAlertAction) in
            onShareTap()
        }
        shareAction.setValue(UIImage(systemName: "square.and.arrow.up"), forKey: "image")
        
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

