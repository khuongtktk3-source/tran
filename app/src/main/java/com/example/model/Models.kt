package com.example.model

import java.util.UUID

enum class StepType(val displayName: String, val category: String) {
    DELAY("Trễ thời gian", "Cơ bản"),
    CLICK_COORD("Nhấn tọa độ (X, Y)", "Tác vụ"),
    CLICK_TEMPLATE("Nhấn vào hình ảnh", "Hành động so khớp"),
    SWIPE("Vuốt màn hình", "Tác vụ"),
    GOTO("Nhảy đến dòng (GOTO)", "Điều hướng"),
    IF_MATCH_GOTO("Nếu khớp ảnh -> GOTO", "So khớp ảnh"),
    IF_COLOR_GOTO("Nếu khớp màu -> GOTO", "So khớp màu"),
    TEXT_INPUT("Nhập văn bản", "Tác vụ"),
    KEY_BACK("Nhấn phím BACK", "Hành động"),
    LOG("Ghi Log hệ thống", "Cơ bản")
}

data class AutomationStep(
    val id: String = UUID.randomUUID().toString(),
    var type: StepType = StepType.DELAY,
    var label: String = "", // Label for jump target (e.g. "loop_start")
    var targetLabel: String = "", // Label target for GOTO or IF declarations
    var delayMs: Long = 500,
    var x1: Int = 100,
    var y1: Int = 100,
    var x2: Int = 200,
    var y2: Int = 200,
    var colorHex: String = "#FF0000", // Hex color to compare
    var templateName: String = "", // Reference to saved TemplateImage.name
    var similarityThreshold: Float = 0.8f,
    var textValue: String = "", // Log text or text input
    var searchRegion: SearchRegion = SearchRegion() // Bounding box
)

data class SearchRegion(
    var left: Int = 0,
    var top: Int = 0,
    var right: Int = 400,
    var bottom: Int = 400,
    var useWholeScreen: Boolean = true
)

// Helper representable script model
data class MacroScript(
    val id: Int = 0,
    val name: String,
    val description: String,
    val steps: List<AutomationStep>,
    val createdAt: Long = System.currentTimeMillis()
)

data class MarketScript(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val downloads: Int,
    val rating: Float,
    val steps: List<AutomationStep>,
    val price: String = "Miễn phí",
    var isDownloaded: Boolean = false
)

