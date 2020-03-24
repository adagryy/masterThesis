package com.example.grycz.imageprocessor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_certificate.*

class CertificateActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate)

        val prefs = getSharedPreferences("selfSignedCertificate", Context.MODE_PRIVATE).all

        certificateBody?.setText(prefs.get("certificate").toString())

        // Handle "Zapisz" button
        btn_continue_save_certificate.setOnClickListener {
            val certificateBody: EditText? = findViewById(R.id.certificateBody)

            val editor = getSharedPreferences("selfSignedCertificate", Context.MODE_PRIVATE).edit()
            editor?.putString("certificate", certificateBody?.text.toString())
            editor?.commit()

            AppConfigurator.cert = certificateBody?.text.toString() // save new certificate in global variable

            // ...and apply it to the application
            AppConfigurator.setSelSignedCertificate()

            // After saving server certificate redirect user to "LoginActivity"
            val redirectIntent = Intent(applicationContext, StartActivity::class.java)
            redirectIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(redirectIntent)
            this.finish() // finish (kill) this activity
        }

        // Handle "czyść" button
        btn_clear_cert_content.setOnClickListener {
            val certificateBody: EditText? = findViewById(R.id.certificateBody)
            certificateBody?.setText("")
        }
    }
}
