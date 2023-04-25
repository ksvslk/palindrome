//
//  Extensions.swift
//  mars
//
//  Created by ksvslk on 25.04.2023.
//

import Foundation

extension CGPoint {
    func distance(point: CGPoint) -> CGFloat {
        return CGFloat(hypotf(Float(point.x - self.x), Float(point.y - self.y)))
    }
}
