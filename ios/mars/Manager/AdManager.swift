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
    var rewardedInterstitialAd: GADRewardedInterstitialAd?

    var rewardToBeGiven:Reward = Reward.none
    weak var delegate: AdAvailabilityDelegate?
    
    var adAvailable: Bool = false {
        didSet {
            // delegate ad availability change to the hints menu (AdAwareAlertController)
            delegate?.adAvailabilityDidChange(isAvailable: adAvailable)
        }
    }
    
    private let retryIntervalConstant: TimeInterval = 5 // Retry interval in seconds
    private let maxRetryIntervalConstant: TimeInterval = 300 // Maximum retry interval in seconds
    private var currentRetryInterval: TimeInterval = 0
    private var retryTimer: Timer?

    private var currentRewardedInterstitialRetryInterval: TimeInterval = 0
    private var retryRewardedInterstitialTimer: Timer?
    
    override init() {
        super.init()
        self.loadRewardedAd()
        self.loadRewardedInterstitialAd()
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
    
    let rewardedInterstitialAdUnit: String = {
        #if DEBUG
        return C.DEBUG_REWARDED_INTERSTITIAL_AD_UNIT
        #else
        return C.RELEASE_REWARDED_INTERSTITIAL_AD_UNIT
        #endif
    }()
    
    
    func loadRewardedAd() {
        let request = GADRequest()
        GADRewardedAd.load(withAdUnitID: rewardedAdUnit, request: request) { [weak self] ad, error in
            guard let self = self else { return }
            
            if let error = error {
                Swift.debugPrint("Failed to load rewarded ad with error: \(error.localizedDescription)")
                
                // Retry loading the ad with exponential backoff until the maximum retry interval is reached
                if self.currentRetryInterval < self.maxRetryIntervalConstant {
                    // Schedule the next retry attempt
                    self.retryTimer = Timer.scheduledTimer(withTimeInterval: self.retryIntervalConstant, repeats: false) { [weak self] _ in
                        self?.loadRewardedAd()
                        // Update the current retry interval
                        self?.currentRetryInterval += self?.retryIntervalConstant ?? 0
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
    
    
    func loadRewardedInterstitialAd() {
        GADRewardedInterstitialAd.load(withAdUnitID: rewardedInterstitialAdUnit, request: GADRequest()) { [weak self] ad, error in
            guard let self = self else { return }
            
            if let error = error {
                Swift.debugPrint("Failed to load rewarded interstitial ad with error: \(error.localizedDescription)")
                
                // Retry loading the ad with exponential backoff until the maximum retry interval is reached
                if self.currentRewardedInterstitialRetryInterval < self.maxRetryIntervalConstant {
                    // Schedule the next retry attempt
                    self.retryRewardedInterstitialTimer = Timer.scheduledTimer(withTimeInterval: self.retryIntervalConstant, repeats: false) { [weak self] _ in
                        self?.loadRewardedInterstitialAd()
                        // Update the current retry interval
                        self?.currentRewardedInterstitialRetryInterval += self?.retryIntervalConstant ?? 0
                    }
                }
                
                return
            }
            
            // Ad loaded successfully, reset the retry interval and cancel any pending retry timer
            self.currentRewardedInterstitialRetryInterval = 0
            self.retryRewardedInterstitialTimer?.invalidate()
            self.rewardedInterstitialAd = ad
            
            Swift.debugPrint("Rewarded interstitial ad loaded.")
        }
    }
    
    
    func showRewardedInterstitialAd(viewController: UIViewController) {
        if let ad = rewardedInterstitialAd {
            ad.present(fromRootViewController: viewController) {
                self.rewardToBeGiven = Reward.continue_playing
                self.rewardedInterstitialAd = nil
                self.loadRewardedInterstitialAd()
            }
        } else {
            Swift.debugPrint("Ad wasn't ready")
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
