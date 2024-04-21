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
    
    func isSingleWordAndOverCharacters(length:Int) -> Bool {
        return !self.contains(" ") && self.count > length
    }
    
    //MARK: Is string palindrome?
    func isPalindrome() -> Bool {
        let testString = self.lowercased()
            .replacingOccurrences(of: #"[\W+]"#, with: "", options: .regularExpression)
        return testString.count >= 3 && testString == String(testString.reversed())
    }
    
    
    // MARK: Get score
    func maxScore() -> Int64 {
        return Int64(self.count * C.SCORE_LETTER_MULTIPLIER)
    }
    
    func getStringOutsideBrackets() -> String {
        let pattern = "\\([^)]*\\)"
        let regex = try? NSRegularExpression(pattern: pattern)
        
        let range = NSRange(self.startIndex..., in: self)
        let result = regex?.stringByReplacingMatches(in: self, range: range, withTemplate: "")
        
        return result ?? self
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
        if (UserDefaults.standard.bool(forKey: C.VIBRATION_ON))
        {
            self.notificationOccurred(notificationType)
        }
    }
}

extension SKAction {
    static func playSound(soundFile: String) -> SKAction
    {
        let soundEnabled = UserDefaults.standard.bool(forKey: C.SOUND_ON)
        guard soundEnabled else {
            return SKAction.wait(forDuration: 0)
        }
        return SKAction.playSoundFileNamed(soundFile, waitForCompletion: false)
    }
}


extension CAShapeLayer {
    func drawCircleAtLocation(location: CGPoint, withRadius radius: CGFloat, andColor color: UIColor, filled: Bool) {
        fillColor = filled ? color.cgColor : UIColor.white.cgColor
        strokeColor = color.cgColor
        let origin = CGPoint(x: location.x - radius, y: location.y - radius)
        path = UIBezierPath(ovalIn: CGRect(origin: origin, size: CGSize(width: radius * 2, height: radius * 2))).cgPath
    }
}

private var handle: UInt8 = 0

extension UIBarButtonItem {
    private var badgeLayer: CAShapeLayer? {
        if let b: AnyObject = objc_getAssociatedObject(self, &handle) as AnyObject? {
            return b as? CAShapeLayer
        } else {
            return nil
        }
    }
    
    func addBadge(number: Int, withOffset offset: CGPoint = CGPoint.zero, andColor color: UIColor = UIColor.red, andFilled filled: Bool = true) {
        guard let view = self.value(forKey: "view") as? UIView else { return }
        
        badgeLayer?.removeFromSuperlayer()
        
        // Initialize Badge
        let badge = CAShapeLayer()
        let radius = CGFloat(7)
        let location = CGPoint(x: view.frame.width - (radius + offset.x), y: (radius + offset.y))
        badge.drawCircleAtLocation(location: location, withRadius: radius, andColor: color, filled: filled)
        view.layer.addSublayer(badge)
        
        // Initialiaze Badge's label
        let label = CATextLayer()
        label.string = "\(number)"
        label.alignmentMode = CATextLayerAlignmentMode.center
        label.fontSize = 11
        label.frame = CGRect(origin: CGPoint(x: location.x - 4, y: offset.y), size: CGSize(width: 8, height: 16))
        label.foregroundColor = filled ? Theme.shared.colors.onError.cgColor : color.cgColor
        label.backgroundColor = UIColor.clear.cgColor
        label.contentsScale = UIScreen.main.scale
        badge.addSublayer(label)
        
        // Save Badge as UIBarButtonItem property
        objc_setAssociatedObject(self, &handle, badge, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    }
    
    func updateBadge(number: Int) {
        if let text = badgeLayer?.sublayers?.filter({ $0 is CATextLayer }).first as? CATextLayer {
            text.string = "\(number)"
        }
    }
    
    func removeBadge() {
        badgeLayer?.removeFromSuperlayer()
    }
}


extension UIViewController {
    
    func showToast(message : String, font: UIFont) {
        
        let toastLabel = UILabel(frame: CGRect(x: self.view.frame.size.width/2 - 75, y: self.view.frame.size.height-100, width: 150, height: 35))
        toastLabel.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        toastLabel.textColor = UIColor.white
        toastLabel.font = font
        toastLabel.textAlignment = .center;
        toastLabel.text = message
        toastLabel.alpha = 1.0
        toastLabel.layer.cornerRadius = 10;
        toastLabel.clipsToBounds  =  true
        self.view.addSubview(toastLabel)
        UIView.animate(withDuration: 4.0, delay: 0.1, options: .curveEaseOut, animations: {
            toastLabel.alpha = 0.0
        }, completion: {(isCompleted) in
            toastLabel.removeFromSuperview()
        })
    }
}


postfix operator ~

postfix func ~ (string: String) -> String {
    let localizedString = NSLocalizedString(string, comment: "")
    assert(localizedString != string, "Localized string not found for key: \(string)")
    return localizedString
}

extension UIColor {
   convenience init(red: Int, green: Int, blue: Int) {
       assert(red >= 0 && red <= 255, "Invalid red component")
       assert(green >= 0 && green <= 255, "Invalid green component")
       assert(blue >= 0 && blue <= 255, "Invalid blue component")

       self.init(red: CGFloat(red) / 255.0, green: CGFloat(green) / 255.0, blue: CGFloat(blue) / 255.0, alpha: 1.0)
   }

   convenience init(rgb: Int) {
       self.init(
           red: (rgb >> 16) & 0xFF,
           green: (rgb >> 8) & 0xFF,
           blue: rgb & 0xFF
       )
   }
}

extension Calendar {
    /// Checks if the current date is between two specified days across the new year.
    /// - Parameters:
    ///   - startMonth: The month of the start date.
    ///   - startDay: The day of the start date.
    ///   - endMonth: The month of the end date in the following year.
    ///   - endDay: The day of the end date.
    /// - Returns: A Boolean indicating if the current date is within the specified date range.
    func isCurrentDateInRange(startMonth: Int, startDay: Int, endMonth: Int, endDay: Int) -> Bool {
        let currentYear = self.component(.year, from: Date())
        let startDateComponents = DateComponents(year: currentYear, month: startMonth, day: startDay)
        let endDateComponents = DateComponents(year: currentYear + 1, month: endMonth, day: endDay)
        
        guard let startDate = self.date(from: startDateComponents),
              let endDate = self.date(from: endDateComponents) else {
            return false
        }
        
        let dateInterval = DateInterval(start: startDate, end: endDate)
        return dateInterval.contains(Date())
    }
}
