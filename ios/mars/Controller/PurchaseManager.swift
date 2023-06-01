//
//  PurchaseManager.swift
//  mars
//
//  Created by Mina on 24.05.2023.
//

import Foundation
import StoreKit


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
    var loading:Bool = false
    
    override init() {
        super.init()
        SKPaymentQueue.default().add(self)
        loadPurchaseStatusFromUserDefaults()
        print("init PurchaseManager")
        requestProducts()
    }
    
    
    func sendLatestInfo()
    {
        delegate?.didUpdateProducts(withProducts: getProducts())
    }
    
    
    // Request products with identifiers
    func requestProducts() {
        print("requestProducts")
        productsRequest = SKProductsRequest(productIdentifiers: productIdentifiers)
        productsRequest?.delegate = self
        productsRequest?.start()
        processDelegate?.didStartProcessing()
        loading = true
    }
    
    
    // Start the purchase flow for a product
    func purchaseProduct(_ productId: String) {
        print("purchaseProduct")
        guard let product = availableProducts.first(where: { $0.productIdentifier == productId }) else { return }
        let payment = SKPayment(product: product)
        SKPaymentQueue.default().add(payment)
        processDelegate?.didStartProcessing()
        loading = true
    }
    
    
    // Restore purchases
    func restorePurchases() {
        print("restorePurchases")
        SKPaymentQueue.default().restoreCompletedTransactions()
        processDelegate?.didStartProcessing()
        loading = true
    }
    
    
    func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
        availableProducts = response.products
        delegate?.didUpdateProducts(withProducts: getProducts())
        print("productsRequest completed with \(availableProducts)")
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
        print("getProductInfo")
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
        for transaction in transactions {
            switch transaction.transactionState {
            case .purchasing:
                // Handle the "Purchasing" state
                // For example, show a loading indicator
                processDelegate?.didStartProcessing()
                loading = true
                print("paymentQueue state: purchasing")
                break
            case .purchased:
                print("paymentQueue state: purchased")
                // Handle the "Purchased" state
                // For example, provide the purchased content
                verifyReceipt { [weak self] success in
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
                }
                SKPaymentQueue.default().finishTransaction(transaction)
            case .restored:
                print("paymentQueue state: restored")
                // Handle the "Restored" state
                // For example, provide access to the previously purchased content
                verifyReceipt { [weak self] success in
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
                }
                SKPaymentQueue.default().finishTransaction(transaction)
            case .failed:
                print("paymentQueue state: failed")
                // Handle the "Failed" state
                // For example, display an error message
                processDelegate?.didFinishProcessing(result: true)
                loading = false
                SKPaymentQueue.default().finishTransaction(transaction)
            case .deferred:
                print("paymentQueue state: deferred")
                processDelegate?.didFinishProcessing(result: true)
                loading = false
                break
            @unknown default:
                break
            }
        }
    }
    
    
    private func verifyReceipt(completion: @escaping (Bool) -> Void) {
        print("verifyReceipt")
        
        /*
        let refreshRequest = SKReceiptRefreshRequest()
        refreshRequest.delegate = self
        refreshRequest.start()
        */
        
        guard let appStoreReceiptURL = Bundle.main.appStoreReceiptURL else {
            completion(false)
            return
        }
        
        do {
            let receiptData = try Data(contentsOf: appStoreReceiptURL)
            let receiptString = receiptData.base64EncodedString(options: [])
            let receiptDictionary = ["receipt-data": receiptString,
                                     "password": C.SS] // Replace with your shared secret
            
            let requestData = try JSONSerialization.data(withJSONObject: receiptDictionary, options: [])
            let validationURL = URL(string: "https://sandbox.itunes.apple.com/verifyReceipt")! // Use production URL for live app
            var request = URLRequest(url: validationURL)
            request.httpMethod = "POST"
            request.httpBody = requestData
            
            let session = URLSession.shared
            let task = session.dataTask(with: request) { (data, response, error) in
                if let error = error {
                    print("Receipt validation failed with error: \(error.localizedDescription)")
                    completion(false)
                    return
                }
                
                guard let data = data else {
                    print("Receipt validation data not found")
                    completion(false)
                    return
                }
                
                do {
                    let jsonResponse = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
                    let status = jsonResponse?["status"] as? Int
                    completion(status == 0)
                } catch {
                    print("Receipt validation response parsing error: \(error.localizedDescription)")
                    completion(false)
                }
            }
            task.resume()
        } catch {
            print("App Store receipt data error: \(error.localizedDescription)")
            completion(false)
        }
    }
    
    
    func paymentQueueRestoreCompletedTransactionsFinished(_ queue: SKPaymentQueue) {
        print("paymentQueueRestoreCompletedTransactionsFinished")
        processDelegate?.didFinishProcessing(result: true)
        loading = false
    }

    func paymentQueue(_ queue: SKPaymentQueue, restoreCompletedTransactionsFailedWithError error: Error) {
        print("Error restoring completed transactions \(error)")
        processDelegate?.didFinishProcessing(result: false)
        loading = false
    }
    
    
    // MARK: Request completed successfully
    func requestDidFinish(_ request: SKRequest) {
        print("requestDidFinish: \(request.description)")
        if let completion = completionHandlers[request] {
            completion(true)
            completionHandlers.removeValue(forKey: request)
        }
        processDelegate?.didFinishProcessing(result: false)
        loading = false
    }
    
    
    // MARK: Request failed
    func request(_ request: SKRequest, didFailWithError error: Error) {
        print("request failed: \(error.localizedDescription)")
        if let completion = completionHandlers[request] {
            completion(false)
            completionHandlers.removeValue(forKey: request)
        }
        processDelegate?.didFinishProcessing(result: false)
        loading = false
    }
    
    
    private func loadPurchaseStatusFromUserDefaults() {
        print("loadPurchaseStatusFromUserDefaults")
        let defaults = UserDefaults.standard
        if let savedPurchasedProductIdentifiers = defaults.object(forKey: C.PURCHASED_PRODUCTS_KEY) as? [String] {
            purchasedProductIdentifiers = Set(savedPurchasedProductIdentifiers)
        }
    }
    
    
    private func savePurchaseStatusToUserDefaults() {
        print("savePurchaseStatusToUserDefaults")
        let defaults = UserDefaults.standard
        let purchasedProducts = Array(purchasedProductIdentifiers)
        defaults.set(purchasedProducts, forKey: C.PURCHASED_PRODUCTS_KEY)
    }
    
    
    private func updatePurchaseStatus(_ productIdentifier: String, isPurchased: Bool) {
        print("updatePurchaseStatus")
        if isPurchased {
            purchasedProductIdentifiers.insert(productIdentifier)
        } else {
            purchasedProductIdentifiers.remove(productIdentifier)
        }
        savePurchaseStatusToUserDefaults()
        delegate?.didUpdateProducts(withProducts: getProducts())
    }
    
}
