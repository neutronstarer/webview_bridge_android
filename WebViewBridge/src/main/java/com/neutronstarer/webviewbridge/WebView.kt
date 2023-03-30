package com.neutronstarer.webviewbridge

import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

enum class BridgePolicy {
    Cancel,
    Allow,
}

@Suppress("unused")
fun WebView.bridgeOf(namespace: String): WebViewBridge {
    return bridgeOf(namespace, true)!!
}

@Suppress("unused")
fun WebView.bridgePolicyOf(uri: Uri): BridgePolicy {
    if (uri.host != "webviewbridge") {
        return BridgePolicy.Allow
    }
    val namespace = uri.getQueryParameter("namespace") ?: return BridgePolicy.Cancel
    val action = uri.getQueryParameter("action") ?: return  BridgePolicy.Cancel
    val bridge = bridgeOf(namespace, creatable = false) ?: return BridgePolicy.Cancel
    when (action) {
        "load" -> bridge.load()
        "query" -> bridge.query()
    }
    return  BridgePolicy.Cancel
}

internal fun WebView.initializeWith(bridge: WebViewBridge){
    val name = "com.neutronstarer.webviewbridge/"+ bridge.namespace
    this.removeJavascriptInterface(name)
    this.addJavascriptInterface(InterfaceObject {
        bridge.receive(it)
    }, name)
}

internal fun WebView.bridgeOf(namespace: String, creatable: Boolean): WebViewBridge? {
    var bridge = this.getField<WebViewBridge>(namespace)
    if (bridge != null){
        return  bridge
    }
    if (!creatable){
        return null
    }
    bridge = WebViewBridge(namespace, this)
    this.addField(namespace, bridge)
    return bridge
}

private class InterfaceObject(val receive: (message: Map<String, Any>?)->Unit){
    @Suppress("unused", "unchecked_cast")
    @JavascriptInterface
    fun postMessage(arg: String?){
        if (arg == null){
            return
        }
        try {
            val m = JSONObject(arg).toMap()
            receive(m)
        }catch (e: java.lang.Exception){
            error(e)
        }
    }
}