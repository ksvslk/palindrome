//
//  GameViewController.swift
//  test
//
//  Created by ksvslk on 16.04.2023.
//

import UIKit
import SpriteKit
import GameplayKit

class GameViewController: UIViewController {
    
    @IBAction func onSettingsButtonTap(_ sender: Any) {
        let sheet = UIAlertController.settingsSheet {
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
        } onPlayerLeaderBoardTap: {
            print("onPlayerLeaderBoardTap")
        } onAchievementsTap: {
            print("onAchievementsTap")
        }
        present(sheet, animated: true, completion: nil)
    }
    
    
    // MARK: Load main scene
    private func loadMainScene()
    {
        Database.shared.changeGameLevel(nr:1)
       
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
