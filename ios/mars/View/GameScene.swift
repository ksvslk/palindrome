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
        print(allCells)
#if DEBUG
        validateNodesCount(grid: gridNode, level: currentLevel, cells: allCells)
#endif
    }
    
    
    private func validateNodesCount(grid: SKNode, level: Level, cells:[String])
    {
        let actualGridChildrenCount = grid.children.count
        let expectedGridChildrenCount = 2 * level.lettersArr.count + level.lettersArr.filter { letterData in letterData.friendIndex != -1
        }.count + cells.compactMap { str in
            str.allSatisfy { chr in
                chr == " "
            }
        }.count
        assert(expectedGridChildrenCount == actualGridChildrenCount)
    }
    
    
    // MARK: Add box nodes to the grid
    // TODO: WARNING! Does not check if the GameFieldCreator and database version of level match (different columns)
    private func alternativeAddGameNodes(
        letters: [LetterData],
        allCells: [String],
        gameField: GameField,
        gridNode: inout SKNode
    ) throws {
        
        var homeIndexesToPositions = [Int64:CGPoint]()
        
        for (rowIndex, rowOfChars) in allCells.enumerated() {
            for (colIndex, char) in rowOfChars.enumerated() {
                
                let index = Int64(rowIndex * gameField.columns + colIndex)
                
                switch (rowIndex < gameField.letterRows.count, char) {
                case (true, " "), (false, " "):
                    let space = getBlankNode(gameField: gameField, rowIndex: rowIndex, colIndex: colIndex)
                    gridNode.addChild(space)
                    
                case (true, _):
                    let letterWithFriend = letters.first { data in
                        data.homeIndex == index
                    }!
                    
                    if letterWithFriend.friendIndex != -1 {
                        let space = getBlankNode(gameField: gameField, rowIndex: rowIndex, colIndex: colIndex)
                        homeIndexesToPositions[Int64(index)] = space.position
                        gridNode.addChild(space)
                    } else {
                        assert(index == letterWithFriend.homeIndex)
                        let letter = getLetterNode(
                            data: letterWithFriend,
                            gameField: gameField,
                            rowIndex: rowIndex,
                            colIndex: colIndex
                        )
                        gridNode.addChild(letter)
                        cellDirector.cells.append(letter)
                    }
                    
                case (false, _):
                    let slot = getSlotNode(
                        currentIndex: index,
                        gameField: gameField,
                        rowIndex: rowIndex,
                        colIndex: colIndex
                    )
                    gridNode.addChild(slot)
                    cellDirector.cells.append(slot)
                    
                    
                    if let letterWithFriend = letters.first(where: { data in
                        data.friendIndex == index
                    }) {
                        
                        let letter = getLetterNode(
                            data: letterWithFriend,
                            gameField: gameField,
                            rowIndex: rowIndex,
                            colIndex: colIndex
                        )
                        slot.friend = letter
                        letter.friend = slot
                        letter.color = Theme.colorSecondary
                        
                        if let position = homeIndexesToPositions[letterWithFriend.homeIndex] {
                            letter.homePosition = position
                        } else {
                            throw LevelCreationError.positionForHomeIndexNotFoundInMap
                        }
                        
                        gridNode.addChild(letter)
                        cellDirector.cells.append(letter)
                    }
                }
            }
        }
        
    }
    
    
    // MARK: Get SKSpriteNode node
    private func getBlankNode(gameField: GameField, rowIndex: Int, colIndex: Int) -> SKSpriteNode
    {
        let space = SKSpriteNode(color: .clear, size: CGSize(width: gameField.boxSize.width, height: gameField.boxSize.height))
        space.position = CGPoint(x: CGFloat(colIndex) * (gameField.boxSize.width + GameFieldCreator.spacing), y: -CGFloat(rowIndex) * (gameField.boxSize.height + GameFieldCreator.spacing))
        space.zPosition = 0
        return space
    }
    
    
    // MARK: Get Letter node
    private func getLetterNode(data: LetterData, gameField: GameField, rowIndex: Int, colIndex: Int) -> Letter
    {
        return Letter(
            data: data, size: CGSize(width: gameField.boxSize.width, height:  gameField.boxSize.height),
            homePosition: CGPoint(x: CGFloat(colIndex) * (gameField.boxSize.width + GameFieldCreator.spacing), y: -CGFloat(rowIndex) * (gameField.boxSize.height + GameFieldCreator.spacing)),
            cellDirector: cellDirector
        )
    }
    
    
    // MARK: Get Slot node
    private func getSlotNode(currentIndex: Int64, gameField: GameField, rowIndex: Int, colIndex: Int) -> Slot
    {
        return Slot(
            homeIndex: currentIndex,
            size: CGSize(width: gameField.boxSize.width, height: gameField.boxSize.height),
            homePosition: CGPoint(x: CGFloat(colIndex) * (gameField.boxSize.width + GameFieldCreator.spacing), y: -CGFloat(rowIndex) * (gameField.boxSize.height + GameFieldCreator.spacing))
        )
    }
    
}
