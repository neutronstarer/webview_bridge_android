package com.neutronstarer.webviewbridge

import android.webkit.WebView
import com.neutronstarer.npc.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.ref.WeakReference

typealias BridgeHandle = (client: Client, param: Any?, reply: Reply, notify: Notify) -> Cancel?

/**
 * Client
 */
class Client(
    webView: WebView,
    /**
     * Client id
     */
    val id: String,
    /**
     * Namespace
     */
    val namespace: String
) {

    private val npc: NPC = NPC()
    private val weakWebView: WeakReference<WebView>

    @Suppress("unused")
    fun connect(){
        npc.connect {
            val mm = mutableMapOf<String, Any>("id" to it.id, "typ" to it.typ.rawValue)
            if (it.method != null){
                mm["method"] = it.method!!
            }
            if (it.param != null){
                mm["param"] = it.param!!
            }
            if (it.error != null){
                mm["error"] = it.error!!
            }
            val m = mapOf<String,Any>(namespace to mapOf(
                "typ" to "transmit",
                "id" to id,
                "body" to mm
            ))
            send(m)
        }
        val m = mapOf<String,Any>(namespace to mapOf<String,Any>(
            "typ" to "connect",
            "to" to id,
        ))
        send(m)
    }

    @Suppress("unused")
    fun disconnect(){
        npc.disconnect(null)
    }

    @Suppress("unused")
    fun on(method: String, handle: Handle?) {
        this[method] = handle
    }

    operator fun get(method: String): Handle? {
       return npc[method]
    }

    operator fun set(method: String, handle: Handle?) {
        npc[method] = handle
    }

    @Suppress("unused")
    fun emit(method: String, param: Any? = null) {
        npc.emit(method, param)
    }

    @Suppress("unused")
    fun deliver(
        method: String,
        param: Any? = null,
        timeout: Long = 0,
        onReply: Reply? = null,
        onNotify: Notify? = null
    ): Cancel {
        return npc.deliver(method, param, timeout, onReply, onNotify)
    }

    fun receive(message: Map<String, Any>){
        val typRawValue = message["typ"] as? Int ?: return
        val typ = Typ.fromRawValue(typRawValue) as? Typ ?: return
        val id = message["id"] as? Int ?: return
        val m = Message(typ,id, message["method"] as? String, message["param"], message["error"])
        npc.receive(m)
    }

    private fun send(message: Map<String,Any>) {
        var s = Json.encodeToString(message)
        s = s.replace("\\", "\\\\")
        s = s.replace("'", "\\'")
        s = s.replace("\"", "\\\"")
        s = s.replace("\u2028", "\\u2028")
        s = s.replace("\u2029", "\\u2029")
        val js = ";(function(){try{return window['webviewbridge/${namespace}'].send('${s}');}catch(e){return ''};})();"
        weakWebView.get()?.evaluateJavascript(js, null)
    }

    init {
        weakWebView = WeakReference(webView)
    }
}