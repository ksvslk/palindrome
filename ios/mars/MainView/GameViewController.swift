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
import Toast
import FirebaseAuth
import FirebaseFirestore
import FirebaseStorage


class GameViewController: UIViewController, UIPopoverPresentationControllerDelegate, SKSceneDelegate, MessageDelegate, GKGameCenterControllerDelegate {
    
    private var gameScene: GameScene?
    var tapGesture:UITapGestureRecognizer? = nil
    private var backgroundLayer:CALayer? = nil
    
    @IBOutlet weak var backgroundUIView: UIView!
    @IBOutlet weak var gameCenterButtonRef: UIBarButtonItem!
    @IBOutlet weak var hintToolbarItem: UIBarButtonItem!
    @IBOutlet weak var progressBar: UIProgressView!
    @IBOutlet weak var levelNumberLabel: UILabel!
    @IBOutlet weak var infoLabelButton: UIButton!
    @IBOutlet weak var snowToggle: UIImageView! {
        didSet {
            snowToggle.tintColor = Theme.shared.colors.onSurfaceVariant
            snowToggle.alpha = UserDefaults.standard.bool(forKey: C.SNOW) ? 1.0 : 0.5
        }
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        if let touch = touches.first {
            let positionInScene = touch.location(in: self.view)
            if (positionInScene.y < progressBar.layer.position.y) {
                UserDefaults.standard.set(snowToggle.alpha == 1.0 ? false : true, forKey: C.SNOW)
                snowToggle.alpha = snowToggle.alpha == 1.0 ? 0.5 : 1.0
                if (snowToggle.alpha == 1.0)
                {
                    self.gameScene?.addSnow()
                }
                else {
                    self.gameScene?.removeSnow()
                }
            }
         }
    }
    
    
    let goodMessages = ["GM_correct"~,"GM_good"~,"GM_keep_going"~,"GM_yes"~,"GM_hurray"~]
    let veryGoodMessages = ["VGM_awesome"~,"VGM_superb"~,"VGM_excellent"~,"VGM_perfect"~,"VGM_outstanding"~]
    let bogusMessages = ["BM_wrong_palindrome"~, "BM_forbidden_palindrome"~, "BM_bogus_palindrome"~]
    let wrongMessages = ["WM_try_again"~,"WM_not_a_palindrome"~, "WM_keep_trying"~, "WM_take_another_stab"~,"WM_palindrome_required"~,"WM_look_it_over"~]
    
    private var adManager: AdManager?
    
    
    private var promoDialogData:PromoDialogData?
    private var promoPictureUrl:URL = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("promo.jpg")
    private var promoMenuButtondata:PromoMenuButtonData?
    private var handle: AuthStateDidChangeListenerHandle?
    
    
    // MARK: View did load
    override func viewDidLoad() {
        //Swift.debugPrint("viewDidLoad")
        super.viewDidLoad()
        addBackgroundColorLayer()
        self.view.makeToastActivity(.center)
        self.view.isUserInteractionEnabled = false
        
        UserPrivacy().requestConsentInformation(viewController: self) { _ in
            DispatchQueue.main.async {
                GADMobileAds.sharedInstance().start { _ in
                    self.adManager = AdManager()
                    self.subscribeToLevelChange()
                    self.subscribeToSolutionCheckResult()
                    self.subscribeToHintsSuggestion()
                    self.moveInfoButtonVerticallyToTopCenter()
                    self.subscribeToPurchaseMessage()
                    self.logInToGameCenter()
                    Database.shared.loadFromCloudKeyValueStore()
                    self.enableGame()
                }
            }
        }
        menuColorsChange(doLayout: true)
        
        Theme.shared.addListener {
            if self.isViewLoaded && self.view.window != nil {
                self.menuColorsChange(doLayout: true)
            }
            
        }
   
    }

    
    
    //MARK: viewWillDisappear
    override func viewWillDisappear(_ animated: Bool) {
        Auth.auth().removeStateDidChangeListener(handle!)
    }
    
    //MARK: viewWillAppear
    override func viewWillAppear(_ animated: Bool) {
        handle = Auth.auth().addStateDidChangeListener { auth, user in
            guard let _ = user?.isAnonymous else { return }
            self.downloadPromos()
        }
    }
    
