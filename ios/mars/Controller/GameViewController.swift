//
//  GameViewController.swift
//  test
//
//  Created by ksvslk on 16.04.2023.
//

import UIKit
import SpriteKit
import GameplayKit
import GameKit
import GoogleMobileAds
import StoreKit

class GameViewController: UIViewController, GKGameCenterControllerDelegate {
    
    private var gameScene: GameScene?
    
    @IBOutlet weak var hintToolbarItem: UIBarButtonItem!
    @IBOutlet weak var progressBar: UIProgressView!
    @IBOutlet weak var levelNumberLabel: UILabel!
    @IBOutlet weak var infoLabelButton: UIButton!
    
    
    private var adManager: AdManager?
    
    // MARK: View did load
    override func viewDidLoad() {
        super.viewDidLoad()
        adManager = AdManager()
        subscribeToLevelChange()
        subscribeToSolutionCheckResult()
        subscribeToHintsSuggestion()
        Database.shared.changeGameLevel(nr: 1)
        
    }
    
    
    // MARK: Listen level changes
    private func subscribeToLevelChange()
    {
        Database.shared.levelChangedClosure = { [weak self] in
            guard let self = self else { return }
            print("Current level changed to \(String(describing: Database.shared.currentLevel?.levelNumber))")
            loadMainScene()
            let levelNr = Int(Database.shared.currentLevel!.levelNumber)
            askForReview(levelNr: levelNr)
            levelNumberLabel.text = String(levelNr)
            changeProgressBarUI(levelNr: levelNr)
        }
    }
    
    
    private func askForReview(levelNr:Int)
    {
        if (levelNr == 15)
        {
            guard let windowScene = self.view.window?.windowScene else { return }
            SKStoreReviewController.requestReview(in: windowScene)
        }
    }
    
    
    // MARK: Listen hint interval
    private func subscribeToHintsSuggestion()
    {
        Database.shared.hintsSuggestionClosure = {
            let popSoundAction = SKAction.playSound(soundFile: "hint_notification.wav")
            self.gameScene?.run(popSoundAction)
            self.hintToolbarItem.addBadge(number: 1, andColor: Theme.hintsNotificationColor)
            Toast.show(message: "You can find hints below ↙️", controller: self, timeShown: 4.0)
        }
    }
    
    
    // MARK: Listen solution changes
    private func subscribeToSolutionCheckResult()
    {
        let feedbackGenerator = UINotificationFeedbackGenerator()
        
        Database.shared.solutionCheckResultClosure = { [weak self] result in
            guard let self = self else { return }
            switch result {
            case .correct:
                print("Correct solution! (GameViewController)")
                feedbackGenerator.notificationOccuredCustom(notificationType: .success)
                StateManager.shared.currentState = .completed
                gameScene?.winningAnimation {
                    print("Show now messages of a great win in GameViewController!")
                    let messages = ["Tap to continue", C.SCORE_TAG, "Great job!"]
                    self.animateMessageSequence(
                        messages: messages,
                        isLastMessageTapToContinue: true
                    )
                }
            case .wrong:
                print("Wrong solution. (GameViewController)")
                feedbackGenerator.notificationOccuredCustom(notificationType: .error)
                gameScene?.shakeAnimation()
                self.animateMessageSequence(
                    messages: ["Wrong solution!"],
                    isLastMessageTapToContinue: false
                )
            case .bogus_palindrome:
                print("Bogus solution. (GameViewController)")
                feedbackGenerator.notificationOccuredCustom(notificationType: .warning)
                gameScene?.shakeAnimation()
                self.animateMessageSequence(
                    messages: ["Bogus solution!"],
                    isLastMessageTapToContinue: false
                )
            case .invalid_guess:
                print("Not enough letters. (GameViewController)")
                StateManager.shared.currentState = .play
            }
        }
    }
    
    
    // MARK: Load main scene
    private func loadMainScene()
    {
        guard let scene = GKScene(fileNamed: "GameScene"),
              let gameScene = scene.rootNode as? GameScene,
              let view = self.view as? SKView else {
            return
        }
        
        gameScene.backgroundColor = Theme.backgroundColor
        
        view.presentScene(gameScene, transition: SKTransition.push(with: SKTransitionDirection.left, duration: 1.0))
        view.ignoresSiblingOrder = true
        view.showsFPS = true
        view.showsNodeCount = true
        view.isMultipleTouchEnabled = false
        
        self.gameScene = gameScene // Set the global variable to the gameScene instance
        hintToolbarItem.removeBadge()
    }
    
    
    // MARK: UI mode change
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        loadMainScene()
    }
    
    @IBAction func infoButtonTouch(_ sender: Any) {
        print("infoLabelToucheDown")
        StateManager.shared.currentState = .reset
        Database.shared.changeToNextLevel()
        let nextLevelSoundAction = SKAction.playSound(soundFile: "level_transition.wav")
        self.gameScene?.run(nextLevelSoundAction)
        infoLabelButton.alpha = 0.0
        infoLabelButton.titleLabel?.text = String()
        infoLabelButton.isEnabled = false
    }
    
    
    // MARK: TAP Settings
    @IBAction func onSettingsButtonTap(_ sender: Any) {
        let sheet = UIAlertController.settingsSheet {
            print("onTutorialTap")
        } onFeedbackTap: {
            print("onFeedbackTap")
            guard let writeReviewURL = URL(string: C.APP_FEEDBACK_URL)
            else { fatalError("Expected a valid URL") }
            UIApplication.shared.open(writeReviewURL, options: [:], completionHandler: nil)
        } onShareTap: {
            print("onShareTap")
            let items: [Any] = ["share_description"~, URL(string: C.APP_URL)!]
            let share = UIActivityViewController(activityItems: items, applicationActivities: nil)
            if let popoverController = share.popoverPresentationController {
                popoverController.barButtonItem = sender as? UIBarButtonItem
            }
            self.present(share, animated: true)
        }
        if let popoverController = sheet.popoverPresentationController {
            popoverController.barButtonItem = sender as? UIBarButtonItem
        }
        present(sheet, animated: true, completion: nil)
    }
    
    
    // MARK: TAP Levels
    @IBAction func onLevelsButtonTap(_ sender: Any) {
        
        let sheet = UIAlertController.levelSelectSheet {
            print("onSubmitLevelTap")
        } onPlayerLevelsTap: {
            print("onPlayerLevelsTap")
        } onGameLevelsTap: {
            print("onGameLevelsTap")
            self.present(GameLevelSelectController(), animated: true, completion: nil)
        }
        if let popoverController = sheet.popoverPresentationController {
            popoverController.barButtonItem = sender as? UIBarButtonItem
        }
        present(sheet, animated: true, completion: nil)
    }
    
    
    
    // MARK: TAP Hints
    @IBAction func onHintsButtonTap(_ sender: Any) {
        hintToolbarItem.removeBadge()
        let sheet = ReactiveAlertController.hintsSheet(
            isHintsPremium: Database.shared.isCategoryRewarded(),
            isLettersPremium: Database.shared.isPremiumLetters(),
            onLetterRewardedAdsTap: {
                if (Database.shared.isPremiumLetters())
                {
                    if (StateManager.shared.currentState == GameState.play)
                    {
                        self.rewardWithLetter()
                    }
                }
                else {
                    if (StateManager.shared.currentState == GameState.play)
                    {
                        print("onLetterRewardedAdsTap")
                        self.adManager?.show(viewController: self, rewardType: Reward.letter)
                    }
                }
            }, onHintRewardedAdsTap: {
                if (Database.shared.isCategoryRewarded())
                {
                    self.rewardWithHint()
                }
                else {
                    if (StateManager.shared.currentState == GameState.play)
                    {
                        print("onHintRewardedAdsTap")
                        self.adManager?.show(viewController: self, rewardType: Reward.hint)
                    }
                }
            },
            onUnlimitedLettersPurchaseTap: {
                
            }, onUnlimitedHintsPurchaseTap: {
                
            }, onRemoveIntermediateAdsPurchaseTap: {
                
            }, onAllInPackPurchaseTap: {
            })
        sheet.updateAdsActions(adAvailable: adManager!.adAvailable)
        
        adManager?.delegate = sheet
        if let popoverController = sheet.popoverPresentationController {
            popoverController.barButtonItem = sender as? UIBarButtonItem
        }
        present(sheet, animated: true, completion: nil)
    }
    
    
    // MARK: TAP GameCenter
    @IBAction func onGameCenterButtonTap(_ sender: Any)
    {
        let sheet = UIAlertController.gameCenterSheet {
            print("onGameLeaderBoardTap")
            let viewController = GKGameCenterViewController(
                leaderboardID: C.GAME_LEADERBOARD,
                playerScope: .global,
                timeScope: .allTime)
            viewController.gameCenterDelegate = self
            self.present(viewController, animated: true, completion: nil)
        } onPlayerLeaderBoardTap: {
            print("onPlayerLeaderBoardTap")
            let viewController = GKGameCenterViewController(
                leaderboardID: C.PLAYER_LEADERBOARD,
                playerScope: .global,
                timeScope: .allTime)
            viewController.gameCenterDelegate = self
            self.present(viewController, animated: true, completion: nil)
        } onAchievementsTap: {
            print("onAchievementsTap")
            let viewController = GKGameCenterViewController(state: .achievements)
            viewController.gameCenterDelegate = self
            self.present(viewController, animated: true, completion: nil)
        }
        if let popoverController = sheet.popoverPresentationController {
            popoverController.barButtonItem = sender as? UIBarButtonItem
        }
        present(sheet, animated: true, completion: nil)
    }
    
    
    // MARK: View did appear
    override func viewDidAppear(_ animated: Bool) {
        print("viewDidAppear")
        guard adManager != nil else { return }
        switch adManager!.rewardToBeGiven {
        case Reward.letter:
            rewardWithLetter()
        case Reward.hint:
            rewardWithHint()
            Database.shared.setCategoryAsRewarded()
        case Reward.none:
            print("NO reward!")
        }
    }
    
    
    // MARK: Reward with letter
    private func rewardWithLetter()
    {
        StateManager.shared.currentState = .animating
        gameScene?.cellDirector.rewardWithRandomLetter()
        let rewardSound = SKAction.playSound(soundFile: "hint_revealed.wav")
        self.gameScene?.run(rewardSound)
    }
    
    
    // MARK: Reward with hint
    private func rewardWithHint()
    {
        let rewardSound = SKAction.playSound(soundFile: "hint_revealed.wav")
        let hintViewController = HintViewController()
        
        present(hintViewController, animated: true, completion: {
            let textToReveal = Database.shared.currentLevel?.solutionsArr.first?.hint ?? ""
            var currentIndex = 0
            self.gameScene?.run(rewardSound)
            Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { timer in
                let currentText = String(textToReveal.prefix(currentIndex))
                hintViewController.label.text = currentText
                currentIndex += 1
                
                if currentIndex > textToReveal.count {
                    timer.invalidate()
                }
            }
        })
    }
    
    
    @objc func dismissModal() {
        dismiss(animated: true, completion: nil)
    }
    
    
    // MARK: Animate messages
    private func animateMessageSequence(messages: [String], isLastMessageTapToContinue:Bool = false) {
        var localMessages = messages
        guard let nextMessage = localMessages.popLast() else { return }
        infoLabelButton.alpha = 0.0
        infoLabelButton.setTitle(nextMessage, for: .normal)
        
        // Define your animation parameters here
        var duration = 1.0
        let appearanceDelay = 0.0
        let disappearanceDelay = 0.4
        if (nextMessage == C.SCORE_TAG)
        {
            //TODO: Ask scores straight from database?
            var currentNumber = 30
            let targetNumber = 100
            let animationDuration = duration * 2 // Animation duration in seconds
            let animationInterval = animationDuration / Double(targetNumber)  // Time interval between number updates
            let tickingSoundAction = SKAction.playSound(soundFile: "score_tick.wav")
            self.gameScene?.run(tickingSoundAction)
            self.infoLabelButton.setTitle("\(currentNumber) of \(targetNumber) points", for: .normal)
        }
        
        UIView.animate(withDuration: duration, delay: appearanceDelay, animations: {
            self.infoLabelButton.alpha = 1.0
        }, completion: { [self] _ in
            if (isLastMessageTapToContinue && messages.count == 1)
            {
                infoLabelButton.isEnabled = true
            }
            else {
                UIView.animate(withDuration: duration, delay: disappearanceDelay, animations: {
                    self.infoLabelButton.alpha = 0.0
                }, completion: { _ in
                    self.animateMessageSequence(
                        messages: localMessages,
                        isLastMessageTapToContinue: isLastMessageTapToContinue
                    )
                })
            }
        })
    }
    
    
    // MARK: Change progress bar
    private func changeProgressBarUI(levelNr:Int)
    {
        if (levelNr <= 100)
        {
            let progressPercentage = levelNr
            let progressValue = Float(progressPercentage) / 100.0
            progressBar.setProgress(progressValue, animated: true)
        }
        // TODO: Handle player levels
        
    }
    
    
    // MARK: UI orientation
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        if UIDevice.current.userInterfaceIdiom == .phone {
            return .allButUpsideDown
        } else {
            return .all
        }
    }
    
    
    // MARK: Status bar
    override var prefersStatusBarHidden: Bool {
        return true
    }
    
    
    //MARK: Dismiss GameCenter
    func gameCenterViewControllerDidFinish(_ gameCenterViewController: GKGameCenterViewController) {
        // Dismiss the view controller.
        gameCenterViewController.dismiss(animated:true)
    }
    
}
