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
        
        // TODO: handle TODO level state

        
        if (Database.shared.currentLevel?.levelProgress == Level.Progress.todo)
        {
            //TODO: change indexes for letters and the progress to in_progress, also column count
       
        }
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
                switchLetterPositions(top: letter, bottom: closestCell as! Letter)
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
        
        // TODO: stuff missing
        letter.friend?.friend = nil
        slot.friend = letter
        letter.friend = slot
        letter.currentPosition = slot.homePosition
    }
    
    private func sendLetterHome(letter: Letter, isRewarded: Bool = false) {
        if (isRewarded) {
            letter.zPosition = C.MIDDLE_LAYER
        }
        if (letter.friend != nil) {
            // TODO: stuff missing
            letter.friend?.friend = nil
            letter.friend = nil
            letter.currentPosition = letter.homePosition
        }
    }
    
    private func switchLetterPositions(
        top: Letter,
        bottom: Letter,
        isRewarded: Bool = false
    ) {
        //TODO: stuff missing
        let tempBottomCurrent = bottom.currentPosition
        bottom.currentPosition = top.currentPosition
        top.currentPosition = tempBottomCurrent
        
        let tempBottomHome = bottom.homePosition
        bottom.homePosition = top.homePosition
        top.homePosition = tempBottomHome
        
        switchFriends(bottom: bottom, top: top)
        bottom.zPosition = C.MIDDLE_LAYER
        bottom.moveLetter()
    }
    
  
    private func switchFriends(bottom: Letter, top: Letter) {
        let bottomFriend = bottom.friend
        let topFriend = top.friend
        if (bottom.friend != nil && top.friend != nil) {
            bottom.friend = topFriend
            top.friend = bottomFriend
            topFriend?.friend = bottom
            bottomFriend?.friend = top
        } else if (bottom.friend != nil && top.friend == nil) {
            top.friend = bottomFriend
            bottomFriend?.friend = top
            bottom.friend = nil
        } else if (top.friend != nil && bottom.friend == nil) {
            bottom.friend = topFriend
            topFriend?.friend = bottom
            top.friend = nil
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
    /*
    private func restoreSlotPlacements(info: Level)
    {
        
    }
    */
}
