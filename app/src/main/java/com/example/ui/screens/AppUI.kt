package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.TemplateEntity
import com.example.engine.ImageMatcher
import com.example.engine.SimulationType
import com.example.model.AutomationStep
import com.example.model.MacroScript
import com.example.model.SearchRegion
import com.example.model.StepType
import com.example.viewmodel.AutomationViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Custom Space-Slate Colors
val SpaceDarkBg = Color(0xFF0C0C10)
val SpaceCardBg = Color(0xFF13131D)
val NeonCyan = Color(0xFF00FFCC)
val NeonBlue = Color(0xFF00BFFF)
val NeonGreen = Color(0xFF39FF14)
val NeonAmber = Color(0xFFFFA500)
val SoftGrey = Color(0xFF8A8A9D)
val Tomato = Color(0xFFFC5C65)

val Color.Companion.WHITE: Color get() = Color.White
val Color.Companion.Tomato: Color get() = com.example.ui.screens.Tomato

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUI(viewModel: AutomationViewModel) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SpaceCardBg,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = viewModel.currentScreen == "home",
                    onClick = { viewModel.navigateTo("home") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
                    label = { Text("Kịch Bản", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = Color(0xFF1F2E3A)
                    )
                )
                NavigationBarItem(
                    selected = viewModel.currentScreen == "macro_runner",
                    onClick = { viewModel.navigateTo("macro_runner") },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Chạy giả lập") },
                    label = { Text("Bàn Máy", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        indicatorColor = Color(0xFF1F2E3A)
                    )
                )
                NavigationBarItem(
                    selected = viewModel.currentScreen == "template_manager",
                    onClick = { viewModel.navigateTo("template_manager") },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Thư viện ảnh") },
                    label = { Text("Thư Viện Ảnh", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonAmber,
                        selectedTextColor = NeonAmber,
                        indicatorColor = Color(0xFF1F2E3A)
                    )
                )
                NavigationBarItem(
                    selected = viewModel.currentScreen == "script_market",
                    onClick = { viewModel.navigateTo("script_market") },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Chợ Script") },
                    label = { Text("Chợ Script", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = Color(0xFF1F2E3A)
                    )
                )
                NavigationBarItem(
                    selected = viewModel.currentScreen == "security_lab",
                    onClick = { viewModel.navigateTo("security_lab") },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Bảo mật & Phòng Lab") },
                    label = { Text("Bảo Mật & Lab", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = Color(0xFF1F2E3A)
                    )
                )
            }
        },
        containerColor = SpaceDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SpaceDarkBg, Color(0xFF1A1A24))
                    )
                )
        ) {
            when (viewModel.currentScreen) {
                "home" -> HomeScreen(viewModel)
                "script_editor" -> ScriptEditorScreen(viewModel)
                "macro_runner" -> MacroRunnerScreen(viewModel)
                "template_manager" -> TemplateManagerScreen(viewModel)
                "script_market" -> ScriptMarketScreen(viewModel)
                "security_lab" -> SecurityLabScreen(viewModel)
                "screenshot_cropper" -> ScreenshotCropperScreen(viewModel)
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: AutomationViewModel) {
    val scripts by viewModel.scripts.collectAsState()
    val templates by viewModel.templates.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoMode,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp)
            )
            Column {
                Text(
                    text = "IMAGE MACRO AUTOMATION",
                    color = Color.WHITE,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Thiết lập kịch bản thông minh bằng so sánh ảnh - JBitMacro Concept",
                    color = SoftGrey,
                    fontSize = 11.sp
                )
            }
        }

        // Stats Dashboard Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF232335))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Kịch bản đã lưu", color = SoftGrey, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${scripts.size}",
                        color = NeonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(45.dp)
                        .background(Color(0xFF2C2C3E))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Hình mẫu so khớp", color = SoftGrey, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${templates.size}",
                        color = NeonAmber,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(45.dp)
                        .background(Color(0xFF2C2C3E))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Môi trường giả lập", color = SoftGrey, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "3 bản",
                        color = NeonGreen,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section Title & Create Script button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bộ kịch bản chuyên sâu",
                color = Color.WHITE,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { viewModel.createNewScript() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005F73)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("home_create_script")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.WHITE, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tạo mới", color = Color.WHITE, fontSize = 12.sp)
            }
        }

        // Scripts Stack List
        if (scripts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.LayersClear, contentDescription = null, modifier = Modifier.size(48.dp), tint = SoftGrey)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Chưa có kịch bản nào được thiết lập", color = SoftGrey, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scripts) { script ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                        border = BorderStroke(1.dp, Color(0xFF2C2C3E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = script.name,
                                    color = Color.WHITE,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = script.description,
                                    color = SoftGrey,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.FormatListNumbered,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = NeonBlue
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${script.steps.size} bước câu lệnh",
                                        color = NeonBlue,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Actions
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        viewModel.selectScript(script)
                                        viewModel.navigateTo("macro_runner")
                                    },
                                    modifier = Modifier.background(Color(0xFF0F2624), CircleShape)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Chạy", tint = NeonCyan, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.selectScript(script)
                                        viewModel.navigateTo("script_editor")
                                    },
                                    modifier = Modifier.background(Color(0xFF132030), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Sửa", tint = NeonBlue, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteScript(script.id) },
                                    modifier = Modifier.background(Color(0xFF2D161A), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Xóa", tint = Color.Tomato, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScriptEditorScreen(viewModel: AutomationViewModel) {
    val currentScript = viewModel.currentScript ?: return
    val templates by viewModel.templates.collectAsState()

    var name by remember { mutableStateOf(currentScript.name) }
    var description by remember { mutableStateOf(currentScript.description) }
    val stepsList = remember { mutableStateListOf<AutomationStep>().apply { addAll(currentScript.steps) } }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Appbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo("home") }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.WHITE)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chỉnh Sửa Script", color = Color.WHITE, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    viewModel.saveEditingScript(name, description, stepsList.toList())
                    viewModel.navigateTo("home")
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_script_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lưu Kịch Bản", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Script Meta Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
            border = BorderStroke(1.dp, Color(0xFF232335))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên Kịch Bản", color = SoftGrey) },
                    placeholder = { Text("Nhập tên kịch bản...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.WHITE,
                        unfocusedTextColor = Color.WHITE,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF2D2D44)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô Tả Chức Năng", color = SoftGrey) },
                    placeholder = { Text("Mô tả cách hoạt động của macro...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.WHITE,
                        unfocusedTextColor = Color.WHITE,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF2D2D44)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        }

        // Code Area Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Danh sách lệnh thực thi (Giống JBitMacro)",
                color = SoftGrey,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("add_step_button")
            ) {
                Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm Dòng", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Script instructions list
        if (stepsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Chưa có dòng lệnh nào được thêm. Bấm Thêm để tạo.", color = SoftGrey, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF0F0F16), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, Color(0xFF1E1E2D)), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(stepsList) { index, step ->
                    CommandStepCard(
                        index = index,
                        step = step,
                        templates = templates,
                        onEdit = { editingStepIndex = index },
                        onDelete = { stepsList.removeAt(index) },
                        onMoveUp = {
                            if (index > 0) {
                                val temp = stepsList[index]
                                stepsList[index] = stepsList[index - 1]
                                stepsList[index - 1] = temp
                            }
                        },
                        onMoveDown = {
                            if (index < stepsList.size - 1) {
                                val temp = stepsList[index]
                                stepsList[index] = stepsList[index + 1]
                                stepsList[index + 1] = temp
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal adding or editing step
    if (showAddDialog) {
        AddStepDialog(
            templates = templates,
            onDismiss = { showAddDialog = false },
            onAdd = { newStep ->
                stepsList.add(newStep)
                showAddDialog = false
            }
        )
    }

    if (editingStepIndex != null) {
        val targetIdx = editingStepIndex!!
        EditStepDetailsDialog(
            step = stepsList[targetIdx],
            templates = templates,
            onDismiss = { editingStepIndex = null },
            onSave = { updatedStep ->
                stepsList[targetIdx] = updatedStep
                editingStepIndex = null
            }
        )
    }
}

@Composable
fun CommandStepCard(
    index: Int,
    step: AutomationStep,
    templates: List<TemplateEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val itemColor = when (step.type.category) {
        "Cơ bản" -> Color(0xFF6B4E71)
        "Tác vụ" -> Color(0xFF0F4C5C)
        "So khớp ảnh" -> Color(0xFF70D6FF)
        "Hành động so khớp" -> Color(0xFF1B4965)
        else -> Color(0xFFC77DFF)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
        border = BorderStroke(1.dp, Color(0xFF2A2A3A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Line Number / Index Pointer
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Color(0xFF1E1E2E), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    color = NeonBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text Label identifier if active
            if (step.label.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF003049), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF33B5E5), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NHãn: ${step.label}",
                        color = Color(0xFF33B5E5),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Command Main representation text
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = step.type.displayName,
                        color = Color.WHITE,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(itemColor.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = step.type.category,
                            color = if (step.type.category == "So khớp ảnh" || step.type.category == "Hành động so khớp") NeonCyan else itemColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))

                // Detail parameters subtitle info
                val infoText = when (step.type) {
                    StepType.DELAY -> "Thời gian dừng: ${step.delayMs} ms"
                    StepType.CLICK_COORD -> "Tọa độ chạm: (${step.x1}, ${step.y1})"
                    StepType.CLICK_TEMPLATE -> "Nếu phát hiện ảnh khớp '${step.templateName}' (Ngưỡng: ${(step.similarityThreshold*100).toInt()}%) -> Nhấp chuột vào tâm. Độ trễ: ${step.delayMs}ms"
                    StepType.SWIPE -> "Kéo từ (${step.x1}, ${step.y1}) sang (${step.x2}, ${step.y2})"
                    StepType.GOTO -> "Chuyển nhảy vô điều kiện -> dòng Nhãn '${step.targetLabel}'"
                    StepType.IF_MATCH_GOTO -> "Nếu khớp '${step.templateName}' (>${(step.similarityThreshold*100).toInt()}%) -> Giao diện nhảy sang dòng '${step.targetLabel}'"
                    StepType.IF_COLOR_GOTO -> "Nếu màu tại (${step.x1}, ${step.y1}) khớp mã '${step.colorHex}' -> GOTO '${step.targetLabel}'"
                    StepType.TEXT_INPUT -> "Mục nhập bàn phím: '${step.textValue}'"
                    StepType.KEY_BACK -> "Mô phỏng phím Back"
                    StepType.LOG -> "Nhật ký: '${step.textValue}'"
                }

                Text(
                    text = infoText,
                    color = SoftGrey,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Up, Down, Edit, Delete Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = SoftGrey)
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = SoftGrey)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonBlue)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Tomato)
                }
            }
        }
    }
}

