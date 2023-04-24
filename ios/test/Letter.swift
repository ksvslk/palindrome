//
//  Letter.swift
//  test
//
//  Created by ksvslk on 17.04.2023.
//

import SpriteKit

class Letter: SKSpriteNode {
    
    static let moveDuration = 1.0
    static let colorChangeDuration = 0.5
    static let scaleAmount: CGFloat = 0.1

    var originalPosition: CGPoint?
    var isDragging = false
    
    init(letter: String, size: CGSize) {
        super.init(texture: nil, color: Theme.colorPrimary, size: size)
        
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
     
         guard let touch = touches.first else { return }
         
         let location = touch.location(in: self.parent!)
         
         if self.frame.contains(location) {
             self.zPosition = 1
             isUserInteractionEnabled = false
             originalPosition = self.position
             isDragging = true
             self.animateColor(to: Theme.colorSecondary, duration: Letter.colorChangeDuration, timingMode: SKActionTimingMode.easeInEaseOut)
             self.run(SKAction.scale(by: 1 + Letter.scaleAmount, duration: Letter.colorChangeDuration))
         }
     }
     
     /*
      When dragging the Letter should move to the same place where the mouse it with some delay/ease animation.
      */
     override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
         guard let touch = touches.first else { return }
         let location = touch.location(in: self.parent!)
         let previousLocation = touch.previousLocation(in: self.parent!)
         
         let newPosition = CGPoint(x: self.position.x + location.x - previousLocation.x,
                                   y: self.position.y + location.y - previousLocation.y)
         self.position = newPosition
         
     }
     
     override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
         guard isDragging else { return }
         
         let moveAction = SKAction.move(to: originalPosition ?? self.position, duration: Letter.moveDuration)
         moveAction.timingMode = .easeInEaseOut
         self.run(moveAction) {
             self.zPosition = 0
             self.isDragging = false
             self.animateColor(to: Theme.colorPrimary, duration: Letter.colorChangeDuration, timingMode: SKActionTimingMode.easeOut)
             self.isUserInteractionEnabled = true
             let newScale = max(self.xScale - 0.2, 1.0)
             let scaleAction = SKAction.scale(to: newScale, duration: Letter.colorChangeDuration)
             self.run(scaleAction)
         }
     }
    
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard isDragging else { return }
        
        let moveAction = SKAction.move(to: originalPosition ?? self.position, duration: Letter.moveDuration)

        moveAction.timingMode = .easeOut
        self.run(moveAction) {
            self.zPosition = 0
            self.isDragging = false
            self.animateColor(to: Theme.colorPrimary, duration: Letter.colorChangeDuration, timingMode: SKActionTimingMode.easeOut)
            self.isUserInteractionEnabled = true
            let newScale = max(self.xScale - 0.2, 1.0)
            let scaleAction = SKAction.scale(to: newScale, duration: Letter.colorChangeDuration)
            self.run(scaleAction)
        }
    }
    
    
    func animateColor(to color: UIColor, duration: CGFloat, timingMode: SKActionTimingMode) {
        let colorAction = SKAction.colorize(with: color, colorBlendFactor: 1, duration: duration)
        colorAction.timingMode = timingMode
        self.run(colorAction)
    }
    
}
