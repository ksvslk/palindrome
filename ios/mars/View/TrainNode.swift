//
//  TrainNode.swift
//  mars
//
//  Created by Mina on 31.05.2023.
//

import Foundation
import SpriteKit

class TrainNode : SKSpriteNode {
    
    private let centerCarriages = ["ic_carriage_center_blue", "ic_carriage_center_yellow","ic_carriage_center_green","ic_carriage_center"]
    private let lastCarriages = ["ic_carriage_last_blue", "ic_carriage_last_yellow","ic_carriage_last_green","ic_carriage_last"]
    var calculatedWidth:Double = 0.0
    
    let numberOfCarriages:Int
    
    init(numberOfCarriages:Int) {
        self.numberOfCarriages = numberOfCarriages
        let texture = SKTexture(imageNamed: "ic_loco1")
        super.init(texture: texture, color: .clear, size: texture.size())
        addCarriages()
        beginAnimation()
    }
    
    private func addCarriages()
    {
        let carriages = getTrainWithCarriages(carriageCount: numberOfCarriages)
        
        let xOffset: CGFloat = -size.width / 2
        var xPosition: CGFloat = 0 - size.width + 30
        for frame in carriages {
            let node = SKSpriteNode(texture: SKTexture(imageNamed: frame))
            node.position = CGPoint(x: xPosition, y: -6)
            addChild(node)
            xPosition += xOffset
            calculatedWidth += abs(xOffset)
        }
        
        // add end carriage
    }
    
    
    func getTrainWithCarriages(carriageCount: Int) -> [String] {
        var trainCarriages: [String] = []
        
        guard carriageCount > 0 else {
            return trainCarriages
        }
        
        if carriageCount > 1 {
            for _ in 1..<carriageCount {
                let centerCarriageIndex = Int.random(in: 0..<centerCarriages.count)
                let centerCarriage = centerCarriages[centerCarriageIndex]
                trainCarriages.append(centerCarriage)
            }
        }
        
        let lastCarriageIndex = Int.random(in: 0..<lastCarriages.count)
        let lastCarriage = lastCarriages[lastCarriageIndex]
        trainCarriages.append(lastCarriage)
        
        return trainCarriages
    }
    
    
    private func beginAnimation() {
        let textureAtlas = SKTextureAtlas(named: "train")
        let frames = ["ic_loco1", "ic_loco2", "ic_loco3", "ic_loco4", "ic_loco5", "ic_loco6"].map { textureAtlas.textureNamed($0) }
        let animate = SKAction.animate(with: frames, timePerFrame: 0.1)
        let forever = SKAction.repeatForever(animate)
        self.run(forever)
    }
    
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
