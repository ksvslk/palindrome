package com.indest.hah.game

import com.android.billingclient.api.SkuDetails

data class SkuDetailsWrapper(var purchased:Boolean = false, val skuDetails:SkuDetails)