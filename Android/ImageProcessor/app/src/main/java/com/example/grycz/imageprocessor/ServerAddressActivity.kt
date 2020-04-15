package com.example.grycz.imageprocessor

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_server_address.*

class ServerAddressActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_address)

        btn_continue.setOnClickListener { _ ->
            val addressView: TextView = findViewById(R.id._addressText) // read provided address from user
            val serverAddressText = addressView.text
            val editor = AppConfigurator.serverPreferences?.edit() // start editing
            editor?.putString("serveraddress", serverAddressText.toString()) // put address into serverPreferences
            editor?.commit()

            // After saving server address redirect user to "LoginActivity"
            val redirectIntent = Intent(applicationContext, StartActivity::class.java)
            redirectIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(redirectIntent)
            this.finish() // finish (kill) this activity
        }
    }
}
