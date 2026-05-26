package com.pira.gnetp.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

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
    
    var showInfoDialog by remember { mutableStateOf(false) }

    // إعدادات الأنيميشن للزر النابض
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // مؤقت تبديل النص والأيقونة كل ثانيتين
    var showIcon by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(2000)
            showIcon = !showIcon
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
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // الزر النابض (مكان الدائرة الخضراء بالصورة)
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/exhxx78"))
                                context.startActivity(intent)
                            }
                            .background(CardBg, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(targetState = showIcon, animationSpec = tween(500)) { isIcon ->
                            if (isIcon) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "قناتنا",
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = "قناتنا",
                                    color = PrimaryTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "بث الإنترنت (VPN)",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "دليل الاستخدام",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // الخطوة 1
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("الخطوة الأولى (١)", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("افتح الإعدادات من الزر أدناه، قم بتشغيل (نقطة الاتصال) في جهازك، ثم عُد إلى هنا.", color = TextGray, fontSize = 15.sp)
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
                        Text("الخطوة الثانية (٢)", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                        Text("الخطوة الثالثة (٣)", color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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

            if (showInfoDialog) {
                Dialog(
                    onDismissRequest = { showInfoDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "دليل الاستخدام الشامل",
                                color = PrimaryTeal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(text = "🌟 ميزة البث المستمر:", color = PrimaryTeal, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            Text(text = "لضمان بقاء البث نشطاً، لا تغلق التطبيق بالكامل. فقط اضغط على زر (الرئيسية / Home) في هاتفك.", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                            Text(text = "سيتحول التطبيق تلقائياً إلى نافذة مربعة عائمة (مثل فيديو اليوتيوب). يمكنك سحبها وإخفائها في حافة الشاشة لتصفح جهازك براحة تامة!", color = TextGray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
                            
                            Divider(color = Color.DarkGray, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(text = "طريقة الربط:", color = PrimaryTeal, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            Text(text = "١. افتح نقطة الاتصال عبر التطبيق وقم بتشغيلها.", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                            Text(text = "٢. عد إلى هنا واضغط على (بدء البث).", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                            Text(text = "٣. في الجهاز المستقبل، اتصل بشبكتك، ادخل الإعدادات واختر تفويض (يدوي).", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                            Text(text = "٤. انسخ الآيبي والمنفذ والصقهما هناك.", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
                            
                            Divider(color = Color.DarkGray, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(text = "المطور: Developer Mohamed Adnan (@m_7004)", color = PrimaryTeal, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            Text(text = "الإصدار: 1.0.0 (الإصدار الأول)", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 20.dp))
                            
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/exhxx78"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("قناة التيليجرام الرسمية", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            TextButton(
                                onClick = { showInfoDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إغلاق", color = TextGray, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
