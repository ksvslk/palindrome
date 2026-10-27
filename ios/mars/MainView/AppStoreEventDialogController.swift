//
//  AppStoreEventDialogController.swift
//  mars
//
//  Created by Mina on 21.04.2024.
//

import Foundation
import UIKit

class AppStoreEventDialogController : UIViewController {

    let imageView = UIImageView()
    let titleLabel = UILabel()
    let stackView = UIStackView()
    let actionButton = PaddedLabel()
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Set up the image view with rounded corners
        imageView.contentMode = .scaleAspectFit
        imageView.layer.cornerRadius = 10
        imageView.clipsToBounds = true
        let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(imageTapped(tapGestureRecognizer:)))
        imageView.isUserInteractionEnabled = true // Enable user interaction
        imageView.addGestureRecognizer(tapGestureRecognizer)

        // Set up the title label
        titleLabel.textAlignment = .center
        titleLabel.font = UIFont(name: Theme.shared.yantraBold, size: 24)
        titleLabel.textColor = Theme.shared.colors.onSurface
        titleLabel.numberOfLines = 5
        
        // Set up the action button

        actionButton.backgroundColor = Theme.shared.colors.primary
        actionButton.textColor = Theme.shared.colors.onPrimary
        actionButton.layer.cornerRadius = 10 // Set corner radius here
        actionButton.clipsToBounds = true  // Make sure the corners appear rounded
        actionButton.font = UIFont(name: Theme.shared.yantraBold, size: 30)
        let actionButtonTapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(buttonTapped))
        actionButton.isUserInteractionEnabled = true // Enable user interaction
        actionButton.addGestureRecognizer(actionButtonTapGestureRecognizer)


        // Set up the stack view
        stackView.axis = .vertical
        stackView.alignment = .center
        stackView.distribution = .equalSpacing
        stackView.spacing = 10

        // Add image view and labels to the stack view
        stackView.addArrangedSubview(imageView)
        stackView.addArrangedSubview(titleLabel)
        stackView.addArrangedSubview(actionButton)
        
        // Add stack view to the view controller's view
        view.addSubview(stackView)

        // Set layout constraints for stack view
        stackView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            stackView.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            stackView.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            stackView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            stackView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20)
        ])

        // Set constraints for image view size if needed
        imageView.widthAnchor.constraint(equalTo: stackView.widthAnchor).isActive = true
        imageView.heightAnchor.constraint(equalTo: imageView.widthAnchor).isActive = true // Keep aspect ratio 1:1

        // Set the background color of the view
        view.backgroundColor = Theme.shared.colors.surface

        // Example usage (Replace with actual image URL, title, and subtitle)
        //updateContent(with: URL(fileURLWithPath: "path/to/your/image.jpg"), title: "Your Title", subtitle: "Your Subtitle")
    }

    func updateContent(with imageName: String, title: String, buttonText: String) {
        let image = UIImage(named: imageName)
        self.imageView.image = image
        self.actionButton.text = buttonText
        self.titleLabel.text = title
    }
    
    @objc func buttonTapped() {
        self.showTutorial()
    }
    
    @objc func imageTapped(tapGestureRecognizer: UITapGestureRecognizer) {
        self.showTutorial()
    }
    
    private func showTutorial()
    {
        let tutorialShown = UserDefaults.standard.bool(forKey: C.TUTORIAL_SHOWN)
        
        if (!tutorialShown)
        {
            weak var pvc = self.presentingViewController
            self.dismiss(animated: true, completion: {
                let tutorialView = TutorialViewController()
                pvc?.present(tutorialView, animated: true)
                UserDefaults.standard.set(true, forKey: C.TUTORIAL_SHOWN)
            })
        }
        else {
            dismiss(animated: true)
        }
    }
}
