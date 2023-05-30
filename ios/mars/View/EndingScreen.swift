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
        imageView.image = UIImage(named: "AppIcon")
        imageView.contentMode = .scaleAspectFit
        
        // Customize the label
        label.text = "end_screen_text"~
        label.textAlignment = .left
        label.font = UIFont(name: Theme.yantraMedium, size: 18)
        label.textColor = Theme.colorOnBackground
        label.numberOfLines = 0
        
        // Create a vertical stack view
        let stackView = UIStackView(arrangedSubviews: [imageView, label])
        stackView.distribution = .fill
        stackView.axis = .vertical
        stackView.spacing = 16
        
        // Add the stack view to the view controller's view
        view.addSubview(stackView)
        
        // Set the background color of the view controller's view
        view.backgroundColor = Theme.backgroundColor
        
        // Add layout constraints for the stack view
        stackView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            stackView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 32),
            stackView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -32),
            stackView.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
        
        // Set aspect ratio constraint for the image view
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.heightAnchor.constraint(equalTo: imageView.widthAnchor, multiplier: 1.0).isActive = true
    }
   
    
    override func viewDidAppear(_ animated: Bool) {
        confettiBurst(duration: 7)
        paintWords()
    }
    
    
    private func paintWords()
    {
        let originalString = label.text!
        let wordsToPaint = ["WOW", "LEVEL", "KAYAK", "RACECAR", "RADAR"]

        let attributedString = NSMutableAttributedString(string: originalString)

        for word in wordsToPaint {
            // Find the range of each word to underline
            if let range = originalString.range(of: word) {
                attributedString.addAttribute(.foregroundColor, value: Theme.levelInProgressColor, range: NSRange(range, in: originalString))
            }
        }

        // Set the attributed string to a label or text view
        label.attributedText = attributedString
    }
    
}
