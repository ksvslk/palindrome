//
//  LevelSelectCell.swift
//  mars
//
//  Created by Mina on 09.05.2023.
//

import Foundation
import UIKit

class LevelSelectCell: UICollectionViewCell {
    let label = UILabel()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        
        // Customize the label as desired
        label.textAlignment = .center
        label.textColor = Theme.colorOnPrimary
        label.font = UIFont(name: Theme.letterFont, size: 20)
        
        // Add the label to the cell's contentView and center it
        contentView.addSubview(label)
        label.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: contentView.centerYAnchor)
        ])
        
        // Add a tap gesture recognizer to the cell's contentView
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(didTapCell(_:)))
        contentView.addGestureRecognizer(tapGesture)
    }
    
    
    // Handle the tap action in this method
    @objc func didTapCell(_ sender: UITapGestureRecognizer) {
        let integerLevel = Int(label.text ?? "-1")
        print("didTapCell nr: \(integerLevel)")
    }
    
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
