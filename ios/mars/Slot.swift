//
//  Slot.swift
//  test
//
//  Created by ksvslk on 17.04.2023.
//

import Foundation
import SpriteKit

class Slot: Cell<Slot, Letter> {
    
    init(homeIndex:Int, size: CGSize, originalPosition: CGPoint) {
        super.init(homeIndex: homeIndex, texture: nil, color: Theme.colorSecondaryVariant, size: size, originalPosition: originalPosition)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
   
}
