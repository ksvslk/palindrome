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
    weak var delegate: AdAvailabilityDelegate?
    
    var adAvailable: Bool = false {
        didSet {
            // delegate ad availability change to the hints menu (AdAwareAlertController)
            delegate?.adAvailabilityDidChange(isAvailable: adAvailable)
        }
    }
    
    private let retryInterval: TimeInterval = 5 // Retry interval in seconds
    private let maxRetryInterval: TimeInterval = 300 // Maximum retry interval in seconds
    private var currentRetryInterval: TimeInterval = 0
    private var retryTimer: Timer?
    
    override init() {
        super.init()
        self.loadRewardedAd()
    }
    
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    
    func sendLatestInfo()
    {
        delegate?.adAvailabilityDidChange(isAvailable: adAvailable)
    }
    
    
    let rewardedAdUnit: String = {
        #if DEBUG
        return C.DEBUG_REWARDED_AD_UNIT
        #else
        return C.RELEASE_REWARDED_AD_UNIT
        #endif
    }()
    
    
    func loadRewardedAd() {
        let request = GADRequest()
        GADRewardedAd.load(withAdUnitID: rewardedAdUnit, request: request) { [weak self] ad, error in
            guard let self = self else { return }
            
            if let error = error {
                Swift.debugPrint("Failed to load rewarded ad with error: \(error.localizedDescription)")
                
                // Retry loading the ad with exponential backoff until the maximum retry interval is reached
                if self.currentRetryInterval < self.maxRetryInterval {
                    // Schedule the next retry attempt
                    self.retryTimer = Timer.scheduledTimer(withTimeInterval: self.retryInterval, repeats: false) { [weak self] _ in
                        self?.loadRewardedAd()
                        // Update the current retry interval
                        self?.currentRetryInterval += self?.retryInterval ?? 0
                    }
                }
                
                return
            }
            
            // Ad loaded successfully, reset the retry interval and cancel any pending retry timer
            self.currentRetryInterval = 0
            self.retryTimer?.invalidate()
            
            self.rewardedAd = ad
            self.adAvailable = true
            
            Swift.debugPrint("Rewarded ad loaded.")
        }
    }
    
    
    func show(viewController: UIViewController, rewardType:Reward) {
        if let ad = rewardedAd {
            ad.present(fromRootViewController: viewController) {
                self.rewardToBeGiven = rewardType
                self.rewardedAd = nil
                self.adAvailable = false
                self.loadRewardedAd()
            }
        } else {
            Swift.debugPrint("Ad wasn't ready")
        }
    }
    
    
    /// Tells the delegate that the ad failed to present full screen content.
    func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        Swift.debugPrint("Ad did fail to present full screen content.")
    }
    
    
    /// Tells the delegate that the ad will present full screen content.
    func adWillPresentFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        Swift.debugPrint("Ad will present full screen content.")
    }
    
    
    /// Tells the delegate that the ad dismissed full screen content.
    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        Swift.debugPrint("Ad did dismiss full screen content.")
    }
    
    
}
