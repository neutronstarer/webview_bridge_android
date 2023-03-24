package com.neutronstarer.webviewbridge

import android.webkit.WebView
import java.lang.ref.WeakReference

class WebViewBridge(val namespace: String, webView: WebView) {
    private var weakWebView: WeakReference<WebView>
    init {
        webView.initializeWith(this)
        weakWebView = WeakReference(webView)
    }

    internal fun load(){

    }

    internal fun query(){

    }

    internal fun receive(message: Map<String, Any>?){

    }

}


