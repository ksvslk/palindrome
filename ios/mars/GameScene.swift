import SpriteKit
import GameplayKit

class GameScene: SKScene {
    
    var gridNode: SKNode!
    let spacing = 5.0
    let edgePadding = 16
    let maxBoxSize = 40.0
    
    let palindromes = [
        "DAD",
        "A MAN, A PLAN, A CANAL - PANAMA!",
        "MALAYALAM",
        "AMEN ICY CINEMA!",
        "TACO CAT",
        "SUMS ARE NOT SET AS A TEST ON ERASMUS SUMS ARE NOT SET AS A TEST ON ERASMUS",
        "DOC, NOTE I DISSENT: A FAST NEVER PREVENTS A FATNESS. I DIET ON COD.",
        "DO ROOTS NAP? STOP! HAMMER TIME! EMIT R.E.M., MAH POTS! PANS, TOO, ROD! MAH POTS! PANS, TOO, ROD!MAH POTS! PANS, TOO, ROD!MAH POTS! PANS, TOO, ROD!"
    ]
    

    override func didMove(to view: SKView) {
      
        let viewSize = CGSize(width: view.bounds.size.width, height: view.bounds.size.height)
        self.size = viewSize
        let gameField = getDensitySettings(palindrome: palindromes[6], viewSize:viewSize)
        let allCells = gameField.letterRows + gameField.solutionRows

        // Create the grid node
        gridNode = SKNode()
        addChild(gridNode)
        
        // Set the number of rows and columns and box size
        let numRows = gameField.letterRows.count
        let numCols = gameField.columns
        let boxSize = gameField.boxSize

        // Set the size of each slot and the spacing between slots
        let slotSize = CGSize(width: Double(boxSize), height: Double(boxSize))

        // Calculate the total width and height of the grid
        let gridWidth = CGFloat(numCols) * slotSize.width + CGFloat(numCols - 1) * spacing
        let gridHeight = CGFloat(numRows) * slotSize.height + CGFloat(numRows - 1) * spacing

        // Set the position of the grid node to the center of the scene
        let centerX = self.frame.midX
        let centerY = self.frame.midY
   


        for (rowIndex, rowOfChars) in allCells.enumerated() {
            for (colIndex, char) in rowOfChars.enumerated() {
                switch (rowIndex < gameField.letterRows.count, char) {
                case (true, " "), (false, " "):
                    let space = SKSpriteNode(color: .clear, size: CGSize(width: slotSize.width, height: slotSize.height))
                    space.position = CGPoint(x: CGFloat(colIndex) * (slotSize.width + spacing), y: -CGFloat(rowIndex) * (slotSize.height + spacing))
                    space.zPosition = 0
                    gridNode.addChild(space)
                case (true, _):
                    let letter = Letter(letter: String(char), size: CGSize(width: slotSize.width, height: slotSize.height))
                    letter.position = CGPoint(x: CGFloat(colIndex) * (slotSize.width + spacing), y: -CGFloat(rowIndex) * (slotSize.height + spacing))
                    print(letter.position)
                    letter.zPosition = 0
                    letter.isUserInteractionEnabled = true
                    gridNode.addChild(letter)
                case (false, _):
                    let slot = SKSpriteNode(color: Theme.colorSecondaryVariant, size: CGSize(width: slotSize.width, height: slotSize.height) )
                    slot.position = CGPoint(x: CGFloat(colIndex) * (slotSize.width + spacing), y: -CGFloat(rowIndex) * (slotSize.height + spacing))
                    slot.zPosition = 0
                    gridNode.addChild(slot)
                }
            }
        }
       
        print(viewSize)
        print(gridWidth)
        let height = ((CGFloat(boxSize) + spacing) * CGFloat(allCells.count-1))
        let width = ((CGFloat(boxSize) + spacing) * CGFloat(numCols-1))
        gridNode.position = CGPoint(x:-width/2,y:height/2)
  
    }

    
    func getDensitySettings(palindrome: String, viewSize: CGSize) -> GameField
    {
        let puzzleOnlyLetters = palindrome.replacingOccurrences(
            of: "\\s+",
            with: "",
            options: .regularExpression
        )
        let columns = palindrome.split(separator: " ").max { $0.count < $1.count }!.count
        let totalAreaRatio = Float(viewSize.width) / Float(viewSize.height)
        let columnsEnd = max(7, palindrome.count)
        var closestColumnsRatio: (Int, Float) = (columns, Float.greatestFiniteMagnitude)
        
        for cols in columns...columnsEnd {
            let usedAreaRatio = getUsedAreaRatio(
                columns: cols,
                puzzleOnlyLetters: puzzleOnlyLetters,
                palindrome: palindrome,
                smallGap: Int(spacing)
            )
            let newDistance = abs(usedAreaRatio - totalAreaRatio)
            let currentBestDistance = abs(closestColumnsRatio.1 - totalAreaRatio)
            if newDistance < currentBestDistance {
                closestColumnsRatio = (cols, usedAreaRatio)
            }
        }
        
        let solutionRows = getSolutionRows(puzzle: palindrome, cols: closestColumnsRatio.0)
        let letterRows = puzzleOnlyLetters.chunked(into: closestColumnsRatio.0)
        
        var boxSize = maxBoxSize
        let boxDecreaseRate = 1.05
        
        var exceedingWidth = getUsedWidth(columns: closestColumnsRatio.0, boxSize: Float(boxSize), gap: Int(spacing)) >= Float(viewSize.width)
   
        var exceedingHeight = getUsedHeight(rowsCount: solutionRows.count, boxSize: Float(boxSize), gap: Int(spacing)) >= Float(viewSize.height)
        
        while(exceedingWidth || exceedingHeight)
        {
            boxSize /= boxDecreaseRate
            exceedingWidth = getUsedWidth(columns: closestColumnsRatio.0, boxSize: Float(boxSize), gap: Int(spacing)) >= Float(viewSize.width)
            exceedingHeight = getUsedHeight(rowsCount: solutionRows.count, boxSize: Float(boxSize), gap: Int(spacing)) >= Float(viewSize.height)
        }


        return GameField(
            columns: closestColumnsRatio.0,
            letterRows: letterRows,
            solutionRows: solutionRows,
            boxSize: Float(boxSize)
        )
    }
    
    
    func getSolutionRows(puzzle: String, cols: Int) -> [String] {
        let words = puzzle.split(separator: " ")
        var lines: [String] = []
        var currentLine = ""
        
        for word in words {
            let wordLength = word.count
            if currentLine.isEmpty {
                currentLine = String(word)
            } else {
                let spaceCount = currentLine.count == 0 ? 0 : 1
                let potentialLength = currentLine.count + spaceCount + wordLength
                if potentialLength <= cols {
                    currentLine.append(" \(word)")
                } else {
                    let padding = String(repeating: " ", count: cols - currentLine.count)
                    lines.append(currentLine + padding)
                    currentLine = String(word)
                }
            }
        }
        
        if !currentLine.isEmpty {
            let padding = String(repeating: " ", count: cols - currentLine.count)
            lines.append(currentLine + padding)
        }
        
        return lines
    }
    
    
    private func getUsedAreaRatio(columns: Int, puzzleOnlyLetters: String, palindrome: String, smallGap: Int) -> Float {
        
        let rowsCount = puzzleOnlyLetters.chunked(into: columns).count
        + getSolutionRows(puzzle: palindrome, cols: columns).count
        let boxSize: Float = 1.0
        let usedHeight = getUsedHeight(
            rowsCount: rowsCount,
            boxSize: boxSize,
            gap: smallGap
        )
        let usedWidth = getUsedWidth(
            columns: columns,
            boxSize: boxSize,
            gap: smallGap
        )
        return usedWidth / usedHeight
    }
    
    private func getUsedHeight(rowsCount: Int, boxSize: Float, gap: Int) -> Float {
        return Float(rowsCount) * (boxSize + Float(gap)) + 2 * (boxSize + Float(gap))
    }
    
    private func getUsedWidth(columns: Int, boxSize: Float, gap: Int) -> Float {
        return Float(columns) * (boxSize + Float(gap)) + 2 * (boxSize + Float(gap))
    }
    
}
