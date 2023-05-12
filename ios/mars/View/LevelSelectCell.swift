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
        addSubview(disabledEffectView)
        label.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: contentView.centerYAnchor)
        ])
        
        // Add a tap gesture recognizer to the cell's contentView
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(didTapCell(_:)))
        contentView.addGestureRecognizer(tapGesture)
        
        // Set default appearance
        updateAppearance()
    }
    
    
    // Handle the tap action in this method
    @objc func didTapCell(_ sender: UITapGestureRecognizer) {
        let integerLevel = Int(label.text ?? "-1")
        print("didTapCell nr: \(integerLevel)")
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
        contentView.isUserInteractionEnabled = isCellEnabled
        disabledEffectView.isUserInteractionEnabled = isCellEnabled
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
