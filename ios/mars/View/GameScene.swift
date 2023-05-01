import SpriteKit
import GameplayKit

class GameScene: SKScene {
    
    enum LevelCreationError: Error {
        case positionForHomeIndexNotFoundInMap
    }
    
    var cellDirector: CellDirector = CellDirector(cells: [Cell]())
    
    // MARK: Initialize level
    override func didMove(to view: SKView) {
        
        // Get current level
        let currentLevel = Database.shared.currentLevel!
        
        // Get and set view size
        let viewSize = CGSize(width: view.bounds.size.width, height: view.bounds.size.height)
        self.size = viewSize
        
        // Create game field representation which will be used to creat the UI grid
        let gameField = GameFieldCreator.getGameField(
            palindrome: currentLevel.solutionsArr.first!.palindrome,
            viewSize:viewSize
        )
        let allCells = gameField.letterRows + gameField.solutionRows
        
        // Create the grid node
        var gridNode = SKNode()
        addChild(gridNode)
        
        // Add `Letter`-s, `Slot`-s and `SKSpriteNode`-s to the grid
        //addGameNodes(allCells: allCells, gameField: gameField, gridNode: &gridNode)
        do {
            try alternativeAddGameNodes(letters: currentLevel.lettersArr, allCells: allCells, gameField: gameField, gridNode: &gridNode)
        } catch {
            print("Could not add nodes to grid.")
        }
        
        // Change the position of the grid
        let height = ((gameField.boxSize.height + GameFieldCreator.spacing) * CGFloat(allCells.count - 1))
        let width = ((gameField.boxSize.width + GameFieldCreator.spacing) * CGFloat(gameField.columns - 1))
        gridNode.position = CGPoint(x:-width/2,y:height/2)
        
    }
    
    private func getBlankNode(gameField: GameField, rowIndex: Int, colIndex: Int) -> SKSpriteNode
    {
        let space = SKSpriteNode(color: .clear, size: CGSize(width: gameField.boxSize.width, height: gameField.boxSize.height))
        space.position = CGPoint(x: CGFloat(colIndex) * (gameField.boxSize.width + GameFieldCreator.spacing), y: -CGFloat(rowIndex) * (gameField.boxSize.height + GameFieldCreator.spacing))
        space.zPosition = 0
        return space
    }
    
    
    private func getLetterNode(char: String, currentIndex: Int, gameField: GameField, rowIndex: Int, colIndex: Int) -> Letter
    {
        return Letter(
            homeIndex: currentIndex,
            letter: char, size: CGSize(width: gameField.boxSize.width, height:  gameField.boxSize.height),
            homePosition: CGPoint(x: CGFloat(colIndex) * (gameField.boxSize.width + GameFieldCreator.spacing), y: -CGFloat(rowIndex) * (gameField.boxSize.height + GameFieldCreator.spacing)),
            cellDirector: cellDirector
        )
    }
    
    private func getSlotNode(currentIndex: Int, gameField: GameField, rowIndex: Int, colIndex: Int) -> Slot
    {
        return Slot(
            homeIndex: currentIndex,
            size: CGSize(width: gameField.boxSize.width, height: gameField.boxSize.height),
            homePosition: CGPoint(x: CGFloat(colIndex) * (gameField.boxSize.width + GameFieldCreator.spacing), y: -CGFloat(rowIndex) * (gameField.boxSize.height + GameFieldCreator.spacing))
        )
    }
    
    
    private func alternativeAddGameNodes(letters:[LetterData], allCells: [String], gameField: GameField, gridNode: inout SKNode) throws
    {
        var homeIndexesToPositions: [Int:CGPoint] = [:]
        for (rowIndex, rowOfChars) in allCells.enumerated()
        {
            for (colIndex, char) in rowOfChars.enumerated() {
                let index = rowIndex * gameField.columns + colIndex
                switch (rowIndex < gameField.letterRows.count, char) {
                case (true, " "), (false, " "):
                    let space = getBlankNode(gameField: gameField, rowIndex: rowIndex, colIndex: colIndex)
                    gridNode.addChild(space)
                case (true, _):
                    // kui see letter on sõbraga siis jäta lisamata, lisa blank node
                    if let letterWithFriend = letters.first (where: { data in
                        data.homeIndex == index && data.friendIndex != -1
                    }) {
                        let space = getBlankNode(gameField: gameField, rowIndex: rowIndex, colIndex: colIndex)
                        homeIndexesToPositions[index] = space.position
                        gridNode.addChild(space)
                    }
                    else {
                        let letter = getLetterNode(char: String(char), currentIndex: index, gameField: gameField, rowIndex: rowIndex, colIndex: colIndex)
                        gridNode.addChild(letter)
                        cellDirector.cells.append(letter)
                    }
                case (false, _):
                    let slot = getSlotNode(currentIndex: index, gameField: gameField, rowIndex: rowIndex, colIndex: colIndex)
                    gridNode.addChild(slot)
                    cellDirector.cells.append(slot)
                    
                    // kui sellel slotil on sõber siis lisa sõber samuti
                    if let letterWithFriend = letters.first (where: { data in
                        data.friendIndex == index
                    }) {
                        let letter = getLetterNode(char: letterWithFriend.letter, currentIndex: Int(letterWithFriend.homeIndex), gameField: gameField, rowIndex: rowIndex, colIndex: colIndex)
                        print(letterWithFriend.homeIndex)
                        letter.friend = slot
                        letter.color = Theme.colorSecondary
            
                        if let position = homeIndexesToPositions[Int(letterWithFriend.homeIndex)]
                        {
                            letter.homePosition = position
                        }
                        else {
                            throw LevelCreationError.positionForHomeIndexNotFoundInMap
                        }
                        
                        gridNode.addChild(letter)
                        cellDirector.cells.append(letter)
                    }
                 
         
                }
            }
        }
    }
    
    
    // MARK: Add box nodes to the grid
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
