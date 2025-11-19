package com.indest.hah.general

import android.app.Application
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.indest.hah.R
import com.indest.hah.ads.AdsLiveData
import com.indest.hah.ads.RewardType
import com.indest.hah.game.viewmodel.FirebaseCustom
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.MinSkuDetails
import com.indest.hah.persistance.level.LevelRepository
import com.indest.hah.views.ButtonState
// RevenueCat Imports
import com.revenuecat.purchases.*
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeneralViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    application: Application
) : AndroidViewModel(application), UpdatedCustomerInfoListener {

    private val TAG = "GeneralViewModel"

    // RevenueCat instance
    private val purchases = Purchases.sharedInstance

    // Data Structures
    // Stores the actual RC Product objects (needed to trigger a purchase)
    private val storeProductsMap: MutableMap<String, StoreProduct> = mutableMapOf()
    // Stores the UI representation (Title, Price, isPurchased)
    private val minSkuDetailsMap: MutableMap<String, MinSkuDetails> = mutableMapOf()

    private var currentCustomerInfo: CustomerInfo? = null

    // Flows
    private val _buttonState = MutableSharedFlow<ButtonInfo>(1)
    val buttonState = _buttonState.asSharedFlow()

    private val _rewardState = MutableStateFlow(RewardType.NONE)
    val rewardState = _rewardState.asStateFlow()

    private val _adsLiveData: AdsLiveData = AdsLiveData(application.applicationContext)

    private val _currentLevel = MutableStateFlow(Level())
    val currentLevel = _currentLevel.asStateFlow()

    var refreshPurchases = false

    init {
        // 1. Register for RevenueCat updates
        purchases.updatedCustomerInfoListener = this

        purchases.syncPurchases()

        viewModelScope.launch {
            // 2. Define your SKUs from resources
            val skus = listOf(
                application.getString(R.string.all_inclusive_pack),
                application.getString(R.string.all_inclusive_pack_2),
                application.getString(R.string.instant_clues),
                application.getString(R.string.sad_mode),
                application.getString(R.string.instant_letters),
                application.getString(R.string.instant_pictures)
            )

            // 3. Fetch Data
            fetchCustomerInfo() // Check what the user already owns
            fetchProducts(skus) // Fetch titles/prices for the SKUs (Offerings replacement)

            // 4. Existing Game Logic
            launch {
                levelRepository.latestLevelFlow().collect { _currentLevel.value = it }
            }

            launch {
                _adsLiveData.asFlow().distinctUntilChanged().collect {
                    when (it.second) {
                        RewardType.CLUE -> updateCategoryRewarded()
                        RewardType.PICTURE -> updatePictureRewarded()
                        else -> {}
                    }
                    updateRewardState(it.second)
                    updateUiState()
                }
            }
        }
    }

    // --- RevenueCat Implementation Starts ---

    private fun fetchCustomerInfo() {
        purchases.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                currentCustomerInfo = customerInfo
                updateUiState()
            }
            override fun onError(error: PurchasesError) {
                // Log error if needed
            }
        })
    }

    /**
     * Because you don't have Offerings set up, we ask RevenueCat to look up
     * these specific Product IDs from the store.
     */
    private fun fetchProducts(skus: List<String>) {
        purchases.getProducts(skus, object : GetStoreProductsCallback {
            override fun onReceived(storeProducts: List<StoreProduct>) {
                storeProducts.forEach { product ->
                    // 1. Save the product object so we can buy it later
                    storeProductsMap[product.id] = product

                    // 2. Map it to your UI model
                    mapProductToMinSkuDetails(product)
                }
                updateUiState()
            }

            override fun onError(error: PurchasesError) {
                // Handle error
            }
        })
    }

    private fun mapProductToMinSkuDetails(product: StoreProduct) {
        val details = MinSkuDetails(
            sku = product.id,
            // Clean up "(AppName)" from the title if present, similar to your old code
            title = product.title.substringBefore(" ("),
            price = product.price.formatted,
            canPurchase = true,
            isPurchased = isProductEntitled(product.id)
        )
        minSkuDetailsMap[product.id] = details
    }

    /**
     * Checks if the user has an active Entitlement corresponding to this SKU.
     */
    private fun isProductEntitled(sku: String): Boolean {
        // 1. Check if mapped to an active Entitlement (Best Practice)
        val hasEntitlement = currentCustomerInfo?.entitlements?.all?.values?.any {
            it.isActive && it.productIdentifier == sku
        } ?: false

        // 2. FALLBACK: Check if the raw SKU exists in the non-subscription transactions
        // This captures old one-time purchases that haven't been mapped to an Entitlement yet
        val hasLegacyPurchase = currentCustomerInfo?.allPurchasedProductIds?.contains(sku) ?: false

        return hasEntitlement || hasLegacyPurchase
    }

    // Callback from RevenueCat when purchase/restore updates happen in background
    override fun onReceived(customerInfo: CustomerInfo) {
        currentCustomerInfo = customerInfo
        updateUiState()
    }

    fun launchBillingFlow(activity: AppCompatActivity, sku: String) {
        val productToBuy = storeProductsMap[sku]

        if (productToBuy != null) {
            val params = PurchaseParams.Builder(activity, productToBuy).build()

            purchases.purchaseWith(
                params,
                onError = { error, userCancelled ->
                    // Handle error or cancellation
                },
                onSuccess = { storeTransaction, customerInfo ->
                    currentCustomerInfo = customerInfo
                    updateUiState()
                }
            )
        } else {
            // Error: Product details haven't loaded from RevenueCat yet
        }
    }

    fun refreshPurchasesTriedRedeem() {
        if (refreshPurchases) {
            refreshPurchases = false
            purchases.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    currentCustomerInfo = customerInfo
                    updateUiState()
                }
                override fun onError(error: PurchasesError) { }
            })
        }
    }

    // --- RevenueCat Implementation Ends ---


    // --- Existing Logic Below ---

    fun updateRewardState(rewardType: RewardType) {
        _rewardState.value = rewardType
    }

    fun showRewardedVideo(activity: AppCompatActivity, rewardType: RewardType) {
        _adsLiveData.showRewardedVideo(activity, rewardType)
    }

    fun updateCategoryRewarded() {
        viewModelScope.launch {
            levelRepository.updateCategoryRewarded(true, currentLevel.value.levelInfo.levelId)
        }
    }

    fun updatePictureRewarded() {
        viewModelScope.launch {
            levelRepository.updatePictureRewarded(true, currentLevel.value.levelInfo.levelId)
        }
    }

    private fun updateUiState() {
        // Refresh the "isPurchased" status in the map based on latest CustomerInfo
        minSkuDetailsMap.forEach { (sku, details) ->
            minSkuDetailsMap[sku] = details.copy(isPurchased = isProductEntitled(sku))
        }

        // Emit new state
        val isAd = _adsLiveData.value?.first == true
        viewModelScope.launch {
            _buttonState.emit(
                ButtonInfo(
                    if (isAd) ButtonState.AD else ButtonState.NO_AD,
                    minSkuDetailsMap,
                    isSadModePurchased()
                )
            )
        }
    }

    fun isSadModePurchased(): Boolean {
        // We check the entitlements for these specific SKUs
        val app = getApplication<Application>()
        val sadModeSku = app.getString(R.string.sad_mode)
        val allInclusiveSku = app.getString(R.string.all_inclusive_pack)
        val allInclusiveSku2 = app.getString(R.string.all_inclusive_pack_2)

        return isProductEntitled(sadModeSku) ||
                isProductEntitled(allInclusiveSku) ||
                isProductEntitled(allInclusiveSku2)
    }

    fun getShareIntent(screen: String): Intent {
        viewModelScope.launch(Dispatchers.Default) {
            FirebaseAnalytics.getInstance(getApplication<Application>().applicationContext).apply {
                this.logEvent(
                    FirebaseCustom.Events.SHARE_GAME_CLICKED,
                    bundleOf(Pair(FirebaseCustom.Params.ORIGIN_SCREEN, screen))
                )
            }
        }
        val shareDescription = getApplication<Application>().applicationContext.getString(R.string.share_description)
        val shareTitle = getApplication<Application>().applicationContext.getString(R.string.share)
        return Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                shareDescription
            )
            type = "text/plain"
        }, shareTitle)
    }

    data class ButtonInfo(
        val state: ButtonState,
        val skus: MutableMap<String, MinSkuDetails>,
        val sadMode: Boolean
    )

    suspend fun isAllowedToSubmitPalindrome():Boolean
    {
        return levelRepository.isAllowedToSubmitPalindrome()
    }

    suspend fun isPlayerLevelsAvailable():Boolean
    {
        return levelRepository.getMaxLevelId() > 1000
    }
}