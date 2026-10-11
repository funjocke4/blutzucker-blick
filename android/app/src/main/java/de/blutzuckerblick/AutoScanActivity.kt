// SPDX-License-Identifier: GPL-3.0-only
package de.blutzuckerblick

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.ImageFormat
import android.os.Bundle
import android.os.SystemClock
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Live display-digit tracking; photos are never uploaded or persisted. */
class AutoScanActivity : ComponentActivity() {
    companion object {
        const val EXTRA_VALUE = "recognized_glucose_mgdl"
        const val EXTRA_TIME = "recognized_timestamp"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var scanner: LiveGlucoseOcr? = null
    private var lastProcessed = 0L
    @Volatile private var completed = false
    private lateinit var preview: PreviewView
    private lateinit var overlay: ScanOverlay
    private lateinit var status: TextView
    private val stability = ReadingStability(3)
    private val digitTracker = DigitMotionTracker()
    // One immutable volatile value avoids races while moving the scan box on touch.
    @Volatile private var targetCenter = ScanWindow.Center()
    private var lastAnalyzedCenter: ScanWindow.Center? = null
    // Activity Result API, compatible with androidx.activity.ComponentActivity.
    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else status.text = "Kamerazugriff abgelehnt. Du kannst den Messwert manuell eingeben."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(14, 25, 32))
            setPadding(0, (22*dp).toInt(), 0, (28*dp).toInt())
        }
        root.addView(TextView(this).apply {
            text = "Display automatisch erkennen"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(10, (12*dp).toInt(), 10, (12*dp).toInt())
        })
        preview = PreviewView(this).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        overlay = ScanOverlay(this)
        // Keep the full image-resolution pipeline, but make the visible camera
        // viewport 40% shorter than before. Center it in the available space.
        val cameraSlot = FrameLayout(this)
        val cameraPanel = FrameLayout(this)
        cameraPanel.addView(preview, FrameLayout.LayoutParams(-1, -1))
        cameraPanel.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        cameraSlot.addView(cameraPanel, FrameLayout.LayoutParams(-1, 1, Gravity.CENTER))
        cameraSlot.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val availableHeight = bottom - top
            if (availableHeight > 0) {
                val newHeight = (availableHeight * ScanWindow.CAMERA_HEIGHT_FRACTION)
                    .toInt().coerceAtLeast(1)
                val params = cameraPanel.layoutParams as FrameLayout.LayoutParams
                if (params.height != newHeight) {
                    params.height = newHeight
                    params.gravity = Gravity.CENTER
                    cameraPanel.layoutParams = params
                }
            }
        }
        root.addView(cameraSlot, LinearLayout.LayoutParams(-1, 0, 1f))
        status = TextView(this).apply {
            text = "Große Ziffern zunächst in den Rahmen halten. Sobald die Zahl erkannt wird, folgt der Rahmen dem Display automatisch. Blinkende Statussymbole werden nicht mitgelesen."
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding((16*dp).toInt(), (12*dp).toInt(), (16*dp).toInt(), (12*dp).toInt())
        }
        root.addView(status)
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((12*dp).toInt(), 0, (12*dp).toInt(), 0)
        }
        buttons.addView(Button(this).apply {
            text = "Abbrechen"
            isAllCaps = false
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(buttons)
        setContentView(root)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (isFinishing || isDestroyed) return@addListener
            try {
                val provider = future.get()
                val previewUseCase = Preview.Builder().build().also {
                    it.setSurfaceProvider(preview.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(960, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { frame -> analyzeFrame(frame) }
                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, analysis
                )
                // Continuous CameraX autofocus; a center-metering request helps close-up LCDs.
                preview.post {
                    val point = preview.meteringPointFactory.createPoint(preview.width / 2f, preview.height / 2f)
                    camera.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                            .setAutoCancelDuration(4, TimeUnit.SECONDS).build()
                    )
                }
                preview.setOnTouchListener { _, motion ->
                    if (motion.action == MotionEvent.ACTION_UP) {
                        // Target and autofocus both follow the digits touched by the user.
                        overlay.centerAtTouch(motion.x, motion.y)?.let { newCenter ->
                            targetCenter = newCenter
                        }
                        val point = preview.meteringPointFactory.createPoint(motion.x, motion.y)
                        camera.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                .setAutoCancelDuration(4, TimeUnit.SECONDS).build()
                        )
                        status.text = "Ziffernrahmen neu ausgerichtet. Alle großen Ziffern vollständig im Rahmen halten …"
                        true
                    } else true
                }
            } catch (err: Exception) {
                status.text = "Kamera konnte nicht gestartet werden: ${err.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeFrame(frame: ImageProxy) {
        if (completed) { frame.close(); return }
        val now = SystemClock.elapsedRealtime()
        if (now - lastProcessed < 500L) { frame.close(); return }
        lastProcessed = now
        var bitmap: Bitmap? = null
        try {
            val picture = orientedBitmap(frame)
            bitmap = picture
            val engine = scanner ?: LiveGlucoseOcr(applicationContext).also { scanner = it }
            val center = targetCenter
            if (lastAnalyzedCenter != center) {
                stability.reset() // User deliberately repositioned: old readings are invalid.
                digitTracker.clear()
                lastAnalyzedCenter = center
            }
            var gray: DigitMotionTracker.GrayFrame? = null
            fun luma(): DigitMotionTracker.GrayFrame {
                gray?.let { return it }
                // orientedBitmap uses grayscale ARGB; all RGB channels are equal.
                val data = IntArray(picture.width * picture.height)
                picture.getPixels(data, 0, picture.width, 0, 0, picture.width, picture.height)
                for (i in data.indices) data[i] = data[i] and 0xff
                return DigitMotionTracker.GrayFrame(picture.width,picture.height,data).also { gray=it }
            }
            val tracked = if (digitTracker.current()!=null) digitTracker.follow(luma(), now) else null
            // While tracking, analyze *only* the large digits. Never let the
            // periodically blinking Beurer Bluetooth status icon influence the
            // consensus or the crop. If tracking is lost, broad acquisition
            // resumes automatically on the following frame.
            val detection = if (tracked == null) engine.detect(picture, center)
                else engine.detect(picture, Rect(tracked.left,tracked.top,tracked.right,tracked.bottom))
            if (detection != null) {
                val b=detection.bounds
                digitTracker.lock(luma(), DigitMotionTracker.RectI(b.left,b.top,b.right,b.bottom), now)
            }
            val stable = stability.offer(detection?.mgdl, now)
            val count = stability.matches
            val width = picture.width; val height = picture.height
            val movingGuide = digitTracker.current()
            runOnUiThread {
                if (completed || isFinishing) return@runOnUiThread
                overlay.highlight(detection?.bounds, width, height, count, center, movingGuide)
                status.text = if (detection == null) {
                    if (count > 0)
                        "Kurz unscharf · bisher $count/3 erkannt. Der Rahmen folgt den Ziffern."
                    else
                        "Noch keine sichere Zahl. Ziffern in den Rahmen halten; danach folgt er automatisch."
                } else "Erkannt: ${detection.mgdl} mg/dL · Prüfung $count/3 – bitte am Messgerät kontrollieren"
            }
            if (stable && detection != null && !completed) {
                completed = true
                val result = Intent()
                    .putExtra(EXTRA_VALUE, detection.mgdl)
                    .putExtra(EXTRA_TIME, System.currentTimeMillis())
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        setResult(Activity.RESULT_OK, result)
                        finish() // MainActivity opens editable confirmation. No auto-save.
                    }
                }
            }
        } catch (err: Throwable) {
            stability.reset()
            digitTracker.clear()
            android.util.Log.e("BlutzuckerOCR", "Live-OCR fehlgeschlagen", err)
            // Show a short reason instead of silently hiding native library/load failures.
            val reason = when (err) {
                is UnsatisfiedLinkError -> "OCR-Bibliothek nicht kompatibel (16-KB-Speicherseiten)."
                else -> "${err.javaClass.simpleName}: ${err.message.orEmpty().take(110)}"
            }
            runOnUiThread {
                if (!completed && !isFinishing) status.text =
                    "Erkennung fehlgeschlagen: $reason"
            }
        } finally {
            bitmap?.recycle()
            frame.close() // mandatory to prevent ImageAnalysis stalling
        }
    }

    override fun onDestroy() {
        completed = true
        executor.execute { scanner?.close(); scanner = null }
        executor.shutdown()
        super.onDestroy()
    }

    /**
     * Use the camera Y (luminance) plane directly for seven-segment recognition.
     * Previously every frame was JPEG-compressed and decoded again. This added
     * CPU delay and compression artefacts across thin illuminated LCD segments.
     * Preview remains in color; OCR needs only the monochrome camera input.
     */
    private fun orientedBitmap(image: ImageProxy): Bitmap {
        require(image.format == ImageFormat.YUV_420_888)
        val w = image.width
        val h = image.height
        val yPlane = image.planes[0]
        val yBuffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val grayPixels = IntArray(w * h)
        for (row in 0 until h) {
            val from = row * rowStride
            val into = row * w
            for (col in 0 until w) {
                val lum = yBuffer.get(from + col * pixelStride).toInt() and 0xff
                grayPixels[into + col] = 0xff000000.toInt() or (lum * 0x010101)
            }
        }
        val raw = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        raw.setPixels(grayPixels, 0, w, 0, 0, w, h)
        val degrees = image.imageInfo.rotationDegrees
        if (degrees == 0) return raw
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return try { Bitmap.createBitmap(raw, 0, 0, w, h, matrix, true) }
        finally { raw.recycle() }
    }

    /** Center guide plus movable OCR-digit outline; the outline tracks digits, not device body. */
    private class ScanOverlay(activity: Activity): View(activity) {
        private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; strokeWidth = 4f; style = Paint.Style.STROKE
        }
        private val guidePen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255); strokeWidth = 3f
            style = Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 10f), 0f)
        }
        private var box: Rect? = null
        private var frameW = 0
        private var frameH = 0
        private var progress = 0
        private var center = ScanWindow.Center()
        private var fittedArea: DigitMotionTracker.RectI? = null
        fun highlight(rect: Rect?, w: Int, h: Int, matches: Int, nextCenter: ScanWindow.Center,
                      tracked: DigitMotionTracker.RectI?) {
            box = rect?.let { Rect(it) }; frameW = w; frameH = h
            progress = matches; center = nextCenter; fittedArea = tracked
            invalidate()
        }
        private fun mapToPreview(rect: Rect): RectF {
            // The analyzed bitmap is already orientation-corrected.
            val scale = maxOf(width.toFloat() / frameW, height.toFloat() / frameH)
            val offX = (width - frameW * scale) / 2f
            val offY = (height - frameH * scale) / 2f
            return RectF(offX + rect.left * scale, offY + rect.top * scale,
                offX + rect.right * scale, offY + rect.bottom * scale)
        }

        /** Map a tap from FILL_CENTER preview pixels back into bitmap coordinates. */
        fun centerAtTouch(x: Float, y: Float): ScanWindow.Center? {
            if (width <= 0 || height <= 0 || frameW <= 0 || frameH <= 0) return null
            val scale = maxOf(width.toFloat() / frameW, height.toFloat() / frameH)
            val offX = (width - frameW * scale) / 2f
            val offY = (height - frameH * scale) / 2f
            val updated = ScanWindow.Center(
                ((x - offX) / (frameW * scale)).coerceIn(0f, 1f),
                ((y - offY) / (frameH * scale)).coerceIn(0f, 1f)
            )
            center = updated
            box = null
            fittedArea = null
            progress = 0
            invalidate()
            return updated
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // Display the SAME ROI that LiveGlucoseOcr analyzes. Account for
            // PreviewView.ScaleType.FILL_CENTER rather than drawing a misleading box.
            val guide = if (frameW > 0 && frameH > 0) {
                val tracked = fittedArea
                val region = if (tracked != null)
                    Rect(tracked.left, tracked.top, tracked.right, tracked.bottom)
                else ScanWindow.digitArea(frameW,frameH,center).let {
                    Rect(it.left,it.top,it.right,it.bottom)
                }
                mapToPreview(region)
            } else {
                val gw = width * ScanWindow.TARGET_WIDTH
                val gh = height * ScanWindow.TARGET_HEIGHT
                RectF((width-gw)/2, (height-gh)/2, (width+gw)/2, (height+gh)/2)
            }
            canvas.drawRoundRect(guide, 14f, 14f, guidePen)
            val b = box ?: return
            if (frameW <= 0 || frameH <= 0) return
            val mapped = mapToPreview(b)
            val pad = 12f
            mapped.inset(-pad, -pad)
            pen.color = if (progress >= 2) Color.rgb(77, 238, 155) else Color.rgb(255, 209, 67)
            canvas.drawRoundRect(mapped, 12f, 12f, pen)
        }
    }
}
