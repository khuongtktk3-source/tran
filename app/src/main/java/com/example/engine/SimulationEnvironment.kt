package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import kotlin.math.sqrt

enum class SimulationType(val displayName: String, val description: String) {
    GOLD_MINER("Độ Đào Vàng (Gold Miner)", "Đào quặng vàng và nhận rương phần thưởng vàng tự động..."),
    RPG_BATTLE("Trận Chiến RPG (Boss Combat)", "Nhấn liên tục để hạ quái vật và click mở rương báu..."),
    SECURE_LOGIN("Đăng Nhập Bảo Mật", "Bấm xác nhận, nhập mật khẩu và bấm gửi biểu mẫu tự động...")
}

class SimulationEnvironment(val type: SimulationType) {

    // Properties for Gold Miner
    var goldX = 100f
    var goldY = 150f
    var goldActive = true
    var goldCount = 0
    var chestActive = false
    var chestX = 200f
    var chestY = 250f
    var rockX = 280f
    var rockY = 100f

    // Properties for RPG Combat
    var bossHp = 100
    var maxBossHp = 100
    var bossState = "ALIVE" // ALIVE, DEAD
    var rewardActive = false
    var rewardX = 180f
    var rewardY = 280f
    var rewardClaimed = 0
    var monsterColor = Color.rgb(230, 50, 50)
    var monsterX = 200f
    var monsterY = 180f

    // Properties for Secure Login
    var checkedCaptcha = false
    var passwordFieldText = ""
    var loginStatus = "WAITING" // WAITING, LOGGED_IN, FAILED
    var attempts = 0

