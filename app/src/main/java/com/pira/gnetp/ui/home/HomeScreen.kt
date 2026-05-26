package com.pira.gnetp.ui.home

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.CompositionLocalProvider

private val DarkBg = Color(0xFF121212)
private val CardBg = Color(0xFF1E1E1E)
private val PrimaryTeal = Color(0xFF00BFA5)
private val TextGray = Color(0xFFAAAAAA)

// هنا حافظنا على كل الوايرات (البراميترات) الأصلية حتى ما ينهار التطبيق
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
    
    val currentIp = if (uiState.selectedIpAddress.isNotEmpty()) uiState.selectedIpAddress else "جاري جلب الآيبي..."
    val isRunning = uiState.isHttpProxyActive || uiState.isSocks5ProxyActive

    // إجبار التطبيق على الترتيب من اليمين لليسار (عربي أصيل)
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
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )

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
                                try {
                                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                                            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                                                super.onStarted(reservation)
                                                Toast.makeText(context, "تم تشغيل نقطة الاتصال بنجاح!", Toast.LENGTH_SHORT).show()
                                            }
                                            override fun onFailed(reason: Int) {
                                                super.onFailed(reason)
                                                Toast.makeText(context, "استخدم زر الإعدادات", Toast.LENGTH_LONG).show()
                                            }
                                        }, null)
                                    } else {
                                        Toast.makeText(context, "إصدار الأندرويد قديم، استخدم الإعدادات", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "الصلاحيات غير كافية، استخدم الإعدادات", Toast.LENGTH_LONG).show()
                                }
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
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(28.dp)
                            )
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

            // الخطوة 3
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("الخطوة الثالثة (3)", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("في الجهاز الآخر، اتبع ما يلي:", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("١. اتصل بشبكة الواي فاي الخاصة بك.", color = TextGray, fontSize = 15.sp)
                    Text("٢. اذهب لإعدادات الشبكة واختر (تفويض يدوي).", color = TextGray, fontSize = 15.sp)
                    Text("٣. أدخل البيانات التالية بدقة:", color = TextGray, fontSize = 15.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(colors = CardDefaults.cardColors(containerColor = Color.Black), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isRunning) "الخادم: $currentIp" else "انتظر بدء البث...", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(if (isRunning) "المنفذ: 8080" else "", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }
            
            // مسافة فارغة لتجنب تغطية شريط التنقل للمحتوى
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
