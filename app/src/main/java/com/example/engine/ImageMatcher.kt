package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import com.example.model.SearchRegion
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object ImageMatcher {

    data class MatchResult(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val similarity: Float
    )

    // Converts Base64 String to Bitmap
    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Converts Bitmap to Base64 String
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Checks distance between two colors
    fun colorDistance(color1: Int, color2: Int): Float {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        val diffR = r1 - r2
        val diffG = g1 - g2
        val diffB = b1 - b2

        // Normalize distance (max distance is sqrt(255^2 * 3) ≈ 441.67)
        val maxDist = 441.673f
        val currentDist = sqrt((diffR * diffR + diffG * diffG + diffB * diffB).toDouble().toFloat())
        return 1.0f - (currentDist / maxDist)
    }

    // High Performance Template Matching in Kotlin
    // Slides template image over screen image to find highest similarity coordinate within search region
    fun findTemplateMatch(
        screen: Bitmap,
        template: Bitmap,
        threshold: Float = 0.8f,
        region: SearchRegion = SearchRegion()
    ): MatchResult? {
        val sW = screen.width
        val sH = screen.height
        val tW = template.width
        val tH = template.height

        if (tW > sW || tH > sH) return null

        // Get bounds of search region
        val startX = if (region.useWholeScreen) 0 else region.left.coerceIn(0, sW - tW)
        val startY = if (region.useWholeScreen) 0 else region.top.coerceIn(0, sH - tH)
        val endX = if (region.useWholeScreen) (sW - tW) else region.right.coerceIn(tW, sW) - tW
        val endY = if (region.useWholeScreen) (sH - tH) else region.bottom.coerceIn(tH, sH) - tH

        if (startX > endX || startY > endY) return null

        // Fast Scan Step: We skip search locations (downsampling search grid to every 4th pixel)
        // to find initial candidates, then perform localized detailed pixel scan.
        // Also inside matching loops, we sample every 3rd pixel of template to scale performance.
        var bestX = -1
        var bestY = -1
        var maxSim = 0.0f

        val stepFast = 4
        val sampleStepT = 2 // Sample every 2nd template pixel to make it fast

        // Step 1: Coarse search
        for (y in startY..endY step stepFast) {
            for (x in startX..endX step stepFast) {
                val sim = scoreSimilarity(screen, template, x, y, sampleStep = sampleStepT)
                if (sim > maxSim) {
                    maxSim = sim
                    bestX = x
                    bestY = y
                }
            }
        }

        // Step 2: Fine search around the best coarse candidate within range of stepFast
        if (bestX != -1) {
            val localStartX = (bestX - stepFast).coerceAtLeast(startX)
            val localEndX = (bestX + stepFast).coerceAtMost(endX)
            val localStartY = (bestY - stepFast).coerceAtLeast(startY)
            val localEndY = (bestY + stepFast).coerceAtMost(endY)

            for (y in localStartY..localEndY) {
                for (x in localStartX..localEndX) {
                    val sim = scoreSimilarity(screen, template, x, y, sampleStep = 1) // Full scan
                    if (sim > maxSim) {
                        maxSim = sim
                        bestX = x
                        bestY = y
                    }
                }
            }
        }

        return if (maxSim >= threshold && bestX != -1) {
            // Return target center point
            MatchResult(
                x = bestX + tW / 2,
                y = bestY + tH / 2,
                width = tW,
                height = tH,
                similarity = maxSim
            )
        } else {
            null
        }
    }

    // Helper to evaluate pixel matching at (screenX, screenY) base coordinate
    private fun scoreSimilarity(
        screen: Bitmap,
        template: Bitmap,
        startX: Int,
        startY: Int,
        sampleStep: Int
    ): Float {
        val tW = template.width
        val tH = template.height

        var totalChecked = 0
        var totalSim = 0.0f

        // Get array of pixels for speed optimization (reduces JNI overhead of getPixel)
        // Since templates are small, we can extract theirs or screen's local pixel arrays.
        // However, extracting small local region is easy. Let's sample directly.
        for (ty in 0 until tH step sampleStep) {
            for (tx in 0 until tW step sampleStep) {
                val sx = startX + tx
                val sy = startY + ty

                if (sx < screen.width && sy < screen.height) {
                    val tPixel = template.getPixel(tx, ty)
                    
                    // Skip transparent pixels or pure white/green backgrounds if they act as masks
                    // In Android PNGs alpha channel represents mask transparency.
                    val alpha = Color.alpha(tPixel)
                    if (alpha < 50) {
                        continue // Mask out transparent pixels
                    }

                    val sPixel = screen.getPixel(sx, sy)
                    
                    // Compute absolute difference in Color channels
                    val tR = Color.red(tPixel)
                    val tG = Color.green(tPixel)
                    val tB = Color.blue(tPixel)

                    val sR = Color.red(sPixel)
                    val sG = Color.green(sPixel)
                    val sB = Color.blue(sPixel)

                    val rDiff = abs(tR - sR)
                    val gDiff = abs(tG - sG)
                    val bDiff = abs(tB - sB)

                    val pixelSim = 1.0f - ((rDiff + gDiff + bDiff).toFloat() / (3f * 255f))
                    totalSim += pixelSim
                    totalChecked++
                }
            }
        }

        if (totalChecked == 0) return 0.0f
        return totalSim / totalChecked
    }
}
