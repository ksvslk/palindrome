//
//  PurchaseManager.swift
//  mars
//
//  Created by Mina on 24.05.2023.
//

import Foundation
import StoreKit
import TPInAppReceipt

struct ProductInfo {
    
    let product: SKProduct
    let isPurchased: Bool
    
    var localizedPrice: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.locale = product.priceLocale
        let formattedPrice = formatter.string(from: product.price) ?? ""
        return "\(formattedPrice)"
    }
    
    init(product: SKProduct, isPurchased: Bool) {
        self.product = product
        self.isPurchased = isPurchased
    }
}


class PurchaseManager: NSObject, SKProductsRequestDelegate, SKPaymentTransactionObserver, SKRequestDelegate {
    
    private var productsRequest: SKProductsRequest?
    private var productIdentifiers: Set<String> = [
        C.PURCHASE_LETTERS_ID,
        C.PURCHASE_HINTS_ID,
        C.PURCHASE_NO_ADS_ID,
        C.PURCHASE_ALL_ID
    ]
    private var completionHandlers: [SKRequest: (Bool) -> Void] = [:]
    private var purchasedProductIdentifiers: Set<String> = []
    private var availableProducts: [SKProduct] = []
    weak var delegate: ProductsUpdateDelegate?
    weak var processDelegate: TransactionProcessDelegate?
    weak var messageDelegate: MessageDelegate?
    var loading:Bool = false
    
    
    override init() {
        Swift.debugPrint("init PurchaseManager")
        super.init()
        SKPaymentQueue.default().add(self)
 
        requestProducts()
        
        if let receipt = try? InAppReceipt.localReceipt() {
            // reset purchases
            self.productIdentifiers.forEach { pid in
                self.updatePurchaseStatus(pid, isPurchased: false, updateUi: false)
            }
            // info based on fresh receipt
            receipt.purchases.forEach { iap in
                Swift.debugPrint("Receipt purchase: \(iap.productIdentifier)")
                self.updatePurchaseStatus(iap.productIdentifier, isPurchased: true)
            }
        }
        
        // save info
        self.loadPurchaseStatusFromUserDefaults()
    }
    
    
    
