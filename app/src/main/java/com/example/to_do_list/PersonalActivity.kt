package com.example.to_do_list

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.to_do_list.ui.theme.To_do_listTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.*

class PersonalActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            To_do_listTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF0F4F8)) {
                    PersonalScreen { finish() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PersonalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appPrefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val userPrefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }

    val firebaseTasksDb = remember { FirebaseDatabase.getInstance().getReference("tasks") }
    val firebaseUsersDb = remember { FirebaseDatabase.getInstance().getReference("users") }

    val defaultColorArgb = Color(0xFF87B5E2).toArgb()
    var currentThemeColorArgb by remember { mutableStateOf(appPrefs.getInt("theme_color_key", defaultColorArgb)) }
    val primaryColor = Color(currentThemeColorArgb)

    var pinEnabled by remember { mutableStateOf(appPrefs.getBoolean("pin_notification", true)) }
    var fullName by remember { mutableStateOf(userPrefs.getString("full_name", "Người dùng") ?: "") }
    var userEmail by remember { mutableStateOf(userPrefs.getString("user_email", "") ?: "") }
    val currentUid = remember { userPrefs.getString("username", "") ?: "" }
    var avatarUri by remember { mutableStateOf(userPrefs.getString("user_avatar_uri", "")) }

    var showThemeDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            avatarUri = it.toString()
            userPrefs.edit().putString("user_avatar_uri", it.toString()).apply()
            if (currentUid.isNotEmpty()) {
                firebaseUsersDb.child(currentUid).child("avatarUri").setValue(it.toString())
            }
        }
    }

    var weekStats by remember { mutableStateOf(0) }
    var monthStats by remember { mutableStateOf(0) }

    LaunchedEffect(currentUid) {
        if (currentUid.isEmpty()) return@LaunchedEffect
        firebaseTasksDb.orderByChild("userId").equalTo(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                for (data in snapshot.children) {
                    data.getValue(Task::class.java)?.let { tasks.add(it) }
                }
                val now = LocalDate.now()
                val currentWeek = now.get(ChronoField.ALIGNED_WEEK_OF_YEAR)

                val weekT = tasks.filter {
                    val taskDate = LocalDate.parse(it.date)
                    taskDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == currentWeek && taskDate.year == now.year
                }
                if (weekT.isNotEmpty()) weekStats = (weekT.count { it.completed } * 100) / weekT.size

                val monthT = tasks.filter {
                    val taskDate = LocalDate.parse(it.date)
                    taskDate.monthValue == now.monthValue && taskDate.year == now.year
                }
                if (monthT.isNotEmpty()) monthStats = (monthT.count { it.completed } * 100) / monthT.size
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang cá nhân", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AVATAR SECTION
            Box(
                modifier = Modifier.size(100.dp))
            {
                Box(
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUri.isNullOrEmpty()) {
                        AsyncImage(model = avatarUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(50.dp))
                    }
                }
                Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color.White).align(Alignment.BottomEnd).padding(2.dp)) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(primaryColor), contentAlignment = Alignment.Center) {
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = fullName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = userEmail, fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            // THỐNG KÊ CARD
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tuần này", color = Color.Gray, fontSize = 13.sp)
                        Text("$weekStats%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFF0F4F8)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tháng này", color = Color.Gray, fontSize = 13.sp)
                        Text("$monthStats%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DANH SÁCH TÍNH NĂNG
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column {
                    ListItem(
                        headlineContent = { Text("Thay đổi màu sắc Theme") },
                        leadingContent = { Icon(Icons.Default.Palette, null, tint = primaryColor) },
                        trailingContent = {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(primaryColor).border(1.dp, Color.LightGray, CircleShape))
                        },
                        modifier = Modifier.clickable { showThemeDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFFF0F4F8), thickness = 1.dp)

                    ListItem(
                        headlineContent = { Text("Thông báo việc quan trọng") },
                        leadingContent = { Icon(Icons.Default.NotificationsNone, null, tint = primaryColor) },
                        trailingContent = {
                            Switch(
                                checked = pinEnabled,
                                onCheckedChange = { isChecked ->
                                    pinEnabled = isChecked
                                    appPrefs.edit().putBoolean("pin_notification", isChecked).apply()

                                    if (isChecked) {
                                        val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                        val currentUid = userPrefs.getString("username", "") ?: ""

                                        if (currentUid.isNotEmpty()) {
                                            val firebaseDb = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("tasks")

                                            firebaseDb.orderByChild("userId").equalTo(currentUid)
                                                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                                                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                                                        val list = mutableListOf<Task>()
                                                        for (data in snapshot.children) {
                                                            data.getValue(Task::class.java)?.let { list.add(it) }
                                                        }
                                                        val todayStr = java.time.LocalDate.now().toString()
                                                        // Lọc các việc quan trọng có sẵn chưa hoàn thành
                                                        val importantTasks = list.filter { it.important && !it.completed && it.date == todayStr }

                                                        NotificationHelper.updatePinnedNotification(context, importantTasks)
                                                    }
                                                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                                                })
                                        }
                                    } else {
                                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        notificationManager.cancel(999)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryColor)
                            )
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF0F4F8), thickness = 1.dp)

                    ListItem(
                        headlineContent = { Text("Đăng xuất", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFD32F2F)) },
                        modifier = Modifier.clickable {
                            // Xóa thông báo ghim của người dùng hiện tại
                            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            nm.cancel(999)
                            // Xóa phiên đăng nhập và quay về màn hình đăng nhập (hàm nằm trong LoginActivity.kt)
                            logoutAndGoToLogin(context)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }

        if (showThemeDialog) {
            val colorList = listOf(
                Color(0xFF87B5E2),
                Color(0xFFE2797C),
                Color(0xFFDEA5CA),
                Color(0xFFBCDEA5),
                Color(0xFFF9F587)
            )

            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Chọn màu chủ đạo của ứng dụng", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Giao diện ứng dụng và các icon biểu tượng hệ thống sẽ tự động biến đổi theo gam màu bạn chọn.", fontSize = 14.sp, color = Color.Gray)

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colorList.forEach { targetColor ->
                                val isSelected = targetColor.toArgb() == currentThemeColorArgb
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(targetColor)
                                        .border(
                                            border = if (isSelected) BorderStroke(3.dp, Color.DarkGray) else BorderStroke(1.dp, Color.LightGray),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            val argbVal = targetColor.toArgb()
                                            currentThemeColorArgb = argbVal
                                            // Lưu cấu hình màu trực tiếp xuống thiết bị
                                            appPrefs.edit().putInt("theme_color_key", argbVal).apply()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Done, null, tint = if (targetColor == Color(0xFFF9F587)) Color.Black else Color.White)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showThemeDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Hoàn tất", color = if (primaryColor == Color(0xFFF9F587)) Color.Black else Color.White)
                    }
                }
            )
        }
    }
}