package dev.ideonq.aetheros

import android.app.WallpaperManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.sin

/**
 * Lightweight native animated wallpaper. No continuous sensors, no internet,
 * no foreground service, and no frames while the wallpaper is invisible.
 * Power saver reduces update frequency.
 */
class AetherWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = AetherEngine()

    private inner class AetherEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val power = getSystemService(PowerManager::class.java)
        private var active = false
        private var xOffset = .5f
        private val ticker = object : Runnable {
            override fun run() {
                if (!active) return
                paintFrame()
                handler.postDelayed(this, if (power?.isPowerSaveMode == true) 1200L else 125L)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            active = visible
            handler.removeCallbacks(ticker)
            if (visible) ticker.run()
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int
        ) {
            this.xOffset = xOffset
            if (active) paintFrame()
        }

        private fun paintFrame() {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas() ?: return
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                val seconds = System.currentTimeMillis() / 1000.0
                canvas.drawColor(Color.rgb(9, 18, 36))
                paint.shader = LinearGradient(
                    0f, 0f, w, h,
                    intArrayOf(Color.rgb(20, 31, 65), Color.rgb(20, 65, 91), Color.rgb(52, 39, 82)),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, paint)
                val parallax = (xOffset - .5f) * w * .22f
                val sway = sin(seconds * .2).toFloat() * (w * .055f)
                val glowX = w * .35f + parallax + sway
                val glowY = h * .28f + sin(seconds * .11).toFloat() * 35f
                paint.shader = RadialGradient(
                    glowX, glowY, w * .82f,
                    intArrayOf(0x8846C1CD.toInt(), 0x225CA6CF, Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(glowX, glowY, w * .82f, paint)
                val secondX = w * .82f - sway
                val secondY = h * .74f
                paint.shader = RadialGradient(
                    secondX, secondY, w * .64f,
                    intArrayOf(0x775B77CB, 0x22506BCE, Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(secondX, secondY, w * .64f, paint)
                paint.shader = null
            } catch (_: Exception) {
                // Wallpaper surfaces can disappear at any time during transitions.
            } finally {
                canvas?.let { runCatching { surfaceHolder.unlockCanvasAndPost(it) } }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            active = false
            handler.removeCallbacks(ticker)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            active = false
            handler.removeCallbacks(ticker)
            super.onDestroy()
        }
    }
}
