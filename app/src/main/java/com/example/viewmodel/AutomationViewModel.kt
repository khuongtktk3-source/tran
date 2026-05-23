package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.database.AppDatabase
import com.example.data.database.TemplateEntity
import com.example.data.repository.AutomationRepository
import com.example.engine.ImageMatcher
import com.example.engine.SimulationEnvironment
import com.example.engine.SimulationType
import com.example.model.AutomationStep
import com.example.model.MacroScript
import com.example.model.SearchRegion
import com.example.model.StepType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutomationViewModel(application: Application) : AndroidViewModel(application) {

    // Database and Repository init
    private val database = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "automation_studio_db"
    ).fallbackToDestructiveMigration().build()

    private val repository = AutomationRepository(
        database.macroDao(),
        database.templateDao()
    )

    // Exposed Flows for UI lists
    val scripts: StateFlow<List<MacroScript>> = repository.allScripts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val templates: StateFlow<List<TemplateEntity>> = repository.allTemplates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current editing/viewing script
    var currentScript by mutableStateOf<MacroScript?>(null)
        private set

    // Active screen route
    var currentScreen by mutableStateOf("home") // home, script_editor, macro_runner, template_manager, compare_lab

    // Live Sandbox/Simulation properties
    var simulation by mutableStateOf(SimulationEnvironment(SimulationType.GOLD_MINER))
        private set

    var simulationType by mutableStateOf(SimulationType.GOLD_MINER)
        private set

    var simScreenBitmap by mutableStateOf<Bitmap?>(null)
        private set

    // Crop box properties for creating custom template images from current virtual screen
    var cropLeft by mutableStateOf(100)
    var cropTop by mutableStateOf(100)
    var cropRight by mutableStateOf(200)
    var cropBottom by mutableStateOf(200)
    var showCropOverlay by mutableStateOf(false)

    // Screenshot Cropper State Engine for real-device screenshot cropping
    var cropperScreenshotType by mutableStateOf("rpg_game") // rpg_game, gold_miner, secure_login, custom
    var customCropperBitmap by mutableStateOf<Bitmap?>(null)
    var cropperLeft by mutableStateOf(100f)
    var cropperTop by mutableStateOf(150f)
    var cropperWidth by mutableStateOf(120f)
    var cropperHeight by mutableStateOf(80f)

    // Compare Lab states (Interactive image testing panel)
    var labSelectedTemplate by mutableStateOf<TemplateEntity?>(null)
    var labSimilarityResult by mutableStateOf<Float?>(null)
    var labMatchCoords by mutableStateOf<Pair<Int, Int>?>(null)
    var labThreshold by mutableStateOf(0.7f)
    var labSearchRegionText by mutableStateOf("Toàn màn hình")

    // Script execution states
    private var executorJob: Job? = null
    var isExecuting by mutableStateOf(false)
        private set
    var activeStepIndex by mutableStateOf(-1)
        private set
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    // Bounding target indicator for live template match visuals
    var targetFoundRect by mutableStateOf<ImageMatcher.MatchResult?>(null)
    var touchRippleTarget by mutableStateOf<Pair<Int, Int>?>(null)

    // ==========================================
    // 1. ANTI-CRACK & SYSTEM INTEGRITY SECURITY STATES
    // ==========================================
    var isSourceCodeEncrypted by mutableStateOf(true)
    var isRootDetected by mutableStateOf(false)
    var isFridaDetected by mutableStateOf(false)
    var isAppSignatureValid by mutableStateOf(true)
    var isDebuggerHookProtected by mutableStateOf(true)
    var isSandboxCloned by mutableStateOf(false)
    var isEmulatorDetected by mutableStateOf(false)
    var emulatorConfidenceScore by mutableStateOf(0)
    var activeSignatureSha256 by mutableStateOf("")
    var complianceScore by mutableStateOf(100)
    var isSecurityScanning by mutableStateOf(false)
    private val _securityLogs = MutableStateFlow<List<String>>(emptyList())
    val securityLogs: StateFlow<List<String>> = _securityLogs.asStateFlow()

    // ==========================================
    // 2. DRAGGABLE FLOATING BUBBLE OVERLAY STATES
    // ==========================================
    var isBubbleVisible by mutableStateOf(false)
    var bubbleX by mutableStateOf(40f)
    var bubbleY by mutableStateOf(200f)
    var isBubbleExpanded by mutableStateOf(false)
    var bubbleRecordingStep by mutableStateOf(false)
    var bubbleIsRunning by mutableStateOf(false)

    // ==========================================
    // 3. SCRIPT MARKET STATES & REPOSITORY CONNECTOR
    // ==========================================
    private val _marketScripts = MutableStateFlow<List<com.example.model.MarketScript>>(emptyList())
    val marketScripts: StateFlow<List<com.example.model.MarketScript>> = _marketScripts.asStateFlow()

    init {
        // Prepare initial mockup templates and scripts so the app starts fully loaded and playable
        viewModelScope.launch(Dispatchers.IO) {
            preloadDefaultTemplatesAndScripts()
            setupInitialMarketScripts()
        }
        startSimulationTimer()
    }

    private fun setupInitialMarketScripts() {
        val standardMinerSteps = listOf(
            AutomationStep(type = StepType.LOG, textValue = "Chạy đào vàng cộng đồng tối ưu Pro..."),
            AutomationStep(type = StepType.DELAY, delayMs = 1000),
            AutomationStep(type = StepType.CLICK_COORD, x1 = 200, y1 = 400, delayMs = 600),
            AutomationStep(type = StepType.DELAY, delayMs = 500)
        )

        val rpgBattleSteps = listOf(
            AutomationStep(type = StepType.LOG, textValue = "Khởi chạy combat RPG mượt mà..."),
            AutomationStep(type = StepType.CLICK_COORD, x1 = 320, y1 = 450, delayMs = 400),
            AutomationStep(type = StepType.DELAY, delayMs = 1200),
            AutomationStep(type = StepType.CLICK_COORD, x1 = 320, y1 = 380, delayMs = 500),
            AutomationStep(type = StepType.LOG, textValue = "Đã thi triển combo Tuyệt Chiêu.")
        )

        val secureLoginSteps = listOf(
            AutomationStep(type = StepType.LOG, textValue = "Bắt đầu đăng nhập bảo mật..."),
            AutomationStep(type = StepType.TEXT_INPUT, textValue = "User_Security_King", delayMs = 800),
            AutomationStep(type = StepType.CLICK_COORD, x1 = 200, y1 = 250, delayMs = 500),
            AutomationStep(type = StepType.LOG, textValue = "Đăng nhập an toàn và ghi nhận token OTP.")
        )

        val giftClaimSteps = listOf(
            AutomationStep(type = StepType.LOG, textValue = "Quét rương điểm danh hằng ngày..."),
            AutomationStep(type = StepType.CLICK_COORD, x1 = 100, y1 = 150, delayMs = 500),
            AutomationStep(type = StepType.DELAY, delayMs = 800),
            AutomationStep(type = StepType.CLICK_COORD, x1 = 200, y1 = 300, delayMs = 500),
            AutomationStep(type = StepType.LOG, textValue = "Đã nhận quà thành công!")
        )

        _marketScripts.value = listOf(
            com.example.model.MarketScript(
                id = "market_1",
                name = "🔥 Auto Đào Vàng SIÊU TỐC Pro",
                description = "Nhắm mục tiêu đào rương, bốc vàng không trượt liên hoàn. Đã tối ưu tốc độ trễ cực thấp.",
                author = "NguyenMines99",
                downloads = 1420,
                rating = 4.9f,
                steps = standardMinerSteps,
                price = "Miễn phí"
            ),
            com.example.model.MarketScript(
                id = "market_2",
                name = "⚔️ RPG Knight Auto Train & Combo",
                description = "Tự động đi ải, farm quái, hồi máu thông minh khi rớt máu dưới 20%. Không kẹt góc.",
                author = "MMO_Warrior",
                downloads = 854,
                rating = 4.8f,
                steps = rpgBattleSteps,
                price = "Miễn phí"
            ),
            com.example.model.MarketScript(
                id = "market_3",
                name = "🛡️ Smart Login Secure API Tester",
                description = "Hỗ trợ điền form đăng nhập giả lập an toàn bảo mật, chống brute force và lưu token OTP.",
                author = "SafeTouch_Dev",
                downloads = 312,
                rating = 4.7f,
                steps = secureLoginSteps,
                price = "Miễn phí"
            ),
            com.example.model.MarketScript(
                id = "market_4",
                name = "🎁 Điểm Danh / Săn Quà Tự Động",
                description = "Hỗ trợ tối ưu hóa click nhận quà điểm danh hằng ngày tự động cho 4 thể loại game simulator.",
                author = "GiftSender_VN",
                downloads = 2011,
                rating = 4.9f,
                steps = giftClaimSteps,
                price = "Miễn phí"
            )
        )
    }

    // ==========================================
    // 4. SECURITY & COMPLIANCE SCAN LOGIC
    // ==========================================
    fun triggerSecurityScan() {
        viewModelScope.launch {
            isSecurityScanning = true
            _securityLogs.value = emptyList()
            val logsList = mutableListOf<String>()
            val context = getApplication<android.app.Application>()

            fun addSecLog(msg: String) {
                logsList.add(msg)
                _securityLogs.value = logsList.toList()
            }

            addSecLog("🔍 [INIT] Khởi chạy công cụ quét toàn vẹn tệp JBit-Shield...")
            delay(500)
            
            // 1. Signature Integrity Check
            addSecLog("🛡️ [QUÉT 1] Phân tích chữ ký số bytecode APK (Anti-Repackage)...")
            delay(400)
            val sigResult = com.example.engine.AppSecurityShield.verifySignatureIntegrity(context)
            isAppSignatureValid = sigResult.isValid
            activeSignatureSha256 = sigResult.currentSignature
            addSecLog("   ↳ Fingerprint SHA-256: ${activeSignatureSha256.take(45)}...")
            if (sigResult.isValid) {
                addSecLog("   ✅ Kết quả: Chữ ký nguyên bản sạch. Không phát hiện dán đập sửa nhị phân.")
            } else {
                addSecLog("   ❌ CẢNH BÁO: Phát hiện chữ ký giả mạo! Ứng dụng có thể bị repackage nghịch.")
            }
            
            // 2. Sandbox Container Cloning Check
            addSecLog("🛡️ [QUÉT 2] Kiểm tra lớp không gian ảo (Anti-App-Cloner)...")
            delay(400)
            val sandboxResult = com.example.engine.AppSecurityShield.detectSandboxCloning(context)
            isSandboxCloned = sandboxResult.isCloned
            addSecLog("   ↳ Đường dẫn đĩa đệm: ${context.filesDir.absolutePath}")
            if (isSandboxCloned) {
                addSecLog("   ❌ CẢNH BÁO: App đang chạy trong môi trường ảo lập! Nguy cơ can thiệp dữ liệu.")
            } else {
                addSecLog("   ✅ Kết quả: App chạy trực tiếp độc quyền trên hạt nhân lưu trữ thực.")
            }

            // 3. Superuser/Root Privilege integrity Check
            addSecLog("🛡️ [QUÉT 3] Rà soát nhị phân đặc quyền hạt nhân (Device Privilege Protection)...")
            delay(500)
            val rootResult = com.example.engine.AppSecurityShield.checkRootIntegrity()
            isRootDetected = rootResult.isRooted
            if (isRootDetected) {
                addSecLog("   ❌ CẢNH BÁO: Thấy nhị phân Root! Chi tiết: ${rootResult.detectionDetails}")
            } else {
                addSecLog("   ✅ Kết quả: Không phát hiện Superuser su binary. Runtime an toàn.")
            }

            // 4. Runtime Debugger & Tracer Check
            addSecLog("🛡️ [QUÉT 4] Quét luồng tiến trình & Tracer PID gỡ lỗi (Anti-Debugging)...")
            delay(400)
            val debugResult = com.example.engine.AppSecurityShield.checkDebuggerProtection(context)
            isDebuggerHookProtected = !debugResult.isDebuggerConnected
            if (debugResult.isDebuggerConnected) {
                addSecLog("   ❌ CẢNH BÁO: Thấy Debugger đang giám sát luồng CPU! TracerPid: ${debugResult.tracerPid}")
            } else {
                addSecLog("   ✅ Kết quả: Cổng gỡ lỗi JDWP được bảo vệ. Không có tiến trình theo dõi.")
            }

            // 5. Memory Injection Tracing (Frida/Xposed)
            addSecLog("🛡️ [QUÉT 5] Đọc liên kết thư viện nhúng /proc/self/maps (Anti-Memory Hack)...")
            delay(500)
            val hookResult = com.example.engine.AppSecurityShield.detectFridaAndHooking()
            isFridaDetected = hookResult.isHookDetected
            if (isFridaDetected) {
                addSecLog("   ❌ CẢNH BÁO: Phát hiện dấu vết hooking hoạt động: [${hookResult.detectedAgents}]")
            } else {
                addSecLog("   ✅ Kết quả: Không tìm thấy Frida-server hoặc XposedBridge trong linker.")
            }

            // 6. Virtual Emulator Evasion Check
            addSecLog("🛡️ [QUÉT 6] Kiểm định chỉ số phần cứng (Anti-Emulator/Evasion)...")
            delay(400)
            val emuResult = com.example.engine.AppSecurityShield.checkEmulatorEvasion()
            isEmulatorDetected = emuResult.isEmulator
            emulatorConfidenceScore = emuResult.confidenceScore
            addSecLog("   ↳ Model: ${android.os.Build.MODEL} | Board: ${android.os.Build.BOARD} | Hardware: ${android.os.Build.HARDWARE}")
            if (isEmulatorDetected) {
                addSecLog("   ❌ CẢNH BÁO: Chỉ số nghi ngờ máy ảo là ${emulatorConfidenceScore}%. Không hoạt động trên máy thật.")
            } else {
                addSecLog("   ✅ Kết quả: Thiết bị vật lý thật xác thực.")
            }

            // Calculating App Security Compliance Score
            delay(500)
            var score = 100
            if (!isAppSignatureValid) score -= 30
            if (isSandboxCloned) score -= 15
            if (isRootDetected) score -= 20
            if (!isDebuggerHookProtected) score -= 20
            if (isFridaDetected) score -= 35
            if (isEmulatorDetected) score -= 15
            complianceScore = score.coerceIn(0, 100)

            addSecLog("🏆 [XONG] Tiến trình rà soát hoàn tất!")
            addSecLog("⭐ Chỉ số an toàn ứng dụng đạt: $complianceScore/100")
            isSecurityScanning = false
        }
    }

    // ==========================================
    // 5. DRAGGABLE BUBBLE ACTIONS
    // ==========================================
    fun toggleBubbleOverlay(context: android.content.Context? = null) {
        isBubbleVisible = !isBubbleVisible
        if (isBubbleVisible) {
            addLog("Đã kích hoạt Bong Bóng Nổi dạt biên screen!")
            context?.let { ctx ->
                try {
                    val intent = android.content.Intent(ctx, com.example.engine.FloatingBubbleService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        ctx.startForegroundService(intent)
                    } else {
                        ctx.startService(intent)
                    }
                } catch (e: Exception) {
                    addLog("Lưu ý: Chưa cấp quyền vẽ lên ứng dụng khác cho JBit, đang chạy bong bóng mô phỏng!")
                }
            }
        } else {
            addLog("Đã tắt Bong Bóng Nổi.")
            isBubbleExpanded = false
            bubbleRecordingStep = false
            context?.let { ctx ->
                try {
                    val intent = android.content.Intent(ctx, com.example.engine.FloatingBubbleService::class.java)
                    ctx.stopService(intent)
                } catch (e: Exception) {}
            }
        }
    }

    fun recordBubbleTap(x: Int, y: Int) {
        if (!bubbleRecordingStep) return
        
        // Grab current script if available, or create quick bubble script buffer
        val script = currentScript ?: return
        val currentSteps = script.steps.toMutableList()
        val newStep = AutomationStep(
            type = StepType.CLICK_COORD,
            x1 = x,
            y1 = y,
            delayMs = 600
        )
        currentSteps.add(newStep)
        
        currentScript = script.copy(steps = currentSteps)
        saveEditingScript(script.name, script.description, currentSteps)
        
        triggerClickRipple(x, y)
        addLog("[Bóng Bay Ghi Nhận Click] Thêm bước nhấn tọa độ ($x, $y) vào kịch bản!")
        bubbleRecordingStep = false // single action tap
    }

    // ==========================================
    // 6. SCRIPT MARKET HUB ACTIONS (DOWNLOAD & SHARE)
    // ==========================================
    fun downloadMarketScript(script: com.example.model.MarketScript) {
        viewModelScope.launch {
            val macro = MacroScript(
                id = 0,
                name = "${script.name.replace("🔥 ", "").replace("⚔️ ", "").replace("🛡️ ", "").replace("🎁 ", "")}",
                description = script.description,
                steps = script.steps
            )
            repository.saveMacro(macro)
            
            // Mark as downloaded in UI state
            val updated = _marketScripts.value.map {
                if (it.id == script.id) it.copy(isDownloaded = true) else it
            }
            _marketScripts.value = updated
            addLog("Tải thành công script '${script.name}' từ Chợ Cộng Đồng!")
        }
    }

    fun publishLocalScriptToMarket(script: MacroScript) {
        viewModelScope.launch {
            val newMarketId = System.currentTimeMillis().toString()
            val cleanSteps = if (isSourceCodeEncrypted) {
                // Apply a simple obfuscating logic on encryption mode
                script.steps.map { step ->
                    step.copy(textValue = step.textValue.reversed().trim() + " // Encrypted")
                }
            } else {
                script.steps
            }
            
            val newMarketScript = com.example.model.MarketScript(
                id = newMarketId,
                name = "🚀 ${script.name}",
                description = script.description,
                author = "Bạn (Tôi)",
                downloads = 0,
                rating = 5.0f,
                steps = cleanSteps,
                price = "Miễn phí"
            )
            
            val currentList = _marketScripts.value.toMutableList()
            currentList.add(0, newMarketScript)
            _marketScripts.value = currentList
            addLog("Đăng tải liên kết script '${script.name}' lên Chợ Việt Nam thành công!")
        }
    }

    fun selectScript(script: MacroScript) {
        currentScript = script
    }

    fun navigateTo(screen: String) {
        currentScreen = screen
        if (screen == "macro_runner") {
            // Setup correct simulation based on editing script name / features
            val scriptName = currentScript?.name?.lowercase() ?: ""
            val targetType = when {
                scriptName.contains("rpg") || scriptName.contains("combat") || scriptName.contains("quái") -> SimulationType.RPG_BATTLE
                scriptName.contains("đăng nhập") || scriptName.contains("login") || scriptName.contains("auth") -> SimulationType.SECURE_LOGIN
                else -> SimulationType.GOLD_MINER
            }
            changeSimulation(targetType)
        }
    }

    fun createNewScript() {
        val newScript = MacroScript(
            id = 0,
            name = "Macro tự chế #${(10..99).random()}",
            description = "Đặt mô tả kịch bản tự động của bạn tại đây...",
            steps = listOf(
                AutomationStep(type = StepType.LOG, textValue = "Bắt đầu chạy kịch bản..."),
                AutomationStep(type = StepType.DELAY, delayMs = 1200)
            )
        )
        currentScript = newScript
        navigateTo("script_editor")
    }

    fun saveEditingScript(name: String, description: String, steps: List<AutomationStep>) {
        val scriptToSave = currentScript?.copy(
            name = name,
            description = description,
            steps = steps
        ) ?: MacroScript(name = name, description = description, steps = steps)

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMacro(scriptToSave)
        }
    }

    fun deleteScript(scriptId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMacro(scriptId)
        }
    }

    fun changeSimulation(type: SimulationType) {
        simulationType = type
        simulation = SimulationEnvironment(type)
        refreshSimulationBitmap()
    }

    fun resetSimulation() {
        simulation.reset()
        refreshSimulationBitmap()
        addLog("Đã thiết lập lại màn hình giả lập.")
    }

    fun tapSimulationDirectly(x: Int, y: Int) {
        val log = simulation.tapCoordinate(x, y)
        triggerClickRipple(x, y)
        refreshSimulationBitmap()
        addLog("[Người dùng nhấn] $log")

        // Thực thi bấm thật trên màn hình máy thật (Nếu người dùng bật Dịch vụ Hỗ trợ Accessibility)
        val macroSvc = com.example.engine.MacroAccessibilityService.instance
        if (macroSvc != null) {
            val context = getApplication<Application>()
            val metrics = context.resources.displayMetrics
            val realX = (x / 400f) * metrics.widthPixels
            val realY = (y / 400f) * metrics.heightPixels
            macroSvc.performClick(realX, realY) { success ->
                if (success) {
                    addLog("🎯 [MÁY THẬT] Đã mô phỏng tap vật lý tại tọa độ thật (${realX.toInt()} px, ${realY.toInt()} px)")
                } else {
                    addLog("⚠️ [MÁY THẬT] Không thể phát lệnh tap (Thử cấp lại quyền hỗ trợ)")
                }
            }
        }
    }

    private fun triggerClickRipple(x: Int, y: Int) {
        viewModelScope.launch {
            touchRippleTarget = Pair(x, y)
            delay(400)
            touchRippleTarget = null
        }
    }

    // Capture standard simulated frame
    fun refreshSimulationBitmap() {
        simScreenBitmap = simulation.getScreenBitmap()
    }

    // Save cropped area as reference template
    fun cropAndSaveTemplate(name: String) {
        val source = simScreenBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Trim bounds
            val cropW = (cropRight - cropLeft).coerceIn(10, source.width)
            val cropH = (cropBottom - cropTop).coerceIn(10, source.height)
            val sX = cropLeft.coerceIn(0, source.width - cropW)
            val sY = cropTop.coerceIn(0, source.height - cropH)

            try {
                val croppedBitmap = Bitmap.createBitmap(source, sX, sY, cropW, cropH)
                val base64Str = ImageMatcher.bitmapToBase64(croppedBitmap)
                val newTemplate = TemplateEntity(
                    id = 0,
                    name = name.trim().replace(" ", "_"),
                    imageBase64 = base64Str,
                    width = cropW,
                    height = cropH
                )
                repository.saveTemplate(newTemplate)
                withContext(Dispatchers.Main) {
                    showCropOverlay = false
                    addLog("Đã tạo Ảnh Template '${newTemplate.name}' kích thước ${cropW}x${cropH}!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Direct cropped bitmap save logic from dynamic screenshot selection activity
    fun saveCroppedCustomTemplate(name: String, croppedBitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val base64Str = ImageMatcher.bitmapToBase64(croppedBitmap)
            val newTemplate = TemplateEntity(
                id = 0,
                name = name.trim().replace(" ", "_").ifEmpty { "cropped_object_${System.currentTimeMillis()}" },
                imageBase64 = base64Str,
                width = croppedBitmap.width,
                height = croppedBitmap.height
            )
            repository.saveTemplate(newTemplate)
            withContext(Dispatchers.Main) {
                addLog("📷 Đã tự động cắt ảnh mẫu '${newTemplate.name}' (${croppedBitmap.width}x${croppedBitmap.height} px) và thêm vào thư viện!")
            }
        }
    }

    fun generateMockScreenshot(type: String): Bitmap {
        val width = 500
        val height = 800
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()

        when (type) {
            "rpg_game" -> {
                canvas.drawColor(Color.rgb(15, 10, 30))
                paint.color = Color.rgb(220, 30, 30)
                canvas.drawRect(20f, 30f, 250f, 50f, paint)
                paint.color = Color.WHITE
                paint.textSize = 14f
                canvas.drawText("HP: 2500 / 2500", 30f, 45f, paint)

                paint.color = Color.YELLOW
                paint.textSize = 24f
                canvas.drawText("👹 HỎA LONG CHÚA [Level 100]", 100f, 150f, paint)

                paint.color = Color.rgb(180, 50, 50)
                canvas.drawCircle(250f, 300f, 80f, paint)
                paint.color = Color.BLACK
                canvas.drawCircle(220f, 280f, 10f, paint)
                canvas.drawCircle(280f, 280f, 10f, paint)

                paint.color = Color.rgb(33, 150, 243)
                canvas.drawCircle(400f, 700f, 60f, paint)
                paint.color = Color.WHITE
                paint.textSize = 16f
                canvas.drawText("CHIÊU 1", 375f, 705f, paint)

                paint.color = Color.rgb(76, 175, 80)
                canvas.drawCircle(250f, 720f, 45f, paint)
                paint.color = Color.WHITE
                paint.textSize = 14f
                canvas.drawText("BƠM MÁU", 215f, 725f, paint)
            }
            "gold_miner" -> {
                canvas.drawColor(Color.rgb(45, 30, 15))
                paint.color = Color.rgb(220, 180, 120)
                canvas.drawRect(220f, 20f, 280f, 80f, paint)

                paint.color = Color.rgb(253, 184, 19)
                canvas.drawCircle(120f, 350f, 40f, paint)
                canvas.drawCircle(350f, 450f, 60f, paint)

                paint.color = Color.GRAY
                canvas.drawCircle(250f, 550f, 50f, paint)

                paint.color = Color.rgb(255, 152, 0)
                canvas.drawRect(150f, 700f, 350f, 760f, paint)
                paint.color = Color.WHITE
                paint.textSize = 18f
                canvas.drawText("🎁 NHẬN THƯỞNG", 175f, 738f, paint)
            }
            "secure_login" -> {
                canvas.drawColor(Color.rgb(18, 18, 29))
                paint.color = Color.WHITE
                paint.textSize = 28f
                canvas.drawText("🔒 SAFE OTP CORE", 120f, 100f, paint)

                paint.color = Color.rgb(44, 44, 62)
                canvas.drawRect(50f, 250f, 450f, 310f, paint)
                paint.color = Color.GRAY
                paint.textSize = 15f
                canvas.drawText("Tên đăng nhập / Email", 70f, 288f, paint)

                paint.color = Color.rgb(44, 44, 62)
                canvas.drawRect(50f, 350f, 450f, 410f, paint)
                paint.color = Color.GRAY
                canvas.drawText("Mật khẩu bảo mật", 70f, 388f, paint)

                paint.color = Color.rgb(33, 150, 243)
                canvas.drawRect(80f, 480f, 120f, 520f, paint)
                paint.color = Color.WHITE
                canvas.drawText("Xác minh không phải Robot", 140f, 505f, paint)

                paint.color = Color.rgb(57, 255, 20)
                canvas.drawRect(50f, 620f, 450f, 690f, paint)
                paint.color = Color.BLACK
                paint.textSize = 20f
                canvas.drawText(" ĐĂNG NHẬP NGAY", 150f, 663f, paint)
            }
            else -> {
                canvas.drawColor(Color.LTGRAY)
                paint.color = Color.RED
                canvas.drawCircle(250f, 400f, 150f, paint)
            }
        }
        return bmp
    }

    fun deleteTemplate(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTemplateById(id)
        }
    }

    // Starts standard tick clock for animations inside simulation environment
    private fun startSimulationTimer() {
        viewModelScope.launch {
            var tickFrame = 0L
            while (true) {
                delay(100)
                tickFrame++
                simulation.tick(tickFrame)
                refreshSimulationBitmap()
            }
        }
    }

    // Clear logs helper
    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun addLog(msg: String) {
        val currentList = _logs.value.toMutableList()
        currentList.add(msg)
        // Keep logs size tidy
        if (currentList.size > 100) {
            currentList.removeAt(0)
        }
        _logs.value = currentList
    }

    // Interactive Compare lab test matching
    fun runLabTest() {
        val currentImg = simScreenBitmap ?: return
        val currentT = labSelectedTemplate ?: return
        val tBitmap = ImageMatcher.base64ToBitmap(currentT.imageBase64) ?: return

        viewModelScope.launch(Dispatchers.Default) {
            val res = ImageMatcher.findTemplateMatch(
                currentImg,
                tBitmap,
                labThreshold,
                SearchRegion(useWholeScreen = true)
            )
            withContext(Dispatchers.Main) {
                if (res != null) {
                    labSimilarityResult = res.similarity
                    labMatchCoords = Pair(res.x, res.y)
                    addLog("Tìm thấy liên hệ: Khớp '${currentT.name}' tại (${res.x}, ${res.y}) với tỷ lệ ${"%.1f".format(res.similarity * 100)}%")
                } else {
                    labSimilarityResult = null
                    labMatchCoords = null
                    addLog("Không tìm thấy kết khớp cho '${currentT.name}' với ngưỡng ${labThreshold * 100}%!")
                }
            }
        }
    }

    // MAIN SCRIPT AUTOMATION INTERPRETER CORE
    fun startExecution() {
        val script = currentScript ?: return
        if (isExecuting) return

        isExecuting = true
        addLog("====== Bắt đầu chạy Script: ${script.name} ======")

        executorJob = viewModelScope.launch(Dispatchers.Default) {
            val steps = script.steps
            var pc = 0 // Program counter
            val stepMax = steps.size
            var loopCounters = mutableMapOf<String, Int>() // Track GOTO or Loops count if needed

            // Safety limit to prevent absolute infinite freezing loops without delay (max total cycles allowed is 500)
            var executionCycles = 0
            val maxCycles = 500

            while (pc < stepMax && isExecuting) {
                activeStepIndex = pc
                val step = steps[pc]

                if (executionCycles++ >= maxCycles) {
                    withContext(Dispatchers.Main) {
                        addLog("[Hệ thống] Tự động dừng khẩn cấp do vượt quá 500 bước chu kỳ kịch bản.")
                    }
                    break
                }

                when (step.type) {
                    StepType.DELAY -> {
                        withContext(Dispatchers.Main) {
                            addLog("[Dừng] Trễ ${step.delayMs} ms")
                        }
                        delay(step.delayMs)
                        pc++
                    }

                    StepType.LOG -> {
                        withContext(Dispatchers.Main) {
                            addLog("[Ghi chú] ${step.textValue}")
                        }
                        delay(200)
                        pc++
                    }

                    StepType.CLICK_COORD -> {
                        withContext(Dispatchers.Main) {
                            addLog("[Click] Nhấn coordinates (${step.x1}, ${step.y1})")
                            tapSimulationDirectly(step.x1, step.y1)
                        }
                        delay(step.delayMs.coerceAtLeast(100L))
                        pc++
                    }

                    StepType.CLICK_TEMPLATE -> {
                        withContext(Dispatchers.Main) {
                            addLog("[Tìm & Nhấp] Đang quét ảnh '${step.templateName}'...")
                        }

                        // Get Template Bitmap
                        val dbTemplate = templates.value.find { it.name == step.templateName }
                        var clicked = false

                        if (dbTemplate != null) {
                            val templateBitmap = ImageMatcher.base64ToBitmap(dbTemplate.imageBase64)
                            val screenSnapshot = simScreenBitmap

                            if (templateBitmap != null && screenSnapshot != null) {
                                // Execute match search
                                val match = ImageMatcher.findTemplateMatch(
                                    screenSnapshot,
                                    templateBitmap,
                                    step.similarityThreshold,
                                    step.searchRegion
                                )

                                if (match != null) {
                                    clicked = true
                                    withContext(Dispatchers.Main) {
                                        targetFoundRect = match
                                        addLog("[Tìm thấy] Khớp '${step.templateName}' (${"%.1f".format(match.similarity * 100)}%)! Thực thi nhấp vào tâm (${match.x}, ${match.y})")
                                        tapSimulationDirectly(match.x, match.y)
                                    }
                                    // Visual flash delay
                                    delay(400)
                                    withContext(Dispatchers.Main) { targetFoundRect = null }
                                }
                            }
                        }

                        if (!clicked) {
                            withContext(Dispatchers.Main) {
                                addLog("[Mất dấu] Không thấy '${step.templateName}' hợp lệ trên khung hình.")
                            }
                        }

                        delay(step.delayMs.coerceAtLeast(100L))
                        pc++
                    }

                    StepType.SWIPE -> {
                        withContext(Dispatchers.Main) {
                            addLog("[Vuốt] Thực hiện vuốt (${step.x1}, ${step.y1}) -> (${step.x2}, ${step.y2})")
                            // Sandbox triggers tap on end coordinate as representation
                            tapSimulationDirectly(step.x2, step.y2)

                            // Thực hiện vuốt thật trên máy vật lý
                            val macroSvc = com.example.engine.MacroAccessibilityService.instance
                            if (macroSvc != null) {
                                val context = getApplication<Application>()
                                val metrics = context.resources.displayMetrics
                                val sX = (step.x1 / 400f) * metrics.widthPixels
                                val sY = (step.y1 / 400f) * metrics.heightPixels
                                val eX = (step.x2 / 400f) * metrics.widthPixels
                                val eY = (step.y2 / 400f) * metrics.heightPixels
                                macroSvc.performSwipe(sX, sY, eX, eY, 400L) { success ->
                                    if (success) {
                                        addLog("🎯 [MÁY THẬT] Đã vuốt từ (${sX.toInt()}, ${sY.toInt()}) đến (${eX.toInt()}, ${eY.toInt()})")
                                    } else {
                                        addLog("⚠️ [MÁY THẬT] Không thể phát lệnh vuốt (Cần bật Hỗ trợ)")
                                    }
                                }
                            }
                        }
                        delay(step.delayMs.coerceAtLeast(300L))
                        pc++
                    }

                    StepType.GOTO -> {
                        val targetLabel = step.targetLabel
                        val targetIndex = steps.indexOfFirst { it.label == targetLabel && it.label.isNotEmpty() }

                        if (targetIndex != -1) {
                            withContext(Dispatchers.Main) {
                                addLog("[GOTO] Chuyển hướng nhảy đến dòng nhãn '${targetLabel}'")
                            }
                            pc = targetIndex
                        } else {
                            withContext(Dispatchers.Main) {
                                addLog("[GOTO Lỗi] Không tìm thấy nhãn '${targetLabel}' để nhảy.")
                            }
                            pc++
                        }
                        delay(200)
                    }

                    StepType.IF_MATCH_GOTO -> {
                        val dbTemplate = templates.value.find { it.name == step.templateName }
                        var conditionMet = false

                        if (dbTemplate != null) {
                            val templateBitmap = ImageMatcher.base64ToBitmap(dbTemplate.imageBase64)
                            val screenSnapshot = simScreenBitmap

                            if (templateBitmap != null && screenSnapshot != null) {
                                val match = ImageMatcher.findTemplateMatch(
                                    screenSnapshot,
                                    templateBitmap,
                                    step.similarityThreshold,
                                    step.searchRegion
                                )
                                if (match != null) {
                                    conditionMet = true
                                    withContext(Dispatchers.Main) {
                                        targetFoundRect = match
                                        addLog("[Nếu Khớp - TRUE] Khớp '${step.templateName}' (${"%.1f".format(match.similarity * 100)}%)!")
                                    }
                                    delay(400)
                                    withContext(Dispatchers.Main) { targetFoundRect = null }
                                }
                            }
                        }

                        if (conditionMet) {
                            val targetLabel = step.targetLabel
                            val targetIndex = steps.indexOfFirst { it.label == targetLabel && it.label.isNotEmpty() }
                            if (targetIndex != -1) {
                                withContext(Dispatchers.Main) {
                                    addLog("[Nếu Khớp - GOTO] Nhảy điều kiện sang nhãn '${targetLabel}'")
                                }
                                pc = targetIndex
                            } else {
                                withContext(Dispatchers.Main) {
                                    addLog("[Nếu Khớp - Lỗi] Không thấy nhãn '${targetLabel}'")
                                }
                                pc++
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                addLog("[Nếu Khớp - FALSE] Đối sánh '${step.templateName}' thất bại. Tiếp tục tuần tự...")
                            }
                            pc++
                        }
                        delay(200)
                    }

                    StepType.IF_COLOR_GOTO -> {
                        val screenSnapshot = simScreenBitmap
                        var conditionMet = false

                        if (screenSnapshot != null) {
                            val checkX = step.x1.coerceIn(0, screenSnapshot.width - 1)
                            val checkY = step.y1.coerceIn(0, screenSnapshot.height - 1)
                            val pixelColor = screenSnapshot.getPixel(checkX, checkY)

                            // Parse config color
                            val parsedColor = try {
                                Color.parseColor(step.colorHex)
                            } catch (e: Exception) {
                                Color.RED
                            }

                            val sim = ImageMatcher.colorDistance(pixelColor, parsedColor)
                            if (sim >= 0.90f) { // Color exact similarity greater than 90%
                                conditionMet = true
                                withContext(Dispatchers.Main) {
                                    addLog("[Khớp Màu - TRUE] Màu tại ($checkX, $checkY) trùng khớp 90%+!")
                                }
                            }
                        }

                        if (conditionMet) {
                            val targetLabel = step.targetLabel
                            val targetIndex = steps.indexOfFirst { it.label == targetLabel && it.label.isNotEmpty() }
                            if (targetIndex != -1) {
                                pc = targetIndex
                            } else {
                                pc++
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                addLog("[Khớp Màu - FALSE] Không thấy màu mục tiêu.")
                            }
                            pc++
                        }
                        delay(200)
                    }

                    StepType.TEXT_INPUT -> {
                        withContext(Dispatchers.Main) {
                            addLog("[Mục Nhập] Viết chuỗi '${step.textValue}'")
                            // Input simulation inside sandbox
                            simulation.passwordFieldText = step.textValue
                            refreshSimulationBitmap()
                        }
                        delay(step.delayMs.coerceAtLeast(200L))
                        pc++
                    }

                    StepType.KEY_BACK -> {
                        withContext(Dispatchers.Main) {
                            addLog("[Nút Back] Nhấn phím thoát Back.")
                            if (simulation.loginStatus != "WAITING") {
                                simulation.loginStatus = "WAITING"
                                simulation.checkedCaptcha = false
                                simulation.passwordFieldText = ""
                                refreshSimulationBitmap()
                            }
                        }
                        delay(step.delayMs.coerceAtLeast(200L))
                        pc++
                    }
                }
            }

            withContext(Dispatchers.Main) {
                isExecuting = false
                activeStepIndex = -1
                addLog("====== Kết thúc chạy Script! ======")
            }
        }
    }

    fun stopExecution() {
        executorJob?.cancel()
        isExecuting = false
        activeStepIndex = -1
        addLog("====== Đã Dừng Kịch Bản Automation ======")
    }

    override fun onCleared() {
        super.onCleared()
        executorJob?.cancel()
    }

    // Preload templates (bitmaps) and standard scripts
    private suspend fun preloadDefaultTemplatesAndScripts() {
        val existingMacros = database.macroDao().getAllMacros()
        // Simple mock templates and scripts insertion if empty
        val existingTemplatesCount = database.templateDao().getAllTemplates()

        // Create standard preloaded templates
        // We generate minimal icons using mock Bitmaps as placeholders for preloaded features
        // This is extremely handy as it allows template match demonstrations on first app load!
        val canvasWidth = 32
        val canvasHeight = 32

        // 1. Gold Ore Icon (Yellow chest or circle with star)
        val goldBmp = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvasG = Canvas(goldBmp)
        canvasG.drawColor(Color.TRANSPARENT)
        val pG = Paint().apply { color = Color.rgb(253, 184, 19); style = Paint.Style.FILL }
        canvasG.drawCircle(16f, 16f, 12f, pG)
        val gBase64 = ImageMatcher.bitmapToBase64(goldBmp)

        val goldTemplate = TemplateEntity(name = "gold_ore", imageBase64 = gBase64, width = 32, height = 32)
        repository.saveTemplate(goldTemplate)

        // 2. Chest Icon (Brown chest)
        val chestBmp = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvasC = Canvas(chestBmp)
        canvasC.drawColor(Color.TRANSPARENT)
        val pC = Paint().apply { color = Color.rgb(139, 69, 19); style = Paint.Style.FILL }
        canvasC.drawRect(4f, 8f, 28f, 28f, pC)
        val cBase64 = ImageMatcher.bitmapToBase64(chestBmp)

        val chestTemplate = TemplateEntity(name = "reward_chest", imageBase64 = cBase64, width = 32, height = 32)
        repository.saveTemplate(chestTemplate)

        // 3. Captcha Unchecked Box template
        val unCheckedBmp = Bitmap.createBitmap(30, 30, Bitmap.Config.ARGB_8888)
        val canvasU = Canvas(unCheckedBmp)
        canvasU.drawColor(Color.rgb(60, 60, 75))
        val uBase64 = ImageMatcher.bitmapToBase64(unCheckedBmp)
        val unCheckedTemplate = TemplateEntity(name = "unchecked_captcha", imageBase64 = uBase64, width = 30, height = 30)
        repository.saveTemplate(unCheckedTemplate)

        // Create 2 preloaded fully ready default scripts
        val goldMinerBot = MacroScript(
            id = 1,
            name = "🤖 Bot Đào Vàng Tự Động",
            description = "Tự động phát hiện quặng vàng bằng so sánh ảnh, bấm khai thác liên tục và tự thu nhặt rương vàng mới xuất hiện.",
            steps = listOf(
                AutomationStep(type = StepType.LOG, textValue = "Khởi chạy script macro khai mỏ chuyên sâu..."),
                AutomationStep(type = StepType.DELAY, delayMs = 600, label = "vong_lap"),
                AutomationStep(
                    type = StepType.IF_MATCH_GOTO,
                    templateName = "reward_chest",
                    similarityThreshold = 0.65f,
                    targetLabel = "mo_ruong"
                ),
                // Click gold
                AutomationStep(
                    type = StepType.CLICK_TEMPLATE,
                    templateName = "gold_ore",
                    similarityThreshold = 0.65f,
                    delayMs = 1200
                ),
                AutomationStep(type = StepType.GOTO, targetLabel = "vong_lap"),
                // Label "mo_ruong" to collect chest
                AutomationStep(type = StepType.LOG, textValue = "Rương xuất hiện! Thực hiện nhặt rương...", label = "mo_ruong"),
                AutomationStep(
                    type = StepType.CLICK_TEMPLATE,
                    templateName = "reward_chest",
                    similarityThreshold = 0.65f,
                    delayMs = 1000
                ),
                AutomationStep(type = StepType.GOTO, targetLabel = "vong_lap")
            )
        )

        val rpgClicker = MacroScript(
            id = 2,
            name = "⚔️ RPG Boss Auto Raider",
            description = "Bấm liên tục tại tọa độ Tâm Boss (200, 180) để hạ quái vật, dọn dẹp phụ bản cực kỳ thực tế.",
            steps = listOf(
                AutomationStep(type = StepType.LOG, textValue = "Khởi chạy kịch bản Auto Boss Clicker"),
                AutomationStep(type = StepType.DELAY, delayMs = 300, label = "combat_start"),
                // Rapid click on coordinates
                AutomationStep(type = StepType.CLICK_COORD, x1 = 200, y1 = 180, delayMs = 600),
                AutomationStep(type = StepType.CLICK_COORD, x1 = 200, y1 = 180, delayMs = 600),
                // Click Loot chest if matches, else return start
                AutomationStep(
                    type = StepType.IF_MATCH_GOTO,
                    templateName = "reward_chest",
                    similarityThreshold = 0.65f,
                    targetLabel = "claim_loot"
                ),
                AutomationStep(type = StepType.GOTO, targetLabel = "combat_start"),
                // Claim reward actions
                AutomationStep(type = StepType.LOG, textValue = "Quái chết! Thu hoạch chiến lợi phẩm", label = "claim_loot"),
                AutomationStep(type = StepType.CLICK_COORD, x1 = 200, y1 = 320, delayMs = 1200),
                AutomationStep(type = StepType.GOTO, targetLabel = "combat_start")
            )
        )

        val loginFiller = MacroScript(
            id = 3,
            name = "🔑 Auto Bypass Secure Login",
            description = "Tự động hóa toàn bộ form đăng nhập: Click Captcha -> Điền mật khẩu 9999 -> Gửi Đăng Nhập.",
            steps = listOf(
                AutomationStep(type = StepType.LOG, textValue = "Bắt đầu chu trình đăng nhập tự động..."),
                AutomationStep(type = StepType.DELAY, delayMs = 800),
                // Click check box Captcha
                AutomationStep(type = StepType.CLICK_COORD, x1 = 100, y1 = 155, delayMs = 1000),
                // Focus Text / write code
                AutomationStep(type = StepType.CLICK_COORD, x1 = 200, y1 = 220, delayMs = 800),
                AutomationStep(type = StepType.TEXT_INPUT, textValue = "9999", delayMs = 1000),
                // Click login button
                AutomationStep(type = StepType.CLICK_COORD, x1 = 200, y1 = 275, delayMs = 2000),
                AutomationStep(type = StepType.LOG, textValue = "Hoàn tất kịch bản Form auth bypass!")
            )
        )

        repository.saveMacro(goldMinerBot)
        repository.saveMacro(rpgClicker)
        repository.saveMacro(loginFiller)
    }
}
