package com.pira.gnetp.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
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

@SuppressLint("MissingPermission")
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
    
    // دالة طلب صلاحية الموقع للهوتسبوت
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    wifiManager.startLocalOnlyHotspot(null, null)
                    Toast.makeText(context, "تم تشغيل نقطة الاتصال!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "استخدم زر الإعدادات اليدوي", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "يجب إعطاء صلاحية الموقع لتشغيل الهوتسبوت برمجياً", Toast.LENGTH_LONG).show()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Text(
                text = "بث الإنترنت (VPN)",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            // زر حماية التطبيق من الإغلاق بالخلفية
            Button(
                onClick = {
                    val intent = Intent()
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "التطبيق محمي بالفعل من الإغلاق بالخلفية ✅", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).height(45.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🛡️ حماية البث من الإغلاق بالخلفية", color = PrimaryTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    Text("قم بتشغيل نقطة الاتصال في جهازك لتتمكن الأجهزة الأخرى من الاتصال بك.", color = TextGray, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            },
                            modifier = Modifier.weight(1f).height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تشغيل (مباشر)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_MAIN, null)
                                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                                    intent.component = android.content.ComponentName("com.android.settings", "com.android.settings.TetherSettings")
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallbackIntent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                                    fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(fallbackIntent)
                                }
                            },
                            modifier = Modifier.size(55.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "الإعدادات", tint = PrimaryTeal, modifier = Modifier.size(28.dp))
                        }
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

            // الخطوة 3 (الأيبيات والنسخ)
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("الخطوة الثالثة (3)", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("في الجهاز الآخر، اختر (تفويض يدوي) وأدخل أحد هذه العناوين:", color = Color.White, fontSize = 15.sp)
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
                                Text("انتظر بدء البث لظهور العناوين...", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