    // Coordinates mapping
    val width = 400
    val height = 400
    private val bufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bufferBitmap)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        reset()
    }

    fun reset() {
        goldX = 80f
        goldY = 120f
        goldActive = true
        goldCount = 0
        chestActive = false
        chestX = 160f
        chestY = 260f
        rockX = 270f
        rockY = 140f

        bossHp = 100
        bossState = "ALIVE"
        rewardActive = false
        rewardClaimed = 0
        monsterX = 200f
        monsterY = 180f

        checkedCaptcha = false
        passwordFieldText = ""
        loginStatus = "WAITING"
        attempts = 0
    }

    // Tick update logic (simulates background world movement animation)
    fun tick(timeMs: Long) {
        when (type) {
            SimulationType.GOLD_MINER -> {
                // If gold is inactive, respawn after some time (roughly 4 seconds)
                if (!goldActive && !chestActive) {
                    if (timeMs % 160 == 0L) {
                        goldX = (50..300).random().toFloat()
                        goldY = (60..220).random().toFloat()
                        goldActive = true
                    }
                }
                // Rock moves back and forth slightly
                rockX = 270f + (Math.sin(timeMs / 10.0) * 15f).toFloat()
            }
            SimulationType.RPG_BATTLE -> {
                if (bossState == "DEAD" && !rewardActive) {
                    if (timeMs % 120 == 0L) {
                        bossHp = 100
                        bossState = "ALIVE"
                        monsterX = (120..280).random().toFloat()
                        monsterY = (120..220).random().toFloat()
                    }
                }
                // Small bounce effect for monster
                if (bossState == "ALIVE") {
                    monsterY = 180f + (Math.sin(timeMs / 5.0) * 8f).toFloat()
                }
            }
            SimulationType.SECURE_LOGIN -> {
                // Slower interval changes if needed
            }
        }
    }

    // Handles interactive virtual click from script runner!
    fun tapCoordinate(x: Int, y: Int): String {
        val clickRadius = 25
        when (type) {
            SimulationType.GOLD_MINER -> {
                if (goldActive && distance(x.toFloat(), y.toFloat(), goldX, goldY) < clickRadius + 15) {
                    goldActive = false
                    goldCount++
                    // Show reward chest randomly or every 3 clicks
                    if (goldCount % 2 == 0) {
                        chestActive = true
                        chestX = (100..300).random().toFloat()
                        chestY = (240..320).random().toFloat()
                    }
                    return "Đã khai thác cục Vàng tại ($x, $y)!"
                }
                if (chestActive && distance(x.toFloat(), y.toFloat(), chestX, chestY) < clickRadius + 15) {
                    chestActive = false
                    return "Đã mở rương phần thưởng khai thác!"
                }
                if (distance(x.toFloat(), y.toFloat(), rockX, rockY) < clickRadius + 10) {
                    return "Hành động: Click nhầm vào hòn đá cản trở tại ($x, $y)!"
                }
            }
            SimulationType.RPG_BATTLE -> {
                if (bossState == "ALIVE" && distance(x.toFloat(), y.toFloat(), monsterX, monsterY) < clickRadius + 20) {
                    bossHp -= 20
                    if (bossHp <= 0) {
                        bossHp = 0
                        bossState = "DEAD"
                        rewardActive = true
                        rewardX = monsterX
                        rewardY = monsterY + 40f
                    }
                    return "Đã tấn công Quái Vật Boss! HP: $bossHp"
                }
                if (rewardActive && distance(x.toFloat(), y.toFloat(), rewardX, rewardY) < clickRadius + 15) {
                    rewardActive = false
                    rewardClaimed++
                    return "Nhận Thưởng: Nhận Rương Chiến Thắng Thành Công!"
                }
            }
            SimulationType.SECURE_LOGIN -> {
                // Capcha Toggle Box (x: 100, y: 150)
                if (x in 80..120 && y in 140..170) {
                    checkedCaptcha = !checkedCaptcha
                    return "Chọn checkbox xác minh captcha!"
                }
                // Password Text Field click
                if (x in 100..300 && y in 200..235) {
                    passwordFieldText = "9999" // Auto fills when clicked or typed
                    return "Nhập mật khẩu: '9999'"
                }
                // Submit Login Button (x: 150..250, y: 260..295)
                if (x in 140..260 && y in 255..295) {
                    if (checkedCaptcha && passwordFieldText == "9999") {
                        loginStatus = "LOGGED_IN"
                        return "Đăng nhập THÀNH CÔNG!"
                    } else {
                        loginStatus = "FAILED"
                        attempts++
                        return "Đăng nhập Thất Bại (Mật khẩu hoặc Captcha lỗi)!"
                    }
                }
                // Reset/Back Button
                if (x in 150..250 && y in 320..355 && loginStatus != "WAITING") {
                    loginStatus = "WAITING"
                    checkedCaptcha = false
                    passwordFieldText = ""
                    return "Khởi tạo lại trạng thái biểu mẫu."
                }
            }
        }
        return "Bấm trượt tại tọa độ ($x, $y)."
    }

    // Render simulated screen onto the byte buffer
    fun getScreenBitmap(): Bitmap {
        // Clear background
        canvas.drawColor(Color.rgb(18, 18, 22)) // Flat dark slate canvas

        // Draw boundaries
        paint.color = Color.rgb(50, 50, 65)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRect(Rect(0, 0, width, height), paint)

        // Draw HUD status bars at top
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(32, 32, 40)
        canvas.drawRect(0f, 0f, width.toFloat(), 40f, paint)

        // HUD Text
        paint.color = Color.rgb(220, 220, 240)
        paint.textSize = 14f
        paint.style = Paint.Style.FILL
        canvas.drawText("Màn hình giả lập - jBitMacro Sandbox", 12f, 25f, paint)

        // Draw specific environments
        when (type) {
            SimulationType.GOLD_MINER -> renderGoldMiner()
            SimulationType.RPG_BATTLE -> renderRPGBattle()
            SimulationType.SECURE_LOGIN -> renderSecureLogin()
        }

        return bufferBitmap
    }

    private fun renderGoldMiner() {
        // Draw stone ground pattern
        paint.color = Color.rgb(25, 23, 20)
        paint.style = Paint.Style.FILL
        canvas.drawRect(5f, 40f, 395f, 395f, paint)

        // Draw some ground lines
        paint.color = Color.rgb(40, 36, 32)
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(10f, 180f, 390f, 180f, paint)
        canvas.drawLine(10f, 280f, 390f, 280f, paint)

        // Score info
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(243, 190, 55)
        paint.textSize = 14f
        canvas.drawText("VÀNG ĐÃ ĐÀO: $goldCount", 270f, 25f, paint)

        // 1. Draw Obstacle stone (rockX, rockY)
        paint.color = Color.rgb(90, 85, 80)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(rockX, rockY, 18f, paint)
        // Stone details
        paint.color = Color.rgb(130, 125, 120)
        canvas.drawCircle(rockX - 4, rockY - 4, 6f, paint)

        // 2. Draw Target Gold chunk (goldX, goldY)
        if (goldActive) {
            // Inner gold body
            paint.color = Color.rgb(253, 184, 19)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(goldX, goldY, 18f, paint)

            // Star emblem inside gold chunk (the anchor feature!)
            paint.color = Color.rgb(255, 255, 255)
            drawStar(canvas, goldX, goldY, 5, 10f, 4f)
            
            // Subtext for template matching help
            paint.color = Color.rgb(255, 230, 100)
            paint.textSize = 10f
            canvas.drawText("CỤC VÀNG", goldX - 22, goldY + 30, paint)
        }

        // 3. Draw Chest (chestX, chestY)
        if (chestActive) {
            paint.color = Color.rgb(139, 69, 19) // Brown chest
            paint.style = Paint.Style.FILL
            canvas.drawRect(chestX - 20, chestY - 15, chestX + 20, chestY + 15, paint)

            // Lid handle
            paint.color = Color.rgb(218, 165, 32) // Gold Lock
            canvas.drawRect(chestX - 6, chestY - 5, chestX + 6, chestY + 5, paint)

            // Subtext helper
            paint.color = Color.rgb(180, 255, 180)
            paint.textSize = 10f
            canvas.drawText("RƯƠNG", chestX - 18, chestY + 28, paint)
        }
    }

    private fun renderRPGBattle() {
        // Draw magma ground
        paint.color = Color.rgb(30, 10, 10)
        paint.style = Paint.Style.FILL
        canvas.drawRect(5f, 40f, 395f, 395f, paint)

        // Monster kills counter
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 100, 100)
        paint.textSize = 14f
        canvas.drawText("RƯƠNG THƯỞNG: $rewardClaimed", 250f, 25f, paint)

        // Draw Boss Monster if alive
        if (bossState == "ALIVE") {
            // Draw Horns
            paint.color = Color.rgb(255, 255, 255)
            paint.style = Paint.Style.FILL
            val lHorn = Path().apply {
                moveTo(monsterX - 15, monsterY - 10)
                lineTo(monsterX - 25, monsterY - 30)
                lineTo(monsterX - 5, monsterY - 15)
                close()
            }
            canvas.drawPath(lHorn, paint)
            val rHorn = Path().apply {
                moveTo(monsterX + 15, monsterY - 10)
                lineTo(monsterX + 25, monsterY - 30)
                lineTo(monsterX + 5, monsterY - 15)
                close()
            }
            canvas.drawPath(rHorn, paint)

            // Red Demon face (monsterX, monsterY)
            paint.color = monsterColor
            canvas.drawCircle(monsterX, monsterY, 24f, paint)

            // Yellow eyes
            paint.color = Color.YELLOW
            canvas.drawCircle(monsterX - 8, monsterY - 4, 4f, paint)
            canvas.drawCircle(monsterX + 8, monsterY - 4, 4f, paint)

            // Monster Label
            paint.color = Color.WHITE
            paint.textSize = 11f
            paint.style = Paint.Style.FILL
            canvas.drawText("SÁT THỦ MA VƯƠNG", monsterX - 50, monsterY - 40, paint)

            // Boss health bar
            paint.color = Color.GRAY
            canvas.drawRect(monsterX - 40, monsterY - 34, monsterX + 40, monsterY - 28, paint)
            paint.color = Color.GREEN
            val hpRatio = (bossHp.toFloat() / maxBossHp.toFloat()).coerceIn(0f, 1f)
            canvas.drawRect(monsterX - 40, monsterY - 34, (monsterX - 40) + (80 * hpRatio), monsterY - 28, paint)
        } else {
            // Dead text
            paint.color = Color.rgb(150, 150, 150)
            paint.textSize = 14f
            canvas.drawText("ĐANG CHỜ RESPAWN...", 120f, 180f, paint)
        }

        // Draw Loot / Claim chest if active
        if (rewardActive) {
            paint.color = Color.rgb(100, 170, 243) // Blue neon chest
            paint.style = Paint.Style.FILL
            canvas.drawCircle(rewardX, rewardY, 15f, paint)

            paint.color = Color.YELLOW
            canvas.drawCircle(rewardX, rewardY, 5f, paint)

            paint.color = Color.rgb(100, 255, 255)
            paint.textSize = 11f
            canvas.drawText("NHẬN QUÀ", rewardX - 25, rewardY + 28, paint)
        }
    }

    private fun renderSecureLogin() {
        // App / Form container background
        paint.color = Color.rgb(28, 28, 36)
        paint.style = Paint.Style.FILL
        canvas.drawRect(5f, 40f, 395f, 395f, paint)

        // Card header
        paint.color = Color.WHITE
        paint.textSize = 18f
        canvas.drawText("ĐĂNG NHẬP HỆ THỐNG", 100f, 75f, paint)

        paint.color = Color.GRAY
        paint.textSize = 11f
        canvas.drawText("Thực tống các bước khớp ảnh xác minh", 100f, 95f, paint)

        // 1. CAPTCHA CHECKBOX
        // Background box
        paint.color = if (checkedCaptcha) Color.rgb(40, 160, 80) else Color.rgb(60, 60, 75)
        paint.style = Paint.Style.FILL
        canvas.drawRect(100f, 140f, 130f, 170f, paint)

        // Stroke border
        paint.color = Color.WHITE
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(100f, 140f, 130f, 170f, paint)

        // If ticked, draw checkmark
        if (checkedCaptcha) {
            paint.color = Color.WHITE
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            val path = Path().apply {
                moveTo(105f, 155f)
                lineTo(113f, 163f)
                lineTo(125f, 147f)
            }
            canvas.drawPath(path, paint)
        }

        // Checkbox label
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 13f
        canvas.drawText("Tôi không phải là Robot", 145f, 160f, paint)

        // 2. PASSWORD TEXT FIELD
        // Field Card
        paint.color = Color.rgb(40, 40, 52)
        canvas.drawRect(100f, 200f, 300f, 235f, paint)

        // Border
        paint.color = Color.rgb(100, 100, 120)
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(100f, 200f, 300f, 235f, paint)

        // Typed Text
        paint.style = Paint.Style.FILL
        paint.color = if (passwordFieldText.isEmpty()) Color.rgb(110, 110, 130) else Color.WHITE
        paint.textSize = 13f
        val showText = if (passwordFieldText.isEmpty()) "Nhập mã khóa (Bấm chọn)..." else "Mật khẩu: • • • • ($passwordFieldText)"
        canvas.drawText(showText, 112f, 222f, paint)

        // 3. LOG IN SUBMIT BUTTON (x: 140..260, y: 255..295)
        paint.color = Color.rgb(65, 105, 225) // Royal Blue
        paint.style = Paint.Style.FILL
        canvas.drawRect(140f, 255f, 260f, 295f, paint)

        paint.color = Color.WHITE
        paint.textSize = 14f
        canvas.drawText("ĐĂNG NHẬP", 163f, 280f, paint)

        // STATUS MESSAGES
        paint.color = when (loginStatus) {
            "LOGGED_IN" -> Color.GREEN
            "FAILED" -> Color.RED
            else -> Color.YELLOW
        }
        paint.textSize = 14f
        val statusText = when (loginStatus) {
            "LOGGED_IN" -> "Xác thực THÀNH CÔNG! Trạng thái: OK."
            "FAILED" -> "SAI THÔNG TIN! Thử lại #$attempts"
            else -> "Chờ kịch bản gửi thông tin..."
        }
        canvas.drawText(statusText, 70f, 320f, paint)

        // If not waiting status, draw a "RESET" indicator button at bottom (x150, y340)
        if (loginStatus != "WAITING") {
            paint.color = Color.rgb(120, 120, 130)
            paint.style = Paint.Style.FILL
            canvas.drawRect(150f, 335f, 250f, 365f, paint)

            paint.color = Color.WHITE
            paint.textSize = 11f
            canvas.drawText("LÀM MỚI (RESET)", 164f, 353f, paint)
        }
    }

    private fun drawStar(canvas: Canvas, x: Float, y: Float, numberOfPoints: Int, rx: Float, ry: Float) {
        val path = Path()
        var angle = Math.PI / 2
        val nextAngle = Math.PI / numberOfPoints

        path.moveTo(x + (rx * Math.cos(angle)).toFloat(), y - (rx * Math.sin(angle)).toFloat())

        for (i in 0 until numberOfPoints * 2) {
            angle += nextAngle
            val r = if (i % 2 == 0) ry else rx
            val px = x + (r * Math.cos(angle)).toFloat()
            val py = y - (r * Math.sin(angle)).toFloat()
            path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt((dx * dx + dy * dy).toDouble().toFloat())
    }
}
