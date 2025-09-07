package com.example.Elarm


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

import java.net.ServerSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

import android.net.wifi.WifiManager
import android.text.format.Formatter

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ElarmServerService : Service() {
    private lateinit var socketServer: SocketServer

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        println("Elarm service start")

        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()
        try{
            val notificationId = 1
            val notification = NotificationCompat.Builder(this, "elarm_channel_id")
                .setContentTitle("Elarm Server service")
                .setContentText("Elarm Server running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

            startForeground(notificationId, notification)

            socketServer = SocketServer(4567)
            socketServer.startServer(this)


        }catch (e: Exception){
            println("ERROR ${e.message}")
        }




        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }
}


class SocketServer(private val port: Int) {

    private var serverSocket: ServerSocket? = null
    private val serverScope = CoroutineScope(Dispatchers.IO)


    public lateinit var mediaPlayer: MediaPlayer
    val time = 1300L

    fun playCount(count: Int){
        var play_count = 0

        repeat(count){
            mediaPlayer.start()
            mediaPlayer.seekTo(0)
            play_count++
            Thread.sleep(time)
            CoroutineScope(Dispatchers.Main).launch{
                //findViewById<TextView?>(R.id.server_info_text)
            }
        }

    }



    fun startServer(context: Context) {


        val resId = R.raw.pip2
        mediaPlayer = MediaPlayer.create(context, resId)

        serverScope.launch {
            try {
                serverSocket = ServerSocket(port)
                println("Server started on port $port")

                while (true) {
                    val clientSocket = serverSocket?.accept()
                    clientSocket?.let {
                        println("Client connected: ${it.inetAddress.hostAddress}")
                        handleClient(it)
                    }
                }
            } catch (e: Exception) {
                println("Server error: ${e.message}")
            } finally {
                closeServer()
            }
        }
    }

    fun handleServerCommand(command: String?, writer: PrintWriter){
        when(command){
            "1" -> {
                Thread.sleep(100L)
                mediaPlayer.start()
                mediaPlayer.seekTo(0)
                Thread.sleep(100L)
                mediaPlayer.start()

            }
            "2" -> playCount(2)
            "3" -> playCount(3)
            "4" -> playCount(4)
            "5" -> playCount(5)
            "ping" -> {
                writer.println("pong")
            }
        }
    }

    private fun handleClient(clientSocket: Socket) {
        serverScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                    val writer = PrintWriter(clientSocket.getOutputStream(), true)

                    var message: String?
                    while (reader.readLine().also { message = it } != null) {
                        println("Client: $message")
                        handleServerCommand(message, writer)

                        writer.println("Server: $message") // Echo back
                    }
                } catch (e: Exception) {
                    println("Client handler error: ${e.message}")
                } finally {
                    clientSocket.close()
                    println("Client disconnected: ${clientSocket.inetAddress.hostAddress}")
                }
            }
        }
    }

    fun closeServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            println("Server closed.")
        } catch (e: Exception) {
            println("Error closing server: ${e.message}")
        }
    }
}

class ServerActivity : ComponentActivity() {
    public lateinit  var server_info: TextView



    private val ioScope = CoroutineScope(Dispatchers.IO) // Create a custom scope for IO tasks

    private val scope = CoroutineScope(Dispatchers.Main) // Use Main dispatcher for UI updates

    fun print_server_info(text: String?){
        server_info.text = text
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "elarm_channel_id"
            val channelName = "Elarm Channel"
            val channelDescription = "Elarm Server channel notification"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getLocalIpAddressAndroid(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.server);

        createNotificationChannel(this)
        println("created notification idf")


        val serverServiceIntend = Intent(this, ElarmServerService::class.java)

        val context: Context = this
        context.startForegroundService(serverServiceIntend)

        println("started foreground service")

        server_info = findViewById<TextView?>(R.id.server_info_text)

        val server_ip: String? = getLocalIpAddressAndroid(this)

        server_info.text = "Server started ${server_ip}"




    }
}
