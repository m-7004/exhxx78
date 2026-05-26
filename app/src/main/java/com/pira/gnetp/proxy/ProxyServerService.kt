package com.pira.gnetp.proxy

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.pira.gnetp.MainActivity
import com.pira.gnetp.R
import com.pira.gnetp.data.LogLevel
import com.pira.gnetp.data.LogRepository
import com.pira.gnetp.data.ProxyConfig
import com.pira.gnetp.data.ProxyType
import com.pira.gnetp.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject
import com.pira.gnetp.utils.PreferenceManager

@AndroidEntryPoint
class ProxyServerService : Service() {
    @Inject
    lateinit var proxyConfig: MutableStateFlow<ProxyConfig>

    @Inject
    lateinit var logRepository: LogRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var httpServerSocket: ServerSocket? = null
    private var socks5ServerSocket: ServerSocket? = null
    private var isHttpRunning = false
    private var isSocks5Running = false
    private var clientThreads = mutableListOf<Thread>()
    
    // دروع الحماية المتقدمة
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var silentAudioTrack: AudioTrack? = null

    companion object {
        private const val TAG = "ProxyServerService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "ProxyServerChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        Logger.d(TAG, "ProxyServerService created")
        logMessage("Service created", LogLevel.INFO)
        createNotificationChannel()

        // 1. تفعيل WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GNetP::ProxyWakeLock")
        wakeLock?.acquire()

        // 2. تفعيل WifiLock (لمنع شريحة الواي فاي من النوم)
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "GNetP::ProxyWifiLock")
        wifiLock?.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d(TAG, "onStartCommand called")
        val config = proxyConfig.value

