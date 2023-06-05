//
//  CellDirector.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation
import SpriteKit

class CellDirector {
    
    var cells: [Cell] = [] {
        didSet {
            for cell in self.cells {
                guard let l = (cell as? Letter) else { continue }
                if (l.data.locked)
                {
                    l.color = Theme.letterRewardedColor
                }
            }
        }
    }
    
    init(cells: [Cell]) {
        self.cells = cells
    }
    
    // MARK: Main handle
    func handleCollisions(letter: Letter) -> CollisionResult
    {
        let result = checkCollisionsForAction(letter: letter)
        
#if DEBUG
        assertThatLettersHaveUniqueFriends()
        assertThatMaximumOfTwoCellsHaveSameCoordinates()
#endif
        return result
    }
    
    
    // MARK: Check collisions for action
    private func checkCollisionsForAction(letter: Letter) -> CollisionResult
    {
        guard let candidateCells = getCandidateCells(letter: letter), !candidateCells.isEmpty else {
            Swift.debugPrint("CellDirector: no candidate cells - send letter home")
            sendLetterHome(letter: letter)
            return CollisionResult.nothing
        }
        guard let closestCell = getClosestCell(to: letter, from: candidateCells) else {
            Swift.debugPrint("CellDirector: no collisions - send letter home")
            sendLetterHome(letter: letter)
            return CollisionResult.nothing
        }
        
        return getMoveResult(letter: letter, closestCell: closestCell)
    }
    
    
    private func getMoveResult(letter:Letter, closestCell:Cell) -> CollisionResult
    {
        switch closestCell.cellType {
        case CellType.letter:
            if (closestCell.state == CellState.home)
            {
                Swift.debugPrint("CellDirector: switchLetterPositions(letter, closestCell)")
                let bottomLetter = closestCell as! Letter
                switchLetterPositions(top: letter, bottom: bottomLetter)
                return CollisionResult.switching
            }
        case CellType.slot:
            if (closestCell.friend == nil)
            {
                Swift.debugPrint("CellDirector: placeLetterInEmptyBox(letter, closestCell)")
                placeLetterInEmptyBox(letter: letter, slot: closestCell as! Slot)
                return CollisionResult.landing
            }
            else if (closestCell.friend == letter) {
                Swift.debugPrint("CellDirector: landLetterInSameSlotItWasDraggedFrom")
                return CollisionResult.landing
            }
        case .none:
            return CollisionResult.nothing
        }
        return CollisionResult.nothing
    }
    
    
    // MARK: Assert cells correctness
#if DEBUG
    private func assertThatMaximumOfTwoCellsHaveSameCoordinates()
    {
        struct Point : Hashable {
            let x: CGFloat
            let y: CGFloat
            
            var debugDescription: String {
                return "X: \(x), Y: \(y)"
            }
        }
        
        let uniqueCoordinates = cells.compactMap({ cell in
            if (cell.cellType == .letter)
            {
                return Point(x: cell.currentPosition.x, y: cell.currentPosition.y)
            }
            else {
                return Point(x: cell.homePosition.x, y: cell.homePosition.y)
            }
        })
        var countDict = Dictionary<Point, Int>()
        for tuple in uniqueCoordinates {
            countDict[tuple, default: 0] += 1
        }
        assert(countDict.filter({ element in
            element.value > 2
        }).count == 0)
    }
    
