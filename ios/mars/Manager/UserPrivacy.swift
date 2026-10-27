//
//  UserPrivacy.swift
//  mars
//
//  Created by Mina on 06.06.2023.
//

import Foundation
import UserMessagingPlatform
import AppTrackingTransparency

struct UserPrivacy
{
    private func showTheAttMessage(completion: @escaping (Bool) -> Void)
    {
        ATTrackingManager.requestTrackingAuthorization(completionHandler: { status in
            
            switch status {
            case .notDetermined:
                Swift.debugPrint("ATT: .notDetermined")
            case .restricted, .denied, .authorized:
                Swift.debugPrint("ATT: .restricted, .denied, .authorized")
            default:
                Swift.debugPrint("ATT: Unknown")
            }
            completion(true)
        })
    }
    
    
    func requestConsentInformation(viewController: UIViewController, completion: @escaping (Bool) -> Void)
    {

        let parameters = UMPRequestParameters()
        
        UMPConsentInformation.sharedInstance.requestConsentInfoUpdate(
            with: parameters,
            completionHandler: { [self] error in
                // The consent information has updated.
                if error != nil {
                    // Handle the error.
                    //Swift.debugPrint(error ?? "Something went wrong with error message when requesting consent.")
                    showTheAttMessage(completion: completion)
                } else {
                    //Swift.debugPrint("The consent information state was updated.")
                    // The consent information state was updated.
                    // You are now ready to see if a form is available.
                    let formStatus = UMPConsentInformation.sharedInstance.formStatus
                    if formStatus == UMPFormStatus.available {
                        //Swift.debugPrint("Load form.")
                        loadForm(viewController:viewController, completion: completion)
                    }
                    else {
                        //Swift.debugPrint("Showing the message.")
                        showTheAttMessage(completion: completion)
                    }
                }
            })
    }

    
    private func loadForm(viewController:UIViewController, completion: @escaping (Bool) -> Void) {
        UMPConsentForm.load(completionHandler: { form, loadError in
            if loadError != nil {
                //Swift.debugPrint(loadError ?? "Load form error.")
                self.showTheAttMessage(completion: completion)
                // Handle the error.
            } else {
                // Present the form. You can also hold on to the reference to present
                // later.
                //Swift.debugPrint(loadError ?? "About to present form. Consent status: \(UMPConsentInformation.sharedInstance.consentStatus)")
                if UMPConsentInformation.sharedInstance.consentStatus == UMPConsentStatus.required {
                    //Swift.debugPrint(loadError ?? "UMPConsentStatus.required, present form")
                    form?.present(
                        from: viewController,
                        completionHandler: { dismissError in
                            if UMPConsentInformation.sharedInstance.consentStatus == UMPConsentStatus.obtained {
                                //Swift.debugPrint("Consent obtained")
                                self.showTheAttMessage(completion: completion)
                            }
                            // Handle dismissal by reloading form.
                            self.loadForm(viewController:viewController, completion: completion)
                        })
                } else {
                    // Keep the form available for changes to user consent.
                    //Swift.debugPrint("We obtained consent, show att message.")
                    showTheAttMessage(completion: completion)
                }
            }
        })
    }
}
