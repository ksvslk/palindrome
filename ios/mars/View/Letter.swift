//
//  Letter.swift
//  test
//
//  Created by ksvslk on 17.04.2023.
//

import SpriteKit

class Letter: Cell {
    
    static let scaleAmount: CGFloat = 0.1
    override var cellType : CellType {
        get { return CellType.letter }
        set {}
    }
    // MARK: Property references
    private var cellDirector: CellDirector
    var data: LetterData
    
    // Sounds
    let PLACE_LETTER_SOUND_ACTION = SKAction.playSound(soundFile: "place_letter.wav")
    let SWITCH_LETTER_SOUND_ACTION = SKAction.playSound(soundFile: "switch_letters.wav")
    
    private var feedbackGenerator : UIImpactFeedbackGenerator? = nil
    
    
    // MARK: Initialize
    init(data: LetterData, size: CGSize, homePosition: CGPoint, cellDirector: CellDirector) {
        self.cellDirector = cellDirector
        self.data = data
        super.init(homeIndex: data.homeIndex, texture: nil, color: Theme.colorPrimary, size: size, homePosition: homePosition)
        let letterLabel = SKLabelNode(text: data.letter)
        letterLabel.fontName = Theme.letterFont
        letterLabel.fontSize = size.width / 1.5
        letterLabel.verticalAlignmentMode = .center
        letterLabel.horizontalAlignmentMode = .center
        letterLabel.fontColor = Theme.colorOnPrimary
        isUserInteractionEnabled = true
        addChild(letterLabel)
        
    }
    
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    
    // MARK: Touches began
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard StateManager.shared.currentState == .play && state == CellState.home && data.locked == false else { return }
        guard let touch = touches.first else { return }
        
        let location = touch.location(in: self.parent!)
        
        if self.frame.contains(location) {
            self.zPosition = C.TOP_LAYER
            currentPosition = self.position
            state = CellState.dragging
            self.animateColor(to: Theme.colorSecondary, duration: C.ON_CLICK_COLOR_CHANGE_ANIMATION, timingMode: SKActionTimingMode.easeInEaseOut)
            self.run(SKAction.scale(by: 1 + Letter.scaleAmount, duration: C.ON_CLICK_COLOR_CHANGE_ANIMATION))
        }
        // Instantiate a new generator.
        feedbackGenerator = UIImpactFeedbackGenerator()
        // Prepare the generator when the gesture begins.
        feedbackGenerator?.impactOccuredCustom(intensity: 0.3)
        feedbackGenerator?.prepare()
    }
    
    
    /*
     When dragging the Letter should move to the same place where the mouse it with some delay/ease animation.
     */
    // MARK: Touches moved
    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard state == CellState.dragging else { return }
        
        guard let touch = touches.first else { return }
        let location = touch.location(in: self.parent!)
        let previousLocation = touch.previousLocation(in: self.parent!)
        let newPosition = CGPoint(x: self.position.x + location.x - previousLocation.x,
                                  y: self.position.y + location.y - previousLocation.y)
        self.position = newPosition
    }
    
    
    // touch event is completed successfully
    // MARK: Touches ended
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard state == CellState.dragging else { return }
        let collisionResult = cellDirector.handleCollisions(letter: self)
        playSound(collisionResult: collisionResult)
        playHapticFeedback(collisionResult: collisionResult)
        print("Collision result: \(collisionResult)")
        moveLetter()
    }
 
    
    // touch event is interrupted or canceled (system events like phone call)
    // maybe abrupt end would be better here?
    // MARK: Touches cancelled
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard state == CellState.dragging else { return }
        self.zPosition = C.NEUTRAL_LAYER
        self.state = CellState.home
        self.scale(to: CGSize(width: 1.0, height: 1.0))
        self.position = currentPosition
    }
    
    
    // MARK: Move action
    func moveLetter(endColor:UIColor = Theme.colorSecondary, durationMultiplier:CGFloat = 1.0, zLayer:CGFloat? = nil)
    {

        if let zLayer = zLayer {
            self.color = Theme.colorSecondary
            self.zPosition = zLayer
        }
     
        // Get some value that represents how much was moved ignoring the actual physical
        // distance. Log applied to Euclidean distance to make it very small.
        let distance = log2((abs(position.distance(point: currentPosition))))
        let duration = distance.isFinite ? abs(distance * C.HOME_ANIMATION_MULTIPLIER * C.ANIMATION_MODIFIER)*durationMultiplier : C.ON_RELEASE_COLOR_CHANGE_ANIMATION * durationMultiplier
        
        let endColor = friend != nil ? endColor : Theme.colorPrimary
        state = CellState.resolving
        let moveAction = SKAction.move(to: currentPosition, duration: duration)
        moveAction.timingMode = .easeInEaseOut
        self.run(moveAction) {
            self.zPosition = C.NEUTRAL_LAYER
            self.animateColor(to: endColor, duration: duration, timingMode: SKActionTimingMode.easeOut)
            let newScale = max(self.xScale - 0.2, 1.0)
            let scaleAction = SKAction.scale(to: newScale, duration: duration)
            self.run(scaleAction) {
                self.state = CellState.home
            }
        }
    }
    
    
    // MARK: Animate color
    func animateColor(to color: UIColor, duration: CGFloat, timingMode: SKActionTimingMode) {
        guard StateManager.shared.currentState != .completed else { return }
        let colorAction = SKAction.colorize(with: color, colorBlendFactor: 1, duration: duration)
        colorAction.timingMode = timingMode
        self.run(colorAction)
    }

    
    private func playSound(collisionResult: CollisionResult)
    {
        if (UserDefaults.standard.bool(forKey: C.SOUND_ON))
        {
            switch collisionResult {
            case .landing:
                self.run(PLACE_LETTER_SOUND_ACTION)
            case .switching:
                self.run(SWITCH_LETTER_SOUND_ACTION)
            case .nothing:
                return
            }
        }
    }
    
    
    private func playHapticFeedback(collisionResult: CollisionResult)
    {
        switch collisionResult {
        case .landing:
            feedbackGenerator?.impactOccuredCustom(intensity: 0.9)
        case .switching:
            feedbackGenerator?.impactOccuredCustom(intensity: 0.6)
        case .nothing:
            feedbackGenerator?.impactOccuredCustom(intensity: 0.3)
        }
    }
    
}