    private func assertThatLettersHaveUniqueFriends()
    {
        let uniqueFriendIndexes = Set(cells.compactMap({ cell in
            if (cell.cellType == .letter)
            {
                return cell.friend?.homeIndex
            }
            else {
                return nil
            }
        }))
        let allLettersWithFriends = cells.filter({ $0.cellType == .letter && $0.friend != nil })
        assert(uniqueFriendIndexes.count == allLettersWithFriends.count)
    }
#endif
    
    
    // MARK: Get colliding Cells
    private func getCandidateCells(letter: Letter) -> Optional<[Cell]> {
        var candidateCells = [Cell]()
        
        for droppedTo in cells {
            if letter.intersects(droppedTo) && letter != droppedTo {
                guard (
                    droppedTo.cellType == CellType.letter &&
                    droppedTo.state == CellState.home) ||
                    (droppedTo.cellType == CellType.slot &&
                    droppedTo.friend == nil) else {
                    continue
                }
                if let l = (droppedTo as? Letter) {
                    if (l.data.locked == true)
                    {
                        continue
                    }
                }
                candidateCells.append(droppedTo)
            }
        }
        
        return candidateCells
    }
    
    
    // MARK: Get closest Cell
    private func getClosestCell(to: Letter, from: [Cell]) -> Cell? {
        return from.min { (a, b) in
            a.position.distance(point: to.position) < b.position.distance(point: to.position)
        }
    }
    
    
    // MARK: Place in empty Slot
    private func placeLetterInEmptyBox(
        letter: Letter,
        slot: Slot,
        isRewarded: Bool = false,
        isLastRewardedLetter: Bool = false
    ) {
        
        Database.shared.changeFriendIndex(
            letter: letter.data,
            newFriendIndex: slot.homeIndex
        )
        
        letter.friend?.friend = nil
        slot.setFriendWithoutDidSet(letter: letter)
        letter.friend = slot
        letter.currentPosition = slot.homePosition
    }
    
    
    // MARK: Send to home
    private func sendLetterHome(letter: Letter, isRewarded: Bool = false) {
        if (isRewarded) {
            letter.zPosition = C.MIDDLE_LAYER
        }
        if (letter.friend != nil) {
            
            Database.shared.changeFriendIndex(
                letter: letter.data,
                newFriendIndex: -1
            )
            
            letter.friend?.friend = nil
            letter.friend = nil
            letter.currentPosition = letter.homePosition
        }
    }
   
    
    
    // MARK: Switch positions
    private func switchLetterPositions(
        top: Letter,
        bottom: Letter,
        isRewarded: Bool = false
    ) {
        //TODO: stuff missing
        Database.shared.switchFriendIndexes(top: top.data, bottom: bottom.data)
        
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
    
    
    // MARK: Switch friends
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
    
    
    // MARK: Reward with letter
    func rewardWithRandomLetter() -> CollisionResult
    {
        var result: CollisionResult = CollisionResult.nothing
      
        // Filter candidate Letter and Slot instances
        let candidateLetters = cells.compactMap { $0 as? Letter }.filter { letter in
            letter.data.locked == false
        }
        let candidateSlots = cells.compactMap { $0 as? Slot }

        // Pick a random Letter and first corresponding Slot, if nothing found return
        var randomLetter:Letter? = nil
        
        for letter in candidateLetters {
            if (letter.data.letter != (letter.friend as? Slot)?.actualChar)
            {
                randomLetter = letter
                break
            }
        }

        guard let randomLetter = randomLetter else { return CollisionResult.nothing }
        
        for slot in candidateSlots {
            let slotFriend:Letter? = slot.friend as? Letter
            
            if (slot.actualChar == randomLetter.data.letter && slotFriend == nil)
            {
                // We found empty Slot for our random Letter, break
                result = getMoveResult(letter: randomLetter, closestCell: slot)
                break
            }
            else if (slot.actualChar == randomLetter.data.letter &&
                     slotFriend?.data.letter != randomLetter.data.letter
            )
            {
                // We found NON-empty Slot, which has wrong Letter in it
                result = getMoveResult(letter: randomLetter, closestCell: slotFriend!)
                break
            }
        }
        Database.shared.lockLetter(letter: randomLetter.data)
        randomLetter.moveLetter(endColor: Theme.letterRewardedColor, durationMultiplier: 3.0, zLayer: C.TOP_LAYER)
#if DEBUG
        assertThatLettersHaveUniqueFriends()
        assertThatMaximumOfTwoCellsHaveSameCoordinates()
#endif
        return result
        
    }
    
}
