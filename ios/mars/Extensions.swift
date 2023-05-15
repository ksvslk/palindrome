//
//  Extensions.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation
import SpriteKit


// MARK: Distance between CGPoint's
extension CGPoint {
    func distance(point: CGPoint) -> CGFloat {
        return CGFloat(hypotf(Float(point.x - self.x), Float(point.y - self.y)))
    }
}


extension String {
    
    // MARK: Chunk string
    func chunked(into size: Int) -> [String] {
        var result = [String]()
        for i in stride(from: 0, to: self.count, by: size) {
            let startIndex = self.index(self.startIndex, offsetBy: i)
            let endIndex = self.index(startIndex, offsetBy: size, limitedBy: self.endIndex) ?? self.endIndex
            var chunk = String(self[startIndex..<endIndex])
            if chunk.count < size {
                chunk = chunk.padding(toLength: size, withPad: " ", startingAt: 0)
            }
            result.append(chunk)
        }
        return result
    }
    
    
    //MARK: Is string palindrome?
    func isPalindrome() -> Bool {
         let testString = self.lowercased()
             .replacingOccurrences(of: #"[\W+]"#, with: "", options: .regularExpression)
         return testString.count >= 3 && testString == String(testString.reversed())
     }

}


extension UIImpactFeedbackGenerator {
    func impactOccuredCustom(intensity:CGFloat)
    {
        if (UserDefaults.standard.bool(forKey: C.VIBRATION_ON))
        {
            self.impactOccurred(intensity: intensity)
        }
    }
}


extension UINotificationFeedbackGenerator {
    func notificationOccuredCustom(notificationType: FeedbackType){
        self.notificationOccurred(notificationType)
    }
}
