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

    override fun doInBackground(vararg params: File): String {
        var data: String? = ""

        try {
            val multiPartEntity = MultipartUtility(url + "serwer/MobileDevices/handleImageFromMobileApp", "UTF-8")
            multiPartEntity.addFormField("selectedAlgorithm", this.selectedAlgorithm)
            multiPartEntity.addFilePart("dfsf", params[0])

            val response = multiPartEntity.finish()

            Log.i("Czas: ", response[0])
        }catch (socketException: SocketTimeoutException){
            return "Connection error"
        }catch(connectException: ConnectException){
            return "Connection error"
        }
        catch (exception: Exception){
            return "Unknown error"
        }
        return data.toString()

    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)

        Toast.makeText(context.get(), "Obraz został przesłany na serwer.", Toast.LENGTH_SHORT).show()
    }

}