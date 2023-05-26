//
//  AppDelegate.swift
//  test
//
//  Created by ksvslk on 16.04.2023.
//

import UIKit
import GameKit
import GoogleMobileAds

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    var window: UIWindow?
    var purchaseManager: PurchaseManager!
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        print("application")
        print("Application directory: \(NSHomeDirectory())")
        GADMobileAds.sharedInstance().start(completionHandler: nil)
        
        // Initialize the PurchaseManager
        purchaseManager = PurchaseManager()
        
        let userDefaults = UserDefaults.standard
        userDefaults.register(
            defaults: [
                C.SOUND_ON: true,
                C.VIBRATION_ON: true
            ]
        )
        
        logInToGameCenter()
        Database.shared.initializeDatabaseWithGameLevels()
        
        return true
    }
    
    
    private func logInToGameCenter()
    {
        GKLocalPlayer.local.authenticateHandler = { viewController, error in
            
            if let _ = viewController {
                // Present the view controller so the player can sign in.
                return
            }
            
            if error != nil {
                // Player could not be authenticated.
                // Disable Game Center in the game.
                return
            }
            
            // Player was successfully authenticated.
        }
    }
    
    
    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
        print("applicationWillResignActive")
    }
    
    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        print("applicationDidEnterBackground")
    }
    
    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
        print("applicationWillEnterForeground")
    }
    
    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
        print("applicationDidBecomeActive")
    }
    
    
}