// Dialog selectors for adding high speed actions
@Composable
fun AddStepDialog(
    templates: List<TemplateEntity>,
    onDismiss: () -> Unit,
    onAdd: (AutomationStep) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF2C2C3E))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Thêm Dòng Lệnh Mới",
                    color = Color.WHITE,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                StepType.values().forEach { st ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                // Provide sensible default parameters based on type chosen
                                val defaultStep = AutomationStep(
                                    type = st,
                                    templateName = if (templates.isNotEmpty()) templates.first().name else "gold_ore"
                                )
                                onAdd(defaultStep)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D2C)),
                        border = BorderStroke(1.dp, Color(0xFF2F2F44))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val ic = when (st.category) {
                                "Cơ bản" -> Icons.Default.Info
                                "Tác vụ" -> Icons.Default.TouchApp
                                "So khớp ảnh" -> Icons.Default.FilterCenterFocus
                                "Hành động so khớp" -> Icons.Default.LocationSearching
                                else -> Icons.Default.TrendingFlat
                            }
                            Icon(ic, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(st.displayName, color = Color.WHITE, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(st.category, color = SoftGrey, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Specific edit parameters modal dialog
@Composable
fun EditStepDetailsDialog(
    step: AutomationStep,
    templates: List<TemplateEntity>,
    onDismiss: () -> Unit,
    onSave: (AutomationStep) -> Unit
) {
    var label by remember { mutableStateOf(step.label) }
    var targetLabel by remember { mutableStateOf(step.targetLabel) }
    var delayMs by remember { mutableStateOf(step.delayMs) }
    var x1 by remember { mutableStateOf(step.x1) }
    var y1 by remember { mutableStateOf(step.y1) }
    var x2 by remember { mutableStateOf(step.x2) }
    var y2 by remember { mutableStateOf(step.y2) }
    var colorHex by remember { mutableStateOf(step.colorHex) }
    var templateName by remember { mutableStateOf(step.templateName) }
    var similarityThreshold by remember { mutableStateOf(step.similarityThreshold) }
    var textValue by remember { mutableStateOf(step.textValue) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF2C2C3E))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tham Số Giao Diện Lệnh",
                    color = NeonCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Optional setting of step Label (for jumping back)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Đặt Nhãn cho dòng này (Tùy chọn)", color = SoftGrey) },
                    placeholder = { Text("E.g: loop_start") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.WHITE,
                        unfocusedTextColor = Color.WHITE,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF2E2E3E)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Conditionally render options based on chosen StepType
                when (step.type) {
                    StepType.DELAY -> {
                        Text("Thời gian dừng Trễ (ms): $delayMs", color = Color.WHITE, fontSize = 13.sp)
                        Slider(
                            value = delayMs.toFloat(),
                            onValueChange = { delayMs = it.toLong() },
                            valueRange = 100f..5000f,
                            steps = 49,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    StepType.CLICK_COORD -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = x1.toString(),
                                onValueChange = { x1 = it.toIntOrNull() ?: 0 },
                                label = { Text("Tọa độ X", color = SoftGrey) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.WHITE, unfocusedBorderColor = Color(0xFF2E2E3E)),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = y1.toString(),
                                onValueChange = { y1 = it.toIntOrNull() ?: 0 },
                                label = { Text("Tọa độ Y", color = SoftGrey) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.WHITE, unfocusedBorderColor = Color(0xFF2E2E3E)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Độ trễ sau click (ms): $delayMs", color = Color.WHITE, fontSize = 13.sp)
                        Slider(
                            value = delayMs.toFloat(),
                            onValueChange = { delayMs = it.toLong() },
                            valueRange = 100f..3000f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan)
                        )
                    }

                    StepType.CLICK_TEMPLATE -> {
                        // Choose Template dropdown simulated as choice buttons
                        Text("Chọn hình ảnh đối soát học máy:", color = Color.WHITE, fontSize = 13.sp)
                        if (templates.isEmpty()) {
                            Text("Thư viện mẫu trống! Tạo ảnh mẫu trước tại tab Thư Viện.", color = Color.Tomato, fontSize = 11.sp)
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .background(Color(0xFF1B1B29), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(templates) { tmp ->
                                        val isSelected = templateName == tmp.name
                                        val borderCol = if (isSelected) NeonCyan else Color.Transparent
                                        Card(
                                            modifier = Modifier
                                                .width(100.dp)
                                                .fillMaxHeight()
                                                .clickable { templateName = tmp.name },
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF27273F)),
                                            border = BorderStroke(2.dp, borderCol)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                // Mini image thumbnail preview
                                                val bmp = ImageMatcher.base64ToBitmap(tmp.imageBase64)
                                                if (bmp != null) {
                                                    Image(
                                                        bitmap = bmp.asImageBitmap(),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(3.dp))
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(tmp.name, color = Color.WHITE, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Match sensitivity
                        Text("Ngưỡng so khớp ảnh chuẩn: ${(similarityThreshold * 100).toInt()}%", color = Color.WHITE, fontSize = 13.sp)
                        Slider(
                            value = similarityThreshold,
                            onValueChange = { similarityThreshold = it },
                            valueRange = 0.5f..0.95f,
                            colors = SliderDefaults.colors(thumbColor = NeonAmber)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Độ trễ sau hành vi (ms): $delayMs", color = Color.WHITE, fontSize = 13.sp)
                        Slider(
                            value = delayMs.toFloat(),
                            onValueChange = { delayMs = it.toLong() },
                            valueRange = 100f..3000f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan)
                        )
                    }

                    StepType.SWIPE -> {
                        Text("Điểm khởi đầu Touch và Điểm Kết thúc Kéo:", color = Color.WHITE, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = x1.toString(),
                                onValueChange = { x1 = it.toIntOrNull() ?: 0 },
                                label = { Text("X1", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = y1.toString(),
                                onValueChange = { y1 = it.toIntOrNull() ?: 0 },
                                label = { Text("Y1", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = x2.toString(),
                                onValueChange = { x2 = it.toIntOrNull() ?: 0 },
                                label = { Text("X2", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = y2.toString(),
                                onValueChange = { y2 = it.toIntOrNull() ?: 0 },
                                label = { Text("Y2", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    StepType.GOTO -> {
                        OutlinedTextField(
                            value = targetLabel,
                            onValueChange = { targetLabel = it },
                            label = { Text("Nhảy đến dòng Nhãn", color = SoftGrey) },
                            placeholder = { Text("Nhập nhãn muốn nhảy tới...") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.WHITE, unfocusedBorderColor = Color(0xFF2E2E3E)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    StepType.IF_MATCH_GOTO -> {
                        Text("Nếu khớp ảnh sau:", color = Color.WHITE, fontSize = 13.sp)
                        // Dropdown choice
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .background(Color(0xFF1B1B29), RoundedCornerShape(6.dp))
                                .padding(4.dp)
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(templates) { tmp ->
                                    val isSelected = templateName == tmp.name
                                    val borderCol = if (isSelected) NeonCyan else Color.Transparent
                                    Button(
                                        onClick = { templateName = tmp.name },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27273F)),
                                        border = BorderStroke(1.dp, borderCol),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        Text(tmp.name, fontSize = 11.sp, color = Color.WHITE)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = targetLabel,
                            onValueChange = { targetLabel = it },
                            label = { Text("Thì GOTO đến Nhãn", color = SoftGrey) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.WHITE, unfocusedBorderColor = Color(0xFF2E2E3E)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Ngưỡng khớp: ${(similarityThreshold * 100).toInt()}%", color = Color.WHITE, fontSize = 13.sp)
                        Slider(
                            value = similarityThreshold,
                            onValueChange = { similarityThreshold = it },
                            valueRange = 0.5f..0.95f
                        )
                    }

                    StepType.IF_COLOR_GOTO -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = x1.toString(),
                                onValueChange = { x1 = it.toIntOrNull() ?: 0 },
                                label = { Text("Khớp tại X", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = y1.toString(),
                                onValueChange = { y1 = it.toIntOrNull() ?: 0 },
                                label = { Text("Khớp tại Y", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = colorHex,
                            onValueChange = { colorHex = it },
                            label = { Text("Trùng với Mã màu HEX", color = SoftGrey) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = targetLabel,
                            onValueChange = { targetLabel = it },
                            label = { Text("Thì nhảy nhảy đến nhãn", color = SoftGrey) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    StepType.TEXT_INPUT -> {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = { Text("Nội dung viết chữ", color = SoftGrey) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.WHITE, unfocusedBorderColor = Color(0xFF2E2E3E)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    StepType.LOG -> {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = { Text("Lời kịch bản debug in log", color = SoftGrey) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.WHITE, unfocusedBorderColor = Color(0xFF2E2E3E)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    StepType.KEY_BACK -> {
                        Text("Chạy lệnh Back để thoát giao diện hoặc đóng modal ảo.", color = SoftGrey, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions Save/Cancel
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Hủy", color = Color.White)
                    }
                    Button(
                        onClick = {
                            val updated = step.copy(
                                label = label,
                                targetLabel = targetLabel,
                                delayMs = delayMs,
                                x1 = x1,
                                y1 = y1,
                                x2 = x2,
                                y2 = y2,
                                colorHex = colorHex,
                                templateName = templateName,
                                similarityThreshold = similarityThreshold,
                                textValue = textValue
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lưu Lại", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MacroRunnerScreen(viewModel: AutomationViewModel) {
    val activeScript = viewModel.currentScript
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to latest log on log state update
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // App header bar on runner panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo("home") }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.WHITE)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = activeScript?.name ?: "Bàn Chạy Tổng Hợp",
                    color = Color.WHITE,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Environments dropdown selection
            Box {
                var showSimSelect by remember { mutableStateOf(false) }
                Button(
                    onClick = { showSimSelect = !showSimSelect },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C3E)),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(viewModel.simulationType.displayName.split(" ")[0], color = Color.WHITE, fontSize = 11.sp)
                }

                DropdownMenu(
                    expanded = showSimSelect,
                    onDismissRequest = { showSimSelect = false },
                    modifier = Modifier.background(SpaceCardBg)
                ) {
                    SimulationType.values().forEach { st ->
                        DropdownMenuItem(
                            text = { Text(st.displayName, color = Color.WHITE, fontSize = 12.sp) },
                            onClick = {
                                viewModel.changeSimulation(st)
                                showSimSelect = false
                            }
                        )
                    }
                }
            }
        }

        // Accessibility Service Real-Device Link Indicator Row
        val localContext = androidx.compose.ui.platform.LocalContext.current
        val isServiceConnected = com.example.engine.MacroAccessibilityService.isServiceActive

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        localContext.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                            localContext.startActivity(intent)
                        } catch (e2: Exception) {}
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceConnected) Color(0xFF10271A) else Color(0xFF1E140C)
            ),
            border = BorderStroke(1.dp, if (isServiceConnected) NeonGreen.copy(alpha = 0.5f) else NeonAmber.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isServiceConnected) NeonGreen else NeonAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isServiceConnected) 
                            "CẤU HÌNH MÁY THẬT: ĐÃ ĐỒNG BỘ HOẠT ĐỘNG" 
                            else "CHẾ ĐỘ MÔ PHỎNG (Chưa cấp Hỗ trợ để bấm máy thật - Click liên kết ngay)",
                        color = if (isServiceConnected) Color.White else SoftGrey,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = if (isServiceConnected) "BẬT" else "LIÊN KẾT NGAY",
                    color = if (isServiceConnected) NeonGreen else NeonAmber,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Layout divide: Sim screen canvas + active code highlights
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Screen simulation Card (400x400 source bitmap)
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(2.dp, Color(0xFF1F1F27))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
                    Text(
                        "MÀN HÌNH MÁY ẢO",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Virtual Screen canvas element Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    // Map local touch bounds from display pixel container to standard 400x400 coordinate
                                    val sizeX = size.width
                                    val sizeY = size.height

                                    val pctX = offset.x / sizeX
                                    val pctY = offset.y / sizeY

                                    val virtualX = (pctX * 400).roundToInt()
                                    val virtualY = (pctY * 400).roundToInt()

                                    if (viewModel.bubbleRecordingStep) {
                                        viewModel.recordBubbleTap(virtualX, virtualY)
                                    } else {
                                        viewModel.tapSimulationDirectly(virtualX, virtualY)
                                    }
                                }
                            }
                    ) {
                        viewModel.simScreenBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Bản đồ giả định",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Render Active Draggable Floating Bubble Overlay inside coordinates frame
                        if (viewModel.isBubbleVisible) {
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = viewModel.bubbleX.dp, y = viewModel.bubbleY.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(NeonCyan, Color(0xFF008D73))
                                        )
                                    )
                                    .clickable {
                                        viewModel.isBubbleExpanded = !viewModel.isBubbleExpanded
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (viewModel.isBubbleExpanded) Icons.Default.Close else Icons.Default.AutoMode,
                                    contentDescription = "Bong bóng điều khiển",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (viewModel.isBubbleExpanded) {
                                Card(
                                    modifier = Modifier
                                        .absoluteOffset(
                                            x = (viewModel.bubbleX + 48).coerceAtMost(220f).dp, 
                                            y = viewModel.bubbleY.dp
                                        )
                                        .width(150.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFA14141E)),
                                    border = BorderStroke(1.dp, NeonCyan),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "BÓNG PHÁT SƠN",
                                            color = NeonCyan,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )

                                        HorizontalDivider(color = Color(0xFF232335), thickness = 1.dp)

                                        // Play / Stop Macro Run Trigger
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (viewModel.isExecuting) {
                                                        viewModel.stopExecution()
                                                    } else {
                                                        viewModel.startExecution()
                                                    }
                                                }
                                                .padding(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (viewModel.isExecuting) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (viewModel.isExecuting) Tomato else NeonGreen
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (viewModel.isExecuting) "Dừng Script" else "Chạy Script",
                                                color = Color.White,
                                                fontSize = 9.sp
                                            )
                                        }

                                        // Record click coordinate option
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.bubbleRecordingStep = !viewModel.bubbleRecordingStep
                                                }
                                                .background(if (viewModel.bubbleRecordingStep) Color(0xFF2C1010) else Color.Transparent)
                                                .padding(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.TouchApp,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (viewModel.bubbleRecordingStep) NeonAmber else SoftGrey
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (viewModel.bubbleRecordingStep) "Ấn màn hình dính" else "Ghim Toạ Độ",
                                                color = if (viewModel.bubbleRecordingStep) NeonAmber else Color.WHITE,
                                                fontSize = 9.sp
                                            )
                                        }

                                        HorizontalDivider(color = Color(0xFF232335), thickness = 1.dp)

                                        Text(
                                            text = if (viewModel.isExecuting) "Trạng thái: Đang chạy" else "Trạng thái: Nghỉ",
                                            color = SoftGrey,
                                            fontSize = 8.sp,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }
                        }

                        // Drawing live matched bounding targets on visual overlays
                        viewModel.targetFoundRect?.let { match ->
                            // Scale 400x400 matching bounds on display overlay
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val sX = maxWidth.value
                                val sY = maxHeight.value

                                val widthPct = match.width.toFloat() / 400f
                                val heightPct = match.height.toFloat() / 400f

                                val leftPct = (match.x - match.width/2).toFloat() / 400f
                                val topPct = (match.y - match.height/2).toFloat() / 400f

                                val fLeft = (leftPct * sX).dp
                                val fTop = (topPct * sY).dp
                                val fWidth = (widthPct * sX).dp
                                val fHeight = (heightPct * sY).dp

                                Box(
                                    modifier = Modifier
                                        .absoluteOffset(x = fLeft, y = fTop)
                                        .size(width = fWidth, height = fHeight)
                                        .border(BorderStroke(2.dp, NeonCyan), RoundedCornerShape(4.dp))
                                        .drawBehind {
                                            drawRect(
                                                color = NeonCyan.copy(alpha = 0.25f),
                                                size = size
                                            )
                                        }
                                ) {
                                    Text(
                                        text = "${(match.similarity*100).toInt()}%",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(NeonCyan)
                                            .padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }

                        // Drawing active simulation touches ripple highlights
                        viewModel.touchRippleTarget?.let { ripple ->
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val sX = maxWidth.value
                                val sY = maxHeight.value

                                val leftPct = ripple.first.toFloat() / 400f
                                val topPct = ripple.second.toFloat() / 400f

                                val fL = (leftPct * sX).dp - 15.dp
                                val fT = (topPct * sY).dp - 15.dp

                                Box(
                                    modifier = Modifier
                                        .absoluteOffset(x = fL, y = fT)
                                        .size(30.dp)
                                        .border(BorderStroke(2.dp, NeonBlue), CircleShape)
                                )
                            }
                        }

                        // Visual overlay for cropping custom anchor templates from simulated screen
                        if (viewModel.showCropOverlay) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .testTag("crop_overlay")
                            ) {
                                val sX = maxWidth.value
                                val sY = maxHeight.value

                                // Map coordinates percentage to display box dimensions
                                val cL = (viewModel.cropLeft.toFloat() / 400f * sX).dp
                                val cT = (viewModel.cropTop.toFloat() / 400f * sY).dp
                                val cW = ((viewModel.cropRight - viewModel.cropLeft).toFloat() / 400f * sX).dp
                                val cH = ((viewModel.cropBottom - viewModel.cropTop).toFloat() / 400f * sY).dp

                                Box(
                                    modifier = Modifier
                                        .absoluteOffset(x = cL, y = cT)
                                        .size(width = cW, height = cH)
                                        .border(BorderStroke(2.dp, NeonAmber), RoundedCornerShape(4.dp))
                                ) {
                                    Text(
                                        text = "Vùng Crop",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(NeonAmber)
                                            .padding(horizontal = 3.dp)
                                            .align(Alignment.TopStart)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Buttons to control Crop Template modal overlay
                    if (!viewModel.showCropOverlay) {
                        Button(
                            onClick = { viewModel.showCropOverlay = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4226)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chụp Lưu Mẫu", fontSize = 11.sp, color = Color.White)
                        }
                    } else {
                        // Slider adjustments inside Crop Tool
                        Column(modifier = Modifier.background(SpaceCardBg).padding(6.dp).clip(RoundedCornerShape(6.dp))) {
                            Text("Đặt rãnh tọa độ chụp mẫu:", color = NeonAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("L/T (${viewModel.cropLeft}, ${viewModel.cropTop})", color = Color.White, fontSize = 9.sp, modifier = Modifier.width(80.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Slider(
                                    value = viewModel.cropLeft.toFloat(),
                                    onValueChange = {
                                        viewModel.cropLeft = it.toInt()
                                        if (viewModel.cropRight <= viewModel.cropLeft) viewModel.cropRight = viewModel.cropLeft + 20
                                    },
                                    valueRange = 0f..350f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Hộp R/B (${viewModel.cropRight}, ${viewModel.cropBottom})", color = Color.White, fontSize = 9.sp, modifier = Modifier.width(80.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Slider(
                                    value = viewModel.cropRight.toFloat(),
                                    onValueChange = {
                                        viewModel.cropRight = it.toInt()
                                        if (viewModel.cropLeft >= viewModel.cropRight) viewModel.cropLeft = viewModel.cropRight - 20
                                    },
                                    valueRange = 50f..400f,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Input name dialog triggers
                            var croppedName by remember { mutableStateOf("sample_pattern") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedTextField(
                                    value = croppedName,
                                    onValueChange = { croppedName = it },
                                    label = { Text("Tên ảnh mẫu", fontSize = 8.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.WHITE),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = { viewModel.cropAndSaveTemplate(croppedName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("LƯU", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.showCropOverlay = false },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("HỦY", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Running Steps flow tracking stack list (live highlight)
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                border = BorderStroke(1.dp, Color(0xFF2C2C3E))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "DÒNG LỆNH HIỆN TẠI",
                        color = NeonBlue,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (activeScript == null || activeScript.steps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Chưa chọn kịch bản.", color = SoftGrey, fontSize = 11.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(activeScript.steps) { index, st ->
                                val isActive = viewModel.activeStepIndex == index
                                val borderCol = if (isActive) NeonCyan else Color.Transparent
                                val textCol = if (isActive) Color.White else SoftGrey

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isActive) Color(0xFF1E2E3A) else Color(0xFF11111A),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(4.dp))
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pointer play arrow
                                    if (isActive) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(11.dp).padding(end = 4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            color = SoftGrey.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(14.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Column {
                                        Text(st.type.displayName, color = textCol, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        if (st.label.isNotEmpty()) {
                                            Text("Nhãn: ${st.label}", color = NeonBlue, fontSize = 8.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions Control panel (Play / Pause / Stop)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
            border = BorderStroke(1.dp, Color(0xFF2C2C3E))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play and Stop buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.startExecution() },
                        enabled = !viewModel.isExecuting,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("run_script_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BẮT ĐẦU", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.stopExecution() },
                        enabled = viewModel.isExecuting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Tomato),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DỪNG LẠI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Restart simulation
                Button(
                    onClick = { viewModel.resetSimulation() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E3E)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tải Lại Giả Lập", color = Color.White, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Toggle Floating Bubble Overlay
                val bubbleContext = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = { viewModel.toggleBubbleOverlay(bubbleContext) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.isBubbleVisible) Color(0xFF0F3A41) else Color(0xFF2E2E3E)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (viewModel.isBubbleVisible) Icons.Filled.Close else Icons.Filled.Circle,
                        contentDescription = "Bong bóng nổi",
                        modifier = Modifier.size(14.dp),
                        tint = if (viewModel.isBubbleVisible) NeonCyan else Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (viewModel.isBubbleVisible) "Tắt Bong Bóng" else "Bật Bong Bóng",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Developer terminal console outputs logs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF07070A)),
            border = BorderStroke(1.dp, Color(0xFF1B1B29))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "BẢNG LOGS THỰC THI (CLI)",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.clearLogs() }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.ClearAll, contentDescription = "Xóa logs", tint = SoftGrey, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nhật ký rỗng. Hãy bấm kịch bản chạy thử.", color = SoftGrey.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logs) { log ->
                            // Custom text color based on log contents
                            val col = when {
                                log.contains("Khớp") || log.contains("THÀNH CÔNG") -> NeonGreen
                                log.contains("Lỗi") || log.contains("Thất Bại") -> Color.Tomato
                                log.contains("Bấm") || log.contains("Click") -> NeonBlue
                                log.contains("Dừng") -> NeonAmber
                                else -> Color.White
                            }
                            Text(
                                text = ">> $log",
                                color = col,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateManagerScreen(viewModel: AutomationViewModel) {
    val templates by viewModel.templates.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Thư Viện Ảnh Báo Mẫu",
            color = Color.WHITE,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Nạp và quản lý các ảnh con để so khớp mẫu trong JBitMacro.",
            color = SoftGrey,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = { viewModel.navigateTo("screenshot_cropper") },
            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.Crop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("TỰ CẮT ẢNH MẪU HOÀN TOÀN TỰ ĐỘNG", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Filled.ImageNotSupported, contentDescription = null, modifier = Modifier.size(48.dp), tint = SoftGrey)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Danh sách ảnh trống!",
                        color = Color.WHITE,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Vào mục Bàn Máy (Run Simulation), chọn Chụp Lưu Mẫu để cắt lấy các đối tượng (như rương, quái, vàng) trực tiếp từ màn hình!",
                        color = SoftGrey,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(templates) { tmp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                        border = BorderStroke(1.dp, Color(0xFF2C2C3E))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Bitmap preview
                            val bmp = remember(tmp.imageBase64) {
                                ImageMatcher.base64ToBitmap(tmp.imageBase64)
                            }

                            if (bmp != null) {
                                Box(
                                    modifier = Modifier
                                        .size(65.dp)
                                        .background(Color(0xFF0F0F15), RoundedCornerShape(6.dp))
                                        .border(BorderStroke(1.dp, Color(0xFF3C3C56)), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = tmp.name,
                                        modifier = Modifier.size(45.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = tmp.name,
                                color = Color.WHITE,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "Kích thước: ${tmp.width}x${tmp.height}",
                                color = SoftGrey,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            IconButton(
                                onClick = { viewModel.deleteTemplate(tmp.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .background(Color(0xFF2D1616), RoundedCornerShape(4.dp))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Tomato)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xóa Mẫu", color = Color.Tomato, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompareLabScreen(viewModel: AutomationViewModel) {
    val templates by viewModel.templates.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Phòng Thí Nghiệm Đối Soát Ảnh",
            color = Color.WHITE,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Kiểm thử trực quan thuật toán so khớp JBitMacro trước khi thêm vào logic kịch bản thực tế.",
            color = SoftGrey,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Selected screen image card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, Color(0xFF232333))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Live Screen output snapshot
                viewModel.simScreenBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Drawing bounding target match overlays in Lab
                viewModel.labMatchCoords?.let { coords ->
                    viewModel.labSelectedTemplate?.let { t ->
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            // Calculate display offsets based inside Box
                            val scaleX = maxWidth.value
                            val scaleY = maxHeight.value

                            val widthPct = t.width.toFloat() / 400f
                            val heightPct = t.height.toFloat() / 400f

                            val lPct = (coords.first - t.width / 2).toFloat() / 400f
                            val tPct = (coords.second - t.height / 2).toFloat() / 400f

                            val fL = (lPct * scaleX).dp
                            val fT = (tPct * scaleY).dp
                            val fW = (widthPct * scaleX).dp
                            val fH = (heightPct * scaleY).dp

                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = fL, y = fT)
                                    .size(width = fW, height = fH)
                                    .border(BorderStroke(2.dp, NeonGreen), RoundedCornerShape(3.dp))
                                    .drawBehind { drawRect(color = NeonGreen.copy(alpha = 0.3f)) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lab controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
            border = BorderStroke(1.dp, Color(0xFF2C2C3E))
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Cấu hình kiểm định:", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(10.dp))

                // Select Template
                Text("1. Chọn ảnh mẫu con:", color = Color.White, fontSize = 12.sp)
                if (templates.isEmpty()) {
                    Text("Không có ảnh mẫu con. Hãy tạo ở mục Bàn Máy.", color = Color.Tomato, fontSize = 11.sp)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0F16), RoundedCornerShape(6.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LazyRow(modifier = Modifier.fillMaxWidth()) {
                            items(templates) { tmp ->
                                val selected = viewModel.labSelectedTemplate?.name == tmp.name
                                val borderCol = if (selected) NeonGreen else Color.Transparent
                                Button(
                                    onClick = { viewModel.labSelectedTemplate = tmp },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C3F)),
                                    border = BorderStroke(1.dp, borderCol),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(tmp.name, color = Color.WHITE, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Threshold slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("2. Ngưỡng khớp mong muốn:", color = Color.White, fontSize = 12.sp)
                    Text("${(viewModel.labThreshold * 100).toInt()}%", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = viewModel.labThreshold,
                    onValueChange = { viewModel.labThreshold = it },
                    valueRange = 0.5f..0.95f
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Trigger Test button
                Button(
                    onClick = { viewModel.runLabTest() },
                    enabled = viewModel.labSelectedTemplate != null,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CHẠY THỬ ĐỐI SOÁT", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Output Result Analysis
                Text("PHÂN TÍCH KẾT QUẢ:", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                viewModel.labSimilarityResult?.let { sim ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(NeonGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "THÀNH CÔNG: Tìm thấy tại (${viewModel.labMatchCoords?.first}, ${viewModel.labMatchCoords?.second}) với tỉ lệ ${"%.1f".format(sim * 100)}%",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } ?: run {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Tomato, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CHƯA PHÁT HIỆN ĐỐI TƯỢNG TRÊN KHUNG HÌNH VỚI NGƯỠNG ĐÃ ĐẶT",
                            color = Color.Tomato,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScriptMarketScreen(viewModel: AutomationViewModel) {
    val scripts by viewModel.scripts.collectAsState()
    val marketScripts by viewModel.marketScripts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Market Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 8.dp)
            )
            Column {
                Text(
                    text = "CHỢ SCRIPT CỘNG ĐỒNG",
                    color = Color.WHITE,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Tải xuống và phát hành kịch bản tự động hóa tối ưu",
                    color = SoftGrey,
                    fontSize = 11.sp
                )
            }
        }

        // Share CTA Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101B24)),
            border = BorderStroke(1.dp, Color(0xFF183849)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF192C3A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Kiếm tiền và chia sẻ Macro?", color = Color.WHITE, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Nơi quy tụ hàng nghìn script bọc game an toàn chuẩn xáo trộn.", color = SoftGrey, fontSize = 10.sp)
                }
            }
        }

        Text(
            text = "KỊCH BẢN ĐỀ XUẤT NỔI BẬT (HOT)",
            color = NeonCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Render List of script catalog from server database
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            marketScripts.forEach { script ->
                val plainSearchName = script.name
                    .replace("🔥 ", "")
                    .replace("⚔️ ", "")
                    .replace("🛡️ ", "")
                    .replace("🎁 ", "")
                    .replace("🚀 ", "")
                    .trim()
                val localExists = scripts.any { it.name.contains(plainSearchName) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                    border = BorderStroke(1.dp, Color(0xFF232335)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = script.name,
                                color = Color.WHITE,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Custom Badge to sidestep experimental M3 elements
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (localExists) Color(0xFF1B3224) else Color(0xFF1F2E3A))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (localExists) "Đã cài" else "Có sẵn",
                                    color = if (localExists) NeonGreen else NeonCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = script.description,
                            color = SoftGrey,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("${script.rating}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(Icons.Default.Person, contentDescription = null, tint = SoftGrey, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(script.author, color = SoftGrey, fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("⬇️ ${script.downloads}", color = SoftGrey, fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    if (!localExists) {
                                        viewModel.downloadMarketScript(script)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (localExists) Color(0xFF1B3224) else Color(0xFF005F73)
                                ),
                                enabled = !localExists,
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (localExists) "ĐÃ TẢI" else "TẢI VỀ",
                                    color = if (localExists) NeonGreen else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color(0xFF232335), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Share personal scripts
        Text(
            text = "PHÁT HÀNH TÁC PHẨM CỦA BẠN",
            color = NeonGreen,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Chọn kịch bản máy bạn tự thiết lập bên dưới để ký số và chia sẻ công khai.",
            color = SoftGrey,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (scripts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceCardBg, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Vui lòng ghim kịch bản của riêng bạn trước!", color = SoftGrey, fontSize = 11.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                scripts.forEach { localScript ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpaceCardBg, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFF1E1E2D)), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(localScript.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${localScript.steps.size} tác vụ", color = SoftGrey, fontSize = 10.sp)
                        }

                        Button(
                            onClick = { viewModel.publishLocalScriptToMarket(localScript) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("ĐĂNG BÁN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityLabScreen(viewModel: AutomationViewModel) {
    val securityLogs by viewModel.securityLogs.collectAsState()
    val logListState = rememberLazyListState()

    LaunchedEffect(securityLogs.size) {
        if (securityLogs.isNotEmpty()) {
            logListState.animateScrollToItem(securityLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Security Center Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 8.dp)
            )
            Column {
                Text(
                    text = "BẢO MẬT & CHỐNG REPACK",
                    color = Color.WHITE,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Hệ thống tự vệ, chống dịch ngược mã nguồn & can thiệp RAM ứng dụng",
                    color = SoftGrey,
                    fontSize = 10.sp
                )
            }
        }

        // Active Shield Configuration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
            border = BorderStroke(1.dp, Color(0xFF232335)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "CHỈ SỐ AN TOÀN TẬP TIN APK",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 1. R8/ProGuard Obfuscation Status
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Xáo trộn Bytecode R8/ProGuard", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Mã hóa tên Class/Method sang a, b, c chống dịch ngược ngược JADX.", color = SoftGrey, fontSize = 9.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF142D20))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🛡️ ĐÃ KÍCH HOẠT",
                            color = NeonGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E1E2D), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                // 2. Original APK Signature verification
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ký số Toàn Vẹn Tệp (Anti-Repackage)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Ngăn chặn kẻ xấu bung APK, đè mã độc rồi đóng gói lại.", color = SoftGrey, fontSize = 9.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (viewModel.isAppSignatureValid) Color(0xFF142D20) else Color(0xFF331414))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (viewModel.isAppSignatureValid) "✅ NGUYÊN BẢN" else "❌ PHÁT HIỆN MOD",
                            color = if (viewModel.isAppSignatureValid) NeonGreen else Color.Red,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (viewModel.activeSignatureSha256.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C12)),
                        border = BorderStroke(1.dp, Color(0xFF1C1C28))
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                "MÃ CHỮ KÝ APK HIỆN TẠI (SHA-256):",
                                color = NeonAmber,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = viewModel.activeSignatureSha256,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E1E2D), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                // 3. App Cloner Protection (Virtual Sandbox)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chặn Không gian ảo / App Cloner", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Phát hiện chạy lồng trong Parallel Space, VirtualApp.", color = SoftGrey, fontSize = 9.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (viewModel.isSandboxCloned) Color(0xFF331414) else Color(0xFF142D20))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (viewModel.isSandboxCloned) "⚠️ PHÁT HIỆN ẢO" else "✅ ĐỘC LẬP OK",
                            color = if (viewModel.isSandboxCloned) Color.Red else NeonGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E1E2D), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                // 4. Su Binary & Privilege Checks (Root Detection)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quét quyền quản trị nâng cao (Root Block)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Chặn bộ nhớ máy bị can thiệp trực tiếp bởi CheatEngine/GameGuardian.", color = SoftGrey, fontSize = 9.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (viewModel.isRootDetected) Color(0xFF331414) else Color(0xFF142D20))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (viewModel.isRootDetected) "⚠️ TRUY CẬP ROOT" else "🛡️ KERNEL SẠCH",
                            color = if (viewModel.isRootDetected) Color.Red else NeonGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E1E2D), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                // 5. Anti-Debugging (PTrace attachments & flag debuggable)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chống Gỡ Lỗi JDWP / GDB (Anti-Debugger)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Tự phát hiện TracerPid khi hacker liên kết IDA Pro / GDB.", color = SoftGrey, fontSize = 9.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (viewModel.isDebuggerHookProtected) Color(0xFF142D20) else Color(0xFF331414))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (viewModel.isDebuggerHookProtected) "🛡️ ĐÃ KHÓA" else "⚠️ BỊ KHAI THÁC",
                            color = if (viewModel.isDebuggerHookProtected) NeonGreen else Color.Red,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E1E2D), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                // 6. Anti-Hooking memory check (Frida / Xposed)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Phòng thủ liên kết linker /proc/self/maps (Anti-Frida)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Quét bộ nhớ đệm tiến trình, tránh tiêm mã độc vào RAM dynamic.", color = SoftGrey, fontSize = 9.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (viewModel.isFridaDetected) Color(0xFF331414) else Color(0xFF142D20))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (viewModel.isFridaDetected) "⚠️ HOOKED" else "🛡️ AN TOÀN",
                            color = if (viewModel.isFridaDetected) Color.Red else NeonGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E1E2D), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                // 7. Virtual Machine Evasion (Anti-Emulator)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nhận dạng thiết bị vật lý thật (Anti-Emulator)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Chặn bot chạy tự động hàng loạt trên trình giả lập PC.", color = SoftGrey, fontSize = 9.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (viewModel.isEmulatorDetected) Color(0xFF331414) else Color(0xFF142D20))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (viewModel.isEmulatorDetected) "⚠️ MÁY ẢO (${viewModel.emulatorConfidenceScore}%)" else "✅ ĐT VẬT LÝ",
                            color = if (viewModel.isEmulatorDetected) Color.Red else NeonGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Diagnostic Console and Meter Indicators Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Safety indicator donut-like meter
            Card(
                modifier = Modifier.weight(1f).height(160.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                border = BorderStroke(1.dp, Color(0xFF232335))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "ĐỘ AN TOÀN APP",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .drawBehind {
                                drawArc(
                                    color = Color(0xFF1E1E2D),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.value)
                                )
                                drawArc(
                                    color = if (viewModel.complianceScore > 80) NeonGreen else if (viewModel.complianceScore > 50) NeonAmber else Tomato,
                                    startAngle = -90f,
                                    sweepAngle = (viewModel.complianceScore.toFloat() / 100f) * 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 5.dp.value,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            }
                    ) {
                        Text(
                            text = "${viewModel.complianceScore}%",
                            color = if (viewModel.complianceScore > 80) NeonGreen else if (viewModel.complianceScore > 50) NeonAmber else Tomato,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.triggerSecurityScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(4.dp),
                        enabled = !viewModel.isSecurityScanning,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (viewModel.isSecurityScanning) "ĐANG QUÉT APP..." else "KIỂM ĐỊNH APP",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Real System Log console Block
            Card(
                modifier = Modifier.weight(1.3f).height(160.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, Color(0xFF132B1B))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "ANTI-REVERSE CONSOLE ENGINE",
                        color = NeonGreen,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (securityLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Chuỗi kiểm tra chữ ký SHA256, TracerPID và maps nhị phân tĩnh chưa khởi chạy. Hãy bấm nút Kiểm Định App bên cạnh.",
                                color = SoftGrey,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 11.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            state = logListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            items(securityLogs) { logMsg ->
                                Text(
                                    text = logMsg,
                                    color = if (logMsg.contains("✅")) NeonGreen else if (logMsg.contains("❌") || logMsg.contains("⚠️")) NeonAmber else Color.White,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF1E1E2D), thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))

        // Compare Lab screen layout container
        Text(
            text = "SO KHỚP TRỰC QUAN (COMPARE LAB)",
            color = Color.WHITE,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp)
                .background(SpaceCardBg, RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, Color(0xFF232335)), RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            CompareLabScreen(viewModel)
        }
    }
}

@Composable
fun ScreenshotCropperScreen(viewModel: AutomationViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var inputName by remember { mutableStateOf("") }
    var selectCustomUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    // Choose selected screenshot frame
    val activeBmp = remember(viewModel.cropperScreenshotType, selectCustomUri) {
        if (viewModel.cropperScreenshotType == "custom" && selectCustomUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(selectCustomUri!!)
                android.graphics.BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                viewModel.generateMockScreenshot("rpg_game")
            }
        } else {
            viewModel.generateMockScreenshot(viewModel.cropperScreenshotType)
        }
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectCustomUri = uri
            viewModel.cropperScreenshotType = "custom"
        }
    }

    // Coerce crop parameters inside chosen bitmap dimensions
    val maxW = activeBmp.width.toFloat()
    val maxH = activeBmp.height.toFloat()

    val cropL = viewModel.cropperLeft.coerceIn(0f, maxW - 20f)
    val cropT = viewModel.cropperTop.coerceIn(0f, maxH - 20f)
    val cropW = viewModel.cropperWidth.coerceIn(20f, maxW - cropL)
    val cropH = viewModel.cropperHeight.coerceIn(20f, maxH - cropT)

    // Generate current live crop preview bitmap
    val liveCroppedBmp = remember(activeBmp, cropL, cropT, cropW, cropH) {
        try {
            android.graphics.Bitmap.createBitmap(
                activeBmp, 
                cropL.toInt(), 
                cropT.toInt(), 
                cropW.toInt(), 
                cropH.toInt()
            )
        } catch (e: Exception) {
            activeBmp
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Back and Title Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo("template_manager") }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Trang chủ", tint = Color.WHITE)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "BÀN TỰ CẮT ẢNH CHỤP MÀN HÌNH",
                    color = Color.WHITE,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Lập trình so khớp mẫu bằng cách tự kéo cắt rương, quái, nút bấm vật lý",
                    color = SoftGrey,
                    fontSize = 11.sp
                )
            }
        }

        // Section A: Source Select Options Cards
        Text(
            text = "BƯỚC 1: LỰA CHỌN ẢNH CHỤP MÀN HÌNH",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "rpg_game" to "🎮 Game RPG",
                "gold_miner" to "👑 Đào Vàng",
                "secure_login" to "🔒 Định Danh Form",
                "custom" to "📁 Tải file máy"
            ).forEach { (typeKey, labelStr) ->
                val selected = viewModel.cropperScreenshotType == typeKey
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (typeKey == "custom") {
                                launcher.launch("image/*")
                            } else {
                                viewModel.cropperScreenshotType = typeKey
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Color(0xFF132D28) else SpaceCardBg
                    ),
                    border = BorderStroke(1.dp, if (selected) NeonGreen else Color(0xFF232335))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelStr,
                            color = if (selected) Color.White else SoftGrey,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Section B: Bounding Box Mask Preview
        Text(
            text = "BƯỚC 2: KÉO KHUNG ĐỂ CẮT ĐỐI TƯỢNG SO KHỚP",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color.Black, RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, Color(0xFF232335)), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Render full screenshot behind active Orange glowing crop border overlay
            Box(
                modifier = Modifier
                    .size(230.dp, 260.dp)
            ) {
                Image(
                    bitmap = activeBmp.asImageBitmap(),
                    contentDescription = "Bản đồ nguồn",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // Render orange crop outline overlay programmatically sized inside the image container bounds
                val normalizedLeft = (cropL / maxW) * 230
                val normalizedTop = (cropT / maxH) * 260
                val normalizedWidth = (cropW / maxW) * 230
                val normalizedHeight = (cropH / maxH) * 260

                Box(
                    modifier = Modifier
                        .absoluteOffset(x = normalizedLeft.dp, y = normalizedTop.dp)
                        .size(normalizedWidth.dp, normalizedHeight.dp)
                        .border(BorderStroke(2.dp, NeonAmber), RoundedCornerShape(2.dp))
                        .background(Color.Yellow.copy(alpha = 0.15f))
                ) {
                    // Small size overlay label
                    Text(
                        text = "VÙNG CẮT",
                        color = NeonAmber,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(Color.Black)
                            .padding(horizontal = 2.dp)
                            .align(Alignment.TopStart)
                    )
                }
            }
        }

        // Section C: Sliders to Adjust Geometry
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "⚙️ ĐIỀU CHỈNH KÍCH THƯỚC KHUNG",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Slider X
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trái (X): ${cropL.toInt()}px", color = SoftGrey, fontSize = 10.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = cropL,
                        onValueChange = { viewModel.cropperLeft = it },
                        valueRange = 0f..(maxW - 20f),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = NeonAmber, activeTrackColor = NeonAmber)
                    )
                }

                // Slider Y
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trên (Y): ${cropT.toInt()}px", color = SoftGrey, fontSize = 10.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = cropT,
                        onValueChange = { viewModel.cropperTop = it },
                        valueRange = 0f..(maxH - 20f),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = NeonAmber, activeTrackColor = NeonAmber)
                    )
                }

                // Slider Width
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rộng (W): ${cropW.toInt()}px", color = SoftGrey, fontSize = 10.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = cropW,
                        onValueChange = { viewModel.cropperWidth = it },
                        valueRange = 20f..(maxW - cropL),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }

                // Slider Height
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cao (H): ${cropH.toInt()}px", color = SoftGrey, fontSize = 10.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = cropH,
                        onValueChange = { viewModel.cropperHeight = it },
                        valueRange = 20f..(maxH - cropT),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }
            }
        }

        // Section D: Visual Live Snippet & Save Registry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cropped Live Thumbnail preview
            Card(
                modifier = Modifier
                    .size(90.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, NeonAmber)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = liveCroppedBmp.asImageBitmap(),
                        contentDescription = "Live Crop Preview",
                        modifier = Modifier.size(70.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Input fields and submission Button
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Tên Ảnh Mẫu (vd: nut_vong)") },
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF232335)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (inputName.trim().isNotEmpty()) {
                            viewModel.saveCroppedCustomTemplate(inputName, liveCroppedBmp)
                            inputName = ""
                            viewModel.navigateTo("template_manager")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LƯU TRỮ VÀO THƯ VIỆN", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

