//
//  HintView.swift
//  mars
//
//  Created by Mina on 19.05.2023.
//

import Foundation
import UIKit


class HintViewController: UIViewController {
    // Create a label property
    let label = UILabel()
    let backButton = UIButton()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Customize the label
        label.text = ""
        label.textAlignment = .center
        label.font = UIFont(name: Theme.shared.yantraBold, size: 24)
        label.textColor = Theme.shared.colors.onBackground
        
        // Add the label to the view controller's view
        view.addSubview(label)
        
        // Set label's layout constraints (e.g., centered in the view)
        label.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
        
        // Customize the back button
        backButton.setTitle(" ↩︎ ", for: .normal)
        backButton.setTitleColor(Theme.shared.colors.secondary, for: .normal)
        backButton.titleLabel?.font = UIFont(name: Theme.shared.yantraMedium, size: 24)
        backButton.addTarget(self, action: #selector(backButtonTapped), for: .touchUpInside)
        
        // Add the back button to the view
        view.addSubview(backButton)
        
        // Back button constraints
        backButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            backButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            backButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20) // 20 points from bottom
        ])
        
        // Set the background color of the view controller's view
        view.backgroundColor = Theme.shared.colors.background
    }
    
    @objc func backButtonTapped() {
        dismiss(animated: true, completion: nil)
    }
    
}
