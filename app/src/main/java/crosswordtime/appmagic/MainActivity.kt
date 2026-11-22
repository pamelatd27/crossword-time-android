package crosswordtime.appmagic

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dynamically creates the container for your web game
        val myWebView = WebView(this)
        setContentView(myWebView)

        val webSettings: WebSettings = myWebView.settings
        // Enables your game logic scripts to run
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true

        myWebView.webViewClient = WebViewClient()
        myWebView.loadUrl("file:///android_asset/index.html")
    }
}
