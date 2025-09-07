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

    public lateinit var mediaPlayer: MediaPlayer
    val time = 1300L

    private val scope = CoroutineScope(Dispatchers.Main) // Use Main dispatcher for UI updates

    fun playCount(count: Int){
        repeat(count){
            mediaPlayer.start()
            mediaPlayer.seekTo(0)
            Thread.sleep(time)
        }

    }

    fun send_command(command: String){
        scope.launch {
            withContext(Dispatchers.IO){
                try{
                    val server_ip_input = findViewById<TextView?>(R.id.server_ip_input)

                    val server_ip: String? = server_ip_input.text.toString()

                    val socket = Socket(server_ip ,4567)

                    val writer = PrintWriter(OutputStreamWriter(socket.outputStream), true)
                    val reader = BufferedReader(InputStreamReader(socket.inputStream))

                    writer.println(command)
                    val response = reader.readLine()
                    println("SERVER: $response")

                    if(response == "pong"){
                        withContext(Dispatchers.Main){
                            val client_status = findViewById<TextView?>(R.id.client_status_text)
                            client_status.text = "server ok"
                        }
                    }else{
                        var count = response.toInt()
                        for(i in 1..<(count+1)){
                            val response = reader.readLine()
                            withContext(Dispatchers.Main){
                                val client_status = findViewById<TextView?>(R.id.client_status_text)
                                client_status.text = "$response"
                            }
                            Thread.sleep(time)
                        }
                        withContext(Dispatchers.Main){
                            val client_status = findViewById<TextView?>(R.id.client_status_text)
                            client_status.text = "play ended"
                        }

                    }

                    socket.close()


                } catch (e: Exception){
                    println("ERROR: ${e.message}")
                    withContext(Dispatchers.Main){
                        val client_status = findViewById<TextView?>(R.id.client_status_text)
                        client_status.text = "not send ${e.message}"
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


        val resId = R.raw.pip2
        mediaPlayer = MediaPlayer.create(this, resId)

        val one = findViewById<Button?>(R.id.button)
        val two = findViewById<Button?>(R.id.button2)
        val three = findViewById<Button?>(R.id.button3)
        val four = findViewById<Button?>(R.id.button4)
        val five = findViewById<Button?>(R.id.button5)

        val ping_button = findViewById<Button>(R.id.ping_button)

        ping_button?.setOnClickListener {
            send_command("ping")
        }


        one?.setOnClickListener {
            //playCount(1)
            send_command("1")

        }

        two?.setOnClickListener {
            //playCount(2)
            send_command("2")
        }

        three?.setOnClickListener {
            //playCount(3)
            send_command("3")
        }

        four?.setOnClickListener {
            //playCount(4)
            send_command("4")
        }

        five?.setOnClickListener {
            //playCount(5)
            send_command("5")
        }


    }
}

