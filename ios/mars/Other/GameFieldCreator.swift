//
//  GameFieldCreator.swift
//  mars
//
//  Created by Mina on 01.05.2023.
//

import Foundation

struct GameFieldCreator {
    
    static let spacing = 5.0
    static let maxBoxSize = 45.0
    
    // MARK: Create game field
    static func getGameField(palindrome: String, viewSize: CGSize, level: Level) -> GameField
    {
        let puzzleOnlyLetters = palindrome.replacingOccurrences(
            of: "\\s+",
            with: "",
            options: .regularExpression
        )
        
        let columns = palindrome.split(separator: " ").max { $0.count < $1.count }!.count
        let totalAreaRatio = Float(viewSize.width) / Float(viewSize.height)
        let columnsEnd = max(3, palindrome.count)
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
        let boxDecreaseRate = 1.02
        
        var exceedingWidth = getUsedWidth(columns: closestColumnsRatio.0, boxSize: Float(boxSize), gap: Int(spacing)) >= Float(viewSize.width)
        var exceedingHeight = getUsedHeight(rowsCount: solutionRows.count, boxSize: Float(boxSize), gap: Int(spacing)) >= Float(viewSize.height)
        
        while(exceedingWidth || exceedingHeight)
        {
            boxSize /= boxDecreaseRate
            exceedingWidth = getUsedWidth(columns: closestColumnsRatio.0, boxSize: Float(boxSize), gap: Int(spacing)) >= Float(viewSize.width)
            exceedingHeight = getUsedHeight(rowsCount: solutionRows.count, boxSize: Float(boxSize), gap: Int(spacing)) >= Float(viewSize.height)
        }
        
        updateFriendIndexesIfNeeded(palindrome: palindrome, puzzleOnlyLetters:puzzleOnlyLetters, info: level, newColumnCount: closestColumnsRatio.0)
        
        return GameField(
            columns: closestColumnsRatio.0,
            letterRows: letterRows,
            solutionRows: solutionRows,
            boxSize: CGSize(width: boxSize, height: boxSize)
        )
    }
    
    
    // MARK: Solution rows
    static func getSolutionRows(puzzle: String, cols: Int) -> [String] {
        let puzzleWords = puzzle.split(separator: " ")
        return puzzleWords.reduce(into: [String]()) { acc, s in
            if acc.isEmpty {
                acc.append(String(s))
            } else if acc.last!.count + s.count + 1 <= cols {
                acc[acc.count - 1] += " " + s
            } else {
                let blankCount = cols - acc.last!.count > 0 ? cols - acc.last!.count : 0
                let last = acc.last! + String(repeating: " ", count: blankCount)
                acc[acc.count - 1] = last
                acc.append(String(s))
            }
        }
    }
    
    
    // MARK: Used area ratio
    static private func getUsedAreaRatio(columns: Int, puzzleOnlyLetters: String, palindrome: String, smallGap: Int) -> Float {
        
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
    
    
    // MARK: Used height
    static private func getUsedHeight(rowsCount: Int, boxSize: Float, gap: Int) -> Float {
        return Float(rowsCount) * (boxSize + Float(gap)) + 2 * (boxSize + Float(gap))
    }
    
    
    // MARK: Used width
    static private func getUsedWidth(columns: Int, boxSize: Float, gap: Int) -> Float {
        return Float(columns) * (boxSize + Float(gap)) + 2 * (boxSize + Float(gap))
    }
    
    
    // MARK: Update friend indexes
    static private func updateFriendIndexesIfNeeded(palindrome: String, puzzleOnlyLetters:String, info: Level, newColumnCount: Int) {
     
        if info.columnCount != -1 && info.columnCount != newColumnCount {

            let firstIndexForNewColumnsCount =
                puzzleOnlyLetters.chunked(into: newColumnCount).count * newColumnCount + 1
            let firstIndexForOldColumnsCount =
                puzzleOnlyLetters.chunked(into: Int(info.columnCount)).count * Int(info.columnCount) + 1
            
            //Swift.debugPrint("firstIndexForOldColumnsCount: \(firstIndexForOldColumnsCount), cols: \(info.columnCount)")
            //Swift.debugPrint("firstIndexForNewColumnsCount: \(firstIndexForNewColumnsCount), cols: \(newColumnCount)")

            let oldList = getSolutionRows(puzzle: palindrome, cols: Int(info.columnCount))
            let newList = getSolutionRows(puzzle: palindrome, cols: newColumnCount)
            
            let newIndexes = newList
                .joined()
                .enumerated()
                .filter { $0.element != " " }
                .map { firstIndexForNewColumnsCount + $0.offset  }
            let oldIndexes = oldList
                .joined()
                .enumerated()
                .filter { $0.element != " " }
                .map { firstIndexForOldColumnsCount + $0.offset  }
            
            //Swift.debugPrint("oldIndexes: ", oldIndexes)
            //Swift.debugPrint("newIndexes: ", newIndexes)
            
            let oldFriendToNewFriendIndex = Dictionary(uniqueKeysWithValues: zip(oldIndexes, newIndexes))
          
            if (!oldFriendToNewFriendIndex.isEmpty)
            {
                Database.shared.updateColumnCount(
                    newColumntCount: Int64(newColumnCount),
                    oldFriendToNewFriendIndex: oldFriendToNewFriendIndex
                )
            }
            
        }
    }
 

}
