package com.example.grycz.imageprocessor

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import java.io.*
import java.lang.Exception
import java.lang.ref.WeakReference
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Created by grycz on 2/9/2018.
 */
class ServerConnect(private val context: WeakReference<Context>, private val url: String, private val selectedAlgorithm: String) : AsyncTask<File, Void, String>() {
    private var exception: Exception? = null

    override fun doInBackground(vararg params: File): String {
        try {
            val multiPartEntity = MultipartUtility(url + "MobileDevices/handleImageFromMobileApp", "UTF-8")
            multiPartEntity.addFormField("selectedAlgorithm", this.selectedAlgorithm)
            multiPartEntity.addFilePart("image", params[0])

            multiPartEntity.finish()
        }
        catch (exception: Exception){
            this.exception = exception
        }
        return ""

    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)

        try {
            AppConfigurator.toastMessageBasedOnException(this.exception!!, context.get()!!)
        }catch (nullPointerException: NullPointerException){
            val appcontext = context.get()
            if(appcontext != null)
                Toast.makeText(appcontext, "Pomyślnie przesłano obraz do serwera", Toast.LENGTH_SHORT).show()
        }
    }

}