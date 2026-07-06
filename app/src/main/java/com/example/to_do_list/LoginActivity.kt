package com.example.to_do_list

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.to_do_list.ui.theme.To_do_listTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.to_do_list.R

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setContent {
            To_do_listTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF0F4F8)) {
                    LoginScreen(
                        onLoginSuccess = {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val userPrefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }
    val mAuth = remember { FirebaseAuth.getInstance() }
    val firebaseDb = remember { FirebaseDatabase.getInstance().getReference("users") }
    var isLoading by remember { mutableStateOf(false) }

    // Cấu hình Google Sign-In yêu cầu Email và ID Token từ hệ thống
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // Bộ launcher xử lý kết quả trả về sau khi người dùng chọn tài khoản Gmail
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    isLoading = true
                    // Xác thực token của Google với Firebase Auth
                    val credential = GoogleAuthProvider.getCredential(token, null)
                    mAuth.signInWithCredential(credential).addOnSuccessListener { authResult ->
                        val firebaseUser = authResult.user
                        if (firebaseUser != null) {
                            val uid = firebaseUser.uid
                            val email = firebaseUser.email ?: ""
                            val name = firebaseUser.displayName ?: "Người dùng Gmail"
                            val avatar = firebaseUser.photoUrl?.toString() ?: ""

                            val localUser = User(uid, email, name, avatar)

                            // Lưu thông tin người dùng lên Firebase Realtime DB
                            firebaseDb.child(uid).setValue(localUser).addOnSuccessListener {
                                // Lưu trạng thái đăng nhập vào thiết bị
                                userPrefs.edit().apply {
                                    putBoolean("is_logged_in", true)
                                    putString("username", uid)
                                    putString("full_name", name)
                                    putString("user_email", email)
                                    putString("user_avatar_uri", avatar)
                                    apply()
                                }
                                isLoading = false
                                Toast.makeText(context, "Đăng nhập Gmail thành công!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            }.addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "Lỗi đồng bộ dữ liệu DB!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.addOnFailureListener {
                        isLoading = false
                        Toast.makeText(context, "Xác thực Firebase thất bại: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: ApiException) {
                isLoading = false
                Toast.makeText(context, "Lỗi kết nối Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_todolist),
            contentDescription = "Logo ứng dụng TodoGo",
            modifier = Modifier
                .size(240.dp)
                .padding(vertical = 4.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF87B5E2))
        } else {
            // Nút bấm Đăng nhập bằng Google
            Button(
                onClick = {
                    googleSignInClient.signOut().addOnCompleteListener {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google Icon",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Đăng nhập bằng Google", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}