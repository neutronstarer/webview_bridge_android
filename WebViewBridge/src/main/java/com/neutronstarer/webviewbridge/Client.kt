package com.neutronstarer.webviewbridge

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.neutronstarer.npc.*
import org.json.JSONObject
import java.lang.ref.WeakReference


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
    private val handler = Handler(Looper.getMainLooper())
    init {
        weakWebView = WeakReference(webView)
    }
    @Suppress("unused")
    internal fun connect(){
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
                "to" to id,
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
    internal fun disconnect(){
        npc.disconnect(null)
    }

    @Suppress("unused")
    internal fun on(method: String, handle: Handle?) {
        this[method] = handle
    }

    internal operator fun get(method: String): Handle? {
       return npc[method]
    }

    internal operator fun set(method: String, handle: Handle?) {
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

    internal fun receive(message: Map<String, Any>){
        val typRawValue = message["typ"] as? Int ?: return
        val typ = Typ.fromRawValue(typRawValue) as? Typ ?: return
        val id = message["id"] as? Int ?: return
        val m = Message(typ,id, message["method"] as? String, message["param"], message["error"])
        npc.receive(m)
    }

    private fun send(message: Map<String,Any>) {
        var s = JSONObject(message).toString()
        s = s.replace("\\", "\\\\")
        s = s.replace("'", "\\'")
        s = s.replace("\"", "\\\"")
        s = s.replace("\u2028", "\\u2028")
        s = s.replace("\u2029", "\\u2029")
        val js = ";(function(){try{return window['webviewbridge/${namespace}'].send('${s}');}catch(e){return ''};})();"
        handler.post {
            weakWebView.get()?.evaluateJavascript(js, null)
        }
    }

}