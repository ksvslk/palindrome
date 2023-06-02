//
//  UserGroupMemberCell.swift
//  PanModal
//
//  Created by Tosin Afolabi on 2/26/19.
//  Copyright © 2019 PanModal. All rights reserved.
//

import UIKit

class MenuItemCell: UITableViewCell {
    
    struct Constants {
        static let contentInsets = UIEdgeInsets(top: 8.0, left: 24.0, bottom: 8.0, right: 24.0)
        static let avatarSize = CGSize(width: 36.0, height: 36.0)
    }
    
    // MARK: - Properties
    
    var presentable = MenuItemInfo(name: "", comment: "", systemImage: UIImage(), closure: {view in })
    
    // MARK: - Views
    
    let iconView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 8.0
        return view
    }()

    
    let nameLabel: UILabel = {
        let label = UILabel()
        label.textColor = #colorLiteral(red: 0.8196078431, green: 0.8235294118, blue: 0.8274509804, alpha: 1)
        label.font = UIFont(name: Theme.yantraBold, size: 17.0)
        label.backgroundColor = .clear
        return label
    }()
    
    let commentLabel: UILabel = {
        let label = PaddedLabel()
        label.textColor = #colorLiteral(red: 0.7019607843, green: 0.7058823529, blue: 0.7137254902, alpha: 1)
        label.backgroundColor = .clear
        label.font = UIFont(name: Theme.yantraBold, size: 13.0)
        return label
    }()
    
    lazy var buttonDetails: UIStackView = {
        let stackView = UIStackView(arrangedSubviews: [nameLabel, commentLabel])
        stackView.axis = .vertical
        stackView.alignment = .leading
        stackView.translatesAutoresizingMaskIntoConstraints = false
        return stackView
    }()
    
    lazy var stackView: UIStackView = {
        let stackView = UIStackView(arrangedSubviews: [iconView, buttonDetails])
        stackView.alignment = .center
        stackView.spacing = 16.0
        stackView.translatesAutoresizingMaskIntoConstraints = false
        return stackView
    }()
    
    // MARK: - Initializers
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
       
        backgroundColor = Theme.backgroundColor
        isAccessibilityElement = true
        
        let backgroundView = UIView()
        backgroundView.backgroundColor = #colorLiteral(red: 0.8196078431, green: 0.8235294118, blue: 0.8274509804, alpha: 1).withAlphaComponent(0.11)
        selectedBackgroundView = backgroundView
        
        contentView.addSubview(stackView)
        
        setupConstraints()
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: - Layout
    
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
    
    // MARK: - Highlight
    
    /**
     On cell selection or highlight, iOS makes all vies have a clear background
     the below methods address the issue for the avatar view
     */
    
    override func setHighlighted(_ highlighted: Bool, animated: Bool) {
        super.setHighlighted(highlighted, animated: animated)
    }
    
    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }
    
    // MARK: - View Configuration
    
    func configure(with presentable: MenuItemInfo) {
        self.presentable = presentable
        nameLabel.text = presentable.name
        commentLabel.text = presentable.comment
        nameLabel.textColor = Theme.colorOnPrimary
        commentLabel.textColor = Theme.colorSecondaryVariant
        iconView.backgroundColor = Theme.colorSecondary
        addImageToIconView(image: presentable.systemImage)
    }
    
    
    func addImageToIconView(image:UIImage)
    {
        let uiImageView = UIImageView(image: image)
        uiImageView.tintColor = Theme.colorOnSecondary
        
        uiImageView.translatesAutoresizingMaskIntoConstraints = false
        
        // Add the image view as a subview to the container view
        iconView.addSubview(uiImageView)
        
        // Configure Auto Layout constraints to center the image view
        uiImageView.centerXAnchor.constraint(equalTo: iconView.centerXAnchor).isActive = true
        uiImageView.centerYAnchor.constraint(equalTo: iconView.centerYAnchor).isActive = true
     
    }
    
    
    func removeImageFromIconView()
    {
        if let icon = iconView.subviews.first(where: { view in
            view is UIImageView
        }) {
            icon.removeFromSuperview()
        }
    }

    
}
