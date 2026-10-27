//
//  NumberLabel.swift
//  mars
//
//  Created by Mina on 12.05.2023.
//

import Foundation
import UIKit

class NumberLabel: UILabel {
    var startNumber: Int = 0
    var endNumber: Int = 100
    var maxNumber: Int = 100
    var duration: TimeInterval = 2.0
    var displayLink: CADisplayLink?
    var startTime: TimeInterval = 0.0
    var currentValue: Int = 0
    
    func animate(endNumber:Int, maxNumber:Int, duration: TimeInterval) {
        self.duration = duration
        self.currentValue = 0
        self.startNumber = 0
        self.endNumber = endNumber
        self.maxNumber = maxNumber
        displayLink?.invalidate()
        displayLink = CADisplayLink(target: self, selector: #selector(updateLabel))
        displayLink?.add(to: .current, forMode: .default)
        startTime = CACurrentMediaTime()
    }
    
    @objc private func updateLabel() {
        let elapsedTime = CACurrentMediaTime() - startTime
        let progress = elapsedTime / duration
        
        if progress >= 1.0 {
            currentValue = endNumber
            text = "\(currentValue) of \(maxNumber) points"
            displayLink?.invalidate()
            displayLink = nil
        } else {
            let value = Int(Double(endNumber - startNumber) * progress) + startNumber
            currentValue = value
            text = "\(currentValue) of \(maxNumber) points"
        }
    }
}
