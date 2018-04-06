package com.example.grycz.imageprocessor

import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import java.net.HttpURLConnection
import java.net.URL
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Message
import android.support.v7.app.AlertDialog
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import kotlinx.android.synthetic.main.activity_receive_image.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.ConnectException
import javax.net.ssl.HttpsURLConnection


class ReceiveImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive_image)

        val actionBar: android.support.v7.widget.Toolbar? = findViewById(R.id.my_toolbar_receiving)
        actionBar?.title = "Odbiór obrazu"

        setSupportActionBar(actionBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        downloadImageFromServer() // download image from server

        afterProcessingData.movementMethod = ScrollingMovementMethod() // enables "afterProcessingData" to scroll
    }

    private fun downloadImageFromServer(){

        DownloadPhotoFromServer(downloaded_image_preview, afterProcessingData, setProgressDialog("Pobieranie obrazu")).execute(AppConfigurator.server_domain + "MobileDevices/GetFileFromDisk",
                AppConfigurator.server_domain + "MobileDevices/getData")
    }

    private fun setProgressDialog(message: String) : AlertDialog {

        val llPadding = 30
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.setPadding(llPadding, llPadding, llPadding, llPadding)
        ll.gravity = Gravity.CENTER
        var llParam = LinearLayout.LayoutParams(110, 110)
        llParam.gravity = Gravity.CENTER

        val progressBar = ProgressBar(this)
        progressBar.isIndeterminate = true
        progressBar.setPadding(0, 0, llPadding, 0)
        progressBar.layoutParams = llParam

        llParam = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llParam.gravity = Gravity.CENTER
        val tvText = TextView(this)
        tvText.text = message
        tvText.setTextColor(Color.parseColor("#000000"))
        tvText.textSize = 20f
        tvText.layoutParams = llParam

        ll.addView(progressBar)
        ll.addView(tvText)

        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(ll)

        val dialog = builder.create()
        dialog.show()
        val window = dialog.getWindow()
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.getWindow().getAttributes())
            layoutParams.width = 756
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog.getWindow().setAttributes(layoutParams)
        }

        return dialog
    }

    private inner class DownloadPhotoFromServer(val iv: TouchImageView, private val view: TextView, private val alertDialog: AlertDialog) : AsyncTask<String, Void, Bitmap?>(){
        private var bitmap: Bitmap? = null
        private var dataUrl: String? = null
        private var exception: Exception? = null

        override fun doInBackground(vararg urls: String?): Bitmap? {
            dataUrl = urls[1]
            try {
                val httpsUrlConnection = AppConfigurator.createHttpsUrlConnectioObject(urls[0]!!)
                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                val code = httpsUrlConnection.responseCode

                when(httpsUrlConnection.responseCode){
                    HttpsURLConnection.HTTP_OK -> {
                        val input = httpsUrlConnection.inputStream
                        bitmap = BitmapFactory.decodeStream(input)
                        input.close()
                        httpsUrlConnection.disconnect()
                        return bitmap
                    }
                    HttpsURLConnection.HTTP_NOT_FOUND -> throw ProcessedImageNotExistsOnServerException()
                }
                return null
            } catch (exception: Exception) {
                this.exception = exception
            }
            return null
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)

            alertDialog.dismiss()

            try {
                AppConfigurator.toastMessageBasedOnException(this.exception!!, applicationContext)
            }catch (nullPointerexception: NullPointerException){
                Toast.makeText(applicationContext, "Pomyślnie odebrano obraz z serwera", Toast.LENGTH_SHORT).show()
            }

            iv.setImageBitmap(bitmap)
            DownloadProcessingResults(view).execute(dataUrl)
        }
    }
}

class ProcessedImageNotExistsOnServerException : Exception()

private class DownloadProcessingResults(val iv: TextView) : AsyncTask<String, Void, Unit>(){
    private var afterProcessingData: String? = null
    override fun doInBackground(vararg params: String?) {

        try{
            val httpsURLConnection = AppConfigurator.createHttpsUrlConnectioObject(params[0]!!)
            val bufferedReader = BufferedReader(InputStreamReader(httpsURLConnection.inputStream))

            if(httpsURLConnection.responseCode == HttpsURLConnection.HTTP_NOT_FOUND)
                throw ProcessedImageNotExistsOnServerException()
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
