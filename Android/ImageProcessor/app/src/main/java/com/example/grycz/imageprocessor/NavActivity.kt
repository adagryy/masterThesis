package com.example.grycz.imageprocessor

import android.content.Context
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_nav.*
import kotlinx.android.synthetic.main.app_bar_nav.*
import android.content.Intent
import kotlinx.android.synthetic.main.content_nav.*
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.net.*


class NavActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        setSupportActionBar(toolbar)

        receiving_activity.setOnClickListener{_ ->
            val intent = Intent(this, SendImageActivity::class.java)
            intent.putExtra("napis", "test")
            startActivity(intent)
        }

        sending_activity.setOnClickListener { _ ->
            val intent = Intent(this, ReceiveImageActivity::class.java)
            intent.putExtra("napis", "test")
            startActivity(intent)
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.title = "Start"

        changeButtonsState(false) // disable buttons in activity until checking of processing status isn't finished

        nav_view.setNavigationItemSelectedListener(this)

        val navigationView = (findViewById<NavigationView>(R.id.nav_view)).getHeaderView(0)
        navigationView.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.primary_darker))
        val navUser = navigationView.findViewById(R.id.firstnameandlastname) as TextView
        val navEmail = navigationView.findViewById(R.id.emailnavheader) as TextView

        // --------------
        // Sometimes there was a problems with running "StartActivity" at the beginning, So that "AppConfigurator.domain" wasn't set and then app show message to the user to restart it
        // This prevents from this by check if address is set and - if not - then reset it again
        // Setup server preferences (ip address or domain name) and user-specific login preferences (remember password, user firstname and lastname)
        AppConfigurator.serverPreferences = getSharedPreferences(getString(R.string.serverPreferences), Context.MODE_PRIVATE) // load serverPreferences into global variable

        val allServerPreferences = AppConfigurator.serverPreferences?.all // read from serverPreferences

        if(AppConfigurator.cert.isEmpty()){
            AppConfigurator.cert = AppConfigurator.certificatePersistent?.all?.get("certificate").toString()
            AppConfigurator.setSelSignedCertificate() // prepare app to use self-signed certificate
        }

        if(AppConfigurator.server_domain.isEmpty()) {
            AppConfigurator.server_domain = "https://" + allServerPreferences?.get("serveraddress").toString() + "/"
        }

        AppConfigurator.loginPreferences = getSharedPreferences("cookies", Context.MODE_PRIVATE)

        val cookiesFromPrefs = AppConfigurator.loginPreferences?.all

        if(AppConfigurator.cookieManager == null) {
            val cookieManager = CookieManager()
            val loginCookie = HttpCookie(cookiesFromPrefs?.get("name").toString(), cookiesFromPrefs?.get("value").toString())
            loginCookie.domain = cookiesFromPrefs?.get("domain").toString()
            loginCookie.version = 0

            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL) // accept all cookies

            CookieHandler.setDefault(cookieManager)

            cookieManager.cookieStore.add(URI(AppConfigurator.server_domain), loginCookie)

            Toast.makeText(applicationContext, cookieManager.cookieStore.cookies.count().toString() , Toast.LENGTH_SHORT).show()

            AppConfigurator.cookieManager = cookieManager
        }
        // --------------

        navUser.text = allServerPreferences?.get("username").toString()
        navEmail.text = allServerPreferences?.get("useremail").toString()

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                IntentFilter("processingFinished"))

        startService(Intent(this, ProcessingStatus::class.java))
    }

    // Broadcast receiver
    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent

            val view: TextView = this@NavActivity.findViewById(R.id.progress_title)
//            Toast.makeText(applicationContext, intent.getStringExtra("glupiError"), Toast.LENGTH_SHORT).show()

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

//        sendButton.isEnabled = state
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
                var editor = AppConfigurator.loginPreferences?.edit()
                editor?.clear()
                editor?.commit()

                AppConfigurator.cookieManager?.cookieStore?.removeAll()

//                // Restart whole app
//                val i = baseContext.packageManager
//                        .getLaunchIntentForPackage(baseContext.packageName)
//                i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                startActivity(i)
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