    func sendLatestInfo()
    {
        delegate?.didUpdateProducts(withProducts: getProducts())
    }
    
    
    // Request products with identifiers
    func requestProducts() {
        Swift.debugPrint("requestProducts")
        productsRequest = SKProductsRequest(productIdentifiers: productIdentifiers)
        productsRequest?.delegate = self
        productsRequest?.start()
        processDelegate?.didStartProcessing()
        loading = true
    }
    
    
    // Start the purchase flow for a product
    func purchaseProduct(_ productId: String) {
        Swift.debugPrint("purchaseProduct")
        guard let product = availableProducts.first(where: { $0.productIdentifier == productId }) else { return }
        let payment = SKPayment(product: product)
        SKPaymentQueue.default().add(payment)
        processDelegate?.didStartProcessing()
        loading = true
    }
    
    
    // Restore purchases
    func restorePurchases() {
        Swift.debugPrint("restorePurchases")
        SKPaymentQueue.default().restoreCompletedTransactions()
        processDelegate?.didStartProcessing()
        loading = true
    }
    
    
    func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
        availableProducts = response.products
        delegate?.didUpdateProducts(withProducts: getProducts())
        Swift.debugPrint("productsRequest completed with \(availableProducts)")
    }
    
    
    func isNoAdsPurchasedInAnyWay() -> Bool
    {
        return purchasedProductIdentifiers.contains { productId in
            productId == C.PURCHASE_NO_ADS_ID || productId == C.PURCHASE_ALL_ID
        }
    }
    
    
    func isHintsPurchasedInAnyWay() -> Bool
    {
        return purchasedProductIdentifiers.contains { productId in
            productId == C.PURCHASE_HINTS_ID || productId == C.PURCHASE_ALL_ID
        }
    }
    
    
    func isLettersPurchasedInAnyWay() -> Bool
    {
        return purchasedProductIdentifiers.contains { productId in
            productId == C.PURCHASE_LETTERS_ID || productId == C.PURCHASE_ALL_ID
        }
    }
    
    
    func getProducts() -> [String:ProductInfo] {
        Swift.debugPrint("getProductInfo")
        var productInfoMap: [String:ProductInfo] = [:]
        let defaults = UserDefaults.standard
        let purchasedProducts = defaults.array(forKey: C.PURCHASED_PRODUCTS_KEY) as? [String] ?? []
        
        for productIdentifier in productIdentifiers {
            guard let product = availableProducts.first(where: { $0.productIdentifier == productIdentifier }) else {
                continue
            }
            
            let isPurchased = purchasedProducts.contains(productIdentifier)
            let productInfo = ProductInfo(product: product, isPurchased: isPurchased)
            productInfoMap[product.productIdentifier] = productInfo
        }
        
        return productInfoMap
    }
    
    
    func paymentQueue(_ queue: SKPaymentQueue, didRevokeEntitlementsForProductIdentifiers productIdentifiers: [String]) {
        for productId in productIdentifiers {
            updatePurchaseStatus(productId, isPurchased: false)
        }
    }
    
    
    func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
        
        // Note: new receipt is available here
        
        for transaction in transactions {
            switch transaction.transactionState {
            case .purchasing:
                // Handle the "Purchasing" state
                // For example, show a loading indicator
                processDelegate?.didStartProcessing()
                loading = true
                Swift.debugPrint("paymentQueue state: purchasing")
                break
            case .purchased:
                Swift.debugPrint("paymentQueue state: purchased")
                // Handle the "Purchased" state
                // For example, provide the purchased content
                verifyReceipt(productId: transaction.payment.productIdentifier) { [weak self] success in
                    if success {
                        // Receipt verification successful
                        // Provide access to the purchased content
                        self?.updatePurchaseStatus(transaction.payment.productIdentifier, isPurchased: true)
                        self?.processDelegate?.didFinishProcessing(result: true)
                        self?.loading = false
                    } else {
                        self?.processDelegate?.didFinishProcessing(result: false)
                        self?.loading = false
                        // Receipt verification failed
                        // Handle the failure case
                    }
                    SKPaymentQueue.default().finishTransaction(transaction)
                }
            case .restored:
                Swift.debugPrint("paymentQueue state: restored")
                // Handle the "Restored" state
                // For example, provide access to the previously purchased content
                verifyReceipt(productId: transaction.payment.productIdentifier) { [weak self] success in
                    if success {
                        // Receipt verification successful
                        // Provide access to the restored content
                        self?.updatePurchaseStatus(transaction.payment.productIdentifier, isPurchased: true)
                        self?.processDelegate?.didFinishProcessing(result: true)
                        self?.loading = false
                    } else {
                        self?.processDelegate?.didFinishProcessing(result: false)
                        self?.loading = false
                        // Receipt verification failed
                        // Handle the failure case
                    }
                    SKPaymentQueue.default().finishTransaction(transaction)
                }
            case .failed:
                Swift.debugPrint("paymentQueue state: failed")
                // Handle the "Failed" state
                // For example, display an error message
                processDelegate?.didFinishProcessing(result: true)
                loading = false
                if (transaction.error as? SKError)?.code != .paymentCancelled {
                    DispatchQueue.main.async {
                        self.messageDelegate?.didSendNewMessage(withMessage: "purchase_failed_not_cancelled"~)
                    }
                }
                SKPaymentQueue.default().finishTransaction(transaction)
            case .deferred:
                Swift.debugPrint("paymentQueue state: deferred")
                processDelegate?.didFinishProcessing(result: true)
                loading = false
                DispatchQueue.main.async {
                    self.messageDelegate?.didSendNewMessage(withMessage: "purchase_deferred"~)
                }
                break
            @unknown default:
                break
            }
        }
    }
 
    
    private func verifyReceipt(productId:String, completion: @escaping (Bool) -> Void) {
        if let receipt = try? InAppReceipt.localReceipt() {
            if receipt.containsPurchase(ofProductIdentifier: productId){
                completion(true)
            }
        } else {
            InAppReceipt.refresh { error in
                if let err = error
                {
                    Swift.debugPrint(err)
                    completion(false)
                } else {
                    if let receipt = try? InAppReceipt.localReceipt() {
                        if receipt.containsPurchase(ofProductIdentifier: productId){
                            completion(true)
                        }
                    }
                }
            }
            Swift.debugPrint("Receipt not found")
        }
    }
    
    
    func paymentQueue(_ queue: SKPaymentQueue, removedTransactions transactions: [SKPaymentTransaction]) {
        Swift.debugPrint("Removed transactions: \(transactions.debugDescription)")
    }
    
    
    func paymentQueueRestoreCompletedTransactionsFinished(_ queue: SKPaymentQueue) {
        Swift.debugPrint("paymentQueueRestoreCompletedTransactionsFinished")
        processDelegate?.didFinishProcessing(result: true)
        loading = false
        messageDelegate?.didSendNewMessage(withMessage: "restore_completed"~)
    }
    
    
    func paymentQueue(_ queue: SKPaymentQueue, restoreCompletedTransactionsFailedWithError error: Error) {
        Swift.debugPrint("Error restoring completed transactions \(error)")
        processDelegate?.didFinishProcessing(result: false)
        loading = false
        if let skError = error as? SKError {
            if skError.code != .paymentCancelled {
                messageDelegate?.didSendNewMessage(withMessage: "restore_failed"~)
            }
        }
    }
    
    
    // MARK: Request completed successfully
    func requestDidFinish(_ request: SKRequest) {
        Swift.debugPrint("requestDidFinish: \(request.description)")
        if let completion = completionHandlers[request] {
            completion(true)
            completionHandlers.removeValue(forKey: request)
        }
        processDelegate?.didFinishProcessing(result: false)
        loading = false
    }
    
    
    // MARK: Request failed
    func request(_ request: SKRequest, didFailWithError error: Error) {
        Swift.debugPrint("request failed: \(error.localizedDescription)")
        if let completion = completionHandlers[request] {
            completion(false)
            completionHandlers.removeValue(forKey: request)
        }
        processDelegate?.didFinishProcessing(result: false)
        loading = false
    }
    
    
    private func loadPurchaseStatusFromUserDefaults() {
        Swift.debugPrint("loadPurchaseStatusFromUserDefaults")
        let defaults = UserDefaults.standard
        if let savedPurchasedProductIdentifiers = defaults.object(forKey: C.PURCHASED_PRODUCTS_KEY) as? [String] {
            purchasedProductIdentifiers = Set(savedPurchasedProductIdentifiers)
        }
    }
    
    
    private func savePurchaseStatusToUserDefaults() {
        Swift.debugPrint("savePurchaseStatusToUserDefaults")
        let defaults = UserDefaults.standard
        let purchasedProducts = Array(purchasedProductIdentifiers)
        defaults.set(purchasedProducts, forKey: C.PURCHASED_PRODUCTS_KEY)
    }
    
    
    private func updatePurchaseStatus(_ productIdentifier: String, isPurchased: Bool, updateUi:Bool = true) {
        Swift.debugPrint("updatePurchaseStatus")
        if isPurchased {
            purchasedProductIdentifiers.insert(productIdentifier)
        } else {
            purchasedProductIdentifiers.remove(productIdentifier)
        }
        savePurchaseStatusToUserDefaults()
        if (updateUi) { delegate?.didUpdateProducts(withProducts: getProducts()) }
    }
    
}
