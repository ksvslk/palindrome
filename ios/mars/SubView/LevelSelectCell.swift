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
        label.textColor = Theme.shared.colors.onSurface
        label.font = UIFont(name: Theme.shared.yantraBold, size: 20)
        
        // Add the label to the cell's contentView and center it
        contentView.addSubview(label)
        label.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: contentView.centerYAnchor)
        ])
        
        // Set default appearance
        updateAppearance()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    var isCellEnabled: Bool = true {
        didSet {
            updateAppearance()
        }
    }
    
    private func updateAppearance() {
        contentView.alpha = isCellEnabled ? 1.0 : 0.3
        label.font = isCellEnabled ? UIFont(name: Theme.shared.yantraBold, size: 25) : UIFont(name: Theme.shared.yantraMedium, size: 20)
    }


}
