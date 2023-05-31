//
//  ScoreItemCell.swift
//  mars
//
//  Created by Mina on 31.05.2023.
//

import Foundation
import UIKit

class ScoreItemCell: UITableViewCell {
    
    struct Constants {
        static let contentInsets = UIEdgeInsets(top: 8.0, left: 24.0, bottom: 8.0, right: 24.0)
        static let avatarSize = CGSize(width: 36.0, height: 36.0)
    }
 
    var presentable = ScoreItemInfo(displayName: "", photo: UIImage(), score: -1, rank: -1, isCurrentPlayer: false)
    
    // MARK: - Views
    let iconView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 8.0
        return view
    }()
    
    
    let nameLabel: UILabel = {
        let label = UILabel()
        label.textColor = Theme.colorOnPrimary
        label.font = UIFont(name: Theme.yantraBold, size: 17.0)
        label.backgroundColor = .clear
        return label
    }()
    
    
    let scoreLabel: UILabel = {
        let label = UILabel()
        label.textColor = Theme.colorOnPrimary
        label.backgroundColor = .clear
        label.font = UIFont(name: Theme.yantraMedium, size: 13.0)
        return label
    }()
    
    let rankLabel: UILabel = {
        let label = UILabel()
        label.textColor = Theme.colorOnPrimary
        label.backgroundColor = .clear
        label.font = UIFont(name: Theme.yantraBold, size: 20)
        return label
    }()
    
    
    lazy var scoreDetails: UIStackView = {
        let stackView = UIStackView(arrangedSubviews: [nameLabel, scoreLabel])
        stackView.axis = .vertical
        stackView.alignment = .leading
        stackView.translatesAutoresizingMaskIntoConstraints = false
        return stackView
    }()
    
    lazy var stackView: UIStackView = {
        let stackView = UIStackView(arrangedSubviews: [iconView, scoreDetails, rankLabel])
        stackView.alignment = .center
        stackView.spacing = 16.0
        stackView.translatesAutoresizingMaskIntoConstraints = false
        return stackView
    }()
    
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.backgroundColor
        isAccessibilityElement = true
        contentView.addSubview(stackView)
        setupConstraints()
        isUserInteractionEnabled = false
       
    }
    
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    
    func setupConstraints() {
        
        stackView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: Constants.contentInsets.top).isActive = true
        stackView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: Constants.contentInsets.left).isActive = true
        stackView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -Constants.contentInsets.right).isActive = true
        stackView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -Constants.contentInsets.bottom).isActive = true
        
        let avatarWidthConstriant = iconView.widthAnchor.constraint(equalToConstant: Constants.avatarSize.width)
        let avatarHeightConstraint = iconView.heightAnchor.constraint(equalToConstant: Constants.avatarSize.height)
        
        [avatarWidthConstriant, avatarHeightConstraint].forEach {
            $0.priority = UILayoutPriority(UILayoutPriority.required.rawValue - 1)
            $0.isActive = true
        }
    }
    
    
    func configure(with presentable: ScoreItemInfo) {
        self.presentable = presentable
        nameLabel.text = presentable.displayName
        scoreLabel.text = "Score: " + String(presentable.score)
        nameLabel.textColor = Theme.colorOnPrimary
        scoreLabel.textColor = Theme.colorOnPrimary
        rankLabel.text = "#" + String(presentable.rank)
        rankLabel.textColor = Theme.colorOnPrimary
        iconView.backgroundColor = Theme.darkBlue
        addImageToIconView(image: presentable.photo)
    

        let tintedColor = Theme.letterRewardedColor.withAlphaComponent(0.75)
        backgroundColor = presentable.isCurrentPlayer ? tintedColor : Theme.backgroundColor
    }
    
    
    func addImageToIconView(image:UIImage)
    {
        let uiImageView = UIImageView(image: image)
        uiImageView.translatesAutoresizingMaskIntoConstraints = false
        
        // Add the image view as a subview to the container view
        iconView.addSubview(uiImageView)
        
        // Configure Auto Layout constraints to center the image view
        uiImageView.centerXAnchor.constraint(equalTo: iconView.centerXAnchor).isActive = true
        uiImageView.centerYAnchor.constraint(equalTo: iconView.centerYAnchor).isActive = true
     
    }
    
}
