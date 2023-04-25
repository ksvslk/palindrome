//
//  Slot.swift
//  test
//
//  Created by ksvslk on 17.04.2023.
//

import Foundation
import SpriteKit

class Slot: Cell {
    
    override var cellType : CellType {
        get { return CellType.slot }
        set {}
    }
    
    init(homeIndex:Int, size: CGSize, homePosition: CGPoint) {
        super.init(homeIndex: homeIndex, texture: nil, color: Theme.colorSecondaryVariant, size: size, homePosition: homePosition)
        self.zPosition = C.BOTTOM_LAYER
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
   
}
