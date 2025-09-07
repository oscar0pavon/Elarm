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

import android.media.AudioAttributes
import android.media.SoundPool

import android.widget.Button


class ElarmServerService : Service() {
    private lateinit var socketServer: SocketServer
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private lateinit var pipPlayer: PipPlayer

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


       pipPlayer = PipPlayer(this)

        Thread {

            try {
                pipPlayer.playLoop()

            } catch (e: Exception) {
                println("Main Server error: ${e.message}")
            }

        }.start()


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


    val time = 1300L

   lateinit var pipPlayer: PipPlayer

    fun playCount(count: Int, writer: PrintWriter){
        var playCount = 0

        writer.println("$count")
        repeat(count){

            pipPlayer.playPip()
            playCount++
            writer.println("Playing $playCount")
            Thread.sleep(time)

        }

    }



    fun startServer(context: Context) {


        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // Or CONTENT_TYPE_GAME, etc.
            .setUsage(AudioAttributes.USAGE_MEDIA) // Or USAGE_MEDIA, etc.
            .build()

       pipPlayer = PipPlayer(context)


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
                pipPlayer.play()
                writer.println("1")
                Thread.sleep(100L)
                writer.println("Playing 1")

            }
            "2" -> playCount(2,writer)
            "3" -> playCount(3,writer)
            "4" -> playCount(4,writer)
            "5" -> playCount(5,writer)
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

                        //writer.println("Server: $message") // Echo back
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

class PipPlayer(private val context: Context){
    private var soundPool: SoundPool
    private var soundId1: Int = 0
    private var loopedSoundId: Int = 1

    private var loopedStreamId: Int = 2

    var pipSound: MediaPlayer? = null

    var loopSound: MediaPlayer? = null


    init{
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // Or CONTENT_TYPE_GAME, etc.
            .setUsage(AudioAttributes.USAGE_MEDIA) // Or USAGE_MEDIA, etc.
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4) // Set the maximum number of simultaneous streams
            .setAudioAttributes(audioAttributes)
            .build()

        soundId1 = soundPool.load(context,R.raw.pip4,1)

        loopedSoundId = soundPool.load(context,R.raw.loop_test,1)

        pipSound = MediaPlayer.create(context,R.raw.pip4)

        loopSound = MediaPlayer.create(context,R.raw.twenty_hz)
        loopSound?.isLooping = true
    }

    fun playLoopedSound(){
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == loopedSoundId && status == 0){
                loopedStreamId = soundPool.play(loopedSoundId,1.0f, 1.0f, 1, 0, 1.0f)
            }
        }
    }

    fun play(){
        soundPool.play(soundId1, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun playPip(){
        pipSound?.start()
    }

    fun playLoop(){
        loopSound?.start()
    }
}

class ServerActivity : ComponentActivity() {
    public lateinit  var server_info: TextView


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

        val serverServiceIntend = Intent(this, ElarmServerService::class.java)

        val context: Context = this
        context.startForegroundService(serverServiceIntend)


        server_info = findViewById<TextView>(R.id.server_info_text)

        val serverIp: String? = getLocalIpAddressAndroid(this)

        server_info.text = "Server started ${serverIp}"


        val pipPlayer = PipPlayer(this)


        val oneButton = findViewById<Button>(R.id.one_button)

        oneButton?.setOnClickListener {
            pipPlayer.play()
        }


    }
}