    //MARK: Download promos
    func downloadPromos() {
        if (promoDialogData == nil || promoMenuButtondata == nil)
        {
            let db = Firestore.firestore()
            db.collection("promos_ios").getDocuments(source: .server) { (querySnapshot, err) in
                if let err = err {
                    Swift.debugPrint("Error getting documents: \(err)")
                } else {
                    for document in querySnapshot!.documents {
                        if document.documentID == "dialog" {
                            document.reference.getDocument(as: PromoDialogData.self) { result in
                                switch result {
                                case .success(let dialogData):
                                    self.promoDialogData = dialogData
                                    self.downloadPromoPicture()
                                    Swift.debugPrint("Successfully deserialized dialog.", dialogData)
                                case .failure(let error):
                                    Swift.debugPrint("Error decoding dialog: \(error)")
                                }
                            }
                        } else if document.documentID == "menu_button" {
                            document.reference.getDocument(as: PromoMenuButtonData.self) { result in
                                switch result {
                                case .success(let menuData):
                                    self.promoMenuButtondata = menuData
                                    Swift.debugPrint("Successfully deserialized menu:", menuData)
                                case .failure(let error):
                                    Swift.debugPrint("Error decoding menu data: \(error)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    //MARK: Download promo picture
    private func downloadPromoPicture() {
        guard let picturePath = promoDialogData?.picturePath else { return }
        let storage = Storage.storage()
        let gsReference = storage.reference(forURL: picturePath)
        
        gsReference.write(toFile: promoPictureUrl) { url, error in
            if let error = error {
                Swift.debugPrint("Error: \(error.localizedDescription)")
            } else {
                Swift.debugPrint("Download successful. File available at \(url)")
                if let url = url {
                    self.promoPictureUrl = url
                    Swift.debugPrint("Set the promo picture to URL \(self.promoPictureUrl)")
                }
            }
        }
    }
    
    
    
    //MARK: Show promo dialog
    private func showPromoDialog(normalizedLevelNr:Int)
    {
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        if (appDelegate.purchaseManager?.isNoAdsPurchasedInAnyWay() == false)
        {
            if FileManager.default.fileExists(atPath: promoPictureUrl.path) {
                if (promoDialogData?.levelIds?.contains(normalizedLevelNr) == true)
                {
                    let alreadySeenAds = UserDefaults.standard.array(forKey: C.ADS_SHOWN) as? [Int]
                    if (alreadySeenAds?.contains(promoDialogData?.adNumber ?? -1) == false)
                    {
                        let promoDialogController = PromoDialogController()
                        promoDialogController.updateContent(
                            with: promoPictureUrl,
                            title: self.promoDialogData?.title ?? "",
                            subtitle: self.promoDialogData?.subtitle ?? "",
                            link: self.promoDialogData?.link ?? "",
                            adNumber: self.promoDialogData?.adNumber ?? -1
                        )
                        self.present(promoDialogController, animated: true, completion: nil)
                    }
                    
                }
            }
            else {
                Swift.debugPrint("Could not show promo dialog because to file at \(promoPictureUrl)")
            }
        }
        
    }
    
    
    private func menuColorsChange(doLayout:Bool = false)
    {
        Swift.debugPrint("menuColorsChange")
        snowToggle.tintColor = Theme.shared.colors.onSurfaceVariant
        backgroundUIView.tintColor = Theme.shared.colors.onSurfaceVariant
        progressBar.tintColor = Theme.shared.colors.primary
        levelNumberLabel.textColor = Theme.shared.colors.onSurfaceVariant
        infoLabelButton.tintColor = Theme.shared.colors.onSurfaceVariant
        infoLabelButton.setTitleColor(Theme.shared.colors.onSurfaceVariant, for: UIControl.State.normal)
        if (doLayout) {
            backgroundUIView.setNeedsLayout()
            backgroundUIView.layoutIfNeeded()
        }
        gameScene?.changeTheme()
    }
    
    
    
    func gameCenterViewControllerDidFinish(_ gameCenterViewController: GKGameCenterViewController) {
        gameCenterViewController.dismiss(animated: true, completion: nil)
    }
    
    
    private func addBackgroundColorLayer()
    {
        backgroundLayer?.removeFromSuperlayer()
        if let skView = self.view as? SKView {
            backgroundLayer = CALayer()
            if let backgroundLayer = backgroundLayer {
                backgroundLayer.backgroundColor = Theme.shared.colors.background.cgColor
                backgroundLayer.frame = skView.bounds
                skView.layer.addSublayer(backgroundLayer)
                backgroundLayer.zPosition = -1
            }
        }
    }
    
    
    private func logInToGameCenter()
    {
        GKLocalPlayer.local.authenticateHandler = { viewController, error in
            //Swift.debugPrint("Game Center auth: \(error)")
            if let _ = viewController {
                // Present the view controller so the player can sign in.
                //Swift.debugPrint("Game Center auth: show view controller because viewCOntroller is not nil")
                return
            }
            
            if error != nil {
                // Player could not be authenticated.
                // Disable Game Center in the game.
                return
            }
            // Player was successfully authenticated.
        }
    }
    
    
    private func enableGame()
    {
        self.view.hideToastActivity()
        self.view.isUserInteractionEnabled = true
        Database.shared.changeLevel(toLatest: true, levelPack: LevelPack.game_pack)
        self.showTutorial()
    }
    
    
    func didSendNewMessage(withMessage message: String) {
        dismissModal()
        self.showToastAboveToolbar(message: message)
    }
    
    
    private func subscribeToPurchaseMessage()
    {
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        appDelegate.purchaseManager.messageDelegate = self
    }
    
    
    private func moveInfoButtonVerticallyToTopCenter()
    {
        infoLabelButton.transform = CGAffineTransform(translationX: 0, y: -view.bounds.height / 4)
    }
    
    private func presentBottomSheet(sourceView: UIView)
    {
        let bottomSheetMenuVC = GameCenterController()
        presentPanModal(bottomSheetMenuVC,sourceView: sourceView, sourceRect: sourceView.bounds)
    }
    
    
    // MARK: Listen level changes
    private func subscribeToLevelChange()
    {
        Database.shared.levelChangedClosure = { [weak self] intervals in
            guard let self = self else { return }
            
            //Swift.debugPrint("Intervals: \(intervals)")
            //Swift.debugPrint("Current level changed to \(String(describing: Database.shared.currentLevel?.levelNumber))")
            
            let appDelegate = UIApplication.shared.delegate as! AppDelegate
            let levelNr = Database.shared.currentLevel!.levelNumber
            
            if intervals.first(where: { i in
                i.name == C.ADS_INTERVAL && i.currentCount == i.limitCount
            }) != nil && levelNr >= 15 && !appDelegate.purchaseManager.isNoAdsPurchasedInAnyWay() && adManager?.isInterstitialAdAvailable() == true {
                adManager?.showInterstitialAd(viewController: self)
            }
            else {
                executeLevelLoadActions(intervals: intervals)
            }
        }
    }
    
    
    //MARK: Execute level load actions
    private func executeLevelLoadActions(intervals:[Interval] = [])
    {
        loadMainScene()
        let levelNr = Int(Database.shared.currentLevel!.levelNumber)
        askForReview(levelNr: levelNr)
        levelNumberLabel.text = Database.shared.getLevelNrAndPackName()
        changeProgressBarUI(levelNr: levelNr)
        if (Database.shared.currentLevel!.levelProgress == Level.Progress.completed)
        {
            enableInfoButton()
            infoLabelButton.setTitle("tap_to_continue"~, for: .normal)
            addNextLevelGestureTap()
        }
        else {
            //Swift.debugPrint("Setting state to .reset")
            StateManager.shared.currentState = .reset
            disableInfoButton()
        }
        
        
        if intervals.first(where: { i in
            i.name == C.SCORES_INTERVAL && i.currentCount == i.limitCount
        }) != nil {
            loadScores()
        }
        gameScene?.changeTheme()
    }
    
    
    private func enableInfoButton()
    {
        infoLabelButton.alpha = 1.0
    }
    
    
    private func disableInfoButton()
    {
        
        if let tapGesture = tapGesture { view.removeGestureRecognizer(tapGesture) }
        infoLabelButton.alpha = 0.0
        infoLabelButton.titleLabel?.text = String()
    }
    
    
    //MARK: Ask for review
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
            self.hintToolbarItem.addBadge(number: 1, andColor: Theme.shared.colors.error)
            self.showToastAboveToolbar(message: "find_hints_below"~)
        }
    }
    
    
    private func showToastAboveToolbar(message:String)
    {
        let toastPosition = CGPoint(x: self.view.center.x, y: self.view.bounds.height - 115)
        self.view.makeToast(message, point: toastPosition, title: nil, image: nil, completion: nil)
    }
    
    
    // MARK: Listen solution changes
    private func subscribeToSolutionCheckResult()
    {
        let feedbackGenerator = UINotificationFeedbackGenerator()
        
        Database.shared.solutionCheckResultClosure = { [weak self] result, scores in
            guard let self = self else { return }
            print("solutionCheckResultClosure: \(result)")
            
            switch result {
            case .correct:
                //Swift.debugPrint("Correct solution! (GameViewController)")
                let isMaxScore = scores.0 == scores.1
                let currentLevel = Database.shared.currentLevel!
                //Swift.debugPrint("Correct solution! (GameViewController). \(currentLevel.levelNumber) progress is \(currentLevel.levelProgress)")
                if (currentLevel.levelProgress != Level.Progress.completed)
                {
                    //Swift.debugPrint("Level was not set to .completed in progress value in db.")
                    feedbackGenerator.notificationOccuredCustom(notificationType: .success)
                    let message = isMaxScore ? veryGoodMessages.randomElement()! : goodMessages.randomElement()!
                    gameScene?.winningAnimation {
                        //Swift.debugPrint("Show now messages of a great win in GameViewController!")
                        let messages = ["tap_to_continue"~, C.SCORE_TAG, message]
                        self.animateMessageSequence(
                            scores: scores,
                            messages: messages,
                            isLastMessageTapToContinue: true
                        )
                    }
                    
                    Database.shared.setLevelCompletedImmediately()
                    
                    if (isMaxScore)
                    {
                        let shouldAnimationPlay = VaryingProbability.isAnimationPlayed(level: Int(currentLevel.levelNumber))
                        if (shouldAnimationPlay) { gameScene?.moveTrainFromLeftToRight() }
                    }
                    StateManager.shared.currentState = .completed
                    if (isMaxScore)
                    {
                        confettiBurst(yAcceleration: 75)
                    }
                }
            case .wrong:
                //Swift.debugPrint("Wrong solution. (GameViewController)")
                if UIApplication.shared.applicationState == .active {
                    feedbackGenerator.notificationOccuredCustom(notificationType: .error)
                    gameScene?.shakeAnimation()
                    let randomWrongMessage = wrongMessages.randomElement()!
                    self.animateMessageSequence(
                        messages: [randomWrongMessage],
                        isLastMessageTapToContinue: false
                    )
                }
            case .bogus_palindrome:
                if UIApplication.shared.applicationState == .active {
                    //Swift.debugPrint("Bogus solution. (GameViewController)")
                    let bogusWrongMessage = bogusMessages.randomElement()!
                    feedbackGenerator.notificationOccuredCustom(notificationType: .warning)
                    gameScene?.shakeAnimation()
                    self.animateMessageSequence(
                        messages: [bogusWrongMessage],
                        isLastMessageTapToContinue: false
                    )
                }
            case .invalid_guess:
                //Swift.debugPrint("Not enough letters. (GameViewController)")
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
        
        gameScene.backgroundColor = Theme.shared.colors.background
        view.presentScene(gameScene, transition: SKTransition.push(with: SKTransitionDirection.left, duration: 1.0))
        view.ignoresSiblingOrder = true
        
#if DEBUG
        view.showsFPS = true
        view.showsNodeCount = true
#endif
        
        view.isMultipleTouchEnabled = false
        
        backgroundLayer?.removeFromSuperlayer()
        
        
        self.gameScene = gameScene // Set the global variable to the gameScene instance
        if (snowToggle.alpha == 1.0)
        {
            Swift.debugPrint("Add snow to: \(self.gameScene)")
            self.gameScene?.addSnow()
        }
        else {
            Swift.debugPrint("Remove snow from: \(self.gameScene)")
            self.gameScene?.removeSnow()
        }
        hintToolbarItem.removeBadge()
    }
    
    
    // MARK: Trait collection change
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        Theme.shared.changeTheme(name: nil)
        self.setToastStyle()
        menuColorsChange(doLayout: true)
        dismissModal()
    }
    
    
    // MARK: Split screen changes
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        print("viewWillTransition")
        
        coordinator.animate(alongsideTransition: { _ in
            // Update your UI based on the new size
        }, completion: { _ in
            
            self.adManager?.reloadRewardedAd()
            self.adManager?.reloadInterstitialAd()
            print("LOAD MAIN SCENE IN viewWillTransition")
            guard let level = Database.shared.currentLevel else { return }
            self.loadMainScene()
            let levelNr = Int(level.levelNumber)
            self.levelNumberLabel.text = Database.shared.getLevelNrAndPackName(levelNr: levelNr)
            self.changeProgressBarUI(levelNr: levelNr)
            
            if (level.levelProgress == Level.Progress.completed)
            {
                self.enableInfoButton()
                self.infoLabelButton.setTitle("tap_to_continue"~, for: .normal)
                self.addNextLevelGestureTap()
            }
            else {
                //Swift.debugPrint("Setting state to .reset")
                StateManager.shared.currentState = .reset
                self.disableInfoButton()
            }
        })
    }

    
    // MARK: Tap to continue touch
    @objc func tapToContinueTouched() {
        //Swift.debugPrint("Screen tapped!")
        if let gr = view.gestureRecognizers?.first {
            //Swift.debugPrint("removing gesture recgonizer \(gr)")
            view.removeGestureRecognizer(gr)
        }
        let normalizedLevelNr = Database.shared.getNormalizedLevelNumber()
        if (normalizedLevelNr == 100 || normalizedLevelNr == 25)
        {
            showEndingScreen()
        }
        showPromoDialog(normalizedLevelNr:normalizedLevelNr)
        Database.shared.changeToNextLevel()
        let nextLevelSoundAction = SKAction.playSound(soundFile: "level_transition.wav")
        self.gameScene?.run(nextLevelSoundAction)
        
    }
    
    
    // MARK: TAP Settings
    @IBAction func onSettingsButtonTap(_ sender: Any) {
        if let buttonView = (sender as! UIBarButtonItem).value(forKey: "view") as? UIView {
            let appDelegate = UIApplication.shared.delegate as! AppDelegate
            let settingsSheetMenu = SettingsViewController(purchaseManager: appDelegate.purchaseManager, promoMenudata: promoMenuButtondata)
            presentPanModal(settingsSheetMenu, sourceView: buttonView, sourceRect: buttonView.bounds)
            
        }
    }
    
    
    // MARK: TAP Levels
    @IBAction func onLevelsButtonTap(_ sender: Any) {
        if let buttonView = (sender as! UIBarButtonItem).value(forKey: "view") as? UIView {
            let appDelegate = UIApplication.shared.delegate as! AppDelegate
            let gameCenterSheetMenu = LevelSelectController(purchaseManager: appDelegate.purchaseManager)
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
        //Swift.debugPrint("handleRewards")
        guard let adManagerChecked = adManager else { return }
        switch adManagerChecked.rewardToBeGiven {
        case Reward.letter:
            if (StateManager.shared.currentState != .completed) { rewardWithLetter() }
        case Reward.hint:
            rewardWithHint()
            Database.shared.setCategoryAsRewarded()
        case Reward.continue_playing:
            executeLevelLoadActions()
            if (Database.shared.currentLevel!.levelProgress == Level.Progress.in_progress)
            {
                StateManager.shared.currentState = GameState.play
            }
        case Reward.none:
            if (Database.shared.currentLevel!.levelProgress == Level.Progress.completed)
            {
                enableInfoButton()
                addNextLevelGestureTap()
            }
            //Swift.debugPrint("NO reward!")
        }
        adManagerChecked.rewardToBeGiven = .none
    }
    
    
    // MARK: View did appear
    override func viewDidAppear(_ animated: Bool) {
        handleRewards()
        
    }
    
    
    //MARK: Show tutorial
    private func showTutorial()
    {
        let tutorialShown = UserDefaults.standard.bool(forKey: C.TUTORIAL_SHOWN)
        
        if (!tutorialShown)
        {
            let tutorialView = TutorialViewController()
            present(tutorialView, animated: true)
            UserDefaults.standard.set(true, forKey: C.TUTORIAL_SHOWN)
        }
    }
    
    
    //MARK: Display scores
    private func showScores(scores:[ScoreItemInfo]) {
        //Swift.debugPrint("Count of scores: \(scores.count)")
        if !scores.isEmpty {
            //Swift.debugPrint("Display scores")
            let bottomSheetMenuVC = PlayerScoresController(scoreItems: scores)
            if let buttonView = gameCenterButtonRef.value(forKey: "view") as? UIView {
                presentPanModal(bottomSheetMenuVC,sourceView: buttonView, sourceRect: buttonView.bounds)
            }
        }
    }
    
    
    //MARK: Load scores
    private func loadScores()
    {
        var scoreItems:[ScoreItemInfo] = []
        let leaderBoard = Database.shared.currentLevel!.levelNumber <= 100 ? C.GAME_LEADERBOARD : C.ANTIMATTER_LEADERBOARD
        
        GKLeaderboard.loadLeaderboards(IDs: [leaderBoard]) { [weak self] (boards, error) in
            if (error == nil)
            {
                guard let leaderboard = boards?.first else { return }
                
                // get rank for player first
                leaderboard.loadEntries(for: [GKLocalPlayer.local], timeScope: GKLeaderboard.TimeScope.allTime) {
                    entry, entries, error in
                    //Swift.debugPrint("Player rank is: \(String(describing: entry?.rank))")
                    //Swift.debugPrint("Total entries count: \(String(describing: entries?.count))")
                    // get the other 3 scores based on players rank
                    guard let entry = entry else { return }
                    
                    // range for 3 scores that will leave the player score in center
                    // happy path range (player is not last and not first)
                    
                    let range = entry.rank == 1 ? NSRange(location: 1, length: 3) : NSRange(location: entry.rank - 1, length: 3)
                    
                    leaderboard.loadEntries(for: GKLeaderboard.PlayerScope.global, timeScope: GKLeaderboard.TimeScope.allTime, range: range) { entry, entries, integer, error in
                        if let entries = entries {
                            //Swift.debugPrint("Fetched \(entries.count) entries.")
                            for entry in entries {
                                let image = UIImage(systemName: "person.circle.fill")!
                                scoreItems.append(ScoreItemInfo(displayName: entry.player.displayName, photo: image, score: entry.score, rank: entry.rank, isCurrentPlayer: entry.player.gamePlayerID == GKLocalPlayer.local.gamePlayerID))
                            }
                            self?.showScores(scores: scoreItems)
                        }
                    }
                }
            }
            else {
                //Swift.debugPrint(error?.localizedDescription ?? "")
            }
        }
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
        //Swift.debugPrint("showEndingScreen")
        let triumphSound = SKAction.playSound(soundFile: "triumph.wav")
        self.gameScene?.run(triumphSound)
        let hintViewController = EndingScreen()
        present(hintViewController, animated: true)
    }
    
    
    @objc func dismissModal() {
        dismiss(animated: true, completion: nil)
    }
    
    
    private func addNextLevelGestureTap()
    {
        if let tapGesture = tapGesture, ((self.view.gestureRecognizers?.contains(tapGesture)) != nil) {
            self.view.removeGestureRecognizer(tapGesture)
        }
        else {
            tapGesture = UITapGestureRecognizer(target: self, action: #selector(self.tapToContinueTouched))
        }
        
        self.view.addGestureRecognizer(tapGesture!)
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
                enableInfoButton()
                addNextLevelGestureTap()
            }
            else {
                
                UIView.animate(withDuration: duration, delay: disappearanceDelay, animations: {
                    self.infoLabelButton.alpha = 0.0
                }, completion: { _ in
                    if (StateManager.shared.currentState != GameState.completed)
                    {
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
        let levelPack = Database.shared.getLevelPack(for: levelNr)
        let progressPercentage = switch levelPack {
        case LevelPack.game_pack:
            levelNr
        default:
            Database.shared.getNormalizedLevelNumber(levelNr: levelNr)
        }
        let progressValue = Float(progressPercentage) / (levelPack == LevelPack.game_pack ? 100.0 : 25.0)
        progressBar.setProgress(progressValue, animated: true)
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
    
    private func setToastStyle()
    {
        var style = ToastStyle()
        style.backgroundColor = Theme.shared.colors.inverseSurface
        style.messageColor = Theme.shared.colors.inverseOnSurface
        style.activityBackgroundColor = Theme.shared.colors.inverseSurface
        style.activityIndicatorColor = Theme.shared.colors.inverseOnSurface
        style.messageFont = UIFont(name: Theme.shared.robotoMedium, size: 15)!
        ToastManager.shared.style = style
    }
    
    
}
