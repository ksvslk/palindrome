import SpriteKit
import GameplayKit

class GameScene: SKScene {
    
    enum LevelCreationError: Error {
        case positionForHomeIndexNotFoundInMap
    }
    
    var cellDirector: CellDirector = CellDirector(cells: [Cell]())
    

    // MARK: Initialize level
    override func didMove(to view: SKView) {
        layoutScene(view: view)
    }
    
    
    private func debugRect(width:CGFloat, height:CGFloat, color:SKColor, topErr:CGFloat = 0.0, bottomErr:CGFloat = 0.0)
    {
        let rectangle = SKShapeNode(rectOf: CGSize(width: width, height: height))
        rectangle.strokeColor = color
        rectangle.lineWidth = 2.0
        rectangle.position.y -= topErr
        rectangle.position.y += bottomErr
        self.addChild(rectangle)
    }

    
    private func layoutScene(view: SKView)
    {
        cellDirector.cells.removeAll()
       
        // Get current level
        let currentLevel = Database.shared.currentLevel!

        let horizontalSafe = 16.0
        let bottomSafe = 34.0 + 44.0 + 8.0
        let topSafe = 59.0 + 4.0 + 8.0
        
        // Get and set view size
        let viewSize = CGSize(
            width: view.bounds.width - horizontalSafe,
            height: view.bounds.height - bottomSafe - topSafe)

        /*
         debugRect(
             width: viewSize.width,
             height: viewSize.height,
             color:SKColor.systemOrange,
             topErr: topSafe,
             bottomErr: bottomSafe
         )
         */
  
        
        print("View size: \(viewSize)")

        // Create game field representation which will be used to creat the UI grid
        let gameField = GameFieldCreator.getGameField(
            palindrome: currentLevel.solutionsArr.first!.palindrome,
            viewSize:viewSize,
            level:currentLevel
        )
      
        
        if (currentLevel.columnCount == -1)
        {
            Database.shared.updateColumnCount(
                newColumntCount: Int64(gameField.columns),
                oldFriendToNewFriendIndex: [:]
            )
        }
     
        let allCells = gameField.letterRows + gameField.solutionRows
        
        // Create the grid node
        var gridNode = SKNode()
        addChild(gridNode)
        
        // Add `Letter`-s, `Slot`-s and `SKSpriteNode`-s to the grid
        do {
            try addGameNodes(letters: currentLevel.lettersArr, allCells: allCells, gameField: gameField, gridNode: &gridNode)
        } catch {
            print("GameScene: Could not add nodes to grid.")
        }

        // Change the position of the grid
        let height = ((gameField.boxSize.height + GameFieldCreator.spacing) * CGFloat(allCells.count - 1))
        let width = ((gameField.boxSize.width + GameFieldCreator.spacing) * CGFloat(gameField.columns - 1))
        gridNode.position = CGPoint(x:-width/2.0,y:height/2.0)
        gridNode.position.y -= topSafe
        gridNode.position.y += bottomSafe

#if DEBUG
        assertThatEachLetterHasSlot(grid: gridNode, level: currentLevel)
        assertDatabaseAndActualLettersCount(grid: gridNode, level: currentLevel)
        assertTotalGridNodesCount(grid: gridNode, level: currentLevel, cells: allCells)
#endif
    }
    
    
    // MARK: Create the grid
    // TODO: WARNING! Does not check if the GameFieldCreator and database version of level match (different columns)
    private func addGameNodes(
        letters: [LetterData],
        allCells: [String],
        gameField: GameField,
        gridNode: inout SKNode
    ) throws {
        
        var homeIndexesToPositions = [Int64:CGPoint]()
        
        for (rowIndex, rowOfChars) in allCells.enumerated() {
            for (colIndex, char) in rowOfChars.enumerated() {
                
                let index = Int64(rowIndex * gameField.columns + colIndex + 1)
               
           
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
    
    
    // MARK: Assert grid correctness
#if DEBUG
    private func assertThatEachLetterHasSlot(grid:SKNode, level: Level)
    {
        let actualLettersCount = grid.children.filter { sknode in
            sknode as? Letter != nil
        }.count
        let actualSlotsCount = grid.children.filter { sknode in
            sknode as? Slot != nil
        }.count
        assert(actualLettersCount == actualSlotsCount)
    }
    
    
    private func assertDatabaseAndActualLettersCount(grid:SKNode, level: Level)
    {
        let actualLettersCount = grid.children.filter { sknode in
            sknode as? Letter != nil
        }.count
        let expectedLettersCount = level.lettersArr.count
        assert(expectedLettersCount == actualLettersCount)
    }
    
    
    private func assertTotalGridNodesCount(grid: SKNode, level: Level, cells:[String])
    {
        let actualGridChildrenCount = grid.children.count
        let expectedGridChildrenCount = cells.joined().count + level.lettersArr.filter { letterData in letterData.friendIndex != -1 }.count
        assert(expectedGridChildrenCount == actualGridChildrenCount)
    }
#endif
    
    
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
