//
//  AdManager.swift
//  mars
//
//  Created by Mina on 18.05.2023.
//

import Foundation
import GoogleMobileAds


class AdManager: NSObject, GADFullScreenContentDelegate {
    
    private var rewardedAd: GADRewardedAd?
    var rewardToBeGiven:Reward = Reward.none
    
 
    override init() {
        //self.adAvailabilityChange = adChange
        super.init()
        self.loadRewardedAd()
    }
    
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func loadRewardedAd() {
        let request = GADRequest()
        GADRewardedAd.load(withAdUnitID: "ca-app-pub-3940256099942544/1712485313", request: request, completionHandler: { [self] ad, error in
            if let error = error {
                print("Failed to load rewarded ad with error: \(error.localizedDescription)")
                return
            }
            rewardedAd = ad
            //adAvailabilityChange()
            print("Rewarded ad loaded.")
        })
    }
    
    
    func show(viewController: UIViewController, rewardType:Reward) {
        if let ad = rewardedAd {
            ad.present(fromRootViewController: viewController) {
                let reward = ad.adReward
                print("Reward received with currency \(reward.amount), amount \(reward.amount.doubleValue)")
                // TODO: Reward the user.
                self.rewardToBeGiven = rewardType
                self.loadRewardedAd()
            }
        } else {
            print("Ad wasn't ready")
        }
    }
    
    
    /// Tells the delegate that the ad failed to present full screen content.
    func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        print("Ad did fail to present full screen content.")
    }
    
    
    /// Tells the delegate that the ad will present full screen content.
    func adWillPresentFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        print("Ad will present full screen content.")
    }
    
    
    /// Tells the delegate that the ad dismissed full screen content.
    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        print("Ad did dismiss full screen content.")
    }

    
}
