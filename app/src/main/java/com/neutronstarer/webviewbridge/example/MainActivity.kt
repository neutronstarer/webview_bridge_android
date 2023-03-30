package com.neutronstarer.webviewbridge.example

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.neutronstarer.npc.Notify
import com.neutronstarer.npc.Reply
import com.neutronstarer.webviewbridge.BridgePolicy
import com.neutronstarer.webviewbridge.Client
import com.neutronstarer.webviewbridge.bridgeOf
import com.neutronstarer.webviewbridge.bridgePolicyOf
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object:WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url ?: return super.shouldOverrideUrlLoading(view, request)
                val policy = view?.bridgePolicyOf(url) ?: return super.shouldOverrideUrlLoading(view, request)
                if (policy == BridgePolicy.Cancel){
                    return  false
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {
                val uri = Uri.parse(url)
                val policy = view?.bridgePolicyOf(uri) ?: return super.shouldInterceptRequest(view, url)
                if (policy == BridgePolicy.Cancel){
                    return null
                }
                return super.shouldInterceptRequest(view, url)
            }
        }
        webView.loadUrl("http://192.168.2.2:8080/")
        val bridge = webView.bridgeOf("com.neutronstarer.webviewbridge")
        bridge["download"] = C@{ _: Client, param: Any?, reply: Reply, notify: Notify ->
            val timer = Timer()
            var i = 0
            val task = object : TimerTask(){
                override fun run() {
                    i++
                    if (i == 3){
                        timer.cancel()
                        reply("did download to $param", null)
                    }else{
                        notify("$i")
                    }
                }
            }
            timer.schedule(task, 0, 1000)
            return@C {
                timer.cancel()
            }
        }
        bridge["open"] =  C@{ client: Client, param: Any?, reply: Reply, notify: Notify ->
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            return@C null
        }
    }
}