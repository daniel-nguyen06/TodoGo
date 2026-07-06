package com.example.to_do_list

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.to_do_list.ui.theme.To_do_listTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import android.app.AlarmManager
import android.app.PendingIntent
import androidx.compose.foundation.BorderStroke
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import android.net.Uri
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*

enum class FilterOption { ALL, IMPORTANT, ALPHABET, TIME }

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            To_do_listTheme {
                val context = LocalContext.current
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(context, "Bạn cần cấp quyền thông báo để nhận nhắc nhở công việc!", Toast.LENGTH_LONG).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF0F4F8)
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun scheduleTaskReminder(context: Context, task: Task) {
    if (task.time == "12:00" || task.time.isEmpty() || task.time == "Không hẹn giờ") return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, TaskReminderReceiver::class.java).apply {
        putExtra("TASK_TITLE", task.title)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, task.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val date = LocalDate.parse(task.date)
    val time = LocalTime.parse(task.time)

    val reminderDateTime = LocalDateTime.of(date, time).minusMinutes(15)
    val triggerTimeMs = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    if (triggerTimeMs > System.currentTimeMillis()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
    }
}

fun openGoogleMapsNavigation(context: Context, latLng: String) {
    if (latLng.isEmpty()) return
    val gmmIntentUri = Uri.parse("google.navigation:q=$latLng")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
    }

    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$latLng"))
        context.startActivity(webIntent)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun parseTaskWithAI(userInput: String): TaskDataAI? = withContext(Dispatchers.IO) {
    try {
        val config = generationConfig {
            responseMimeType = "application/json"
        }

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = "AQ.Ab8RN6JP7Xu7tYLYczvRgozGltXnPJZA_FA5ULReTVHOQf4jBw",
            generationConfig = config
        )

        val todayStr = LocalDate.now().toString()

        val prompt = """
            Bạn là một trợ lý ảo chuyên bóc tách thông tin công việc. 
            Hãy phân tích câu sau của người dùng: "$userInput"
            Biết rằng ngày hôm nay là: $todayStr.
            Hãy trả về một đối tượng JSON duy nhất có cấu trúc chính xác như sau:
            {
              "title": "Tên công việc cụ thể",
              "date": "Định dạng YYYY-MM-DD dựa vào câu nói (ví dụ: 'ngày mai' thì phải tự cộng thêm 1 ngày so với hôm nay)",
              "time": "Định dạng HH:mm (ví dụ: '6 giờ tối' chuyển thành '18:00'). Nếu không nhắc tới thời gian, hãy trả về 'Không hẹn giờ'",
              "locationName": "Tên địa điểm được nhắc tới nếu có, nếu không có để trống ''"
            }
        """.trimIndent()

        val response = generativeModel.generateContent(prompt)
        val jsonText = response.text ?: return@withContext null

        val jsonObject = JSONObject(jsonText)
        return@withContext TaskDataAI(
            title = jsonObject.optString("title", ""),
            date = jsonObject.optString("date", todayStr),
            time = jsonObject.optString("time", "Không hẹn giờ"),
            locationName = jsonObject.optString("locationName", "")
        )
    } catch (e: Exception) {
        e.printStackTrace()
        android.util.Log.e("GEMINI_ERROR", "Lỗi gọi AI: ${e.message}", e)
        return@withContext null
    }
}

