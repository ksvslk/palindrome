//
//  Slot.swift
//  test
//
//  Created by ksvslk on 17.04.2023.
//

import Foundation
import SpriteKit

class Slot: SKSpriteNode {
    
    var letter: Letter?
    
    func addLetter(_ letter: Letter) {
        guard self.letter == nil else { return }
        
        self.letter = letter
        letter.removeFromParent()
        letter.position = self.position
        self.parent?.addChild(letter)
    }
    
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let letter = letter, let touch = touches.first else { return }
        
        let location = touch.location(in: self.parent!)
        
        if self.frame.contains(location) {
            letter.isDragging = false
            
            let moveAction = SKAction.move(to: self.position, duration: 0.2)
            moveAction.timingMode = .easeOut
            letter.run(moveAction)
            
            self.letter = nil
        } else {
            letter.isDragging = true
        }
    }
    
}
