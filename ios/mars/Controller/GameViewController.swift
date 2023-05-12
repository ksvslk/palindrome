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

class GameViewController: UIViewController, GKGameCenterControllerDelegate {
    
    @IBOutlet weak var infoLabel: NumberLabel!
    
    func gameCenterViewControllerDidFinish(_ gameCenterViewController: GKGameCenterViewController) {
        // Dismiss the view controller.
        gameCenterViewController.dismiss(animated:true)
    }
    
    
    @objc func handleLevelEnding(_ sender: UITapGestureRecognizer) {
        print("handleLevelEnding")
        self.infoLabel.alpha = 0.0
        self.infoLabel.text = String()
        self.view.removeGestureRecognizer(sender)
    }

    
    func animateMessageSequence(messages: [String], isLastMessageTapToContinue:Bool = false) {
        var localMessages = messages
        guard let nextMessage = localMessages.popLast() else { return }
        infoLabel.alpha = 0.0
        infoLabel.text = nextMessage
        
        // Define your animation parameters here
        var duration = 1.0
        let appearanceDelay = 0.0
        let disappearanceDelay = 1.0
        if (nextMessage == C.SCORE_TAG)
        {
            duration = 2.0
            //TODO: Ask scores straight from database?
            infoLabel.animate(endNumber: 300, maxNumber: 300, duration: duration / 2)
        }
        
        UIView.animate(withDuration: duration, delay: appearanceDelay, animations: {
            self.infoLabel.alpha = 1.0
        }, completion: { [self] _ in
            if (isLastMessageTapToContinue && messages.count == 1)
            {
                let tapGesture = UITapGestureRecognizer(target: self, action: #selector(self.handleLevelEnding(_:)))
                tapGesture.cancelsTouchesInView = true
                view.addGestureRecognizer(tapGesture)
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
    
    
    @IBAction func onSettingsButtonTap(_ sender: Any) {
        let sheet = UIAlertController.settingsSheet {
            //let messages = ["Tap to continue", C.SCORE_TAG, "Great job!"]
            let messages = ["Wrong solution!"]
            self.animateMessageSequence(
                messages: messages,
                isLastMessageTapToContinue: false
            )
            print("onTutorialTap")
        } onFeedbackTap: {
            print("onFeedbackTap")
        } onShareTap: {
            print("onShareTap")
        }

        present(sheet, animated: true, completion: nil)
    }
    
    
    @IBAction func onLevelsButtonTap(_ sender: Any) {
        let sheet = UIAlertController.levelSelectSheet {
            print("onSubmitLevelTap")
        } onPlayerLevelsTap: {
            print("onPlayerLevelsTap")
        } onGameLevelsTap: {
            print("onGameLevelsTap")
            self.present(GameLevelSelectController(), animated: true, completion: nil)
        }
        
        present(sheet, animated: true, completion: nil)
    }
    
    @IBAction func onHintsButtonTap(_ sender: Any) {
        let sheet = UIAlertController.hintsSheet {
            print("onLetterRewardedAdsTap")
        } onHintRewardedAdsTap: {
            print("onHintRewardedAdsTap")
        } onUnlimitedLettersPurchaseTap: {
            print("onUnlimitedLettersPurchaseTap")
        } onUnlimitedHintsPurchaseTap: {
            print("onUnlimitedHintsPurchaseTap")
        } onRemoveIntermediateAdsPurchaseTap: {
            print("onRemoveIntermediateAdsPurchaseTap")
        } onAllInPackPurchaseTap: {
            print("onAllInPackPurchaseTap!")
        }

        present(sheet, animated: true, completion: nil)
    }
    
    
    @IBAction func onGameCenterButtonTap(_ sender: Any)
    {
        let sheet = UIAlertController.gameCenterSheet {
            print("onGameLeaderBoardTap")
            self.showLeaderboard(name: "game_leaderboard")
        } onPlayerLeaderBoardTap: {
            print("onPlayerLeaderBoardTap")
            self.showLeaderboard(name: "player_leaderboard")
        } onAchievementsTap: {
            print("onAchievementsTap")
            // Display a list of achievements.
            let viewController = GKGameCenterViewController(state: .achievements)
            viewController.gameCenterDelegate = self
            self.present(viewController, animated: true, completion: nil)
        }
        present(sheet, animated: true, completion: nil)
    }
    
    
    private func showLeaderboard(name:String)
    {
        let viewController = GKGameCenterViewController(
                        leaderboardID: name,
                        playerScope: .global,
                        timeScope: .allTime)
        viewController.gameCenterDelegate = self
        self.present(viewController, animated: true, completion: nil)
    }
    
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        
        print("Collection view at row \(collectionView.tag) selected index path \(indexPath)")
    }
    
    
    // MARK: Load main scene
    private func loadMainScene(levelNr: Int = 1)
    {
        Database.shared.changeGameLevel(nr:levelNr)
       
        // Load 'GameScene.sks' as a GKScene. This provides gameplay related content
        // including entities and graphs.
        if let scene = GKScene(fileNamed: "GameScene") {
            
            // Get the SKScene from the loaded GKScene
            if let sceneNode = scene.rootNode as! GameScene? {
                
                // Set the scale mode to scale to fit the window
                sceneNode.backgroundColor = Theme.backgroundColor
                
                // Present the scene
                if let view = self.view as! SKView? {
                    view.presentScene(sceneNode, transition: SKTransition.crossFade(withDuration: 1))
                    view.ignoresSiblingOrder = true
                    view.showsFPS = true
                    view.showsNodeCount = true
                
                }
               
            }
        }
        
    }

    
    // MARK: UI mode change
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        loadMainScene()
    }
    
    
    // MARK: View did load
    override func viewDidLoad() {
        super.viewDidLoad()
        loadMainScene()
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
