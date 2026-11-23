package crosswordtime.appmagic

import androidx.annotation.StringRes
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import crosswordtime.magicapp.R

internal class RemoveAdsBilling(
    private val activity: MainActivity,
    private val onOwnership: (Ownership) -> Unit,
    private val onMessage: (Int) -> Unit,
) {
    enum class Ownership { UNKNOWN, FREE, PENDING, OWNED }

    private var connecting = false
    private var closed = false
    private var revision = 0
    private var ownership = Ownership.UNKNOWN
    private val acknowledgementsInFlight = mutableSetOf<String>()

    private val client = BillingClient.newBuilder(activity)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .setListener { result, purchases ->
            onMain {
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        if (purchases.orEmpty().any { PRODUCT_ID in it.products }) {
                            applyPurchases(purchases.orEmpty())
                            if (ownership == Ownership.PENDING) onMessage(R.string.billing_pending)
                        }
                        refresh()
                    }
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh(showResult = true)
                    BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                    else -> onMessage(R.string.billing_unavailable)
                }
            }
        }
        .build()

    fun refresh(showResult: Boolean = false) {
        if (closed) return
        if (!client.isReady) {
            if (connecting) {
                if (showResult) onMessage(R.string.billing_checking)
                return
            }
            connecting = true
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) = onMain {
                    connecting = false
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryOwnership(showResult)
                    } else if (showResult) {
                        onMessage(R.string.billing_unavailable)
                    }
                }

                override fun onBillingServiceDisconnected() = Unit
            })
            return
        }
        queryOwnership(showResult)
    }

    private fun queryOwnership(showResult: Boolean, onAvailable: (() -> Unit)? = null) {
        val queryRevision = ++revision
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            onMain {
                if (queryRevision != revision) return@onMain
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    applyPurchases(purchases)
                    if (showResult) reportOwnership()
                    if (onAvailable != null) {
                        if (ownership == Ownership.FREE) onAvailable() else reportOwnership()
                    }
                } else if (showResult || onAvailable != null) {
                    onMessage(R.string.billing_unavailable)
                }
            }
        }
    }

    private fun applyPurchases(purchases: List<Purchase>) {
        val matching = purchases.filter { PRODUCT_ID in it.products }
        val completed = matching.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        ownership = when {
            completed.isNotEmpty() -> Ownership.OWNED
            matching.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> Ownership.PENDING
            else -> Ownership.FREE
        }
        onOwnership(ownership)
        completed.filterNot { it.isAcknowledged }.forEach { purchase ->
            val token = purchase.purchaseToken
            if (acknowledgementsInFlight.add(token)) {
                client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
                ) {
                    onMain { acknowledgementsInFlight.remove(token) }
                }
            }
        }
    }

    fun requestPurchase(showOffer: (String, () -> Unit) -> Unit) {
        if (!client.isReady) {
            onMessage(R.string.billing_checking)
            refresh()
            return
        }
        queryOwnership(showResult = false) {
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            val query = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
            client.queryProductDetailsAsync(query) { result, detailsResult ->
                onMain {
                    if (ownership != Ownership.FREE) return@onMain
                    val details = detailsResult.productDetailsList.firstOrNull()
                    val offer = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()
                    if (result.responseCode != BillingClient.BillingResponseCode.OK ||
                        details == null || offer == null
                    ) {
                        onMessage(R.string.billing_offer_unavailable)
                        return@onMain
                    }
                    showOffer(offer.formattedPrice) launch@{
                        if (ownership != Ownership.FREE) return@launch
                        val item = BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .setOfferToken(offer.offerToken)
                            .build()
                        val flow = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(listOf(item))
                            .build()
                        when (client.launchBillingFlow(activity, flow).responseCode) {
                            BillingClient.BillingResponseCode.OK -> Unit
                            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh(showResult = true)
                            else -> onMessage(R.string.billing_unavailable)
                        }
                    }
                }
            }
        }
    }

    private fun reportOwnership() {
        @StringRes val message = when (ownership) {
            Ownership.OWNED -> R.string.billing_ad_free_active
            Ownership.PENDING -> R.string.billing_pending
            Ownership.FREE -> R.string.billing_no_purchase
            Ownership.UNKNOWN -> R.string.billing_checking
        }
        onMessage(message)
    }

    private fun onMain(action: () -> Unit) {
        activity.runOnUiThread {
            if (!closed) action()
        }
    }

    fun close() {
        closed = true
        client.endConnection()
    }

    companion object {
        private const val PRODUCT_ID = "remove_ads"
    }
}
