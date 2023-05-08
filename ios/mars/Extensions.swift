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


// MARK: Slice String to equal parts
extension String {
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
}


extension SKScene {
    
    public struct CustomSafeAreaInsets {
        public let top: CGFloat
        public let left: CGFloat
        public let bottom: CGFloat
        public let right: CGFloat
        
        public static var zero: CustomSafeAreaInsets {
            CustomSafeAreaInsets(top: 0, left: 0, bottom: 0, right: 0)
        }
    }
    
    
    fileprivate var scaleFactor: CGFloat {
      guard let view = view else { fatalError("Scene has no hosting view") }
      return size.height / view.frame.size.height
    }
    
    
    public var customSafeAreaInsets: CustomSafeAreaInsets {
      guard let view = view else { return .zero }

      return CustomSafeAreaInsets(
        top: view.safeAreaInsets.top * scaleFactor,
        left: view.safeAreaInsets.left * scaleFactor,
        bottom: view.safeAreaInsets.bottom * scaleFactor,
        right: view.safeAreaInsets.right * scaleFactor)
    }
 
}


extension SKView {
  override open func safeAreaInsetsDidChange() {
    scene?.didChangeSize(scene!.size)
  }
}


