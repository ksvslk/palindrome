//
//  PromoDialogController.swift
//  mars
//
//  Created by Mina on 28.11.2023.
//

import Foundation
import UIKit

class PromoDialogController : UIViewController {

    let imageView = UIImageView()
    let titleLabel = UILabel()
    let subtitleLabel = UILabel()
    let stackView = UIStackView()
    let noThanksButton = UIButton()
    var linkURL:URL?
    var adNumber:Int = -1
    
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

        // Set up the subtitle label
        subtitleLabel.textAlignment = .center
        subtitleLabel.font = UIFont(name: Theme.shared.yantraMedium, size: 18)
        subtitleLabel.textColor = Theme.shared.colors.onSurfaceVariant
        
        // Set up do not ask again
        noThanksButton.setTitle("Yup, seen.", for: .normal)
     
        noThanksButton.titleLabel?.font =  UIFont(name: Theme.shared.yantraMedium, size: 14)
        noThanksButton.setTitleColor(Theme.shared.colors.secondary, for: .normal)

        noThanksButton.addTarget(self, action: #selector(noThanksTapped), for: .touchDown)
        
        // Set up the stack view
        stackView.axis = .vertical
        stackView.alignment = .center
        stackView.distribution = .equalSpacing
        stackView.spacing = 10

        // Add image view and labels to the stack view
        stackView.addArrangedSubview(imageView)
        stackView.addArrangedSubview(titleLabel)
        stackView.addArrangedSubview(subtitleLabel)
        stackView.addArrangedSubview(noThanksButton)

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
    
    @objc func noThanksTapped(sender: UIButton) {
        if (adNumber != -1)
        {
            var alreadySeenAds = UserDefaults.standard.array(forKey: C.ADS_SHOWN) as? [Int]
            alreadySeenAds?.append(adNumber)
            if let adNumbers = alreadySeenAds {
                let uniqueAdNumbers = Array(Set(adNumbers))
                UserDefaults.standard.set(uniqueAdNumbers, forKey: C.ADS_SHOWN)
            }
        }
        dismiss(animated: true)
    }

    func updateContent(with imageURL: URL, title: String, subtitle: String, link:String, adNumber:Int) {
        let image = UIImage(contentsOfFile: imageURL.path)
        self.linkURL = URL(string:link)
        self.imageView.image = image
        self.titleLabel.text = title
        self.subtitleLabel.text = subtitle
        self.adNumber = adNumber
    }
    
    @objc func imageTapped(tapGestureRecognizer: UITapGestureRecognizer) {
        if let url = linkURL, UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
        } else {
            print("Invalid URL or URL not set")
        }
    }
}
