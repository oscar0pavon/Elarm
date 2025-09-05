package com.example.e


import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import android.media.MediaPlayer
import kotlinx.coroutines.*
import android.content.Intent

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_menu);

        val client_button = findViewById<Button?>(R.id.client_button)
        val servert_button = findViewById<Button?>(R.id.server_button)

        client_button?.setOnClickListener {
            val intent = Intent(this@MainActivity, ClientActivity::class.java)
            startActivity(intent)

        }

        servert_button?.setOnClickListener {
            val intent = Intent(this@MainActivity, ServerActivity::class.java)
            startActivity(intent)

        }


    }

}

