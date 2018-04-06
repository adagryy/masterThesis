package com.example.grycz.imageprocessor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import com.example.grycz.imageprocessor.R.id.cropImageView
import kotlinx.android.synthetic.main.activity_send_image.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import android.os.Environment.DIRECTORY_PICTURES
import android.support.v4.app.NavUtils
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import kotlinx.android.synthetic.main.nav_header_nav.view.*
import java.lang.ref.WeakReference
import java.net.ConnectException
import java.text.SimpleDateFormat
import java.util.*


class SendImageActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val RequestImageFromCamera = 1
    private val RequestPickImageFromGallery = 2
    private var chosenBitmap: Bitmap? = null
    private var selectedAlgorithm = ""
    private var mCurrentPhotoPath: String? = null
    private var photoURI: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_image)

        var actionBar: android.support.v7.widget.Toolbar? = findViewById(R.id.my_toolbar_sending)
        actionBar?.title = "Wysyłanie obrazu"

        setSupportActionBar(actionBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        take_photo.setOnClickListener{view ->
            dispatchTakePictureIntent()
        }

        choose_photo.setOnClickListener { view ->
            pickGalleryImage()
        }

        send_photo.setOnClickListener { view ->
            this.chosenBitmap = cropping.croppedImage
            sendPhotoToServer()
        }

        left_rot.setOnClickListener { view ->
            cropping.rotateImage(-1)
        }

        right_rot.setOnClickListener { view ->
            cropping.rotateImage(1)
        }

        crop_button.setOnClickListener { view ->
            cropping.setImageBitmap(cropping.croppedImage)
        }

        cropping.setOnCropImageCompleteListener { view, result ->
            sendPhotoToServer()
        }

        val spinner: Spinner = findViewById(R.id.spinner_algorithms)
        spinner.onItemSelectedListener = this

        AlgorithmList(AppConfigurator.server_domain + "MobileDevices/getAlgorithms", spinner ).execute()

    }

    private inner class AlgorithmList(private val url: String, private val spinner: Spinner) : AsyncTask<String, Void, Unit>(){
        private var response: List<String>? = null
        private var algorithms: MutableList<String> = ArrayList()
        private var exception: Exception? = null

        override fun doInBackground(vararg params: String?) {
            try {
                val mu = MultipartUtility(url, "UTF-8")
                response = mu.finish()

                val json = JSONObject(response!![0])

                val keys: Iterator<String> = json.keys()
                while (keys.hasNext()){
                    algorithms.add(json.get(keys.next()) as String)
                }
            }catch (exception: Exception){
                this.exception = exception
            }
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)

            try {
                AppConfigurator.toastMessageBasedOnException(this.exception!!, applicationContext)
            }catch (nullPointerException: NullPointerException){}


            var adapter: ArrayAdapter<String> = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, algorithms)
            spinner.adapter = adapter
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        this.selectedAlgorithm = parent.getItemAtPosition(pos) as String
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity. RESULT_OK && requestCode == RequestImageFromCamera) {
            chosenBitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoURI)
            cropping.setImageBitmap(chosenBitmap)
//            val extras = data?.extras
//            val imageBitmap = extras?.get("data") as Bitmap
        }

        if (resultCode == Activity.RESULT_OK && requestCode == RequestPickImageFromGallery) {
            val uri = data?.data
            var postImageSend = ""
            try {
                chosenBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
//                photo_preview.setImageBitmap(chosenBitmap)

                cropping.setImageBitmap(chosenBitmap)
                } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun persistImage(bitmap: Bitmap, name: String) : File {
        val filesDir = baseContext.getFilesDir()
        val imageFile = File(filesDir, name + ".jpg")

        val os: OutputStream
        try {
            os = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Error writing bitmap", e)
        }
        return imageFile
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Toast.makeText(applicationContext, "Błąd. Nie można utworzyć pliku obrazu", Toast.LENGTH_SHORT).show()
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    photoURI = FileProvider.getUriForFile(applicationContext,"com.example.android.fileprovider", photoFile)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, RequestImageFromCamera)
                }catch (e: Exception){
                    println(e)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        storageDir.listFiles().forEach { item ->
//            var ext = item.extension
            item.delete()
        }

        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    private fun pickGalleryImage(){
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra("return-data", true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RequestPickImageFromGallery)
    }

    private fun sendPhotoToServer(){
        val alertDialog = setProgressDialog("Wysyłanie...")
        try {
            ServerConnect(AppConfigurator.server_domain, this.selectedAlgorithm, alertDialog).execute(persistImage(this.chosenBitmap!!, "output"))
        }catch (e: Exception){
            alertDialog.dismiss()
            Toast.makeText(applicationContext, "Najpierw wybierz zdjęcie", Toast.LENGTH_SHORT).show()
        }
    }

    internal inner class ServerConnect( private val url: String, private val selectedAlgorithm: String, private val alertDialog: AlertDialog) : AsyncTask<File, Void, String>() {
        private var exception: java.lang.Exception? = null

        override fun doInBackground(vararg params: File): String {
            try {
                val multiPartEntity = MultipartUtility(url + "MobileDevices/handleImageFromMobileApp", "UTF-8")
                multiPartEntity.addFormField("selectedAlgorithm", this.selectedAlgorithm)
                multiPartEntity.addFilePart("image", params[0])

                multiPartEntity.finish()
            }
            catch (exception: java.lang.Exception){
                this.exception = exception
            }
            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            try {
                AppConfigurator.toastMessageBasedOnException(this.exception!!, this@SendImageActivity.applicationContext)
            }catch (nullPointerException: NullPointerException){
                Toast.makeText(this@SendImageActivity.applicationContext, "Pomyślnie przesłano obraz do serwera", Toast.LENGTH_SHORT).show()
            }
            alertDialog.dismiss()
            NavUtils.navigateUpFromSameTask(this@SendImageActivity)
            this@SendImageActivity.finish()
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
}
