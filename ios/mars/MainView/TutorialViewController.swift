//
//  TutorialViewController.swift
//  mars
//
//  Created by Mina on 01.06.2023.
//

import Foundation
import UIKit

class TutorialViewController: UIViewController {
    
    let mainStackView:UIStackView = UIStackView()
    let fakeGameView:UIStackView = UIStackView()
    let lettersStackView:UIStackView = UIStackView()
    let slotsStackView:UIStackView = UIStackView()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Create the main stack view
        mainStackView.axis = .vertical
        mainStackView.spacing = 5
        mainStackView.alignment = .fill
        mainStackView.distribution = .equalSpacing

        
        let leftRightArrow = UIImageView()
        let symbolName = "arrow.right.circle"
        let symbolConfig = UIImage.SymbolConfiguration(scale: .large)

        if let symbolImage = UIImage(systemName: symbolName, withConfiguration: symbolConfig) {
            let coloredImage = symbolImage.withTintColor(Theme.colorOnSecondary, renderingMode: .alwaysOriginal)
            let targetSize = CGSize(width: 100, height: 100)
            let renderer = UIGraphicsImageRenderer(size: targetSize)
            let scaledImage = renderer.image { _ in
                coloredImage.draw(in: CGRect(origin: .zero, size: targetSize))
            }
            
            leftRightArrow.image = scaledImage
        }
        leftRightArrow.contentMode = .scaleAspectFit
        
        
        // Create the fake stack view - VERTICAL
        fakeGameView.axis = .vertical
        fakeGameView.spacing = 3
        fakeGameView.alignment = .center
        fakeGameView.distribution = .equalSpacing
        
        // Create the letters stack view - HORIZONTAL
        lettersStackView.axis = .horizontal
        lettersStackView.spacing = 2
        lettersStackView.alignment = .fill
        lettersStackView.distribution = .equalSpacing
        lettersStackView.translatesAutoresizingMaskIntoConstraints = false
        lettersStackView.layer.zPosition = 1
        
        // Create the slots stack view - HORIZONTAL
        slotsStackView.axis = .horizontal
        slotsStackView.spacing = 2
        slotsStackView.alignment = .fill
        slotsStackView.distribution = .equalSpacing
        slotsStackView.translatesAutoresizingMaskIntoConstraints = false
   
        
        for char in "HHA"
        {
            let letter = getLetter(char: String(char))
            lettersStackView.addArrangedSubview(letter)
            slotsStackView.addArrangedSubview(getSlot())
        }
        
        fakeGameView.addArrangedSubview(lettersStackView)
        fakeGameView.addArrangedSubview(slotsStackView)
        
        let howToPlay = getTextLabel(text: "Drag and drop letters so the..")
        let sameWord = getTextLabel(text: "..word will read same from either side!")
        
        
        mainStackView.addArrangedSubview(howToPlay)
        mainStackView.addArrangedSubview(fakeGameView)
        mainStackView.addArrangedSubview(sameWord)
        mainStackView.addArrangedSubview(leftRightArrow)
      
        
        // Add the label to the view controller's view
        view.addSubview(mainStackView)
        
        // Set label's layout constraints (e.g., centered in the view)
        mainStackView.translatesAutoresizingMaskIntoConstraints = false
        
        let verticalValue:CGFloat = UIDevice.current.userInterfaceIdiom == .pad ? 175 : 75
        let horizontalValue:CGFloat = UIDevice.current.userInterfaceIdiom == .pad ? 25 : 16
        NSLayoutConstraint.activate([
            mainStackView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: verticalValue),
            mainStackView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: horizontalValue),
            mainStackView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -horizontalValue),
            mainStackView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -verticalValue),
        ])
        
        // Set the background color of the view controller's view
        view.backgroundColor = Theme.backgroundColor
        
        // Add tap gesture recognizer to the view
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        view.addGestureRecognizer(tapGesture)
    }
    
    @objc private func handleTap() {
         dismiss(animated: true)
     }
    
   
    private func getLetter(char:String) -> UIView
    {
        let containerView = UIView()
        containerView.translatesAutoresizingMaskIntoConstraints = false
        containerView.widthAnchor.constraint(equalTo: containerView.heightAnchor).isActive = true // Apply square aspect ratio constraint
        
        let label = UILabel()
        label.text = char
        label.textColor = Theme.colorOnPrimary
        label.backgroundColor = Theme.colorPrimary
        label.font = UIFont(name: Theme.yantraBold, size: 35)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        label.layer.borderWidth = 0.25
        label.layer.borderColor = Theme.colorOnSecondary.cgColor
        
        containerView.addSubview(label)
        NSLayoutConstraint.activate([
            label.topAnchor.constraint(equalTo: containerView.topAnchor),
            label.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            label.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            label.bottomAnchor.constraint(equalTo: containerView.bottomAnchor)
        ])
        
        return containerView
    }
    
    
    private func getSlot() -> UIView
    {
        let containerView = UIView()
        containerView.translatesAutoresizingMaskIntoConstraints = false
        containerView.widthAnchor.constraint(equalTo: containerView.heightAnchor).isActive = true // Apply square aspect ratio constraint
        
        let label = UILabel()
        label.text = " "
        label.backgroundColor = Theme.colorSecondaryVariant
        label.font = UIFont(name: Theme.yantraBold, size: 35)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        
        containerView.addSubview(label)
        NSLayoutConstraint.activate([
            label.topAnchor.constraint(equalTo: containerView.topAnchor),
            label.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            label.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            label.bottomAnchor.constraint(equalTo: containerView.bottomAnchor)
        ])
        
        return containerView
    }
    
    
    private func getTextLabel(text:String) -> UILabel
    {
        let label = UILabel()
        label.text = text
        label.textAlignment = .center
        label.font = UIFont(name: Theme.yantraBold, size: 24)
        label.textColor = Theme.colorOnBackground
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        label.lineBreakMode = .byTruncatingTail
        label.numberOfLines = 0
        return label
    }
    
    
    override func viewDidAppear(_ animated: Bool) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.animateLetters()
        }
    }
    
    
    private func animateLetters() {
          // Iterate over the arranged subviews of the lettersStackView
        for (index, letterView) in lettersStackView.arrangedSubviews.enumerated() {
         
              // Get the corresponding slot view at the same index from the slotsStackView
              var slotView = slotsStackView.arrangedSubviews[index]
              
              if (index == 0) {
                  slotView = slotsStackView.arrangedSubviews[2]
                  letterView.layer.zPosition = 2
              }
              else if (index == 1) {
                  slotView = slotsStackView.arrangedSubviews[0]
                  letterView.layer.zPosition = 0
              }
              else if (index == 2)
              {
                  letterView.layer.zPosition = 2
                  slotView = slotsStackView.arrangedSubviews[1]
              }

              // Calculate the center position of the slot
             let slotCenter = slotsStackView.convert(slotView.center, to: lettersStackView.coordinateSpace)
              
              // Animate the letter view to the slot position
              UIView.animate(withDuration: 3, delay: TimeInterval(index) * 1, options: .curveEaseOut, animations: {
                  letterView.center = CGPoint(x: slotCenter.x, y: slotCenter.y)
              }, completion: nil)
          
          }
      }
    
    
}
