package com.overlay.codm

import android.app.*
import android.content.*
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var overlayView: SurfaceView
    private lateinit var toggleButton: Button

    private var espEnabled = true
    private var scanJob: Job? = null

    private lateinit var mem: MemoryReader
    private lateinit var scanner: EntityScanner
    private lateinit var renderer: ESPRenderer

    private var codmPid: Int = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        setupOverlay()
        setupToggleButton()

        codmPid = findPid("com.activision.callofduty.shooter")
        if (codmPid == -1) {
            stopSelf(); return
        }

        mem      = MemoryReader(codmPid)
        scanner  = EntityScanner(mem)

        val dm = resources.displayMetrics
        renderer = ESPRenderer(dm.widthPixels, dm.heightPixels)

        startScanLoop()
    }

    private fun setupOverlay() {
        overlayView = SurfaceView(this)
        overlayView.setZOrderOnTop(true)
        overlayView.holder.setFormat(PixelFormat.TRANSPARENT)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(overlayView, params)
    }

    private fun setupToggleButton() {
        toggleButton = Button(this).apply {
            text = "ESP ON"
            alpha = 0.7f
            setBackgroundColor(Color.argb(180, 20, 20, 20))
            setTextColor(Color.WHITE)
        }

        val params = WindowManager.LayoutParams(
            220, 90,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20; y = 200
        }

        toggleButton.setOnClickListener {
            espEnabled = !espEnabled
            toggleButton.text = if (espEnabled) "ESP ON" else "ESP OFF"
            toggleButton.setBackgroundColor(
                if (espEnabled) Color.argb(180, 20, 20, 20)
                else Color.argb(180, 80, 0, 0)
            )
            if (!espEnabled) clearCanvas()
        }

        // drag to reposition
        var dX = 0f; var dY = 0f
        toggleButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { dX = params.x - event.rawX; dY = params.y - event.rawY; false }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (event.rawX + dX).toInt()
                    params.y = (event.rawY + dY).toInt()
                    wm.updateViewLayout(v, params); true
                }
                else -> false
            }
        }

        wm.addView(toggleButton, params)
    }

    private fun startScanLoop() {
        scanJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                if (!espEnabled) { delay(100); continue }
                try {
                    val entities  = scanner.scanEntities()
                    val camPos    = scanner.getCameraLocation()
                    val camFOV    = scanner.getCameraFOV()
                    // rotation — read from camera manager POV rotation (pitch/yaw)
                    val camYaw    = mem.readFloat(
                        mem.readLong(mem.readLong(scanner.getLocalPlayer() + Offsets.PLAYER_CONTROLLER)
                        + Offsets.CAMERA_MANAGER) + Offsets.POV_ROTATION)
                    val camPitch  = mem.readFloat(
                        mem.readLong(mem.readLong(scanner.getLocalPlayer() + Offsets.PLAYER_CONTROLLER)
                        + Offsets.CAMERA_MANAGER) + Offsets.POV_ROTATION + 4)

                    withContext(Dispatchers.Main) { drawFrame(entities, camPos, camYaw, camPitch, camFOV) }
                } catch (_: Exception) {}
                delay(33)  // ~30fps scan
            }
        }
    }

    private fun drawFrame(
        entities: List<PlayerEntity>,
        camPos: Vec3, camYaw: Float, camPitch: Float, fov: Float
    ) {
        val holder = overlayView.holder
        val canvas = holder.lockCanvas() ?: return
        try {
            renderer.draw(canvas, entities, camPos, camYaw, camPitch, fov)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun clearCanvas() {
        val holder = overlayView.holder
        val canvas = holder.lockCanvas() ?: return
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        holder.unlockCanvasAndPost(canvas)
    }

    private fun findPid(packageName: String): Int {
        Runtime.getRuntime().exec("su -c pidof $packageName")
            .inputStream.bufferedReader().readLine()
            ?.trim()?.toIntOrNull()?.let { return it }
        return -1
    }

    private fun buildNotification(): Notification {
        val channelId = "overlay_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("System Service")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .build()
    }

    override fun onDestroy() {
        scanJob?.cancel()
        mem.close()
        runCatching { wm.removeView(overlayView) }
        runCatching { wm.removeView(toggleButton) }
        super.onDestroy()
    }
}
