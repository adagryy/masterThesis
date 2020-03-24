package com.example.grycz.imageprocessor

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception
import java.lang.ref.WeakReference
import java.net.*

private val cookieManager = CookieManager()

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL) // accept all cookies

        CookieHandler.setDefault(cookieManager)

        // Setup server preferences (ip address or domain name) and user-specific login preferences (remember password, user firstname and lastname)
        AppConfigurator.serverPreferences = getSharedPreferences(getString(R.string.serverPreferences), Context.MODE_PRIVATE) // load serverPreferences into global variable

        AppConfigurator.certificatePersistent = getSharedPreferences("selfSignedCertificate", Context.MODE_PRIVATE)

        val certificate = AppConfigurator.certificatePersistent?.all

        if(certificate != null && certificate.isEmpty()){
            val editor = AppConfigurator.certificatePersistent?.edit()
            editor?.putString("certificate", getString(R.string.defaultCertificate))
            editor?.commit()
        }

        AppConfigurator.cert = certificate?.get("certificate").toString()

        // prepare app to use self-signed certificate
        AppConfigurator.setSelSignedCertificate()

        val allServerPreferences = AppConfigurator.serverPreferences?.all // read from serverPreferences
        if(allServerPreferences != null && allServerPreferences.isEmpty()){ // if serverPreferences are not null and empty, then redirect to serverAddressActivity to ask user for ip address for server
            // Activity is filled with default serverAddress
            val redirectIntent = Intent(applicationContext, ServerAddressActivity::class.java)
            redirectIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(redirectIntent)
            this.finish()
            return
        }

        AppConfigurator.server_domain = "https://" + allServerPreferences?.get("serveraddress").toString() + "/"

        AppConfigurator.loginPreferences = getSharedPreferences("cookies", Context.MODE_PRIVATE)
        val cookiesFromPrefs = AppConfigurator.loginPreferences?.all

        try {
            if (cookiesFromPrefs!!.isNotEmpty()) {
                val loginCookie = HttpCookie(cookiesFromPrefs.get("name").toString(), cookiesFromPrefs.get("value").toString())
                loginCookie.domain = cookiesFromPrefs.get("domain").toString()
                loginCookie.version = 0

                cookieManager.cookieStore.add(URI(AppConfigurator.server_domain), loginCookie)
            }
        }catch (e: NullPointerException){}

        TestLogging(AppConfigurator.server_domain + "MobileDevices/checkIfMobileAppLoggedIn", cookieManager, WeakReference(applicationContext), WeakReference(this)).execute()
    }

    companion object {
        // Test if user is logged in or not
        internal class TestLogging(private val url: String, private val cookieManager: CookieManager, private val contextWeak: WeakReference<Context>, private val activityWeak: WeakReference<StartActivity>) : AsyncTask<String, Unit, Unit>(){

            private var exception: Exception? = null
            private var loggedIn: Boolean = false // false = user not logged in, true - user logged in

            override fun doInBackground(vararg params: String?) {
                try {
                    val httpsConn = AppConfigurator.createHttpsUrlConnectioObject(url)

                    if(httpsConn.responseCode == 200)
                        loggedIn = true
                    httpsConn.disconnect()
                }catch (e: Exception){
                    this.exception = e
                }
                return
            }

            override fun onPostExecute(result: Unit?) {
                super.onPostExecute(result)

                AppConfigurator.cookieManager = cookieManager

                try {
                    if(loggedIn && this.exception == null){
                        val redirectIntent = Intent(contextWeak.get()!!, NavActivity::class.java)
                        redirectIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)
                        contextWeak.get()!!.startActivity(redirectIntent)
                        activityWeak.get()!!.finish()
                    }else{
                        val redirectIntent = Intent(contextWeak.get()!!, LoginActivity::class.java)
                        redirectIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)
                        contextWeak.get()!!.startActivity(redirectIntent)
                        activityWeak.get()!!.finish()

                        AppConfigurator.toastMessageBasedOnException(exception!!, contextWeak.get()!!) // if "exception" is null, then NPE will be thrown
                    }
                }catch(e: NullPointerException){}
            }
        }
    }
}