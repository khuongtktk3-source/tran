package com.example.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat

/**
 * FloatingBubbleService: Standard Android system overlay service showing a movable
 * chat bubble window supporting real-device script control in background.
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var controlPanelView: View? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var isPanelExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        setupFloatingBubble()
        setupControlPanel()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "floating_bubble_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Báo bong bóng điều khiển",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bong bóng điều khiển JBit")
            .setContentText("Kích hoạt bong bóng điều hành nhấp rập trên máy thật...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(999, notification)
    }

    private fun setupFloatingBubble() {
        // Inflate a programmatic simple icon layout
        val bubbleFrame = FrameLayout(this)
        val image = ImageView(this).apply {
            setImageResource(android.R.drawable.presence_online)
            alpha = 0.95f
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val layoutParams = FrameLayout.LayoutParams(130, 130)
        bubbleFrame.addView(image, layoutParams)

        bubbleView = bubbleFrame

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            130,
            130,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 350
        }

        // Draggable gesture movement implementation
        bubbleView?.setOnTouchListener(object : View.OnTouchListener {
            private var lastX = 0
            private var lastY = 0
            private var firstX = 0
            private var firstY = 0
            private var touchDownTime = 0L

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = bubbleParams!!.x
                        lastY = bubbleParams!!.y
                        firstX = event.rawX.toInt()
                        firstY = event.rawY.toInt()
                        touchDownTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX.toInt() - firstX
                        val deltaY = event.rawY.toInt() - firstY
                        bubbleParams!!.x = lastX + deltaX
                        bubbleParams!!.y = lastY + deltaY
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - touchDownTime
                        val deltaX = event.rawX.toInt() - firstX
                        val deltaY = event.rawY.toInt() - firstY
                        // Click gesture threshold detection
                        if (duration < 250 && Math.abs(deltaX) < 15 && Math.abs(deltaY) < 15) {
                            toggleControlPanel()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager.addView(bubbleView, bubbleParams)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun setupControlPanel() {
        // Create an overlay layout dynamically with clean text information & toggles
        val container = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#12121D"))
        }

        val titleView = TextView(this).apply {
            text = "🎮 JBit Macro Tool"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 12f
            setPadding(16, 16, 16, 16)
        }

        val descView = TextView(this).apply {
            text = "Accessibility: HOẠT ĐỘNG\\nBấm vào vị trí bất kỳ để lập trình kịch bản nhấp"
            setTextColor(android.graphics.Color.parseColor("#8A8A9D"))
            textSize = 9f
            setPadding(16, 0, 16, 16)
        }

        val frameLayout = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        container.addView(titleView)
        container.addView(descView)

        controlPanelView = container

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        panelParams = WindowManager.LayoutParams(
            600,
            400,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    private fun toggleControlPanel() {
        if (isPanelExpanded) {
            try {
                windowManager.removeView(controlPanelView)
            } catch (e: Exception) {}
            isPanelExpanded = false
            Toast.makeText(this, "Thu hồi bảng điều khiển", Toast.LENGTH_SHORT).show()
        } else {
            try {
                windowManager.addView(controlPanelView, panelParams)
                isPanelExpanded = true
                Toast.makeText(this, "Mở rộng J-System HUD", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Vui lòng cấp quyền vẽ chồng lên ứng dụng khác", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        controlPanelView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        isPanelExpanded = false
    }
}
