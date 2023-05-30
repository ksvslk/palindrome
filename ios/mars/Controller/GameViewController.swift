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

class GameViewController: UIViewController, UIPopoverPresentationControllerDelegate {
    
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
        Database.shared.changeLevel(toLatest: true)
        
    }

    
    private func presentBottomSheet(sourceView: UIView)
    {
        let bottomSheetMenuVC = GameCenterController()
        presentPanModal(bottomSheetMenuVC,sourceView: sourceView, sourceRect: sourceView.bounds)
    }
    
    
    // MARK: Show intermediate ads
    private func showIntermediateAds(adsInterval:Interval)
    {
        if (adsInterval.currentCount == adsInterval.limitCount)
        {
            adManager?.show(viewController: self, rewardType: Reward.none)
        }
    }
    
    
    // MARK: Listen level changes
    private func subscribeToLevelChange()
    {
        Database.shared.levelChangedClosure = { [weak self] intervals in
            guard let self = self else { return }
            print("Intervals: \(intervals)")
          
            print("Current level changed to \(String(describing: Database.shared.currentLevel?.levelNumber))")
            loadMainScene()
            let levelNr = Int(Database.shared.currentLevel!.levelNumber)
            askForReview(levelNr: levelNr)
            levelNumberLabel.text = String(levelNr)
            changeProgressBarUI(levelNr: levelNr)
            if (Database.shared.currentLevel!.progress == Level.Progress.completed.rawValue)
            {
                infoLabelButton.alpha = 1.0
                infoLabelButton.setTitle("tap_to_continue"~, for: .normal)
                let tapGesture = UITapGestureRecognizer(target: self, action: #selector(tapToContinueTouched))
                view.addGestureRecognizer(tapGesture)
                gameScene?.cellDirector.cells.forEach({ cell in
                    if let letter = cell as? Letter {
                        letter.color = Theme.successBoxColor
                    }
                })
            }
            else {
                StateManager.shared.currentState = .reset
                hideInfoButton()
            }
        }
    }
    
    
    private func hideInfoButton()
    {
        infoLabelButton.alpha = 0.0
        infoLabelButton.titleLabel?.text = String()
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
        
        Database.shared.solutionCheckResultClosure = { [weak self] result, scores in
            guard let self = self else { return }
           
            switch result {
            case .correct:
                print("Correct solution! (GameViewController)")
                if (Database.shared.currentLevel!.progress != Level.Progress.completed.rawValue)
                {
                    feedbackGenerator.notificationOccuredCustom(notificationType: .success)
                    gameScene?.winningAnimation {
                        print("Show now messages of a great win in GameViewController!")
                        let messages = ["Tap to continue", C.SCORE_TAG, "Great job!"]
                        self.animateMessageSequence(
                            scores: scores,
                            messages: messages,
                            isLastMessageTapToContinue: true
                        )
                    }
                    Database.shared.setLevelCompletedImmediately()
                    
                }
                StateManager.shared.currentState = .completed
                if (scores.0 == scores.1)
                {
                    confettiBurst()
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

    
    // MARK: Tap to continue touch
    @objc func tapToContinueTouched() {
        print("Screen tapped!")
        if let gr = view.gestureRecognizers?.first {
            print("removing gesture recgonizer \(gr)")
            view.removeGestureRecognizer(gr)
        }
        if (Database.shared.currentLevel?.levelNumber == 100)
        {
            showEndingScreen()
        }
        Database.shared.changeToNextLevel()
        let nextLevelSoundAction = SKAction.playSound(soundFile: "level_transition.wav")
        self.gameScene?.run(nextLevelSoundAction)
      
     }
    
    
    // MARK: TAP Settings
    @IBAction func onSettingsButtonTap(_ sender: Any) {
        if let buttonView = (sender as! UIBarButtonItem).value(forKey: "view") as? UIView {
            let appDelegate = UIApplication.shared.delegate as! AppDelegate
            let settingsSheetMenu = SettingsViewController(purchaseManager: appDelegate.purchaseManager)
            presentPanModal(settingsSheetMenu, sourceView: buttonView, sourceRect: buttonView.bounds)
        }
    }
    
    
    // MARK: TAP Levels
    @IBAction func onLevelsButtonTap(_ sender: Any) {
        if let buttonView = (sender as! UIBarButtonItem).value(forKey: "view") as? UIView {
            let gameCenterSheetMenu = LevelSelectController()
            presentPanModal(gameCenterSheetMenu, sourceView: buttonView, sourceRect: buttonView.bounds)
        }
    }
    
    
    // MARK: TAP Hints
    @IBAction func onHintsButtonTap(_ sender: Any) {
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
     
        if let buttonView = (sender as! UIBarButtonItem).value(forKey: "view") as? UIView {
            let hintsMenu = HintsViewController(adManager: adManager, purchaseManager: appDelegate.purchaseManager, gameViewController: self)
            presentPanModal(hintsMenu, sourceView: buttonView, sourceRect: buttonView.bounds)
        }
        hintToolbarItem.removeBadge()
    }
    
    
    // MARK: TAP GameCenter
    @IBAction func onGameCenterButtonTap(_ sender: Any)
    {
        if let buttonView = (sender as! UIBarButtonItem).value(forKey: "view") as? UIView {
            let gameCenterSheetMenu = GameCenterController()
            presentPanModal(gameCenterSheetMenu, sourceView: buttonView, sourceRect: buttonView.bounds)
        }
    }
    

    private func handleRewards()
    {
        print("viewDidAppear")
        guard let adManagerChecked = adManager else { return }
        switch adManagerChecked.rewardToBeGiven {
        case Reward.letter:
            if (StateManager.shared.currentState != .completed) { rewardWithLetter() }
        case Reward.hint:
            rewardWithHint()
            Database.shared.setCategoryAsRewarded()
        case Reward.none:
            print("NO reward!")
        }
        adManagerChecked.rewardToBeGiven = .none
    }
    
    
    // MARK: View did appear
    override func viewDidAppear(_ animated: Bool) {
        handleRewards()
    }
    
    
    // MARK: Reward with letter
    func rewardWithLetter()
    {
        StateManager.shared.currentState = .animating
        gameScene?.cellDirector.rewardWithRandomLetter()
        let rewardSound = SKAction.playSound(soundFile: "hint_revealed.wav")
        self.gameScene?.run(rewardSound)
    }
    
    
    // MARK: Reward with hint
    func rewardWithHint()
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
    
    //MARK: Show ending screen
    private func showEndingScreen()
    {
        print("showEndingScreen")
        let hintViewController = EndingScreen()
        present(hintViewController, animated: true)
    }
    
    
    @objc func dismissModal() {
        dismiss(animated: true, completion: nil)
    }
    
    
    // MARK: Animate messages
    private func animateMessageSequence(scores: (Int, Int) = (-1,-1), messages: [String], isLastMessageTapToContinue:Bool = false) {
        var localMessages = messages
        guard let nextMessage = localMessages.popLast() else { return }
        infoLabelButton.alpha = 0.0
        infoLabelButton.setTitle(nextMessage, for: .normal)
        
        // Define your animation parameters here
        let duration = 1.0
        let appearanceDelay = 0.0
        let disappearanceDelay = 0.4
        if (nextMessage == C.SCORE_TAG)
        {
            let currentNumber = scores.0
            let targetNumber = scores.1
            let tickingSoundAction = SKAction.playSound(soundFile: "score_tick.wav")
            self.gameScene?.run(tickingSoundAction)
            self.infoLabelButton.setTitle("\(currentNumber) of \(targetNumber) points", for: .normal)
        }
        
        UIView.animate(withDuration: duration, delay: appearanceDelay, animations: {
            self.infoLabelButton.alpha = 1.0
        }, completion: { [self] _ in
            if (StateManager.shared.currentState != GameState.completed)
            {
                UIView.animate(withDuration: duration, delay: disappearanceDelay, animations: {
                    self.infoLabelButton.alpha = 0.0
                }, completion: nil)
                return
            }
            if (isLastMessageTapToContinue && messages.count == 1)
            {
                infoLabelButton.isEnabled = true
                let tapGesture = UITapGestureRecognizer(target: self, action: #selector(tapToContinueTouched))
                view.addGestureRecognizer(tapGesture)
            }
            else {
                
                UIView.animate(withDuration: duration, delay: disappearanceDelay, animations: {
                    self.infoLabelButton.alpha = 0.0
                }, completion: { _ in
                    if (StateManager.shared.currentState != GameState.completed)
                    {
                        self.hideInfoButton()
                        return
                    }
                    self.animateMessageSequence(
                        scores: scores,
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
 
    
}