        val preferenceManager = PreferenceManager.getInstance(this)
        preferenceManager.saveProxySettings(config)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIFICATION_ID, createNotification(), 16)
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        if (config.isHttpActive || config.isSocks5Active) {
            startProxyServers(config)
            startSilentAudio() // تشغيل السحر الأسود (الصوت الصامت)
        } else {
            stopProxyServers()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        
        // استخدام START_STICKY لإعادة إحياء التطبيق تلقائياً إذا انقتل
        return START_STICKY
    }

    // ==========================================
    // السحر الأسود: الصوت الصامت لكسر حماية OEM
    // ==========================================
    private fun startSilentAudio() {
        try {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            silentAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val silence = ShortArray(bufferSize / 2)
            silentAudioTrack?.play()

            // حلقة لا نهائية تبث صمت لتوهم النظام أننا مشغل موسيقى
            serviceScope.launch {
                while (isHttpRunning || isSocks5Running) {
                    silentAudioTrack?.write(silence, 0, silence.size)
                    delay(1000) // إرسال صمت كل ثانية
                }
            }
            Logger.d(TAG, "Silent audio track started (OEM Killer Defeated!)")
        } catch (e: Exception) {
            Logger.e(TAG, "Silent audio failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(TAG, "ProxyServerService destroyed")
        logMessage("Service destroyed", LogLevel.INFO)
        
        stopProxyServers()
        
        // تحرير كل الدروع
        try { silentAudioTrack?.stop() } catch (e: Exception) {}
        try { silentAudioTrack?.release() } catch (e: Exception) {}
        if (wakeLock?.isHeld == true) wakeLock?.release()
        if (wifiLock?.isHeld == true) wifiLock?.release()
        
        serviceScope.cancel()
        
        // إعادة إحياء الخدمة فورا عند القتل (أسلوب الهاكرز)
        val restartIntent = Intent(applicationContext, ProxyServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { startForegroundService(restartIntent) } catch (e: Exception) {}
        } else {
            try { startService(restartIntent) } catch (e: Exception) {}
        }
    }

    private fun startProxyServers(config: ProxyConfig) {
        isHttpRunning = config.isHttpActive
        isSocks5Running = config.isSocks5Active
        
        serviceScope.launch {
            try {
                startServers(config)
            } catch (e: Exception) {
                Logger.e(TAG, "Error starting proxy servers", e)
            }
        }
    }

    private fun stopProxyServers() {
        isHttpRunning = false
        isSocks5Running = false

        val updatedConfig = proxyConfig.value.copy(isHttpActive = false, isSocks5Active = false)
        proxyConfig.value = updatedConfig
        PreferenceManager.getInstance(this).saveProxySettings(updatedConfig)

        clientThreads.forEach { try { it.interrupt() } catch (e: Exception) {} }
        clientThreads.clear()

        try { httpServerSocket?.close() } catch (e: Exception) {}
        httpServerSocket = null
        try { socks5ServerSocket?.close() } catch (e: Exception) {}
        socks5ServerSocket = null
    }

    private fun startServers(config: ProxyConfig) {
        if (config.isHttpEnabled) startHttpServer(config.httpPort)
        if (config.isSocks5Enabled) startSocks5Server(config.socks5Port)
    }

    private fun startHttpServer(port: Int) {
        try {
            httpServerSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", port))
            }
            Logger.i(TAG, "HTTP server started on port $port")

            serviceScope.launch {
                while (isHttpRunning) {
                    try {
                        val clientSocket = httpServerSocket?.accept() ?: continue
                        clientSocket.keepAlive = true // إبقاء السوكت حي
                        
                        val clientThread = Thread {
                            try { handleClientConnection(clientSocket, ProxyType.HTTP) }
                            catch (e: Exception) {}
                            finally { try { clientSocket.close() } catch (e: Exception) {} }
                        }
                        clientThreads.add(clientThread)
                        clientThread.start()
                    } catch (e: SocketException) { break }
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "HTTP Server Error", e)
        }
    }

    private fun startSocks5Server(port: Int) {
        try {
            socks5ServerSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", port))
            }
            Logger.i(TAG, "SOCKS5 server started on port $port")

            serviceScope.launch {
                while (isSocks5Running) {
                    try {
                        val clientSocket = socks5ServerSocket?.accept() ?: continue
                        clientSocket.keepAlive = true // إبقاء السوكت حي
                        
                        val clientThread = Thread {
                            try { handleClientConnection(clientSocket, ProxyType.SOCKS5) }
                            catch (e: Exception) {}
                            finally { try { clientSocket.close() } catch (e: Exception) {} }
                        }
                        clientThreads.add(clientThread)
                        clientThread.start()
                    } catch (e: SocketException) { break }
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "SOCKS5 Server Error", e)
        }
    }

    private fun handleClientConnection(clientSocket: Socket, proxyType: ProxyType) {
        when (proxyType) {
            ProxyType.HTTP -> handleHttpProxy(clientSocket)
            ProxyType.SOCKS5 -> handleSocks5Proxy(clientSocket)
        }
    }

    // ====== بقية دوال البروكسي (نفسها تماماً لضمان الاستقرار) ======
    private fun handleHttpProxy(clientSocket: Socket) {
        try {
            val clientInputStream = clientSocket.getInputStream()
            val reader = clientInputStream.bufferedReader()
            val requestLine = reader.readLine() ?: return
            if (requestLine.startsWith("CONNECT ")) {
                handleHttpsConnect(clientSocket, requestLine)
                return
            }
            val parts = requestLine.split(" ")
            if (parts.size < 3) return
            val method = parts[0]
            val url = parts[1]
            if (!url.startsWith("http")) {
                var host: String? = null
                var line: String?
                val headers = mutableListOf<String>()
                while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                    headers.add(line!!)
                    if (line!!.startsWith("Host:", ignoreCase = true)) host = line!!.substring(6).trim()
                }
                if (host != null) forwardHttpRequest(clientSocket, method, "http://$host$url", headers)
            } else {
                forwardHttpRequest(clientSocket, method, url, mutableListOf())
            }
        } catch (e: Exception) {}
    }

    private fun forwardHttpRequest(clientSocket: Socket, method: String, url: String, headers: MutableList<String>) {
        try {
            val urlObj = java.net.URL(url)
            val port = if (urlObj.port != -1) urlObj.port else 80
            val path = if (urlObj.path.isNullOrEmpty()) "/" else urlObj.path + if (urlObj.query != null) "?${urlObj.query}" else ""
            val targetSocket = Socket(urlObj.host, port)
            val targetOutputStream = targetSocket.getOutputStream()
            val requestBuilder = StringBuilder().append("$method $path HTTP/1.1\r\n")
            var hostHeaderSent = false
            for (header in headers) {
                if (!header.startsWith("Proxy-")) {
                    requestBuilder.append(header).append("\r\n")
                    if (header.startsWith("Host:", ignoreCase = true)) hostHeaderSent = true
                }
            }
            if (!hostHeaderSent) requestBuilder.append("Host: ${urlObj.host}\r\n")
            requestBuilder.append("Connection: close\r\n\r\n")
            targetOutputStream.write(requestBuilder.toString().toByteArray())
            targetOutputStream.flush()
            relayData(targetSocket.getInputStream(), clientSocket.getOutputStream())
            targetSocket.close()
        } catch (e: Exception) {}
    }

    private fun handleHttpsConnect(clientSocket: Socket, requestLine: String) {
        try {
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val targetParts = parts[1].split(":")
            val port = if (targetParts.size > 1) targetParts[1].toInt() else 443
            val targetSocket = Socket(targetParts[0], port)
            clientSocket.getOutputStream().apply {
                write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                flush()
            }
            relayDataBidirectional(clientSocket, targetSocket)
            targetSocket.close()
        } catch (e: Exception) {}
    }

    private fun handleSocks5Proxy(clientSocket: Socket) {
        try {
            val inputStream = DataInputStream(clientSocket.getInputStream())
            val outputStream = DataOutputStream(clientSocket.getOutputStream())
            inputStream.readFully(ByteArray(inputStream.readByte().toInt())) // تجاوز الـ Methods
            outputStream.apply { write(byteArrayOf(0x05, 0x00)); flush() }
            inputStream.readByte() // Ver
            val cmd = inputStream.readByte()
            inputStream.readByte() // Rsv
            val atyp = inputStream.readByte()
            if (cmd != 0x01.toByte()) return

            var hostname: String? = null
            when (atyp.toInt()) {
                0x01 -> { val ipv4 = ByteArray(4); inputStream.readFully(ipv4); hostname = ipv4.joinToString(".") { (it.toInt() and 0xFF).toString() } }
                0x03 -> { val domain = ByteArray(inputStream.readByte().toInt()); inputStream.readFully(domain); hostname = String(domain) }
            }
            if (hostname == null) return
            
            val portBytes = ByteArray(2)
            inputStream.readFully(portBytes)
            val port = ((portBytes[0].toInt() and 0xFF) shl 8) or (portBytes[1].toInt() and 0xFF)
            
            val targetSocket = Socket(hostname, port)
            outputStream.apply { write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)); flush() }
            relayDataBidirectional(clientSocket, targetSocket)
            targetSocket.close()
        } catch (e: Exception) {}
    }

    private fun relayDataBidirectional(clientSocket: Socket, targetSocket: Socket) {
        val t1 = Thread { try { relayData(clientSocket.getInputStream(), targetSocket.getOutputStream()) } catch (e: Exception) {} }
        val t2 = Thread { try { relayData(targetSocket.getInputStream(), clientSocket.getOutputStream()) } catch (e: Exception) {} }
        t1.start(); t2.start()
        t1.join(); t2.join()
    }

    private fun relayData(inputStream: InputStream, outputStream: OutputStream) {
        try {
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                outputStream.flush()
            }
        } catch (e: Exception) {}
    }

    private fun logMessage(message: String, level: LogLevel) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Proxy Service", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("بث الإنترنت (VPN)")
            .setContentText("الخدمة تعمل بقوة في الخلفية 🚀")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
