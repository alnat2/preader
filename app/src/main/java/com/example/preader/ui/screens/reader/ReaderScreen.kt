package com.example.preader.ui.screens.reader

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import com.example.preader.data.SettingsRepository
import com.example.preader.domain.ReadingPage
import com.example.preader.domain.SourceType
import com.example.preader.webview.LocalFileResourceHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderScreen(
    pageId: String,
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit,
    onDeletePage: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    var page by remember { mutableStateOf<ReadingPage?>(null) }
    var currentProgress by remember { mutableIntStateOf(0) }
    var currentRatio by remember { mutableDoubleStateOf(0.0) }
    
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(pageId) {
        val pages = settingsRepository.pagesFlow.firstOrNull() ?: emptyList()
        page = pages.find { it.id == pageId }
        page?.let { 
            currentProgress = it.progressPercent
            currentRatio = it.positionRatio
        }
    }

    // Auto-save every 10 seconds
    LaunchedEffect(pageId) {
        while (true) {
            delay(10000)
            page?.let { p ->
                val updatedPage = p.copy(
                    positionRatio = currentRatio,
                    progressPercent = currentProgress
                )
                settingsRepository.addOrUpdatePage(updatedPage)
                page = updatedPage
            }
        }
    }

    // Save on dispose (when leaving screen)
    DisposableEffect(pageId) {
        onDispose {
            page?.let { p ->
                val updatedPage = p.copy(
                    positionRatio = currentRatio,
                    progressPercent = currentProgress
                )
                coroutineScope.launch {
                    settingsRepository.addOrUpdatePage(updatedPage)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.preader.R.drawable.back),
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        settingsRepository.resetProgress(pageId)
                                        currentProgress = 0
                                        currentRatio = 0.0
                                        webViewRef?.scrollTo(0, 0)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.preader.R.drawable.reset),
                                contentDescription = "Сбросить",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Delete Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .border(2.dp, MaterialTheme.colorScheme.error, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .clickable {
                                    onDeletePage(pageId)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.preader.R.drawable.delete),
                                contentDescription = "Удалить",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Open Button removed per user request
                    }
                }
            )
        }
    ) { padding ->
        if (page == null) {
            // Loading state
            return@Scaffold
        }

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { ctx ->
                CustomWebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDarkTheme)
                    } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        WebSettingsCompat.setForceDark(
                            settings,
                            if (isDarkTheme) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                        )
                    }
                    
                    var assetLoader: WebViewAssetLoader? = null
                    val file = File(page!!.sourcePath)

                    if (page!!.sourceType == SourceType.Folder) {
                        assetLoader = WebViewAssetLoader.Builder()
                            .addPathHandler(
                                "/assets/",
                                LocalFileResourceHandler(file)
                            )
                            .build()
                    } else {
                        assetLoader = WebViewAssetLoader.Builder()
                            .addPathHandler(
                                "/local/",
                                LocalFileResourceHandler(file.parentFile!!)
                            )
                            .build()
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            val urlStr = request.url.toString()
                            
                            // Force text/html for the main file to prevent raw source code rendering
                            if (page!!.sourceType == SourceType.HtmlFile && urlStr.contains(android.net.Uri.encode(file.name))) {
                                try {
                                    val stream = java.io.FileInputStream(file)
                                    return WebResourceResponse("text/html", "UTF-8", stream)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
                            return assetLoader?.shouldInterceptRequest(request.url) 
                                ?: super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            val url = request.url.toString()
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    ctx.startActivity(intent)
                                    return true
                                } catch (e: Exception) {
                                    // Fallback if no browser is installed
                                }
                            }
                            return super.shouldOverrideUrlLoading(view, request)
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            // Restore scroll position
                            val maxScroll = getMaxScrollRange()
                            if (maxScroll > 0) {
                                val yToScroll = (currentRatio * maxScroll).roundToInt()
                                scrollTo(0, yToScroll)
                            }
                        }
                    }

                    // Setup scroll listener
                    setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                        val maxScroll = getMaxScrollRange()
                        if (maxScroll > 0) {
                            currentRatio = scrollY.toDouble() / maxScroll.toDouble()
                            currentProgress = (currentRatio * 100).roundToInt().coerceIn(0, 100)
                        }
                    }

                    // Load URL
                    if (page!!.sourceType == SourceType.HtmlFile) {
                        val encodedName = android.net.Uri.encode(file.name)
                        loadUrl("https://appassets.androidplatform.net/local/$encodedName")
                    } else {
                        val htmlFiles = file.listFiles()?.filter { 
                            it.isFile && (it.name.endsWith(".html", ignoreCase = true) || it.name.endsWith(".htm", ignoreCase = true)) 
                        } ?: emptyList()
                        
                        val mainFile = htmlFiles.find { it.name.equals("index.html", ignoreCase = true) || it.name.equals("index.htm", ignoreCase = true) } 
                            ?: htmlFiles.firstOrNull()

                        if (mainFile != null) {
                            val path = mainFile.name
                            loadUrl("https://appassets.androidplatform.net/assets/$path")
                        } else {
                            loadData("<html><body><h1>HTML файл не найден в папке</h1></body></html>", "text/html", "UTF-8")
                        }
                    }
                    
                    webViewRef = this
                }
            },
            update = {
                // Update block
            }
        )
    }
}

private class CustomWebView(context: android.content.Context) : WebView(context) {
    fun getMaxScrollRange(): Int {
        return computeVerticalScrollRange() - height
    }
}
