package crosswordtime.appmagic

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import crosswordtime.magicapp.R

class MainActivity : AppCompatActivity() {
    private lateinit var billing: RemoveAdsBilling
    private lateinit var ads: BannerAds
    private lateinit var game: WebView
    private var ownership = RemoveAdsBilling.Ownership.UNKNOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        val monetizationButton = Button(this).apply {
            setText(R.string.monetization_menu)
            setOnClickListener { showMonetizationMenu(it) }
        }
        root.addView(
            monetizationButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.END },
        )

        game = WebView(this)
        root.addView(game, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        game.settings.javaScriptEnabled = true
        game.settings.domStorageEnabled = true
        game.settings.allowFileAccess = true
        game.webViewClient = WebViewClient()
        game.loadUrl("file:///android_asset/index.html")

        val banner = FrameLayout(this).apply { visibility = View.GONE }
        root.addView(banner, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        ads = BannerAds(this, banner)
        billing = RemoveAdsBilling(this, { state ->
            ownership = state
            ads.setEligible(state == RemoveAdsBilling.Ownership.FREE ||
                state == RemoveAdsBilling.Ownership.PENDING)
        }, { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })
    }

    private fun showMonetizationMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        when (ownership) {
            RemoveAdsBilling.Ownership.OWNED -> popup.menu.add(R.string.billing_ad_free_active).isEnabled = false
            RemoveAdsBilling.Ownership.PENDING -> popup.menu.add(R.string.billing_pending).isEnabled = false
            else -> popup.menu.add(R.string.billing_remove_ads).setOnMenuItemClickListener {
                billing.requestPurchase { price, buy ->
                    AlertDialog.Builder(this)
                        .setTitle(R.string.billing_remove_ads)
                        .setMessage(getString(R.string.billing_purchase_confirmation, price))
                        .setPositiveButton(R.string.billing_continue) { _, _ -> buy() }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
                true
            }
        }
        popup.menu.add(R.string.billing_restore).setOnMenuItemClickListener {
            billing.refresh(showResult = true)
            true
        }
        if (ads.privacyOptionsRequired) {
            popup.menu.add(R.string.ads_privacy_options).setOnMenuItemClickListener {
                ads.showPrivacyOptions()
                true
            }
        }
        popup.show()
    }

    override fun onResume() {
        super.onResume()
        ads.resume()
        billing.refresh()
    }

    override fun onPause() {
        ads.pause()
        super.onPause()
    }

    override fun onDestroy() {
        billing.close()
        ads.close()
        (game.parent as? LinearLayout)?.removeView(game)
        game.destroy()
        super.onDestroy()
    }
}
