package com.example.grycz.imageprocessor

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_nav.*
import kotlinx.android.synthetic.main.app_bar_nav.*
import android.content.Intent
import android.os.BatteryManager
import kotlinx.android.synthetic.main.content_nav.*
import java.security.MessageDigest
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.support.v4.content.LocalBroadcastManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast


class NavActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val RequestImageFromCamera = 1
    private val RequestPickImageFromGallery = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        setSupportActionBar(toolbar)

//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show()
//        }

        receiving_activity.setOnClickListener{view ->
            val intent = Intent(this, SendImageActivity::class.java)
            intent.putExtra("napis", "test")
            startActivity(intent)
//            dispatchTakePictureIntent()
        }

        sending_activity.setOnClickListener { view ->
            val intent = Intent(this, ReceiveImageActivity::class.java)
            intent.putExtra("napis", "test")
            startActivity(intent)
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.title = "Start"

        nav_view.setNavigationItemSelectedListener(this)


        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                IntentFilter("processingFinished"))

        startService(Intent(this, ProcessingStatus::class.java))
    }

    // Broadcast receiver
    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent

//            Toast.makeText(applicationContext, intent.getStringExtra("responseCode"), Toast.LENGTH_SHORT).show()

            val view: TextView = this@NavActivity.findViewById(R.id.progress_title)

            when (intent.getStringExtra("responseCode")){
                "200" ->  {
                    changeButtonsState(true)
                    view.text = "Status przetwarzania: \nZakończono"
                }
                "400" ->  {
                    changeButtonsState(false)
                    view.text = "Status przetwarzania: \nBłędne żądanie"
                }
                "404" ->  {
                    changeButtonsState(false)
                    view.text = "Status przetwarzania: \nW toku"
                }
                "error" -> {
                    view.text = "Status przetwarzania: \n" + intent.getStringExtra("errorMessage")
                }
            }
        }
    }

    private fun changeButtonsState(state: Boolean){
        val sendButton: Button = this@NavActivity.findViewById(R.id.receiving_activity)
        val downloadButton: Button = this@NavActivity.findViewById(R.id.sending_activity)

        sendButton.isEnabled = state
        downloadButton.isEnabled = state
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.nav, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_send_image -> {
                val intent = Intent(this, SendImageActivity::class.java)
                intent.putExtra("napis", "test")
                startActivity(intent)
            }
            R.id.nav_receive_image -> {
                val intent = Intent(this, ReceiveImageActivity::class.java)
                intent.putExtra("napis", "test")
                startActivity(intent)
            }
            R.id.nav_exit_app -> {
                finish()
                System.exit(0)
            }
            R.id.logOff -> {
                finish()
                val editor = AppConfigurator.sharedpreferences?.edit()
                editor?.clear()
                editor?.commit()

                AppConfigurator.cookieManager = null

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun getSHA_256sumOfString(s: String) : StringBuilder{
        var messageDigest =  MessageDigest.getInstance("SHA-256")

        messageDigest.update(s.toByteArray(Charsets.UTF_8))

        var hash: ByteArray = messageDigest.digest()
        val sb = StringBuilder()

        for (b in hash) {
            sb.append(Integer.toString((b.toInt() and 0xff) + 0x100, 16).substring(1))
        }
        return sb
    }

    private fun diagnostics(){

        val cookieStore = AppConfigurator.cookieManager

        for (eachCookie in cookieStore?.cookieStore!!.cookies)
            println(( eachCookie.name + ",   " + eachCookie.getValue()))
    }
}
