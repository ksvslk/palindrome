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
import FirebaseMessaging
import FirebaseAnalytics
import FirebaseFirestore
import UserNotifications


@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        #if DEBUG
        Swift.debugPrint("sgpush: APNs token received (\(deviceToken.count) bytes)")
        #endif
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        #if DEBUG
        Swift.debugPrint("sgpush: APNs registration FAILED: \(error)")
        #endif
    }

    /// Stagenator: register this install's push token keyed by the auth uid so
    /// the agent can deliver personal rewards; subscribe to the broadcast topic.
    func registerStagenatorPush(uid: String) {
        Messaging.messaging().subscribe(toTopic: "new_levels")
        Messaging.messaging().subscribe(toTopic: "player_levels")
        PlayerLevelsSync.sync()
        Messaging.messaging().token { token, error in
            #if DEBUG
            if let error = error { Swift.debugPrint("sgpush: FCM token error: \(error)") }
            if let token = token { Swift.debugPrint("sgpush: FCM token: \(token)") }
            #endif
            guard let token = token else { return }
            let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
            Firestore.firestore().collection("fcmTokensIos").document(uid).setData([
                "token": token,
                "updatedAt": FieldValue.serverTimestamp(),
                "platform": "ios",
                "appVersion": version ?? "",
            ], merge: true)
        }
    }
    
    var window: UIWindow?
    var purchaseManager: PurchaseManager!
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        //Swift.debugPrint("Application directory: \(NSHomeDirectory())")
        let providerFactory = MyAppCheckProviderFactory()
        AppCheck.setAppCheckProviderFactory(providerFactory)
        
        FirebaseApp.configure()
        Auth.auth().signInAnonymously { [weak self] authResult, error in
            guard let user = authResult?.user else { return }
            // Stagenator tier-1 identity: pseudonymous analytics id + push
            // registration keyed by uid (same pattern as the other games).
            Analytics.setUserID(user.uid)
            self?.registerStagenatorPush(uid: user.uid)
        }
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
        application.registerForRemoteNotifications()
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
                C.SNOW: true,
                C.PARTICIPANT_OF_422_EVENT: false
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



// MARK: - Stagenator push handling

extension AppDelegate: MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        // Token rotated: re-register under the current uid.
        if let uid = Auth.auth().currentUser?.uid {
            registerStagenatorPush(uid: uid)
        }
    }
}

extension AppDelegate: UNUserNotificationCenterDelegate {
    /// Show notifications as banners even while the app is in the foreground
    /// (default iOS behavior delivers them silently).
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .list, .sound])
    }

    /// Notification tapped: open a Stagenator claim link if one is attached.
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let data = response.notification.request.content.userInfo
        if let url = data["claimUrl"] as? String,
           url.hasPrefix("https://"),
           let link = URL(string: url) {
            UIApplication.shared.open(link)
        } else if let levelString = data["levelNumber"] as? String,
                  let levelNumber = Int(levelString) {
            // Level announcement: sync player levels first (the announced level
            // may be brand new), then navigate to it.
            PlayerLevelsSync.sync {
                DispatchQueue.main.async {
                    Database.shared.openLevel(absolute: levelNumber)
                }
            }
        }
        completionHandler()
    }
}


// MARK: - Player levels sync (Stagenator)

/// Pulls approved player-created levels from Firestore into the local Core
/// Data store, mirroring the Android app's player-level collection.
enum PlayerLevelsSync {
    static func sync(completion: (() -> Void)? = nil) {
        Firestore.firestore().collection("user_submitted_levels")
            .whereField("isFeatured_v3", isEqualTo: true)
            .whereField("levelId", isGreaterThan: 1080)
            .whereField("levelId", isLessThan: 10000)
            .getDocuments { snapshot, error in
                defer { completion?() }
                #if DEBUG
                Swift.debugPrint("sgsync: fetched \(snapshot?.documents.count ?? -1) player levels, error: \(String(describing: error))")
                #endif
                guard let docs = snapshot?.documents else { return }
                for doc in docs {
                    let data = doc.data()
                    guard let levelId = data["levelId"] as? Int,
                          let solutionToCategory = data["solutionToCategory"] as? [String: Any],
                          let palindrome = solutionToCategory.keys.first else { continue }
                    let hints = solutionToCategory[palindrome] as? [String: Any]
                    let hint = (hints?["hint"] as? String) ?? ""
                    let author = (data["player"] as? String) ?? ""
                    Database.shared.addNewLevel(
                        levelNr: levelId,
                        palindrome: palindrome,
                        solutionsToHints: [palindrome: hint],
                        author: author,
                        progress: Level.Progress.in_progress
                    )
                }
            }
    }
}
