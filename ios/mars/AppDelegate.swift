//
//  AppDelegate.swift
//  test
//
//  Created by ksvslk on 16.04.2023.
//

import UIKit
import GameKit
import Toast
import FirebaseCore
import FirebaseAuth
import FirebaseAppCheck


@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    var window: UIWindow?
    var purchaseManager: PurchaseManager!
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        //Swift.debugPrint("Application directory: \(NSHomeDirectory())")
        let providerFactory = MyAppCheckProviderFactory()
        AppCheck.setAppCheckProviderFactory(providerFactory)
        
        FirebaseApp.configure()
        Auth.auth().signInAnonymously { authResult, error in
            guard let user = authResult?.user else { return }
        }
        purchaseManager = PurchaseManager()
        registerUserDefaults()
        setToastStyle()
        Database.shared.initializeDatabaseWithGameLevels()
        Database.shared.initializeLevelPacks()
        return true
    }
    
    
    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
        //Swift.debugPrint("applicationWillResignActive")
    }
    
    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        //Swift.debugPrint("applicationDidEnterBackground")
    }
    
    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
        //Swift.debugPrint("applicationWillEnterForeground")
    }
    
    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
        //Swift.debugPrint("applicationDidBecomeActive")
    }
    
    
    private func registerUserDefaults()
    {
        let userDefaults = UserDefaults.standard
        userDefaults.register(
            defaults: [
                C.SOUND_ON: true,
                C.VIBRATION_ON: true,
                C.COLOR_THEME: ThemeName.purple.rawValue,
                C.ADS_SHOWN: [0],
                C.SNOW: true
            ]
        )
    }
    
    
    private func setToastStyle()
    {
        var style = ToastStyle()
        style.backgroundColor = Theme.shared.colors.inverseSurface
        style.messageColor = Theme.shared.colors.inverseOnSurface
        style.activityBackgroundColor = Theme.shared.colors.inverseSurface
        style.activityIndicatorColor = Theme.shared.colors.inverseOnSurface
        style.messageFont = UIFont(name: Theme.shared.robotoMedium, size: 15)!
        ToastManager.shared.style = style
    }
    
  
}

