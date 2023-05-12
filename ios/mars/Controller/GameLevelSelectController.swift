//
//  GameLevelSelectController.swift
//  mars
//
//  Created by Mina on 09.05.2023.
//

import UIKit

class GameLevelSelectController: UIViewController {
    var data:[Level.Progress] = Database.shared.getAllGameLevels()
    
    // a property to hold the collectionView
    var collectionView: UICollectionView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.collectionView.dataSource = self
        self.collectionView.delegate = self
        self.collectionView.register(LevelSelectCell.self, forCellWithReuseIdentifier: "subclassedcell")
        self.collectionView.alwaysBounceVertical = true
        self.collectionView.isPrefetchingEnabled = true
        // Add blur effect
        self.collectionView.backgroundColor = UIColor.clear
        let blurEffect = UIBlurEffect(style: .prominent)
        let blurEffectView = UIVisualEffectView(effect: blurEffect)
        blurEffectView.frame = self.collectionView.bounds
        self.collectionView.backgroundView = blurEffectView
    }
    
    override func loadView() {
        // create a layout to be used
        let layout: UICollectionViewFlowLayout = UICollectionViewFlowLayout()
        // make sure that there is a slightly larger gap at the top of each row
        layout.sectionInset = UIEdgeInsets(top: 20, left: 10, bottom: 10, right: 10)
        // set a standard item size
        layout.itemSize = CGSize(width: 80, height: 80)
        // the layout scrolls horizontally
        layout.scrollDirection = .vertical
        // set the frame and layout
        collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        // set the view to be this UICollectionView
        self.view = collectionView
    }

}

extension GameLevelSelectController: UICollectionViewDelegate {
    // if the user clicks on a cell, display a message
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        
        print("Collection view at row \(collectionView.tag) selected index path \(indexPath)")
    }
}

extension GameLevelSelectController: UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        data.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        if let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "subclassedcell", for: indexPath) as? LevelSelectCell {
            let levelId = indexPath.item + 1
            let progress: Level.Progress = data[indexPath.item]
            cell.label.text = String(levelId)
            switch progress {
            case .in_progress:
                cell.isCellEnabled = true
            case .completed:
                cell.isCellEnabled = true
            case .locked:
                cell.isCellEnabled = false
            }
            return cell
        }
        fatalError("Unable to dequeue subclassed cell")
    }
}
