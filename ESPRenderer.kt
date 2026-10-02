package com.overlay.codm

import android.graphics.*
import kotlin.math.*

class ESPRenderer(
    private val screenW: Int,
    private val screenH: Int
) {
    // paints
    private val boxPaint = Paint().apply {
        color = Color.RED; style = Paint.Style.STROKE
        strokeWidth = 2f; isAntiAlias = true
    }
    private val filledPaint = Paint().apply {
        color = Color.argb(60, 255, 0, 0); style = Paint.Style.FILL
    }
    private val healthBgPaint = Paint().apply {
        color = Color.DKGRAY; style = Paint.Style.FILL
    }
    private val healthFgPaint = Paint().apply {
        color = Color.GREEN; style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE; textSize = 28f
        typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        setShadowLayer(3f, 1f, 1f, Color.BLACK)
    }
    private val skeletonPaint = Paint().apply {
        color = Color.YELLOW; strokeWidth = 2f
        style = Paint.Style.STROKE; isAntiAlias = true
    }
    private val snaplinePaint = Paint().apply {
        color = Color.argb(180, 255, 100, 0)
        strokeWidth = 1.5f; style = Paint.Style.STROKE
    }

    // project world → screen using simple perspective projection
    // real build: read actual view-projection matrix from camera manager
    fun worldToScreen(
        worldPos: Vec3,
        camPos: Vec3,
        camYaw: Float,      // degrees
        camPitch: Float,
        fov: Float
    ): PointF? {
        val dx = worldPos.x - camPos.x
        val dy = worldPos.y - camPos.y
        val dz = worldPos.z - camPos.z

        val yawRad   = Math.toRadians(camYaw.toDouble())
        val pitchRad = Math.toRadians(camPitch.toDouble())

        val cosY = cos(yawRad); val sinY = sin(yawRad)
        val cosP = cos(pitchRad); val sinP = sin(pitchRad)

        // rotate into camera space
        val cx = (dx * cosY + dy * sinY).toFloat()
        val cy = (-dx * sinY + dy * cosY).toFloat()
        val cz = dz.toFloat()

        val camX = cx
        val camY = (cy * cosP - cz * sinP)
        val camZ = (cy * sinP + cz * cosP)

        if (camY <= 0.1f) return null  // behind camera

        val fovRad = Math.toRadians(fov.toDouble()).toFloat()
        val scale  = (screenH / 2f) / tan(fovRad / 2f)

        val sx = screenW / 2f + (camX / camY) * scale
        val sy = screenH / 2f - (camZ / camY) * scale

        if (sx < -screenW || sx > screenW * 2 || sy < -screenH || sy > screenH * 2)
            return null

        return PointF(sx, sy)
    }

    fun draw(
        canvas: Canvas,
        entities: List<PlayerEntity>,
        camPos: Vec3,
        camYaw: Float,
        camPitch: Float,
        fov: Float,
        showSkeleton: Boolean = true,
        showSnaplines: Boolean = true,
        showHealth: Boolean = true,
        showDistance: Boolean = true
    ) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val screenCenter = PointF(screenW / 2f, screenH.toFloat())

        for (entity in entities) {
            val headPos = Vec3(entity.location.x, entity.location.y, entity.location.z + 70f)
            val footPos = entity.location

            val headScreen = worldToScreen(headPos, camPos, camYaw, camPitch, fov) ?: continue
            val footScreen = worldToScreen(footPos, camPos, camYaw, camPitch, fov) ?: continue

            val boxH = abs(headScreen.y - footScreen.y)
            val boxW = boxH * 0.4f
            val boxX = footScreen.x - boxW / 2f
            val boxY = headScreen.y

            // team color
            val teamColor = if (entity.teamId == 0) Color.RED else Color.CYAN
            boxPaint.color = teamColor
            filledPaint.color = Color.argb(40,
                Color.red(teamColor), Color.green(teamColor), Color.blue(teamColor))

            // filled box
            canvas.drawRect(boxX, boxY, boxX + boxW, boxY + boxH, filledPaint)
            // border box
            canvas.drawRect(boxX, boxY, boxX + boxW, boxY + boxH, boxPaint)

            // health bar (left side)
            if (showHealth) {
                val ratio  = (entity.health / entity.maxHealth).coerceIn(0f, 1f)
                val barX   = boxX - 8f
                val barTop = boxY
                val barBot = boxY + boxH
                val barW   = 5f
                canvas.drawRect(barX - barW, barTop, barX, barBot, healthBgPaint)
                val fillTop = barBot - (boxH * ratio)
                healthFgPaint.color = when {
                    ratio > 0.6f -> Color.GREEN
                    ratio > 0.3f -> Color.YELLOW
                    else         -> Color.RED
                }
                canvas.drawRect(barX - barW, fillTop, barX, barBot, healthFgPaint)
            }

            // distance label
            if (showDistance) {
                val dist = camPos.distanceTo(entity.location) / 100f  // units → meters approx
                val label = "${dist.toInt()}m"
                canvas.drawText(label, headScreen.x - textPaint.measureText(label) / 2f,
                    headScreen.y - 6f, textPaint)
            }

            // snapline
            if (showSnaplines) {
                canvas.drawLine(screenCenter.x, screenCenter.y,
                    footScreen.x, footScreen.y, snaplinePaint)
            }

            // skeleton
            if (showSkeleton && entity.bones.size >= 6) {
                drawSkeleton(canvas, entity.bones, camPos, camYaw, camPitch, fov)
            }
        }
    }

    private fun drawSkeleton(
        canvas: Canvas, bones: List<Vec3>,
        camPos: Vec3, camYaw: Float, camPitch: Float, fov: Float
    ) {
        // UE4 CODM bone connection pairs (index into bones list)
        val connections = listOf(
            0 to 1, 1 to 2, 2 to 3,       // spine chain
            3 to 4, 3 to 5,                // shoulders
            4 to 6, 5 to 7,                // upper arms
            6 to 8, 7 to 9,                // lower arms
            1 to 10, 1 to 11,              // hips
            10 to 12, 11 to 13,            // thighs
            12 to 14, 13 to 15             // shins
        )
        val screenBones = bones.map { worldToScreen(it, camPos, camYaw, camPitch, fov) }
        for ((a, b) in connections) {
            if (a >= screenBones.size || b >= screenBones.size) continue
            val pa = screenBones[a] ?: continue
            val pb = screenBones[b] ?: continue
            canvas.drawLine(pa.x, pa.y, pb.x, pb.y, skeletonPaint)
        }
    }
}
