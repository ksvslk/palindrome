//
//  Slot.swift
//  test
//
//  Created by ksvslk on 17.04.2023.
//

import Foundation
import SpriteKit

class Slot: Cell {
    
    let actualChar: String
    
    override var cellType : CellType {
        get { return CellType.slot }
        set {}
    }
    
    func setFriendWithoutDidSet(letter: Letter?, ignoreScoreChange:Bool = false) {
        friend = letter
        guard let letter = (friend as? Letter) else { return }
        if (!ignoreScoreChange && letter.data.letter != actualChar)
        {
            print("New friend is \(letter.data.letter) but should be \(actualChar)")
            print("Current score: \(Database.shared.currentLevel?.score)")
            Database.shared.decreaseScore()
            print("Decreased score: \(Database.shared.currentLevel?.score)")
        }
    }
    
    init(homeIndex:Int64, size: CGSize, homePosition: CGPoint, actualChar:String) {
        self.actualChar = actualChar
        super.init(homeIndex: homeIndex, texture: nil, color: Theme.colorSecondaryVariant, size: size, homePosition: homePosition)
        self.zPosition = C.BOTTOM_LAYER
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
   
}
