package com.indest.hah.persistance.billing

import androidx.room.Dao
import androidx.room.Query
import com.indest.hah.persistance.dto.Sku
import kotlinx.coroutines.flow.Flow

@Dao
interface BillingDao {

    @Query("select sku from sku where purchased=1")
    fun getAllPurchasedSkus(): Flow<List<String>>

    @Query("select sku from sku")
    suspend fun getAllSkuNames(): List<String>

    @Query("update sku set purchased = :value where sku=:sku")
    suspend fun setSkuPurchased(sku: String, value:Boolean)

    @Query("select exists(select 1 from sku where sku=:sku and purchased=1)")
    suspend fun isSkuPurchased(sku: String): Boolean
}