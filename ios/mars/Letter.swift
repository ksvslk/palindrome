//
//  Letter.swift
//  test
//
//  Created by ksvslk on 17.04.2023.
//

import SpriteKit

class Letter: Cell {
    
    static let moveDuration = 0.7
    static let colorChangeDuration = 0.5
    static let scaleAmount: CGFloat = 0.1
    override var cellType : CellType {
        get { return CellType.letter }
        set {}
    }
    private var cellDirector: CellDirector
    
    init(homeIndex:Int, letter: String, size: CGSize, homePosition: CGPoint, cellDirector: CellDirector) {
        self.cellDirector = cellDirector
        super.init(homeIndex: homeIndex, texture: nil, color: Theme.colorPrimary, size: size, homePosition: homePosition)
        isUserInteractionEnabled = true
        let letterLabel = SKLabelNode(text: letter)
        letterLabel.fontName = Theme.letterFont
        letterLabel.fontSize = size.width / 1.5
        letterLabel.verticalAlignmentMode = .center
        letterLabel.horizontalAlignmentMode = .center
        letterLabel.fontColor = Theme.colorOnPrimary
        addChild(letterLabel)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        
        guard state == CellState.home else { return }
        guard let touch = touches.first else { return }
        
        let location = touch.location(in: self.parent!)
        
        if self.frame.contains(location) {
            self.zPosition = C.TOP_LAYER
            isUserInteractionEnabled = false
            currentPosition = self.position
            state = CellState.dragging
            self.animateColor(to: Theme.colorSecondary, duration: Letter.colorChangeDuration, timingMode: SKActionTimingMode.easeInEaseOut)
            self.run(SKAction.scale(by: 1 + Letter.scaleAmount, duration: Letter.colorChangeDuration))
        }
    }
    
    /*
     When dragging the Letter should move to the same place where the mouse it with some delay/ease animation.
     */
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
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard state == CellState.dragging else { return }
       
        cellDirector.handleCollisions(letter: self)
        
        moveLetter()
    }
    
    func moveLetter()
    {
        state = CellState.resolving
        let moveAction = SKAction.move(to: currentPosition, duration: Letter.moveDuration)
        moveAction.timingMode = .easeInEaseOut
        self.run(moveAction) {
            self.zPosition = C.NEUTRAL_LAYER
            self.animateColor(to: Theme.colorPrimary, duration: Letter.colorChangeDuration, timingMode: SKActionTimingMode.easeOut)
            let newScale = max(self.xScale - 0.2, 1.0)
            let scaleAction = SKAction.scale(to: newScale, duration: Letter.colorChangeDuration)
            self.run(scaleAction) {
                self.state = CellState.home
                self.isUserInteractionEnabled = true
            }
        }
    }
    
    // touch event is interrupted or canceled (system events like phone call)
    // maybe abrupt end would be better here?
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard state == CellState.dragging else { return }
        self.zPosition = C.NEUTRAL_LAYER
        self.state = CellState.home
        self.scale(to: CGSize(width: 1.0, height: 1.0))
        self.position = currentPosition
        self.isUserInteractionEnabled = true
    }
    
    
    func animateColor(to color: UIColor, duration: CGFloat, timingMode: SKActionTimingMode) {
        let colorAction = SKAction.colorize(with: color, colorBlendFactor: 1, duration: duration)
        colorAction.timingMode = timingMode
        self.run(colorAction)
    }
    
}
