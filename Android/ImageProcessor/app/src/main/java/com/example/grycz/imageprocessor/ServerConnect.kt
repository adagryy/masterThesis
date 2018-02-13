package com.example.grycz.imageprocessor

import android.os.AsyncTask
import android.util.Log
import java.io.*
import java.lang.Exception
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap

/**
 * Created by grycz on 2/9/2018.
 */
class ServerConnect : AsyncTask<File, Void, String>() {
    override fun doInBackground(vararg params: File): String {
        var data: String? = ""

        try {

            val multiPartEntity: MultipartUtility = MultipartUtility("http://192.168.0.3:62000/serwer/MobileDevices/handleImageFromMobileApp", "UTF-8")
            multiPartEntity.addFormField("sdf", "sdf")
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

}