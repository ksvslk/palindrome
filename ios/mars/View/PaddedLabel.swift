//
//  PaddedLabel.swift
//  mars
//
//  Created by Mina on 27.05.2023.
//

import Foundation

import UIKit

class PaddedLabel: UILabel {
    // Customize the padding values as per your requirements
    let padding = UIEdgeInsets(top: 0, left: 4, bottom: -2, right: 4)
    
    override func drawText(in rect: CGRect) {
        super.drawText(in: rect.inset(by: padding))
    }
    
    override var intrinsicContentSize: CGSize {
        let superContentSize = super.intrinsicContentSize
        let width = superContentSize.width + padding.left + padding.right
        let height = superContentSize.height + padding.top + padding.bottom
        return CGSize(width: width, height: height)
    }
}
