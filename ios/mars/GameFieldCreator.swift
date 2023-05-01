//
//  GameFieldCreator.swift
//  mars
//
//  Created by Mina on 01.05.2023.
//

import Foundation

struct GameFieldCreator {
    
    static let spacing = 5.0
    static let edgePadding = 16
    static let maxBoxSize = 40.0
    
    // MARK: Create game field
    static func getGameField(palindrome: String, viewSize: CGSize) -> GameField
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
            boxSize: CGSize(width: boxSize, height: boxSize)
        )
    }
    
    
    // MARK: Solution rows
    static func getSolutionRows(puzzle: String, cols: Int) -> [String] {
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
    
}
