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
    
    
    enum HintButtonState {
        case ad_available
        case no_ads
        case hint_revealed
        case hint_waiting
    }
    
    
    // MARK: Hints menu
    static func hintsSheet(
        isHintsPremium: Bool,
        isLettersPremium: Bool,
        onLetterRewardedAdsTap: @escaping () -> Void,
        onHintRewardedAdsTap: @escaping () -> Void,
        onUnlimitedLettersPurchaseTap: @escaping () -> Void,
        onUnlimitedHintsPurchaseTap: @escaping () -> Void,
        onRemoveIntermediateAdsPurchaseTap: @escaping () -> Void,
        onAllInPackPurchaseTap: @escaping () -> Void
    ) -> ReactiveAlertController {

        let alertController =
        ReactiveAlertController(title:"hints_title"~, message: "hints_subtitle"~, preferredStyle: .actionSheet)
        alertController.view.tintColor = Theme.colorOnBackground
        alertController.premiumHint = isHintsPremium
        alertController.premiumLetter = isLettersPremium
        
        let videoCircleImage = UIImage(systemName: "video.circle")?.withTintColor(
            Theme.hintsNotificationColor,
            renderingMode: UIImage.RenderingMode.alwaysOriginal
        )
        
        let premiumIcon = UIImage(systemName: "cursorarrow.rays")?.withTintColor(
            Theme.successTextColor,
            renderingMode: UIImage.RenderingMode.alwaysOriginal
        )
        
        // Ads Letters button
        let letterRewardedAdsAction = UIAlertAction(title: "watch_letter_ad"~, style: .default) { (action:UIAlertAction) in
            onLetterRewardedAdsTap()
        }
        letterRewardedAdsAction.accessibilityIdentifier = "watch_letter_ad"
        letterRewardedAdsAction.setValue(isLettersPremium ? premiumIcon : videoCircleImage, forKey: "image")
        
        
        // Ads Hints button
        let hintRewardedAdsAction = UIAlertAction(title: "watch_hint_ad"~, style: .default) { (action:UIAlertAction) in
            onHintRewardedAdsTap()
        }
        hintRewardedAdsAction.accessibilityIdentifier = "watch_hint_ad"
        hintRewardedAdsAction.setValue(isHintsPremium ? premiumIcon : videoCircleImage, forKey: "image")
    
        
        
        // Purchase letters button
        let unlimitedLettersPurchaseAction = UIAlertAction(title: "purchase_letters"~, style: .default) { (action:UIAlertAction) in
            onUnlimitedLettersPurchaseTap()
        }
        
        let dollarSignImage = UIImage(systemName: "dollarsign.circle")?.withTintColor(
            Theme.playNewLevelColor,
            renderingMode: UIImage.RenderingMode.alwaysOriginal
        )
        unlimitedLettersPurchaseAction.setValue(dollarSignImage, forKey: "image")
        
        
        // Purchase hints button
        let unlimitedHintsPurchaseAction = UIAlertAction(title: "purchase_hints"~, style: .default) { (action:UIAlertAction) in
            onUnlimitedHintsPurchaseTap()
        }
        unlimitedHintsPurchaseAction.setValue(dollarSignImage, forKey: "image")
        
        
        // Purchase no ads button
        let removeIntermediateAdsPurchaseAction = UIAlertAction(title: "purchase_no_ads"~, style: .default) { (action:UIAlertAction) in
            onRemoveIntermediateAdsPurchaseTap()
        }
        removeIntermediateAdsPurchaseAction.setValue(dollarSignImage, forKey: "image")
        
        
        // Purchase all-in button
        let allInPackPurchaseAction = UIAlertAction(title: "purchase_all"~, style: .default) { (action:UIAlertAction) in
            onAllInPackPurchaseTap()
        }
        allInPackPurchaseAction.setValue(dollarSignImage, forKey: "image")
        
        
        let cancelAction = UIAlertAction(title: "cancel"~, style: .cancel) { (action:UIAlertAction) in
            // Handle Cancel
        }
        
        // ADD OR NOT? Purchased or not?
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
        
        
        // Tutorial button
        let tutorialAction = UIAlertAction(title: "Tutorial", style: .default) { (action:UIAlertAction) in
            onTutorialTap()
        }
        tutorialAction.setValue(UIImage(systemName: "questionmark.app"), forKey: "image")
        
        
        // Give feedback button
        let feedbackAction = UIAlertAction(title: "Give Feedback", style: .default) { (action:UIAlertAction) in
            onFeedbackTap()
        }
        feedbackAction.setValue(UIImage(systemName: "square.and.pencil"), forKey: "image")
        
        
        // Share button
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

postfix operator ~

postfix func ~ (string: String) -> String {
    let localizedString = NSLocalizedString(string, comment: "")
    assert(localizedString != string, "Localized string not found for key: \(string)")
    return localizedString
}
