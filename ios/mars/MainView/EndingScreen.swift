//
//  EndingScreen.swift
//  mars
//
//  Created by Mina on 30.05.2023.
//

import Foundation
import UIKit

class EndingScreen: UIViewController {
    // Create properties for the image view and label
    let imageView = UIImageView()
    let label = UILabel()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Customize the image view
        imageView.image = UIImage(named: "wrath")
        imageView.contentMode = .scaleAspectFit
        
        // Customize the label
        label.textAlignment = .center
        
        label.font = UIFont(name: Theme.shared.yantraMedium, size: 18)
        label.textColor = Theme.shared.colors.onBackground
        label.numberOfLines = 0

        
        // Create a vertical stack view
        let stackView = UIStackView(arrangedSubviews: [imageView, label])
        stackView.distribution = .fill
        stackView.axis = .vertical
        stackView.spacing = 16
        
        // Add the stack view to the view controller's view
        view.addSubview(stackView)
        
        // Set the background color of the view controller's view
        view.backgroundColor = Theme.shared.colors.background
        
        // Add layout constraints for the stack view
        stackView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            stackView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 64),
            stackView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -64),
            stackView.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
        
        // Set aspect ratio constraint for the image view
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.heightAnchor.constraint(equalTo: imageView.widthAnchor, multiplier: 1.0).isActive = true
        
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        dismiss(animated: true)
    }
   
    
    override func viewDidAppear(_ animated: Bool) {
        confettiBurst(duration: 7)
    }
    

    
}
