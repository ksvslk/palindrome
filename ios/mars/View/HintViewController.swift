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

    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Customize the label
        label.text = ""
        label.textAlignment = .center
        label.font = UIFont(name: Theme.letterFont, size: 24)
        label.textColor = Theme.colorOnBackground

        // Add the label to the view controller's view
        view.addSubview(label)
        
        // Set label's layout constraints (e.g., centered in the view)
        label.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
        
        // Set the background color of the view controller's view
        view.backgroundColor = Theme.backgroundColor
  
    }
    

}
