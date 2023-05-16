//
//  StateManager.swift
//  mars
//
//  Created by Mina on 16.05.2023.
//

import Foundation

class StateManager {
    static let shared = StateManager()
    
    private var _currentState: GameState = .play
     
     var currentState: GameState {
         get {
             return _currentState
         }
         set(newState) {
             guard isTransitionAllowed(from: _currentState, to: newState) else {
                 return // transition not allowed, do nothing
             }
             print("Transitioning from \(_currentState) to \(newState)")
             _currentState = newState
             stateDidChange?(newState)
         }
     }
     
     var stateDidChange: ((GameState) -> Void)?
     
     private func isTransitionAllowed(from oldState: GameState, to newState: GameState) -> Bool {
         switch oldState {
         case .play:
             return newState == .animating
         case .animating:
             return newState == .play || newState == .completed
         case .completed:
             return false
         }
     }
    
    private init() {}
}
