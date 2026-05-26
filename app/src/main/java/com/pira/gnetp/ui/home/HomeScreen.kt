package com.pira.gnetp.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

private val DarkBg = Color(0xFF121212)
private val CardBg = Color(0xFF1E1E1E)
private val PrimaryTeal = Color(0xFF00BFA5)
private val TextGray = Color(0xFFAAAAAA)

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartBothProxies: () -> Unit,
    onStopBothProxies: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHotspot: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onVpnPermissionRequest: (Intent) -> Unit,
    onSelectIpAddress: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val isRunning = uiState.isHttpProxyActive || uiState.isSocks5ProxyActive
    
    // حالة للتحكم في ظهور نافذة التعليمات
    var showInfoDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "يجب تفعيل الإشعارات لضمان عدم انقطاع البث بالخلفية!", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // شريط العنوان العلوي مع زر (عن التطبيق) في أقصى اليسار
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(48.dp)) // مساحة للموازنة
                    Text(
                        text = "بث الإنترنت (VPN)",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "عن التطبيق",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // زر الحماية من إغلاق البطارية
                Button(
                    onClick = {
                        val intent = Intent()
                        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "التطبيق محمي من الإغلاق المؤقت ✅", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🛡️ تفعيل الحماية من الإغلاق بالخلفية", color = PrimaryTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // الخطوة 1
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("الخطوة الأولى (1)", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("انقر أدناه لفتح الإعدادات، ثم قم بتفعيل (نقطة الاتصال) في جهازك وارجع للتطبيق.", color = TextGray, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_MAIN, null)
                                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                                    intent.component = android.content.ComponentName("com.android.settings", "com.android.settings.TetherSettings")
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallbackIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                                    fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(fallbackIntent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "الإعدادات", tint = Color.White, modifier = Modifier.size(24.dp).padding(end = 8.dp))
                            Text("فتح إعدادات نقطة الاتصال", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // الخطوة 2
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("الخطوة الثانية (2)", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isRunning) onStopBothProxies() else onStartBothProxies()
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) Color(0xFFD32F2F) else PrimaryTeal
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isRunning) "إيقاف البث" else "بدء البث", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // الخطوة 3
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("الخطوة الثالثة (3)", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("في إعدادات واي فاي الجهاز الآخر، اختر (تفويض يدوي) وانسخ البيانات التالية:", color = Color.White, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isRunning && uiState.availableIPs.isNotEmpty()) {
                            uiState.availableIPs.forEach { ip ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(ip))
                                            Toast.makeText(context, "تم نسخ الآيبي: $ip", Toast.LENGTH_SHORT).show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("الخادم (IP): $ip", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("المنفذ (Port): 8080", color = TextGray, fontSize = 14.sp)
                                        }
                                        Text("📋 نسخ", color = PrimaryTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Card(colors = CardDefaults.cardColors(containerColor = Color.Black), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("انتظر بدء البث لظهور قائمة العناوين...", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(90.dp))
            }

            // نافذة (عن التطبيق)
            if (showInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    containerColor = CardBg,
                    title = {
                        Text(text = "عن التطبيق والتعليمات", color = PrimaryTeal, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text("هذا التطبيق مخصص لبث اتصال الـ VPN أو الإنترنت المشفر للأجهزة الأخرى بسهولة عبر نقطة الاتصال.", color = Color.White, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("طريقة الاستخدام:", color = PrimaryTeal, fontWeight = FontWeight.Bold)
                            Text("1. قم بتشغيل نقطة الاتصال (Hotspot) من الخطوة الأولى.", color = TextGray, fontSize = 13.sp)
                            Text("2. اضغط (بدء البث) من الخطوة الثانية.", color = TextGray, fontSize = 13.sp)
                            Text("3. اتصل بشبكتك من الجهاز الآخر، ثم اذهب لإعدادات الشبكة المتصل بها وضع الإعدادات على (يدوي) وأدخل الآيبي والمنفذ.", color = TextGray, fontSize = 13.sp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("الإصدار: 1.0.4", color = TextGray, fontSize = 12.sp)
                            Text("التطوير: Developer Mohamed Adnan (m_7004)", color = PrimaryTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/exhxx78"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC))
                        ) {
                            Text("قناتنا على تليجرام", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showInfoDialog = false }) {
                            Text("إغلاق", color = TextGray)
                        }
                    }
                )
            }
        }
    }
}
