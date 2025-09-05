package com.example.e

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

import java.net.ServerSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.io.*

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.withContext

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket



class SocketServer(private val port: Int) {

    private var serverSocket: ServerSocket? = null
    private val serverScope = CoroutineScope(Dispatchers.IO)


    public lateinit var mediaPlayer: MediaPlayer
    val time = 1300L

    fun playCount(count: Int){
        repeat(count){
            mediaPlayer.start()
            mediaPlayer.seekTo(0)
            Thread.sleep(time)
        }

    }
    fun startServer(context: Context) {


        val resId = R.raw.pip
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

    fun handleServerCommand(command: String?){
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
                        println("Received from client: $message")
                        handleServerCommand(message)

                        writer.println("Server received: $message") // Echo back
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

    private lateinit var socketServer: SocketServer

    private val ioScope = CoroutineScope(Dispatchers.IO) // Create a custom scope for IO tasks

    private val scope = CoroutineScope(Dispatchers.Main) // Use Main dispatcher for UI updates

    fun print_server_info(text: String?){
        server_info.text = text
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.server);

        println("this is a test")

        socketServer = SocketServer(4567)
        socketServer.startServer(this)

        server_info = findViewById<TextView?>(R.id.server_info_text)




    }
}
