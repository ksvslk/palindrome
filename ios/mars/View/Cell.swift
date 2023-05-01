//
//  Cell.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import SpriteKit

class Cell: SKSpriteNode {
    
    var homeIndex: Int64
    var friend: Cell?
    var homePosition: CGPoint
    var currentPosition: CGPoint
    var state: CellState = CellState.home
    var cellType: CellType = CellType.none
    
    
    init(homeIndex: Int64, texture: SKTexture?, color: UIColor, size: CGSize, homePosition: CGPoint) {
        self.homePosition = homePosition
        self.currentPosition = homePosition
        self.homeIndex = homeIndex
        super.init(texture: texture, color: color, size: size)
        self.position = self.homePosition
        self.zPosition = C.NEUTRAL_LAYER
        self.anchorPoint = CGPoint(x: 0.5, y: 0.5)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    
}
