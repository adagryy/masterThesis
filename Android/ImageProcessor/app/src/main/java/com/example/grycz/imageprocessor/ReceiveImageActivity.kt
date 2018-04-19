package com.example.grycz.imageprocessor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.*
import com.example.grycz.imageprocessor.R.id.download
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.android.synthetic.main.activity_receive_image.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.ref.WeakReference
import javax.net.ssl.HttpsURLConnection
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NavUtils
import android.support.v4.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*


class ReceiveImageActivity : AppCompatActivity(){
    private val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: Int = 1
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: Int = 2
    private var hasUserAllowedWriteExternalStorage = false
    private var hasUserAllowedReadExternalStorage = false

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
        DownloadPhotoFromServer(WeakReference(downloaded_image_preview), WeakReference(afterProcessingData), setProgressDialog("Pobieranie obrazu"), WeakReference(applicationContext), WeakReference(this)).execute(AppConfigurator.server_domain + "MobileDevices/GetFileFromDisk",
                AppConfigurator.server_domain + "MobileDevices/getData")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.reveiving_items, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        download -> { // Handle saving image button (saves image into public gallery)
            if(checkIfHasPermissionToSave()) {
                try {
                    item.isEnabled = false // disable action button to prevent multi-click

                    val bitmap: Bitmap = (downloaded_image_preview.drawable as BitmapDrawable).bitmap // read bitmap from view
                                        ?: throw NullPointerException() // or throw exception if bitmap is null

                    val root = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES).toString()
                    val saveDir = File("$root/AnalizatorObrazow")
                    if (!saveDir.exists())
                        saveDir.mkdirs()
                    // Collision-resistant filename
                    val fileToSave = File(saveDir, "processedImage" + SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) + ".JPG")
                    val imageOutputStream = FileOutputStream(fileToSave)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, imageOutputStream)

                    // Tell the mediascanner to be saved image available in gallery immediately
                    MediaScannerConnection.scanFile(this, arrayOf(fileToSave.toString()), null, null)

                    NavUtils.navigateUpFromSameTask(this) // return to home (prevents multiple downloads)

