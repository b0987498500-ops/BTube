package com.btube.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var blackOverlay: View
    private lateinit var tvTimerDisplay: TextView
    private lateinit var btnMinus10: Button
    private lateinit var btnPlus10: Button
    private lateinit var btnResetTimer: Button
    private lateinit var btnEnterPocketMode: Button

    private var isPocketMode = false
    private var tapCount = 0
    private var lastTapTime = 0L

    // 近接感應器
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var proximityListener: SensorEventListener? = null

    // 睡眠計時器 (預設 30 分鐘 = 1800 秒)
    private var countDownTimer: CountDownTimer? = null
    private var remainingTimeSeconds: Long = 30 * 60L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupWebView()
        setupProximitySensor()
        setupListeners()
        setupBackHandler()

        // 啟動預設 30 分鐘計時器
        startTimer(remainingTimeSeconds)
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        blackOverlay = findViewById(R.id.blackOverlay)
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay)
        btnMinus10 = findViewById(R.id.btnMinus10)
        btnPlus10 = findViewById(R.id.btnPlus10)
        btnResetTimer = findViewById(R.id.btnResetTimer)
        btnEnterPocketMode = findViewById(R.id.btnEnterPocketMode)
    }

    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // 設定 Chrome Mobile User-Agent 確保 YouTube 網頁與帳號登入正常
        settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        // 開啟 Cookie 支援，維持 Google/YouTube 登入狀態
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // 所有連結皆在 App 內的 WebView 開啟
                return false
            }
        }

        webView.webChromeClient = WebChromeClient()
        webView.loadUrl("https://m.youtube.com")
    }

    private fun setupListeners() {
        // 手動進入黑幕模式
        btnEnterPocketMode.setOnClickListener {
            enterPocketMode()
        }

        // 計時器控制：-10 分鐘
        btnMinus10.setOnClickListener {
            adjustTimer(-10 * 60)
        }

        // 計時器控制：+10 分鐘
        btnPlus10.setOnClickListener {
            adjustTimer(10 * 60)
        }

        // 計時器控制：重置 / 開啟
        btnResetTimer.setOnClickListener {
            resetTimer()
        }

        // 黑幕模式連點 8 次解鎖與防觸控攔截
        blackOverlay.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime <= 500) {
                    tapCount++
                } else {
                    tapCount = 1
                }
                lastTapTime = currentTime

                if (tapCount >= 8) {
                    exitPocketMode()
                    Toast.makeText(this@MainActivity, "解鎖成功：已連點 8 次解除黑幕", Toast.LENGTH_SHORT).show()
                }
            }
            true // 攔截所有觸控事件，防止口袋誤觸
        }
    }

    // --- 黑幕模式核心邏輯 (Pocket Mode) ---
    private fun enterPocketMode() {
        if (isPocketMode) return
        isPocketMode = true

        // a. 顯示純黑遮罩
        blackOverlay.visibility = View.VISIBLE

        // b. 將手機螢幕亮度調至最低 (0.01f)
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 0.01f
        window.attributes = layoutParams

        // c. 加上 FLAG_KEEP_SCREEN_ON 標誌，防止手機自動休眠
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tapCount = 0
        Toast.makeText(this, "已進入黑幕模式 (連點螢幕8次解鎖)", Toast.LENGTH_SHORT).show()
    }

    private fun exitPocketMode() {
        if (!isPocketMode) return
        isPocketMode = false

        // a. 隱藏純黑遮罩
        blackOverlay.visibility = View.GONE

        // b. 將螢幕亮度恢復為系統預設值
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams

        // c. 移除 FLAG_KEEP_SCREEN_ON 標誌
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tapCount = 0
    }

    // --- 近接感應器自動切換 ---
    private fun setupProximitySensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (proximitySensor == null) {
            return
        }

        proximityListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val distance = it.values[0]
                    val maxRange = proximitySensor?.maximumRange ?: 5f

                    // 偵測到靠近遮擋 (放入口袋或螢幕朝下)
                    if (distance < maxRange && distance < 5f) {
                        enterPocketMode()
                    } else {
                        // 離開遮擋 (拿出手機)
                        exitPocketMode()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // --- 睡眠計時器邏輯 ---
    private fun startTimer(seconds: Long) {
        countDownTimer?.cancel()
        remainingTimeSeconds = seconds.coerceAtLeast(0)

        if (remainingTimeSeconds <= 0) {
            onTimerFinished()
            return
        }

        countDownTimer = object : CountDownTimer(remainingTimeSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeSeconds = millisUntilFinished / 1000
                updateTimerUI()
            }

            override fun onFinish() {
                remainingTimeSeconds = 0
                updateTimerUI()
                onTimerFinished()
            }
        }.start()
    }

    private fun adjustTimer(deltaSeconds: Long) {
        val newTime = remainingTimeSeconds + deltaSeconds
        if (newTime > 0) {
            startTimer(newTime)
        } else {
            resetTimer()
        }
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        remainingTimeSeconds = 30 * 60L
        startTimer(remainingTimeSeconds)
        Toast.makeText(this, "計時器已重置為 30 分鐘", Toast.LENGTH_SHORT).show()
    }

    private fun updateTimerUI() {
        val minutes = remainingTimeSeconds / 60
        val seconds = remainingTimeSeconds % 60
        tvTimerDisplay.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun onTimerFinished() {
        tvTimerDisplay.text = "00:00 (已關閉)"

        // a. 清除 FLAG_KEEP_SCREEN_ON 標誌
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // b. 暫停 WebView 媒體播放
        webView.evaluateJavascript("document.querySelector('video')?.pause();", null)
        webView.onPause()

        // c. 釋放螢幕常亮控制權，解除黑幕，讓手機進入系統硬體關屏休眠
        exitPocketMode()
        Toast.makeText(this, "睡眠定時時間到，播放已暫停並關閉螢幕常亮", Toast.LENGTH_LONG).show()
    }

    // --- 生命週期與 Back 鍵處置 ---
    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPocketMode) {
                    exitPocketMode()
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.let { sensor ->
            proximityListener?.let { listener ->
                sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        proximityListener?.let { listener ->
            sensorManager?.unregisterListener(listener)
        }
        webView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        webView.destroy()
    }
}
