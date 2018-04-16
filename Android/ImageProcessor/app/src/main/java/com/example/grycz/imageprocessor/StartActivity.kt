package com.example.grycz.imageprocessor

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import java.io.*
import java.lang.Exception
import java.lang.ref.WeakReference
import java.net.*
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

private val cookieManager = CookieManager()

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL) // accept all cookies

        CookieHandler.setDefault(cookieManager)

        // Load CAs from an InputStream
        // (could be from a resource or ByteArrayInputStream or ...)
        val cf = CertificateFactory.getInstance("X.509")

        val certificate = String(StringBuilder(
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIC/jCCAeagAwIBAgIQV8I5rY1IzI5LPy+HxSpjXTANBgkqhkiG9w0BAQsFADAa\n" +
                "MRgwFgYDVQQDEw9ERVNLVE9QLVNUUTRJNDQwHhcNMTcxMjA3MjEyNzEyWhcNMTgx\n" +
                "MjA3MDAwMDAwWjAaMRgwFgYDVQQDEw9ERVNLVE9QLVNUUTRJNDQwggEiMA0GCSqG\n" +
                "SIb3DQEBAQUAA4IBDwAwggEKAoIBAQDZ2TiAhneH5xRYj7NyCLEAPu2hL4FhqYA7\n" +
                "vD8JVbnutQZQW4cnaK9x86WKR32bfRPUv7XFTv7B0o3RYga1XLMoeaGFz54lyO6D\n" +
                "8V3mK7fLJYKxBjifaGxh6qKtKFXxl4WrRbPqJkqbiITonJL279yjjNo/KQh8B7Hw\n" +
                "wP2pWS6J7ZByMLjU1mny7I+a59JMmXoCBwDDbI0DEoKtZZ1a32Hdo8+2iYkXNq6n\n" +
                "CCVc5pLW2KFypajeb/dGqHGpFytx/gOGCr88Eb6FiXR+iFytp5MzcwyKZOijmSIr\n" +
                "EQoSe9SfHybi0OizDMyMJYvJH37PMcpQ8x6DKggDzN9+djlGN9rtAgMBAAGjQDA+\n" +
                "MAsGA1UdDwQEAwIEMDATBgNVHSUEDDAKBggrBgEFBQcDATAaBgNVHREEEzARgg9E\n" +
                "RVNLVE9QLVNUUTRJNDQwDQYJKoZIhvcNAQELBQADggEBAKacbqrEZwNr1zqQF3wU\n" +
                "8/sFr9bBBb1yxZQ9uZfTD2Jj5/kAQSERMfFyRu7ZpQJxo6mfa1roodYVmXtyY+bo\n" +
                "npuZQGbPIjxkP0Vk8kdDMW9cQZKH0ktlgNZ6DbHnxAgl4oQo+7l+rXChP9qT7o0d\n" +
                "LRlQBhfSCm2KClElQ1TB4e//0Z9cam71+b7QYDeDZKGuOVw/L4zB7Bof4v4rijRq\n" +
                "/I7mN1PtHe8jXNtmSP8ageSsQOV5EF3tygJBupw/uOe6lmz4KXRAKEXplkvm7a3W\n" +
                "NjwDqftWTkAlYbmbMt0x/P+JXs3nkh4GWrqyAkvfpfBwAH0d2YmDWKorNgFmab+t\n" +
                "Jkg=\n" +
                "-----END CERTIFICATE-----\n"))

        // From https://www.washington.edu/itconnect/security/ca/load-der.crt
        val caInput = BufferedInputStream(ByteArrayInputStream(certificate.toByteArray()))
        var ca: Certificate? = null
        try {
            ca = cf.generateCertificate(caInput)
            System.out.println("ca=" + (ca as X509Certificate).subjectDN)
        }catch (e: Exception){
            println(e)
        }
        finally {
            caInput.close()
        }

        // Create a KeyStore containing our trusted CAs
        val keyStoreType = KeyStore.getDefaultType()
        val keyStore = KeyStore.getInstance(keyStoreType)
        keyStore.load(null, null)
        keyStore.setCertificateEntry("ca", ca)

        // Create a TrustManager that trusts the CAs in our KeyStore
        val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
        val tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
        tmf.init(keyStore)

        // Create an SSLContext that uses our TrustManager
        val context = SSLContext.getInstance("TLS")
        context.init(null, tmf.trustManagers, null)

        AppConfigurator.sslContext = context

        // Setup server preferences (ip address or domain name) and user-specific login preferences (remember password, user firstname and lastname)
        AppConfigurator.serverPreferences = getSharedPreferences(getString(R.string.serverPreferences), Context.MODE_PRIVATE) // load serverPreferences into global variable

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

        if(cookiesFromPrefs != null){
            val loginCookie = HttpCookie(cookiesFromPrefs.get("name").toString(), cookiesFromPrefs.get("value").toString())
            loginCookie.domain = cookiesFromPrefs.get("domain").toString()
            loginCookie.version = 0

            cookieManager.cookieStore.add(URI(AppConfigurator.server_domain), loginCookie)
        }

        TestLogging(AppConfigurator.server_domain + "MobileDevices/checkIfMobileAppLoggedIn", cookieManager, WeakReference(applicationContext), WeakReference(this)).execute()
    }

    companion object {
        // Test if user is logged in or not
        internal class TestLogging(private val url: String, private val cookieManager: CookieManager, private val contextWeak: WeakReference<Context>, private val activityWeak: WeakReference<StartActivity>) : AsyncTask<String, Unit, Unit>(){

            private var noRouteToHostException: NoRouteToHostException? = null
            private var connectException: ConnectException? = null
            private var exception: Exception? = null
            private var loggedIn: Boolean = false // false = user not logged in, true - user logged in

            override fun doInBackground(vararg params: String?) {
                try {
                    val httpsConn = AppConfigurator.createHttpsUrlConnectioObject(url)

                    if(httpsConn.responseCode == 200)
                        loggedIn = true
                    httpsConn.disconnect()
                } catch (e: NoRouteToHostException) {
                    this.noRouteToHostException = e
                }catch (e: ConnectException){
                    this.connectException = e
                }catch (e: Exception){
                    this.exception = e
                }
                return
            }

            override fun onPostExecute(result: Unit?) {
                super.onPostExecute(result)

                AppConfigurator.cookieManager = cookieManager

                try {
                    if (loggedIn && noRouteToHostException == null && exception == null && connectException == null) { // user logged in, no other errors. Redirect user to NavActivity
                        val redirectIntent = Intent(contextWeak.get()!!, NavActivity::class.java)
                        redirectIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)
                        contextWeak.get()!!.startActivity(redirectIntent)
                        activityWeak.get()!!.finish()
                    } else if (!loggedIn && noRouteToHostException == null && exception == null && connectException == null) { // User not logged in, no other errors. Redirect user to LoginActivity
                        val redirectIntent = Intent(contextWeak.get()!!, LoginActivity::class.java)
                        redirectIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)
                        contextWeak.get()!!.startActivity(redirectIntent)
                        activityWeak.get()!!.finish()
                    } else if (noRouteToHostException != null || connectException != null) { // No connection with server. Show user error in Toast
                        val toast: Toast = Toast.makeText(contextWeak.get()!!, "Problem z połączniem z serwerem.", Toast.LENGTH_SHORT)
                        toast.show()
                        val redirectIntent = Intent(contextWeak.get()!!, LoginActivity::class.java)
                        redirectIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)
                        contextWeak.get()!!.startActivity(redirectIntent)
                        activityWeak.get()!!.finish()
                    } else { // Other error
                        val toast: Toast = Toast.makeText(contextWeak.get()!!, "Nieznany błąd.", Toast.LENGTH_SHORT)
                        toast.show()
                        val redirectIntent = Intent(contextWeak.get()!!, LoginActivity::class.java)
                        redirectIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)
                        contextWeak.get()!!.startActivity(redirectIntent)
                        activityWeak.get()!!.finish()
                    }
                }catch(e: NullPointerException){}
            }
        }
    }
}