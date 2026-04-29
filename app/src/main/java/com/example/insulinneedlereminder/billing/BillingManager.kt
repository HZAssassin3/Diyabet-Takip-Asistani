package com.example.insulinneedlereminder.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.insulinneedlereminder.util.PrefsManager

class BillingManager(
    context: Context,
    private val onEntitlementChanged: (Boolean) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads_forever"
    }

    private val prefs = PrefsManager(context.applicationContext)
    private var removeAdsProductDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            queryExistingPurchases()
            queryProductDetails()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryExistingPurchases()
                    queryProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun launchRemoveAdsPurchase(activity: Activity): Boolean {
        val productDetails = removeAdsProductDetails ?: return false
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
        return true
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    private fun queryProductDetails() {
        val queryProduct = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(REMOVE_ADS_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(queryProduct))
            .build()

        billingClient.queryProductDetailsAsync(params) { _, detailsList ->
            removeAdsProductDetails = detailsList.firstOrNull()
        }
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { _, purchasesList ->
            var hasRemoveAds = false
            purchasesList.forEach {
                if (it.products.contains(REMOVE_ADS_PRODUCT_ID)) {
                    hasRemoveAds = true
                    handlePurchase(it)
                }
            }
            if (!hasRemoveAds && prefs.adsRemoved) {
                prefs.adsRemoved = false
                onEntitlementChanged(false)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!purchase.products.contains(REMOVE_ADS_PRODUCT_ID)) return

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        grantAdRemoval()
                    }
                }
            } else {
                grantAdRemoval()
            }
        }
    }

    private fun grantAdRemoval() {
        if (!prefs.adsRemoved) {
            prefs.adsRemoved = true
        }
        onEntitlementChanged(true)
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
