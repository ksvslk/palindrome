//
//  Cell.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import SpriteKit

class Cell<T, F>: SKSpriteNode {
    
    var homeIndex: Int?
    var friend: F?
    var originalPosition: CGPoint
    var state: CellState = CellState.home
    
    
    init(homeIndex: Int?, texture: SKTexture?, color: UIColor, size: CGSize, originalPosition: CGPoint) {
        self.originalPosition = originalPosition
        super.init(texture: texture, color: color, size: size)
        self.homeIndex = homeIndex
        self.position = self.originalPosition
        self.zPosition = 0
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
  
    
}
