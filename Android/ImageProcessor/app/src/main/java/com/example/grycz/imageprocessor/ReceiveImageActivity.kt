package com.example.grycz.imageprocessor

import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import java.net.HttpURLConnection
import java.net.URL
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_receive_image.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*


class ReceiveImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive_image)

        val actionBar: android.support.v7.widget.Toolbar? = findViewById(R.id.my_toolbar_receiving)
        actionBar?.title = "Odbiór obrazu"

        setSupportActionBar(actionBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        download_photo.setOnClickListener{view ->
            downloadImageFromServer()
        }

        afterProcessingData.movementMethod = ScrollingMovementMethod() // enables "afterProcessingData" to scroll
    }

    private fun downloadImageFromServer(){

        DownloadPhotoFromServer(downloaded_image_preview, afterProcessingData).execute(getString(R.string.server_domain) + "serwer/MobileDevices/GetFileFromDisk",
                getString(R.string.server_domain) + "serwer/MobileDevices/getData")
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
//            val url = URL(urls[0])
//            connection = url.openConnection() as HttpURLConnection
//            connection.requestMethod = "POST"
//            connection.connect()

            val httpsUrlConnection = AppConfigurator.createHttpsUrlConnectioObject(urls[0]!!)
            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (httpsUrlConnection.responseCode !== HttpURLConnection.HTTP_OK) {
                return null
            }

            val input = httpsUrlConnection.inputStream
            bitmap = BitmapFactory.decodeStream(input)
            input.close()
            return bitmap

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

        try{
            val httpsURLConnection = AppConfigurator.createHttpsUrlConnectioObject(params[0]!!)
            val bufferedReader = BufferedReader(InputStreamReader(httpsURLConnection.inputStream))

            afterProcessingData = bufferedReader.readLine()

        }catch (e: Exception){
            println(e)
        }
    }

    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)

        try {
            val jsonObject = JSONObject(this.afterProcessingData)

            val keys: Iterator<String> = jsonObject.keys()

            val parsedAfterProcessingData = StringBuilder("")

            keys.forEach { item -> parsedAfterProcessingData
                    .append(item)
                    .append(": ")
                    .append(jsonObject.get(item))
                    .append(System.lineSeparator())}

            iv.text = parsedAfterProcessingData
        }catch (e: JSONException){
            iv.text = "Serwer zwrócił niepoprawne dane"
        }catch (e: Exception){
            println(e)
        }

    }
}
