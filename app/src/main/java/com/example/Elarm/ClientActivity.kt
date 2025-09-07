package com.example.Elarm

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity

import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket



class ClientActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main) // Use Main dispatcher for UI updates

    fun sendCommand(command: String){
        scope.launch {
            withContext(Dispatchers.IO){
                try{
                    val serverIpInput = findViewById<TextView>(R.id.server_ip_input)

                    val serverIp: String? = serverIpInput.text.toString()

                    val socket = Socket(serverIp ,4567)

                    val writer = PrintWriter(OutputStreamWriter(socket.outputStream), true)
                    val reader = BufferedReader(InputStreamReader(socket.inputStream))

                    writer.println(command)
                    val response = reader.readLine()
                    println("SERVER: $response")

                    var clientStatus: TextView

                    if(response == "pong"){
                        withContext(Dispatchers.Main){
                            clientStatus = findViewById<TextView>(R.id.client_status_text)
                            clientStatus.text = "server ok"
                        }
                    }else{
                        var count = response.toInt()
                        for(i in 1..<(count+1)){
                            val response = reader.readLine()
                            withContext(Dispatchers.Main){
                                clientStatus = findViewById<TextView>(R.id.client_status_text)
                                clientStatus.text = "$response"
                            }
                            Thread.sleep(1300L)
                        }
                        withContext(Dispatchers.Main){
                            clientStatus = findViewById<TextView>(R.id.client_status_text)
                            clientStatus.text = "play ended"
                        }

                    }

                    socket.close()


                } catch (e: Exception){
                    println("ERROR: ${e.message}")
                    withContext(Dispatchers.Main){
                        val clientStatus = findViewById<TextView>(R.id.client_status_text)
                        clientStatus.text = "not send ${e.message}"
                    }
                }


            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.client_menu);

        println("client")


        val one = findViewById<Button?>(R.id.button)
        val two = findViewById<Button?>(R.id.button2)
        val three = findViewById<Button?>(R.id.button3)
        val four = findViewById<Button?>(R.id.button4)
        val five = findViewById<Button?>(R.id.button5)

        val pingButton = findViewById<Button>(R.id.ping_button)

        pingButton?.setOnClickListener {
            sendCommand("ping")
        }


        one?.setOnClickListener {
            sendCommand("1")

        }

        two?.setOnClickListener {

            sendCommand("2")
        }

        three?.setOnClickListener {

            sendCommand("3")
        }

        four?.setOnClickListener {

            sendCommand("4")
        }

        five?.setOnClickListener {

            sendCommand("5")
        }


    }
}

