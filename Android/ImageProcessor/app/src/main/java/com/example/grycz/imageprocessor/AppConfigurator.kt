package com.example.grycz.imageprocessor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.*
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.TrustManagerFactory


/**
 * Created by grycz on 2/16/2018.
 */
class AppConfigurator {
    companion object {
        var cookieManager: CookieManager? = null
        var sslContext: SSLContext = SSLContext.getDefault()
        var server_domain: String = ""
        var loginPreferences: SharedPreferences? = null // stores cookies and login related (firstname, lastname, email) data on device internal memory. This settings are cleared on logout
        var serverPreferences: SharedPreferences? = null // stores server addres in device internal memory
        var certificatePersistent: SharedPreferences? = null // stores self-signed certificate in memory if server has one
        var cert = ""

        fun getSSLContext(): SSLContext{
            return this.sslContext
        }

        fun createHttpsUrlConnectioObject(urlString: String): HttpsURLConnection{
            val url = URL(urlString)
            val httpsURLConnection = url.openConnection() as HttpsURLConnection
            httpsURLConnection.requestMethod = "POST"

            // Common Name of certificate doesn't match running server Common Name
            httpsURLConnection.setHostnameVerifier({_, _ ->  true }) // we can substitute unused lambda parameters with "_"
            httpsURLConnection.sslSocketFactory = AppConfigurator.sslContext.socketFactory

            return httpsURLConnection
        }

        fun toastMessageBasedOnException(exception: Exception, context: Context){
            when(exception) {
//                null -> Toast.makeText(context, context.getString(R.string.image_sent), Toast.LENGTH_SHORT).show() // everything is fine
                is SocketTimeoutException -> Toast.makeText(context, context.getString(R.string.socket_exception), Toast.LENGTH_SHORT).show() // for example serwer computer is running, but server application is not
                is NoRouteToHostException -> Toast.makeText(context, context.getString(R.string.no_route_to_host_exception), Toast.LENGTH_SHORT).show() // no connection with server / internet
                is ConnectException -> Toast.makeText(context, context.getString(R.string.connect_exception), Toast.LENGTH_SHORT).show() // no connection with server / internet
                is SSLHandshakeException -> Toast.makeText(context, context.getString(R.string.SSLHandshakeException), Toast.LENGTH_SHORT).show() // Self signed certificate problem (SSLHandshakeException is subclass of IOException, so it must be first in this "when" clause)
//                is SSLHandshakeException -> Toast.makeText(context, cert, Toast.LENGTH_SHORT).show() // Self signed certificate problem (SSLHandshakeException is subclass of IOException, so it must be first in this "when" clause)
                is IOException -> Toast.makeText(context, context.getString(R.string.IOException), Toast.LENGTH_SHORT).show() // for example incorrect data was sent to the server and server returned non-OK status
                is ProcessedImageNotExistsOnServerException -> Toast.makeText(context, context.getString(R.string.processedImageNotExistsOnServerException), Toast.LENGTH_SHORT).show() // ProcessingInProgress
                else -> Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show() // other error
            }
        }

        fun setSelSignedCertificate(){
            // Load CAs from an InputStream
            // (could be from a resource or ByteArrayInputStream or ...)
            val cf = CertificateFactory.getInstance("X.509")

            // From https://www.washington.edu/itconnect/security/ca/load-der.crt
            val caInput = BufferedInputStream(ByteArrayInputStream(this.cert.toByteArray()))
            var ca: Certificate? = null
            try {
                ca = cf.generateCertificate(caInput)
                System.out.println("ca=" + (ca as X509Certificate).subjectDN)
            }catch (e: java.lang.Exception){
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

            this.sslContext = context
        }
    }
}