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
    
    @IBOutlet weak var progressBar: UIProgressView!
    @IBOutlet weak var levelNumberLabel: UILabel!
    @IBOutlet weak var infoLabelButton: UIButton!
    @IBOutlet weak var infoLabel: NumberLabel!
    
    private var adManager: AdManager?
    
    // MARK: View did load
    override func viewDidLoad() {
        super.viewDidLoad()
        adManager = AdManager()
        subscribeToLevelChange()
        subscribeToSolutionCheckResult()
        Database.shared.changeGameLevel(nr: 1)
    }
    
    
    // MARK: Listen level changes
    private func subscribeToLevelChange()
    {
        Database.shared.levelChangedClosure = { [weak self] in
            guard let self = self else { return }
            print("Current level changed to \(Database.shared.currentLevel?.levelNumber)")
            loadMainScene()
            let levelNr = Database.shared.currentLevel!.levelNumber
            levelNumberLabel.text = String(levelNr)
            changeProgressBarUI(levelNr: Int(levelNr))
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
                    //let messages = ["Wrong solution!"]
                    self.animateMessageSequence(
                        messages: messages,
                        isLastMessageTapToContinue: true
                    )
                }
                //TODO: animation with messages
            case .wrong:
                print("Wrong solution. (GameViewController)")
                feedbackGenerator.notificationOccuredCustom(notificationType: .error)
                gameScene?.shakeAnimation()
                //TODO: animation with messages
            case .bogus_palindrome:
                print("Bogus solution. (GameViewController)")
                feedbackGenerator.notificationOccuredCustom(notificationType: .warning)
                gameScene?.shakeAnimation()
                //TODO: animation with messages
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
    }
    
    
    // MARK: UI mode change
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        loadMainScene()
    }
    
    
    // MARK: TAP info label (button)
    @IBAction func infoLabelToucheDown(_ sender: UIButton) {
        print("infoLabelToucheDown")
        StateManager.shared.currentState = .reset
        Database.shared.changeToNextLevel()
        let nextLevelSoundAction = SKAction.playSound(soundFile: "level_transition.wav")
        self.gameScene?.run(nextLevelSoundAction)
        infoLabel.alpha = 0.0
        infoLabel.text = String()
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
    
    
    // MARK: Handle earned rewards
    override func viewDidAppear(_ animated: Bool) {
        print("viewDidAppear")
        guard adManager != nil else { return }
        switch adManager!.rewardToBeGiven {
        case Reward.letter:
            rewardWithLetter()
        case Reward.hint:
            rewardWithHint()
        case Reward.none:
            print("NO reward!")
        }
    }
    
    
    // MARK: Reward with letter
    private func rewardWithLetter()
    {
        print("REWARD GIVEN: Moving letter 'A' to first position")
    }
    
    
    // MARK: Reward with hint
    private func rewardWithHint()
    {
        let modalViewController = UIViewController()
        modalViewController.modalPresentationStyle = .formSheet
        modalViewController.view.backgroundColor = Theme.backgroundColor
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dismissModal))
        modalViewController.view.addGestureRecognizer(tapGesture)
        
        let wordLabel = UILabel()
        wordLabel.text = Database.shared.currentLevel!.solutionsArr.first?.hint
        wordLabel.textAlignment = .center
        wordLabel.font = UIFont(name: Theme.letterFont, size: 24)
        wordLabel.frame = CGRect(x: 0, y: 0, width: 200, height: 200)
        wordLabel.center = modalViewController.view.center
        modalViewController.view.addSubview(wordLabel)
        
        present(modalViewController, animated: true, completion: nil)
    }
    
    
    @objc func dismissModal() {
        dismiss(animated: true, completion: nil)
    }
    
    
    // MARK: TAP Hints
    @IBAction func onHintsButtonTap(_ sender: Any) {
        
        let sheet = ReactiveAlertController.hintsSheet {
            if (StateManager.shared.currentState == GameState.play)
            {
                print("onLetterRewardedAdsTap")
                self.adManager?.show(viewController: self, rewardType: Reward.letter)
            }
            
        } onHintRewardedAdsTap: {
            if (StateManager.shared.currentState == GameState.play)
            {
                print("onHintRewardedAdsTap")
                self.adManager?.show(viewController: self, rewardType: Reward.hint)
            }
        } onUnlimitedLettersPurchaseTap: {
            print("onUnlimitedLettersPurchaseTap")
        } onUnlimitedHintsPurchaseTap: {
            print("onUnlimitedHintsPurchaseTap")
        } onRemoveIntermediateAdsPurchaseTap: {
            print("onRemoveIntermediateAdsPurchaseTap")
        } onAllInPackPurchaseTap: {
            print("onAllInPackPurchaseTap!")
        }
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
    
    
    // MARK: Animate messages
    private func animateMessageSequence(messages: [String], isLastMessageTapToContinue:Bool = false) {
        var localMessages = messages
        guard let nextMessage = localMessages.popLast() else { return }
        infoLabel.alpha = 0.0
        infoLabel.text = nextMessage
        
        // Define your animation parameters here
        var duration = 0.5
        let appearanceDelay = 0.0
        let disappearanceDelay = 0.4
        if (nextMessage == C.SCORE_TAG)
        {
            duration = duration * 2
            //TODO: Ask scores straight from database?
            infoLabel.animate(endNumber: 300, maxNumber: 300, duration: duration / 2)
            let tickingSoundAction = SKAction.playSound(soundFile: "score_tick.wav")
            self.gameScene?.run(tickingSoundAction)
        }
        
        UIView.animate(withDuration: duration, delay: appearanceDelay, animations: {
            self.infoLabel.alpha = 1.0
        }, completion: { [self] _ in
            if (isLastMessageTapToContinue && messages.count == 1)
            {
                infoLabelButton.isEnabled = true
            }
            else {
                
                UIView.animate(withDuration: duration, delay: disappearanceDelay, animations: {
                    self.infoLabel.alpha = 0.0
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
