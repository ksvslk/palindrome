import SpriteKit
import GameplayKit

class GameScene: SKScene {
    
    var cellDirector: CellDirector = CellDirector(cells: [Cell]())
    
    // MARK: Initialize level
    override func didMove(to view: SKView) {
        
        // Get current level
        let currentLevel = Database.shared.currentLevel!
        
        // Get and set view size
        let viewSize = CGSize(width: view.bounds.size.width, height: view.bounds.size.height)
        self.size = viewSize
        
        // Create game field representation which will be used to creat the UI grid
        let gameField = GameFieldCreator.getDensitySettings(
            palindrome: currentLevel.solutionsArr.first!.palindrome,
            viewSize:viewSize
        )
        let allCells = gameField.letterRows + gameField.solutionRows
  
        // Create the grid node
        var gridNode = SKNode()
        addChild(gridNode)
        
        // Add `Letter`-s, `Slot`-s and `SKSpriteNode`-s to the grid
        addGameNodes(allCells: allCells, gameField: gameField, gridNode: &gridNode)
      
        // Change the position of the grid
        let height = ((gameField.boxSize.height + GameFieldCreator.spacing) * CGFloat(allCells.count - 1))
        let width = ((gameField.boxSize.width + GameFieldCreator.spacing) * CGFloat(gameField.columns - 1))
        gridNode.position = CGPoint(x:-width/2,y:height/2)
       
    }
    
    // MARK: Add letters, slots and empty nodes to the grid node
    private func addGameNodes(allCells: [String], gameField: GameField, gridNode: inout SKNode)
    {
        // Can we add all just Cells - only Letters have LetterData?
        for (rowIndex, rowOfChars) in allCells.enumerated() {
            for (colIndex, char) in rowOfChars.enumerated() {
         
                let index = rowIndex * gameField.columns + colIndex
                print(index)
                switch (rowIndex < gameField.letterRows.count, char) {
                case (true, " "), (false, " "):
                    let space = SKSpriteNode(color: .clear, size: CGSize(width: gameField.boxSize.width, height: gameField.boxSize.height))
                    space.position = CGPoint(x: CGFloat(colIndex) * (gameField.boxSize.width + GameFieldCreator.spacing), y: -CGFloat(rowIndex) * (gameField.boxSize.height + GameFieldCreator.spacing))
                    space.zPosition = 0
                    gridNode.addChild(space)
                case (true, _):
                    let letter = Letter(
                        homeIndex: index,
                        letter: String(char), size: CGSize(width: gameField.boxSize.width, height:  gameField.boxSize.height),
                        homePosition: CGPoint(x: CGFloat(colIndex) * (gameField.boxSize.width + GameFieldCreator.spacing), y: -CGFloat(rowIndex) * (gameField.boxSize.height + GameFieldCreator.spacing)),
                        cellDirector: cellDirector
                    )
                    gridNode.addChild(letter)
                    cellDirector.cells.append(letter)
                case (false, _):
                    let slot = Slot(
                        homeIndex: index,
                        size: CGSize(width: gameField.boxSize.width, height: gameField.boxSize.height),
                        homePosition: CGPoint(x: CGFloat(colIndex) * (gameField.boxSize.width + GameFieldCreator.spacing), y: -CGFloat(rowIndex) * (gameField.boxSize.height + GameFieldCreator.spacing))
                    )
                    gridNode.addChild(slot)
                    cellDirector.cells.append(slot)
                }
            }
        }
    }
    
}
