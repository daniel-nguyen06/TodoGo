package com.example.to_do_list

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.to_do_list.ui.theme.To_do_listTheme
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.delay

/**
 * Đăng ký tài khoản (Firebase Authentication) chỉ hoàn tất SAU KHI email được xác minh:
 *  1. Nhập email, tên người dùng, mật khẩu, xác nhận mật khẩu
 *  2. App tạo tài khoản Auth và gửi email xác minh, rồi hiện màn hình chờ
 *  3. App tự kiểm tra mỗi 3 giây; khi người dùng bấm liên kết trong email thì mới lưu
 *     tên người dùng vào Realtime Database (users/{tên viết thường}) và chuyển về đăng nhập
 *  4. Người dùng hủy/thoát giữa chừng: tài khoản chưa xác minh bị xóa, không giữ chỗ tên
 */
class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Mở màn hình mới (không phải xoay màn hình): dọn tài khoản chưa xác minh còn sót lại
        if (savedInstanceState == null) {
            val auth = FirebaseAuth.getInstance()
            auth.currentUser?.let { if (!it.isEmailVerified) it.delete() }
            auth.signOut()
        }

        setContent {
            To_do_listTheme {
                RegisterScreen(
                    onRegistered = { username, email ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra("username", username).putExtra("email", email)
                        )
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Thoát hẳn khi đang chờ xác minh: xóa tài khoản chưa xác minh
        if (isFinishing) {
            val auth = FirebaseAuth.getInstance()
            auth.currentUser?.let { if (!it.isEmailVerified) it.delete() }
            auth.signOut()
        }
    }
}

// ---------- Tiện ích dùng chung cho Login & Register ----------

/** Tên người dùng: 3-20 ký tự, chỉ gồm chữ không dấu, số và dấu gạch dưới. */
fun isValidUsername(name: String) = Regex("^[A-Za-z0-9_]{3,20}$").matches(name)

/** Đổi lỗi của Firebase Auth sang tiếng Việt. loginContext = true thì gộp lỗi sai tài khoản/mật khẩu. */
fun authErrorMessage(e: Exception?, loginContext: Boolean = false): String {
    if (e is FirebaseNetworkException) return "Không có kết nối mạng, hãy kiểm tra lại internet."
    val code = (e as? FirebaseAuthException)?.errorCode
    return when (code) {
        "ERROR_INVALID_EMAIL" -> "Email không hợp lệ."
        "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" ->
            if (loginContext) "Tên người dùng hoặc mật khẩu không đúng." else "Thông tin không hợp lệ."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Email này đã được đăng ký, hãy dùng email khác."
        "ERROR_WEAK_PASSWORD" -> "Mật khẩu quá yếu (tối thiểu 6 ký tự)."
        "ERROR_USER_DISABLED" -> "Tài khoản đã bị vô hiệu hóa."
        "ERROR_TOO_MANY_REQUESTS" -> "Bạn thử quá nhiều lần, vui lòng đợi một lúc rồi thử lại."
        else -> e?.localizedMessage ?: "Đã có lỗi xảy ra, hãy thử lại."
    }
}

@Composable
fun authFieldColors(themeColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = themeColor,
    focusedLabelColor = themeColor,
    unfocusedBorderColor = Color.White.copy(alpha = 0.9f),
    focusedContainerColor = Color.White.copy(alpha = 0.55f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.35f),
    focusedTextColor = GlassTextPrimary,
    unfocusedTextColor = GlassTextPrimary,
    cursorColor = themeColor
)

// ---------- Màn hình đăng ký ----------
@Composable
fun RegisterScreen(onRegistered: (username: String, email: String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val themeColor = remember {
        Color(prefs.getInt("theme_color_key", Color(0xFF87B5E2).toArgb()))
    }
    val auth = remember { FirebaseAuth.getInstance() }
    val usersRef = remember { FirebaseDatabase.getInstance().getReference("users") }

    var email by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Trạng thái chờ xác minh (rỗng = đang ở form). Dùng rememberSaveable để không mất khi xoay màn hình.
    var waitingKey by rememberSaveable { mutableStateOf("") }
    var waitingName by rememberSaveable { mutableStateOf("") }
    var waitingMail by rememberSaveable { mutableStateOf("") }
    var claiming by remember { mutableStateOf(false) }
    var lastResendAt by remember { mutableStateOf(0L) }
    val waiting = waitingKey.isNotEmpty()

    val fieldColors = authFieldColors(themeColor)

    fun resetWaiting() {
        waitingKey = ""
        waitingName = ""
        waitingMail = ""
        claiming = false
    }

    // Hủy đăng ký đang chờ: xóa tài khoản chưa xác minh để không giữ chỗ email/tên
    fun cancelPending() {
        auth.currentUser?.let { if (!it.isEmailVerified) it.delete() }
        auth.signOut()
        resetWaiting()
    }

    // Email đã xác minh -> giữ chỗ tên người dùng trong database rồi hoàn tất
    fun claimUsername(user: FirebaseUser) {
        val key = waitingKey
        val name = waitingName
        val mail = waitingMail
        usersRef.child(key).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value != null) return Transaction.abort()
                currentData.value = mapOf(
                    "username" to name,
                    "email" to mail,
                    "uid" to user.uid,
                    "createdAt" to System.currentTimeMillis()
                )
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                android.util.Log.d("REGISTER", "Lưu database: committed=$committed, lỗi=${error?.message}")
                if (error != null || !committed) {
                    user.delete()
                    auth.signOut()
                    resetWaiting()
                    errorMsg = if (error != null)
                        "Không thể lưu tài khoản: ${error.message}"
                    else
                        "Tên người dùng này vừa có người khác đăng ký, hãy chọn tên khác."
                    return
                }
                auth.signOut()
                onRegistered(key, mail)
            }
        })
    }

    // Tự kiểm tra xác minh email mỗi 3 giây khi đang chờ
    LaunchedEffect(waiting) {
        while (waiting) {
            delay(3000)
            val u = auth.currentUser ?: break
            u.reload().addOnCompleteListener { r ->
                val cu = auth.currentUser
                if (r.isSuccessful && cu != null && cu.isEmailVerified && !claiming && waitingKey.isNotEmpty()) {
                    claiming = true
                    claimUsername(cu)
                }
            }
        }
    }

    BackHandler(enabled = waiting) { cancelPending() }

    fun submit() {
        if (isLoading) return
        val mail = email.trim().lowercase()
        val name = username.trim()

        when {
            !Patterns.EMAIL_ADDRESS.matcher(mail).matches() ->
                errorMsg = "Vui lòng nhập email hợp lệ (ví dụ: ten@gmail.com)."
            !isValidUsername(name) ->
                errorMsg = "Tên người dùng gồm 3-20 ký tự: chữ không dấu, số hoặc dấu gạch dưới."
            password.length < 6 ->
                errorMsg = "Mật khẩu phải có ít nhất 6 ký tự."
            password != confirmPassword ->
                errorMsg = "Mật khẩu xác nhận không khớp."
            else -> {
                errorMsg = null
                isLoading = true
                val key = name.lowercase()

                // Bước 1: kiểm tra tên người dùng đã có người dùng chưa
                usersRef.child(key).get()
                    .addOnSuccessListener { snap ->
                        android.util.Log.d("REGISTER", "Bước 1 OK - tên người dùng đã tồn tại: ${snap.exists()}")
                        if (snap.exists()) {
                            isLoading = false
                            errorMsg = "Tên người dùng này đã tồn tại, hãy chọn tên khác."
                            return@addOnSuccessListener
                        }

                        // Bước 2: tạo tài khoản Auth (Firebase tự chặn email trùng)
                        auth.createUserWithEmailAndPassword(mail, password)
                            .addOnCompleteListener { res ->
                                val user = res.result?.user
                                android.util.Log.d("REGISTER", "Bước 2 - tạo Auth thành công: ${res.isSuccessful}", res.exception)
                                if (!res.isSuccessful || user == null) {
                                    isLoading = false
                                    errorMsg = authErrorMessage(res.exception)
                                    return@addOnCompleteListener
                                }

                                // Bước 3: gửi email xác minh rồi chuyển sang màn hình chờ
                                user.sendEmailVerification().addOnCompleteListener { sendRes ->
                                    isLoading = false
                                    if (!sendRes.isSuccessful) {
                                        android.util.Log.e("REGISTER", "Gửi email xác minh lỗi", sendRes.exception)
                                        user.delete()
                                        auth.signOut()
                                        errorMsg = "Không gửi được email xác minh: " +
                                                authErrorMessage(sendRes.exception)
                                        return@addOnCompleteListener
                                    }
                                    lastResendAt = System.currentTimeMillis()
                                    waitingKey = key
                                    waitingName = name
                                    waitingMail = mail
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("REGISTER", "Bước 1 lỗi - không đọc được database", e)
                        isLoading = false
                        errorMsg = "Không thể kết nối: ${e.localizedMessage ?: "hãy kiểm tra internet."}"
                    }
            }
        }
    }

    GlassBackground(themeColor = themeColor) {
        if (waiting) {
            // ===== Màn hình chờ xác minh email =====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Sphere3D(color = themeColor, diameter = 96.dp) {
                    Icon(Icons.Default.Email, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Xác minh email của bạn",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GlassTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(28.dp), alpha = 0.5f)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Chúng tôi đã gửi email xác minh tới",
                        fontSize = 14.sp,
                        color = GlassTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        waitingMail,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Hãy mở hộp thư (cả mục Spam) và bấm vào liên kết xác minh. " +
                                "Giữ màn hình này mở, app sẽ tự chuyển tiếp khi bạn xác minh xong.",
                        fontSize = 14.sp,
                        color = GlassTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = themeColor
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Đang chờ xác minh...", fontSize = 13.sp, color = GlassTextSecondary)

                    if (errorMsg != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(errorMsg!!, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Gửi lại email xác minh",
                    color = themeColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastResendAt < 30_000) {
                                Toast.makeText(context, "Vui lòng đợi 30 giây rồi gửi lại.", Toast.LENGTH_SHORT).show()
                            } else {
                                auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { r ->
                                    lastResendAt = System.currentTimeMillis()
                                    val msg = if (r.isSuccessful) "Đã gửi lại email xác minh."
                                    else authErrorMessage(r.exception)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        .padding(vertical = 6.dp)
                )

                Text(
                    "Hủy đăng ký",
                    color = GlassTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { cancelPending() }
                        .padding(vertical = 6.dp)
                )
            }
        } else {
            // ===== Form đăng ký =====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Quay lại", tint = themeColor, modifier = Modifier.size(32.dp))
                    }
                }

                Sphere3D(color = themeColor, diameter = 84.dp) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    "Tạo tài khoản",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GlassTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bạn cần xác minh email để hoàn tất đăng ký",
                    fontSize = 14.sp,
                    color = GlassTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(28.dp), alpha = 0.5f)
                        .padding(20.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = null },
                        label = { Text("Email") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = themeColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMsg = null },
                        label = { Text("Tên người dùng") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = themeColor) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = { Text("Mật khẩu") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = themeColor) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    "Hiện/ẩn mật khẩu",
                                    tint = GlassTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMsg = null },
                        label = { Text("Xác nhận mật khẩu") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = themeColor) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(errorMsg!!, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(24.dp))

                ClayButton(
                    text = if (isLoading) "Đang xử lý..." else "Đăng ký",
                    themeColor = themeColor,
                    onClick = { submit() }
                )

                Spacer(Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Đã có tài khoản? ", color = GlassTextSecondary, fontSize = 14.sp)
                    Text(
                        "Đăng nhập",
                        color = themeColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}