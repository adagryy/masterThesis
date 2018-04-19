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
                is ConnectException -> Toast.makeText(context, context.getString(R.string.connect_exception), Toast.LENGTH_SHORT).show() // no connection with server / internet
                is IOException -> Toast.makeText(context, context.getString(R.string.IOException), Toast.LENGTH_SHORT).show() // for example incorrect data was sent to the server and server returned non-OK status
                is ProcessedImageNotExistsOnServerException -> Toast.makeText(context, context.getString(R.string.processedImageNotExistsOnServerException), Toast.LENGTH_SHORT).show() // ProcessingInProgress
                else -> Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show() // other error
            }
        }

        fun setSelSignedCertificate(){
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