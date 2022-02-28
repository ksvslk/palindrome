package com.indest.hah.persistance.billing

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    private val billingDao: BillingDao
) {
    private val TAG = "BillingRepository"

    fun getAllPurchasedSkus(): Flow<List<String>> = billingDao.getAllPurchasedSkus()
    suspend fun getAllSkuNames(): List<String> = billingDao.getAllSkuNames()
    suspend fun setSkuPurchased(sku: String, value:Boolean = true) = billingDao.setSkuPurchased(sku, value)
    suspend fun isSkuPurchased(sku: String):Boolean = billingDao.isSkuPurchased(sku)
}