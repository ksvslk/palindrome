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
        guard let candidateCells = getCandidateCells(letter: letter), !candidateCells.isEmpty else {
            print("no candidate cells - send letter home")
            sendLetterHome(letter: letter)
            return
        }
        guard let closestCell = getClosestCell(to: letter, from: candidateCells) else {
            print("no collisions - send letter home")
            sendLetterHome(letter: letter)
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
                placeLetterInEmptyBox(letter: letter, slot: closestCell as! Slot)
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
    
    
    private func placeLetterInEmptyBox(
        letter: Letter,
        slot: Slot,
        isRewarded: Bool = false,
        isLastRewardedLetter: Bool = false
    ) {
        
        // TODO: Implement rewarded letter
        letter.friend = nil
        slot.friend = letter
        letter.friend = slot
        letter.currentPosition = slot.homePosition
        // TODO: adjust state of letter to animation?
        // TODO: set different animation length for rewarded letter
        // TODO: update friend index in database and if rewarded set it locked
        // TODO: check if correct move and increase mistakes if so
    }
    
    private func sendLetterHome(letter: Letter, isRewarded: Bool = false) {
        if (isRewarded) {
            letter.zPosition = C.MIDDLE_LAYER
        }
        if (letter.friend != nil) {
            // TODO: increase mistakes
            letter.friend?.friend = nil
            letter.friend = nil
            letter.currentPosition = letter.homePosition
            // TODO: update friend indexes
        }
    }
    
    
    private func getClosestCell(to: Letter, from: [Cell]) -> Cell? {
        return from.min { (a, b) in
            a.position.distance(point: to.position) < b.position.distance(point: to.position)
        }
    }
    
    
    private func getCandidateCells(letter: Letter) -> Optional<[Cell]> {
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
