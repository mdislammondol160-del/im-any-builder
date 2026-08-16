package com.imanybuilder.app



import android.annotation.SuppressLint

import android.os.Bundle

import android.webkit.WebChromeClient

import android.webkit.WebResourceRequest

import android.webkit.WebSettings

import android.webkit.WebView

import android.webkit.WebViewClient

import androidx.appcompat.app.AppCompatActivity



class MainActivity : AppCompatActivity() {
  
    private lateinit var webView: WebView
  

  
    @SuppressLint("SetJavaScriptEnabled")
    
    override fun onCreate(savedInstanceState: Bundle?) {
      
        super.onCreate(savedInstanceState)
        
        webView = WebView(this)
        
        setContentView(webView)
        

        
        webView.settings.apply {
          
            javaScriptEnabled = true
          
            domStorageEnabled = true
          
            allowFileAccess = false
          
            allowContentAccess = false
          
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
          
            builtInZoomControls = false
          
            safeBrowsingEnabled = true
          
            displayZoomControls = false
          
        }
        
        webView.webViewClient = object : WebViewClient() {
          
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
              
                val scheme = request.url.scheme.orEmpty()
                
                val isLocalAsset = scheme == "file" || scheme == "about"
              
                val isHttps = scheme == "https"
              
                return if (BuildConfig.WEB_VIEW_MODE == "Offline") {
                  
                    !isLocalAsset
                  
                } else {
                  
                    !(isLocalAsset || isHttps)
                    
                }
                
            }
            
        }
        
        webView.webChromeClient = WebChromeClient()
        
        check(BuildConfig.IM_BRANDING == "mandatory") { "IM branding must remain mandatory" }
        
        webView.loadUrl("file:///android_asset/index.html")
        
    }
    

    
    override fun onDestroy() {
      
        webView.destroy()
        
        super.onDestroy()
        
    }
    
}









































