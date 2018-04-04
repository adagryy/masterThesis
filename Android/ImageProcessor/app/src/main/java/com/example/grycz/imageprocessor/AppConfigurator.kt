package com.example.grycz.imageprocessor

import android.content.Context
import android.graphics.Color
import android.widget.Toast
import java.io.IOException
import java.net.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import android.widget.LinearLayout
import android.view.WindowManager
import android.graphics.Color.parseColor
import android.support.v7.app.AlertDialog
import android.widget.TextView
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ProgressBar
import com.example.grycz.imageprocessor.R.styleable.AlertDialog


/**
 * Created by grycz on 2/16/2018.
 */
class AppConfigurator {
    companion object {
        var cookieManager: CookieManager? = null
        var sslContext: SSLContext = SSLContext.getDefault()

        fun getSSLContext(): SSLContext{
            return this.sslContext
        }

        fun createHttpsUrlConnectioObject(url: String): HttpsURLConnection{
            val url = URL(url)
            val httpsURLConnection = url.openConnection() as HttpsURLConnection
            httpsURLConnection.requestMethod = "POST"
//            httpsURLConnection.connect()

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
    }
}