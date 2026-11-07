package com.example.to_do_list

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

/**
 * Đăng nhập bằng tên người dùng + mật khẩu (Firebase Authentication).
 *  1. Tra tên người dùng -> email trong Realtime Database (users/{tên viết thường})
 *  2. Đăng nhập Firebase Auth bằng email đó + mật khẩu
 *  3. Chỉ cho vào khi email đã được xác minh
 * Thành công: lưu "username" vào SharedPreferences "user_prefs" (khóa mà MainActivity dùng
 * để lọc công việc) rồi mở StartActivity.
 */
class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val auth = FirebaseAuth.getInstance()
        val saved = prefs.getString("username", "") ?: ""
        val user = auth.currentUser

        // Phiên hợp lệ (đã đăng nhập + đã xác minh email) -> vào thẳng StartActivity
        if (saved.isNotEmpty() && user != null && user.isEmailVerified) {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
            return
        }

        // Phiên không hợp lệ (phiên cũ, chưa xác minh...) -> dọn sạch để đăng nhập lại
        auth.signOut()
        prefs.edit().clear().apply()

        setContent {
            To_do_listTheme {
                LoginScreen(onLoggedIn = { username, email, displayName ->
                    prefs.edit()
                        .putString("username", username)
                        .putString("user_email", email)
                        .putString("full_name", displayName)
                        .apply()
                    startActivity(Intent(this, StartActivity::class.java))
                    finish()
                })
            }
        }
    }
}

/**
 * Đăng xuất: đăng xuất Firebase, xóa toàn bộ phiên đăng nhập rồi quay về màn hình đăng nhập.
 * Gọi hàm này từ nút "Đăng xuất":  logoutAndGoToLogin(context)
 */
fun logoutAndGoToLogin(context: Context) {
    FirebaseAuth.getInstance().signOut()
    context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    context.startActivity(
        Intent(context, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
}

@Composable
fun LoginScreen(onLoggedIn: (username: String, email: String, displayName: String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val themeColor = remember {
        Color(prefs.getInt("theme_color_key", Color(0xFF87B5E2).toArgb()))
    }
    val auth = remember { FirebaseAuth.getInstance() }
    val usersRef = remember { FirebaseDatabase.getInstance().getReference("users") }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    // Tài khoản đăng nhập đúng mật khẩu nhưng chưa xác minh email (để gửi lại email xác minh)
    var unverifiedUser by remember { mutableStateOf<FirebaseUser?>(null) }

    // Mở màn hình đăng ký, nhận lại thông tin để điền sẵn và nhắc xác minh email
    val registerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            username = result.data?.getStringExtra("username") ?: ""
            val mail = result.data?.getStringExtra("email") ?: ""
            password = ""
            errorMsg = null
            unverifiedUser = null
            Toast.makeText(
                context,
                "Đăng ký thành công! Email $mail đã được xác minh, hãy đăng nhập.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val fieldColors = authFieldColors(themeColor)

    fun submit() {
        if (isLoading) return
        val name = username.trim()

        if (name.isEmpty() || password.isEmpty()) {
            errorMsg = "Vui lòng nhập tên người dùng và mật khẩu."
            return
        }
        if (!isValidUsername(name)) {
            errorMsg = "Tên người dùng hoặc mật khẩu không đúng."
            return
        }

        errorMsg = null
        unverifiedUser = null
        isLoading = true
        val key = name.lowercase()

        // Bước 1: tra email từ tên người dùng
        usersRef.child(key).get()
            .addOnSuccessListener { snapshot ->
                val email = snapshot.child("email").getValue(String::class.java) ?: ""
                if (!snapshot.exists() || email.isEmpty()) {
                    isLoading = false
                    errorMsg = "Tên người dùng hoặc mật khẩu không đúng."
                    return@addOnSuccessListener
                }
                val displayName = snapshot.child("username").getValue(String::class.java) ?: name

                // Bước 2: đăng nhập Firebase Auth
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { res ->
                    isLoading = false
                    val user = res.result?.user
                    if (!res.isSuccessful || user == null) {
                        errorMsg = authErrorMessage(res.exception, loginContext = true)
                        return@addOnCompleteListener
                    }

                    // Bước 3: bắt buộc đã xác minh email
                    if (user.isEmailVerified) {
                        onLoggedIn(key, email, displayName)
                    } else {
                        unverifiedUser = user
                        errorMsg = "Email $email chưa được xác minh. Hãy bấm vào liên kết trong email đã gửi cho bạn."
                    }
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMsg = "Không thể kết nối: ${e.localizedMessage ?: "hãy kiểm tra internet."}"
            }
    }

    fun resendVerification() {
        val user = unverifiedUser ?: return
        user.sendEmailVerification().addOnCompleteListener { res ->
            if (res.isSuccessful) {
                Toast.makeText(context, "Đã gửi lại email xác minh, hãy kiểm tra hộp thư.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, authErrorMessage(res.exception), Toast.LENGTH_LONG).show()
            }
            auth.signOut()
            unverifiedUser = null
            errorMsg = null
        }
    }

    fun resetPassword() {
        val name = username.trim()
        if (!isValidUsername(name)) {
            errorMsg = "Nhập tên người dùng của bạn ở trên để nhận email đặt lại mật khẩu."
            return
        }
        errorMsg = null
        usersRef.child(name.lowercase()).child("email").get()
            .addOnSuccessListener { snap ->
                val email = snap.getValue(String::class.java) ?: ""
                if (email.isEmpty()) {
                    errorMsg = "Tên người dùng không tồn tại."
                    return@addOnSuccessListener
                }
                auth.sendPasswordResetEmail(email).addOnCompleteListener { res ->
                    if (res.isSuccessful) {
                        Toast.makeText(context, "Đã gửi email đặt lại mật khẩu tới $email.", Toast.LENGTH_LONG).show()
                    } else {
                        errorMsg = authErrorMessage(res.exception)
                    }
                }
            }
            .addOnFailureListener { e ->
                errorMsg = "Không thể kết nối: ${e.localizedMessage ?: "hãy kiểm tra internet."}"
            }
    }

    GlassBackground(themeColor = themeColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(24.dp))

            Sphere3D(color = themeColor, diameter = 96.dp) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Chào mừng trở lại",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GlassTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Đăng nhập để tiếp tục quản lý công việc",
                fontSize = 14.sp,
                color = GlassTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(28.dp), alpha = 0.5f)
                    .padding(20.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMsg = null; unverifiedUser = null },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(20.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(errorMsg!!, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                if (unverifiedUser != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Gửi lại email xác minh",
                        color = themeColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { resendVerification() }
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "Quên mật khẩu?",
                    color = themeColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { resetPassword() }
                        .padding(vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            ClayButton(
                text = if (isLoading) "Đang xử lý..." else "Đăng nhập",
                themeColor = themeColor,
                onClick = { submit() }
            )

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chưa có tài khoản? ", color = GlassTextSecondary, fontSize = 14.sp)
                Text(
                    "Đăng ký ngay",
                    color = themeColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { registerLauncher.launch(Intent(context, RegisterActivity::class.java)) }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}