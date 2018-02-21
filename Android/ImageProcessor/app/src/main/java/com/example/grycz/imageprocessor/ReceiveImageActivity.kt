package com.example.grycz.imageprocessor

import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import java.net.HttpURLConnection
import java.net.URL
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.example.grycz.imageprocessor.R.id.downloaded_image_preview
import kotlinx.android.synthetic.main.activity_receive_image.*
import java.io.*


class ReceiveImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive_image)

        var actionBar: android.support.v7.widget.Toolbar? = findViewById(R.id.my_toolbar_receiving)
        actionBar?.title = "Odbiór obrazu"

        setSupportActionBar(actionBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        download_photo.setOnClickListener{view ->
            downloadImageFromServer()
        }
    }

    private fun downloadImageFromServer(){
//        val downloadedBitmap = DownloadPhotoFromServer(downloaded_image_preview).execute("http://192.168.0.3:62000/serwer/MobileDevices/GetFileFromDisk").get()

        DownloadPhotoFromServer(downloaded_image_preview, afterProcessingData).execute(getString(R.string.server_domain) + "serwer/MobileDevices/GetFileFromDisk",
                getString(R.string.server_domain) + "serwer/MobileDevices/getData")
//        downloaded_image_preview.setImageBitmap(downloadedBitmap)
    }
}

class DownloadPhotoFromServer(val iv: ImageView, private val view: TextView) : AsyncTask<String, Void, Bitmap?>(){
    private var bitmap: Bitmap? = null
    private var dataUrl: String? = null
    override fun doInBackground(vararg urls: String?): Bitmap? {
        dataUrl = urls[1]
        var input: InputStream? = null
        var output: OutputStream? = null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urls[0])
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connect()

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.responseCode !== HttpURLConnection.HTTP_OK) {
                return null
            }

            val input = connection.inputStream
            bitmap = BitmapFactory.decodeStream(input)
            input.close()

            val inputt = BufferedReader(InputStreamReader(connection.inputStream))
            var inputLine: String?
            inputLine = inputt.readLine()
            while (true) {
                println(inputLine)
                inputLine = inputt.readLine()
                if(inputLine == null)
                    break
            }
            inputt.close()
            return bitmap
//            // download the file
//            input = connection.inputStream
//            output = FileOutputStream("/sdcard/file_name.extension")
//
//            val data = ByteArray(4096)
//            var total: Long = 0
//            var count: Int = input.read(data)
//            while (count != -1) {
//                // allow canceling with back button
//                if (isCancelled) {
//                    input.close()
//                    return null
//                }
//                total += count.toLong()
//
//                output.write(data, 0, count)
//                count = input.read(data)
//            }
        } catch (e: Exception) {
            Log.e("Errpr: ", e.toString())
            return null
        } finally {
            try {
                output?.close()
                input?.close()
            } catch (ignored: IOException) {
            }

            connection?.disconnect()
        }
        return null
    }

    override fun onPostExecute(result: Bitmap?) {
        super.onPostExecute(result)

        iv.setImageBitmap(bitmap)
        DownloadProcessingResults(view).execute(dataUrl)
    }
}

private class DownloadProcessingResults(val iv: TextView) : AsyncTask<String, Void, Unit>(){
    private var afterProcessingData: String? = null
    override fun doInBackground(vararg params: String?) {
        var input: InputStream? = null
        var output: OutputStream? = null
        var connection: HttpURLConnection? = null
        try{
            val url = URL(params[0])
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val bufferedReader = BufferedReader(InputStreamReader(connection.inputStream))

            afterProcessingData = bufferedReader.readLine()

        }catch (e: Exception){}
    }

    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)

        iv.text = this.afterProcessingData
    }
}
