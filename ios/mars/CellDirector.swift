//
//  CellDirector.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation

class CellDirector {
     
    var cells: [Cell]
    
    init(cells: [Cell]) {
        self.cells = cells
    }
    
    
    func handleCollisions(letter: Letter)
    {
        let candidateCells = getCandidateCells(letter: letter)
        guard let closestCell = getClosestCell(to: letter, from: candidateCells) else {
            print("sendLetterHome - no collisions")
            return
        }
        
        switch closestCell.cellType {
        case CellType.letter:
            if (closestCell.state == CellState.home)
            {
                print(" switchLetterPositions(letter, closestCell)")
                return
            }
        case CellType.slot:
            if (closestCell.friend == nil)
            {
                print("placeLetterInEmptyBox(letter, closestCell)")
                return
            }
            else if (closestCell.friend == letter) {
                print("landLetterInSameSlotItWasDraggedFrom")
                return
            }
        case .none:
            return
        }

    }
    
    
    private func getClosestCell(to: Letter, from: [Cell]) -> Cell? {
        return from.min { (a, b) in
            a.position.distance(point: to.position) < b.position.distance(point: to.position)
        }
    }
    
    
    private func getCandidateCells(letter: Letter) -> [Cell] {
        var candidateCells = [Cell]()
        
        for droppedTo in cells {
            if letter.intersects(droppedTo) && letter != droppedTo {
                guard (droppedTo.cellType == CellType.letter && droppedTo.state == CellState.home) || droppedTo.cellType == CellType.slot else {
                    continue
                }
                candidateCells.append(droppedTo)
            }
        }
        
        return candidateCells
    }
    
    private func restoreSlotPlacements(info: Level)
    {
        
    }
    
}
