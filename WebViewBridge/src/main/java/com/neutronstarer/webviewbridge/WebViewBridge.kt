package com.neutronstarer.webviewbridge

import android.webkit.WebView
import com.neutronstarer.npc.Cancel
import com.neutronstarer.npc.Notify
import com.neutronstarer.npc.Reply
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.lang.ref.WeakReference

typealias BridgeHandle = (client: Client, param: Any?, reply: Reply, notify: Notify) -> Cancel?

class WebViewBridge(val namespace: String, webView: WebView) {
    private var weakWebView: WeakReference<WebView>
    private val handles = mutableMapOf<String, BridgeHandle>()
    private val clientById = mutableMapOf<String, Client>()
    private val loadJS by lazy {
        val inputStream = if (BuildConfig.DEBUG){
            webView.context.resources.openRawResource(R.raw.webview_bridge_umd_development)
        }else{
            webView.context.resources.openRawResource(R.raw.webview_bridge_umd_production_min)
        }
        val text = String(inputStream.readBytes()).replace("<namespace>", namespace)
        inputStream.close()
        text
    }
    private val queryJS by lazy {
        ";(function(){try{return window['webviewbridge/${namespace}'].query();}catch(e){return '[]'};})();"
    }
    init {
        webView.initializeWith(this)
        weakWebView = WeakReference(webView)
    }

    @Suppress("unused")
    fun on(method: String, handle: BridgeHandle?) {
        this[method] = handle
    }

    operator fun get(method: String): BridgeHandle? {
        return handles[method]
    }

    operator fun set(method: String, handle: BridgeHandle?) {
        if (handle == null){
            handles.remove(method)
            clientById.forEach {
                it.value[method] = null
            }
        }else{
            handles[method] = handle
            clientById.forEach {
                val client = it.value
                client[method] = C@{ param: Any?, reply: Reply, notify: Notify ->
                    return@C handle(client, param, reply, notify)
                }
            }
        }
    }
    internal fun load(){
        weakWebView.get()?.evaluateJavascript(loadJS) {
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun query(){
        weakWebView.get()?.evaluateJavascript(queryJS) { it ->
            try {
                val m = Json.decodeFromString<Map<String,Any>>(it)
                receive(m)
            }catch (_: Exception){

            }
        }
    }

   @Suppress("unchecked_cast")
    internal fun receive(message: Map<String, Any>?){
        val m = message?.get(namespace) as? Map<*, *>?:return
        val from = m["from"] as? String?:return
        val typ = m["typ"] as? String?:return
        when(typ){
            "transmit" -> {
                val client = clientById[from] ?: return
                val body = message["body"] as? Map<String,Any> ?: return
                client.receive(body)
            }
            "connect" -> {
                val webView = weakWebView.get() ?: return
                val client = Client(webView,from,namespace)
                client.connect()
                handles.forEach {
                    client[it.key] = C@{ param: Any?, reply: Reply, notify: Notify ->
                        return@C it.value(client, param, reply, notify)
                    }
                }
                clientById[from] = client
            }
            "disconnect"->{
                clientById[from]?.disconnect()
                clientById.remove(from)
            }
        }
    }

}


