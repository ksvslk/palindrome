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
        label.font = UIFont(name: Theme.yantraBold, size: 20)
        
        // Add the label to the cell's contentView and center it
        contentView.addSubview(label)
        addSubview(disabledEffectView)
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
        contentView.alpha = isCellEnabled ? 1.0 : 0.5
        disabledEffectView.isHidden = isCellEnabled
    }
    
    private let disabledEffectView: UIVisualEffectView = {
        let blurEffect = UIBlurEffect(style: .regular)
        let effectView = UIVisualEffectView(effect: blurEffect)
        effectView.translatesAutoresizingMaskIntoConstraints = false
        effectView.alpha = 0.8
        effectView.layer.cornerRadius = 8
        effectView.layer.masksToBounds = true
        return effectView
    }()
}