                    Toast.makeText(applicationContext, "Pomyślnie zapisano obraz w galerii", Toast.LENGTH_SHORT).show()
                }catch (e: NullPointerException){ // image does not exist in
                    NavUtils.navigateUpFromSameTask(this) // return to home (prevents multiple downloads)
                    Toast.makeText(applicationContext, "Brak obrazu do zapisania!", Toast.LENGTH_SHORT).show()
                }
            }else{
                NavUtils.navigateUpFromSameTask(this) // return to home (prevents multiple downloads)
                Toast.makeText(applicationContext, "Odmowa dostępu do zapisu", Toast.LENGTH_SHORT).show()
            }
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun checkIfHasPermissionToSave() : Boolean{
        // check permissions if device is Amdroid 6.0 (Marshmallow) or higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        else
            // If Android version is below Android 6.0 (Marshmallow), then this is always true, because the only way to install app is to grant all its permissions
            return true
    }

    private fun checkPermissions(permission: String, id: Int) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                        permission)
                != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            arrayOf(permission),
                            id)
        } else {
            // Permission has already been granted
            if(id == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                this.hasUserAllowedWriteExternalStorage = true // write permission granted
            else if(id == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
                this.hasUserAllowedReadExternalStorage = true // read permission granted
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                this.hasUserAllowedWriteExternalStorage = (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                return
            }
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                this.hasUserAllowedReadExternalStorage = (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                return
            }

        // Add other 'when' lines to check for other
        // permissions this app might request.

            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        if (newConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "Układ poziomy", Toast.LENGTH_SHORT).show()
        } else if (newConfig?.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "Układ pionowy", Toast.LENGTH_SHORT).show()
        }
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
        val window = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window.attributes)
            layoutParams.width = 756
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog.window.attributes = layoutParams
        }

        return dialog
    }

    companion object {
        private class DownloadPhotoFromServer(val iv: WeakReference<PhotoView>, private val view: WeakReference<TextView>,
                                              private val alertDialog: AlertDialog, private val contextWeak: WeakReference<Context>,
                                              private val activityWeak: WeakReference<ReceiveImageActivity>
                                              ) : AsyncTask<String, Void, Bitmap?>() {
            private var bitmap: Bitmap? = null
            private var dataUrl: String? = null
            private var exception: Exception? = null
            private var debugMsg = ""
            private var url0 = ""

            override fun doInBackground(vararg urls: String?): Bitmap? {
                dataUrl = urls[1]
                url0 = urls[0]!!
                try {
                    val httpsUrlConnection = AppConfigurator.createHttpsUrlConnectioObject(urls[0]!!)
                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file

                    when (httpsUrlConnection.responseCode) {
                        HttpsURLConnection.HTTP_OK -> {
                            val input = httpsUrlConnection.inputStream
                            bitmap = BitmapFactory.decodeStream(input)
                            input.close()
                            httpsUrlConnection.disconnect()
                            return bitmap
                        }
                        HttpsURLConnection.HTTP_NOT_FOUND -> throw ProcessedImageNotExistsOnServerException()
                    }
                    httpsUrlConnection.disconnect()
                    return null
                } catch (exception: Exception) {
                    debugMsg = exception.toString()
                    this.exception = exception
                }
                return null
            }

            override fun onPostExecute(result: Bitmap?) {
                super.onPostExecute(result)

                alertDialog.dismiss()

                try {
//                    Toast.makeText(contextWeak.get()!!, url0 + ", test: $debugMsg", Toast.LENGTH_SHORT).show()
                    if(exception != null)
                        AppConfigurator.toastMessageBasedOnException(this.exception!!, contextWeak.get()!!)
                    else
                        Toast.makeText(contextWeak.get()!!, "Pomyślnie odebrano obraz z serwera", Toast.LENGTH_SHORT).show()
                } catch (nullPointerexception: NullPointerException) {

                }

                try {
                    iv.get()!!.setImageBitmap(bitmap) // set image into view
                    DownloadProcessingResults(view).execute(dataUrl)
                    // Needed to save photo in gallery. Request permissions after image is loaded and alertDialob is dismissed
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        activityWeak.get()!!.checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, activityWeak.get()!!.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                        activityWeak.get()!!.checkPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, activityWeak.get()!!.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
                    }
                }catch(e: NullPointerException){
//                    Toast.makeText(contextWeak.get()!!, e.toString(), Toast.LENGTH_SHORT).show()
                }

            }
        }

        private class DownloadProcessingResults(private val ivWeak: WeakReference<TextView>) : AsyncTask<String, Void, Unit>() {
            private var afterProcessingData: String? = null
            override fun doInBackground(vararg params: String?) {

                try {
                    val httpsURLConnection = AppConfigurator.createHttpsUrlConnectioObject(params[0]!!)
                    val bufferedReader = BufferedReader(InputStreamReader(httpsURLConnection.inputStream))

                    if (httpsURLConnection.responseCode == HttpsURLConnection.HTTP_NOT_FOUND)
                        throw ProcessedImageNotExistsOnServerException()
                    afterProcessingData = bufferedReader.readLine()

                    httpsURLConnection.disconnect()
                } catch (e: Exception) {
                    println(e)
                }
            }

            override fun onPostExecute(result: Unit?) {
                super.onPostExecute(result)

                try {
                    val jsonObject = JSONObject(this.afterProcessingData)

                    val keys: Iterator<String> = jsonObject.keys()

                    val parsedAfterProcessingData = StringBuilder("")

                    keys.forEach { item ->
                        parsedAfterProcessingData
                                .append(item)
                                .append(": ")
                                .append(jsonObject.get(item))
                                .append(System.lineSeparator())
                    }
                    try{ivWeak.get()!!.text = parsedAfterProcessingData}catch (e: NullPointerException){}
                } catch (e: JSONException) {
                    try{ivWeak.get()!!.text = "Serwer zwrócił niepoprawne dane"}catch (e: NullPointerException){}
                } catch (e: Exception) { }


            }
        }
    }
}

class ProcessedImageNotExistsOnServerException : Exception()