data class TaskDataAI(
    val title: String,
    val date: String,
    val time: String,
    val locationName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val appPrefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val defaultColorArgb = remember { Color(0xFF87B5E2).toArgb() }

    var currentThemeColorArgb by remember { mutableStateOf(appPrefs.getInt("theme_color_key", defaultColorArgb)) }
    val themeColor = remember(currentThemeColorArgb) { Color(currentThemeColorArgb) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                currentThemeColorArgb = appPrefs.getInt("theme_color_key", defaultColorArgb)
            }
        })
    }
    val userPrefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }
    val currentUid = remember { userPrefs.getString("username", "") ?: "" }
    val avatarUri = remember { userPrefs.getString("user_avatar_uri", "") }

    val firebaseDb = remember { FirebaseDatabase.getInstance().getReference("tasks") }

    val currentDate = LocalDate.now()
    var selectedDate by remember { mutableStateOf(currentDate) }
    var currentMonthYear by remember { mutableStateOf(YearMonth.from(currentDate)) }

    var taskList by remember { mutableStateOf(listOf<Task>()) }
    var showBottomSheet by remember { mutableStateOf(false) }

    var isExpanded by remember { mutableStateOf(false) }
    var totalDrag by remember { mutableStateOf(0f) }

    var currentFilter by remember { mutableStateOf(FilterOption.ALL) }
    var menuExpanded by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var taskLocationName by remember(editingTask) { mutableStateOf(editingTask?.locationName ?: "") }
    var taskLocationLatLng by remember(editingTask) { mutableStateOf(editingTask?.locationLatLng ?: "") }

    val calendar = remember { Calendar.getInstance() }
    val currentTimeString = remember {
        String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }

    var taskTitle by remember(editingTask) { mutableStateOf(editingTask?.title ?: "") }
    var taskDateStr by remember(editingTask) { mutableStateOf(editingTask?.date ?: selectedDate.toString()) }
    var taskTimeStr by remember(editingTask) { mutableStateOf(editingTask?.time ?: currentTimeString) }
    var isTimeEnabled by remember(editingTask) { mutableStateOf(editingTask?.time != null && editingTask?.time != "Không hẹn giờ") }
    var isImportant by remember(editingTask) { mutableStateOf(editingTask?.important ?: false) }

    var isListening by remember { mutableStateOf(false) }
    var isAiLoading by remember { mutableStateOf(false) }

    val speechRecognizerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""

            if (spokenText.isNotEmpty()) {
                isAiLoading = true
                scope.launch {
                    val aiResult = parseTaskWithAI(spokenText)
                    if (aiResult != null) {
                        taskTitle = aiResult.title
                        taskDateStr = aiResult.date
                        taskLocationName = aiResult.locationName

                        if (aiResult.time.contains(":") && aiResult.time.length == 5) {
                            taskTimeStr = aiResult.time
                            isTimeEnabled = true
                        } else {
                            taskTimeStr = currentTimeString
                            isTimeEnabled = false
                        }

                        Toast.makeText(context, "AI đã điền thông tin, hãy kiểm tra rồi bấm Lưu!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "AI chưa bóc tách được thông tin, hãy thử lại!", Toast.LENGTH_SHORT).show()
                    }
                    isAiLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)
    }
    LaunchedEffect(currentUid) {
        if (currentUid.isEmpty()) return@LaunchedEffect
        firebaseDb.orderByChild("userId").equalTo(currentUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Task>()
                    for (data in snapshot.children) {
                        data.getValue(Task::class.java)?.let { list.add(it) }
                    }
                    taskList = list
                    val today = java.time.LocalDate.now().toString()
                    val importantTasks = list.filter { it.important && !it.completed && it.date == today }
                    NotificationHelper.updatePinnedNotification(context, importantTasks)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    val filteredTasks = taskList.filter { it.date == selectedDate.toString() }.let { list ->
        when (currentFilter) {
            FilterOption.IMPORTANT -> list.sortedByDescending { it.important }
            FilterOption.ALPHABET -> list.sortedBy { it.title.lowercase() }
            FilterOption.TIME -> list.sortedBy { it.time }
            FilterOption.ALL -> list
        }
    }

    val hasCompletedTasks = filteredTasks.any { it.completed }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    taskTitle = ""
                    taskDateStr = selectedDate.toString()
                    taskTimeStr = currentTimeString
                    isTimeEnabled = false
                    isImportant = false
                    taskLocationName = ""
                    taskLocationLatLng = ""
                    showBottomSheet = true
                },
                containerColor = themeColor,
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "Thêm công việc") }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .animateContentSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
                                if (totalDrag > 50) isExpanded = true
                                else if (totalDrag < -50) isExpanded = false
                            }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (isExpanded) {
                                currentMonthYear = currentMonthYear.minusMonths(1)
                            } else {
                                selectedDate = selectedDate.minusDays(7)
                                currentMonthYear = YearMonth.from(selectedDate)
                            }
                        }) {
                            Icon(Icons.Default.KeyboardArrowLeft, null, tint = themeColor)
                        }
                        Text(
                            text = "Tháng ${currentMonthYear.monthValue} / ${currentMonthYear.year}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        IconButton(onClick = {
                            if (isExpanded) {
                                currentMonthYear = currentMonthYear.plusMonths(1)
                            } else {
                                selectedDate = selectedDate.plusDays(7)
                                currentMonthYear = YearMonth.from(selectedDate)
                            }
                        }) {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = themeColor)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(themeColor)
                            .clickable {
                                context.startActivity(Intent(context, PersonalActivity::class.java))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isExpanded) {
                    val firstDayOfMonth = currentMonthYear.atDay(1)
                    val gridStartDate = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val datesList = List(42) { index -> gridStartDate.plusDays(index.toLong()) }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.height(240.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(datesList) { date ->
                            val isCurrentMonth = YearMonth.from(date) == currentMonthYear
                            DayCell(date = date, selectedDate = selectedDate, isCurrentMonth = isCurrentMonth, themeColor = themeColor) {
                                selectedDate = date
                                currentMonthYear = YearMonth.from(date)
                            }
                        }
                    }
                } else {
                    var horizontalDragAmount by remember { mutableStateOf(0f) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { horizontalDragAmount = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        horizontalDragAmount += dragAmount
                                    },
                                    onDragEnd = {
                                        if (horizontalDragAmount < -50) {
                                            selectedDate = selectedDate.plusDays(7)
                                            currentMonthYear = YearMonth.from(selectedDate)
                                        } else if (horizontalDragAmount > 50) {
                                            selectedDate = selectedDate.minusDays(7)
                                            currentMonthYear = YearMonth.from(selectedDate)
                                        }
                                    }
                                )
                            },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val baseDate = if (YearMonth.from(selectedDate) == currentMonthYear) selectedDate else currentMonthYear.atDay(1)
                        val firstDayOfWeek = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        val weekDates = (0..6).map { firstDayOfWeek.plusDays(it.toLong()) }

                        weekDates.forEach { date ->
                            val isCurrentMonth = YearMonth.from(date) == currentMonthYear
                            DayCell(date = date, selectedDate = selectedDate, isCurrentMonth = isCurrentMonth, themeColor = themeColor) {
                                selectedDate = date
                                currentMonthYear = YearMonth.from(date)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Danh sách công việc", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showDeleteDialog = true }, enabled = hasCompletedTasks) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa hoàn thành", tint = if (hasCompletedTasks) themeColor else Color.LightGray)
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.List, "Lọc", tint = if (currentFilter != FilterOption.ALL) themeColor else Color.Gray)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Tất cả công việc") }, onClick = { currentFilter = FilterOption.ALL; menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Check, null) })
                            DropdownMenuItem(text = { Text("Lọc mức độ quan trọng") }, onClick = { currentFilter = FilterOption.IMPORTANT; menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300)) })
                            DropdownMenuItem(text = { Text("Lọc chữ cái (A-Z)") }, onClick = { currentFilter = FilterOption.ALPHABET; menuExpanded = false }, leadingIcon = { Icon(Icons.Default.SortByAlpha, null) })
                        }
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Không có công việc nào", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(filteredTasks) { task ->
                        TaskItemRow(
                            task = task,
                            themeColor = themeColor,
                            onCheckedChange = { isChecked -> firebaseDb.child(task.id).child("completed").setValue(isChecked) },
                            onEditClick = { editingTask = task; showBottomSheet = true }
                        )
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                containerColor = Color.White,
                dragHandle = null
            ) {
                val datePickerDialog = DatePickerDialog(
                    context,
                    { _, y, m, d -> taskDateStr = LocalDate.of(y, m + 1, d).toString() },
                    LocalDate.parse(taskDateStr).year,
                    LocalDate.parse(taskDateStr).monthValue - 1,
                    LocalDate.parse(taskDateStr).dayOfMonth
                ).apply {
                    setOnShowListener {
                        getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(themeColor.toArgb())
                        getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(themeColor.toArgb())
                    }
                }

                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val timePickerDialog = TimePickerDialog(
                    context, { _, h, min -> taskTimeStr = String.format("%02d:%02d", h, min) },
                    currentHour, currentMinute, true
                ).apply {
                    setOnShowListener {
                        try {
                            val id = context.resources.getIdentifier("toggle_mode", "id", "android")
                            findViewById<android.view.View>(id)?.performClick()
                            getButton(TimePickerDialog.BUTTON_POSITIVE)?.setTextColor(themeColor.toArgb())
                            getButton(TimePickerDialog.BUTTON_NEGATIVE)?.setTextColor(themeColor.toArgb())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { datePickerDialog.show() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Chọn ngày",
                                tint = themeColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "$taskDateStr",
                            fontSize = 14.sp,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)
                    ) {
                        TextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            placeholder = { Text("Bạn định làm gì?", color = Color.Gray) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = taskLocationName,
                            onValueChange = { taskLocationName = it },
                            label = { Text("Địa điểm (Tùy chọn)") },
                            placeholder = { Text("") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = themeColor) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor
                            )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Hẹn giờ", fontSize = 14.sp, color = Color.DarkGray)
                            Spacer(Modifier.width(8.dp))

                            Switch(
                                checked = isTimeEnabled,
                                onCheckedChange = { isTimeEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = themeColor)
                            )

                            if (isTimeEnabled) {
                                IconButton(onClick = { timePickerDialog.show() }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = themeColor)
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            IconButton(
                                onClick = { isImportant = !isImportant },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isImportant) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = if (isImportant) Color(0xFFFFB300) else Color.Gray
                                )
                            }

                            if (isAiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 12.dp).size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = themeColor
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        val speechIntent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                                            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Hãy nói công việc bạn muốn thêm...")
                                        }
                                        try {
                                            isListening = true
                                            speechRecognizerLauncher.launch(speechIntent)
                                        } catch (e: Exception) {
                                            isListening = false
                                            Toast.makeText(context, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Trợ lý giọng nói AI",
                                        tint = if (isListening) Color.Red else themeColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val finalTime = if (isTimeEnabled) taskTimeStr else "Không hẹn giờ"
                                    if (taskTitle.trim().isEmpty()) return@Button

                                    if (editingTask != null) {
                                        val updatedTask = editingTask!!.copy(
                                            title = taskTitle, date = taskDateStr, time = finalTime,
                                            important = isImportant, locationName = taskLocationName, locationLatLng = taskLocationLatLng
                                        )
                                        firebaseDb.child(editingTask!!.id).setValue(updatedTask).addOnSuccessListener {
                                            if (isTimeEnabled) scheduleTaskReminder(context, updatedTask)
                                            showBottomSheet = false
                                            editingTask = null
                                        }
                                    } else {
                                        val newKey = firebaseDb.push().key ?: UUID.randomUUID().toString()
                                        val newTask = Task(
                                            id = newKey, title = taskTitle, date = taskDateStr,
                                            time = finalTime, completed = false, important = isImportant,
                                            userId = currentUid, locationName = taskLocationName, locationLatLng = taskLocationLatLng
                                        )
                                        firebaseDb.child(newKey).setValue(newTask).addOnSuccessListener {
                                            if (isTimeEnabled) scheduleTaskReminder(context, newTask)
                                            showBottomSheet = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                            ) { Text("Lưu") }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(text = "Xác nhận xóa", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = { Text(text = "Bạn có chắc chắn muốn dọn dẹp các công việc đã hoàn thành không?") },
                confirmButton = {
                    Button(
                        onClick = {
                            filteredTasks.filter { it.completed }.forEach { firebaseDb.child(it.id).removeValue() }
                            Toast.makeText(context, "Đã dọn dẹp công việc!", Toast.LENGTH_SHORT).show()
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) { Text("Xóa", color = Color.White, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) { Text("Hủy", color = Color.Black) }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun TaskItemRow(
    task: Task,
    themeColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = when {
        task.completed -> Color(0xFFF5F5F5)
        task.important -> themeColor.copy(alpha = 0.15f)
        else -> Color.White
    }
    val titleTextColor = when {
        task.completed -> Color.Gray
        task.important -> themeColor.copy(alpha = 0.9f)
        else -> Color(0xFF2C3E50)
    }
    val indicatorColor = if (task.important) themeColor else themeColor.copy(alpha = 0.5f)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.completed, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = themeColor))
            Box(modifier = Modifier.width(4.dp).height(24.dp).background(indicatorColor, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, fontSize = 16.sp, fontWeight = if (task.important) FontWeight.SemiBold else FontWeight.Medium, color = titleTextColor, textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = task.time, fontSize = 12.sp, color = Color.Gray)
                    if (task.locationName.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColor.copy(alpha = 0.1f))
                                .clickable {
                                    val destination = if (task.locationLatLng.isNotEmpty()) task.locationLatLng else task.locationName
                                    openGoogleMapsNavigation(context, destination)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = themeColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = task.locationName, fontSize = 11.sp, color = themeColor, maxLines = 1)
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.important) {
                    Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = if (task.completed) Color.LightGray else Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Chỉnh sửa", tint = themeColor)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayCell(
    date: LocalDate,
    selectedDate: LocalDate,
    isCurrentMonth: Boolean,
    themeColor: Color,
    onClick: () -> Unit
) {
    val isSelected = date == selectedDate
    val systemToday = java.time.LocalDate.now()

    val isTodayCurrent = date.dayOfMonth == systemToday.dayOfMonth && date.monthValue == systemToday.monthValue && date.year == systemToday.year

    val boxBackground = if (isSelected) themeColor else if (isTodayCurrent) Color(0xFF939598) else Color.Transparent
    val textColor = when {
        isSelected -> Color.White
        isTodayCurrent -> Color.White
        isCurrentMonth -> Color.Black
        else -> Color(0xFFC4C4C4)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(boxBackground)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "${date.dayOfMonth}", color = textColor, fontWeight = if (isSelected || isTodayCurrent) FontWeight.Bold else if (isCurrentMonth) FontWeight.Normal else FontWeight.Light)
    }
}