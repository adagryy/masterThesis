package com.example.grycz.imageprocessor

import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection


/**
 * Created by grycz on 4/4/2018.
 */
class ProcessingStatus : IntentService("ProcessingStatus") {

    override fun onHandleIntent(workIntent: Intent) {
        // Send proper intent
        val intent = Intent("processingFinished")
        intent.putExtra("responseCode", "error") // when user loses connection during application working
        intent.putExtra("errorMessage", "error")

        val context = this
        val timer = Timer()

        val counter = AtomicInteger() // thread safe counter started from 0

        timer.scheduleAtFixedRate(object : TimerTask() { // check periodically if processing has been finished
            override fun run() {
                var httpsConn: HttpsURLConnection? = null
                try {
                    httpsConn = AppConfigurator.createHttpsUrlConnectioObject(AppConfigurator.server_domain + "MobileDevices/checkIfProcessingIsFinished")
                    intent.putExtra("responseCode", httpsConn.responseCode.toString()) // 200 - processing finished, 404 - processing in progress, 400 - incorrect request or unknown error // 200 - processing finished, 404 - processing in progress, 400 - incorrect request or unknown error
                    if (httpsConn.responseCode == 200 || counter.getAndIncrement() > 180) { // if response is correct or attempting more than half hour to checking if processing is finished, then finish
                        timer.cancel()
                        timer.purge()
                    }
                }catch (e: NoRouteToHostException){
                    intent.putExtra("errorMessage", "Brak połączenia")
                }catch (e: ConnectException){
                    intent.putExtra("errorMessage", "Brak połączenia")
                }catch (e: Exception){
                    intent.putExtra("errorMessage", "Nieznany błąd")
                }
                finally {
                    httpsConn?.disconnect()
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                }

                Thread.currentThread().interrupt() // stop current thread
            }
        }, 0, 7000)
    }
}