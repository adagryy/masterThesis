package com.example.grycz.imageprocessor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import org.json.JSONObject
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.grycz.imageprocessor.R.id.*
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.*
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


class SendImageActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener{
    private val requestImageFromCamera = 1
    private val requestPickImageFromGallery = 2
    private var chosenBitmap: Bitmap? = null
    private var resetChosenBitmap: Bitmap? = null
    private var selectedAlgorithm = ""
    private var mCurrentPhotoPath: String? = null
    private var photoURI: Uri? = null
    private var imagePath: String? = null
    private lateinit var mCropImageView: CropImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_image)

        val actionBar: Toolbar? = findViewById(my_toolbar_sending)
        actionBar?.title = ""

        setSupportActionBar(actionBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mCropImageView = findViewById(cropping) as CropImageView

        mCropImageView.setOnCropImageCompleteListener { _, _ ->
            sendPhotoToServer()
        }

        val spinner: Spinner = findViewById(spinner_algorithms)
        spinner.onItemSelectedListener = this

        AlgorithmList(AppConfigurator.server_domain + "MobileDevices/getAlgorithms", WeakReference(spinner), WeakReference(applicationContext), WeakReference(baseContext)).execute()

    }

    companion object {
        private class AlgorithmList(private val url: String, private val spinnerWeak: WeakReference<Spinner>, private val contextWeak: WeakReference<Context>,private val baseContextWeak: WeakReference<Context> ) : AsyncTask<String, Void, Unit>(){
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
                    if(exception != null)
                        AppConfigurator.toastMessageBasedOnException(this.exception!!, contextWeak.get()!!)
                    val adapter: ArrayAdapter<String> = ArrayAdapter(baseContextWeak.get()!!, android.R.layout.simple_list_item_1, algorithms) // important to use "baseContext" instead of "applicationContext"
                    spinnerWeak.get()!!.adapter = adapter
                }catch (nullPointerException: NullPointerException){ }
            }
        }

        internal class ServerConnect( private val url: String, private val selectedAlgorithm: String, private val alertDialog: AlertDialog, private val activityWeak: WeakReference<SendImageActivity>) : AsyncTask<File, Void, String>() {
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
                    if(exception != null)
                        AppConfigurator.toastMessageBasedOnException(this.exception!!, activityWeak.get()!!.applicationContext)
                    else
                        Toast.makeText(activityWeak.get()!!.applicationContext, "Pomyślnie przesłano obraz do serwera", Toast.LENGTH_SHORT).show()
                }catch (nullPointerException: NullPointerException){

                }
                alertDialog.dismiss()
                try {
                    NavUtils.navigateUpFromSameTask(activityWeak.get()!!)
                    activityWeak.get()!!.finish()
                }catch (e: NullPointerException){}
            }
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
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity. RESULT_OK && requestCode == requestImageFromCamera) {
            chosenBitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoURI) // remember taken photo in "chosenBitmap" (global for this class)
            rotateImageFromCameraIfNecessary() // rotate taken photo if it is not in the portrait orientation
            resetChosenBitmap = chosenBitmap // save photo into temporary variable (it allows reset image without re-taking a new photo)
            mCropImageView.setImageBitmap(chosenBitmap) // set image in view
        }

        if (resultCode == Activity.RESULT_OK && requestCode == requestPickImageFromGallery) {
            val uri = data?.data
            try {
                chosenBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

                resetChosenBitmap = chosenBitmap // save photo into temporary variable (it allows reset image without re-choosing a new photo from gallery)

                mCropImageView.setImageBitmap(chosenBitmap)
                } catch (e: IOException) { }
        }
    }

    // Some cameras take photo in landscape orientation, while others in portrait. So this method rotates all landscape-oriented photos into portrait-oriented ones
    private fun rotateImageFromCameraIfNecessary(){
        try{
            val exifInterface = ExifInterface(this.imagePath!!) // read metadata of taken photo, throws NPE if "imagePath" is null

            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED) // read orientation (it can be: not rotated(portrait orientation),
                                                                                                                                // 90 degrees, 180 degrees or 270 degrees rotated)
            // then rotate if needed
            when(orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    chosenBitmap = rotateBitmap(chosenBitmap!!, 90f)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    chosenBitmap = rotateBitmap(chosenBitmap!!, 180f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    chosenBitmap = rotateBitmap(chosenBitmap!!, 270f)
                }
                ExifInterface.ORIENTATION_NORMAL -> {

                }
                else -> { }
            }
        }catch (e: IOException){} // do nothing, it can appear, when file already not exists for any reason (remind: photo is temp file).
                                  // This approach was recommended in official Google documentation
        catch (e: Exception){} // It handles NullPointerException, for instance when "imagePath" was not set for any reason
    }

    // rotates photo which is given in "source" parameter by angle given in "angle" parameter
    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.sending_menus, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        rotate_left -> { // handle rotate left image selection
            mCropImageView.rotateImage(-1)
            true
        }
        rotate_right -> {
            mCropImageView.rotateImage(1)
            true
        }
        crop -> {
            val cropImageView: CropImageView = findViewById(R.id.cropping)
            CropImage.activity(cropImageView.imageUri).setInitialCropWindowPaddingRatio(0.0f)
            mCropImageView.setImageBitmap(mCropImageView.croppedImage)
            true
        }
        camera -> {
            dispatchTakePictureIntent()
            true
        }
        gallery -> {
            pickGalleryImage()
            true
        }
        send -> {
            this.chosenBitmap = mCropImageView.croppedImage
            sendPhotoToServer()
            true
        }
        reset -> { // user can restore original image when he mistakes during cropping
            mCropImageView.rotatedDegrees = 0 // reset rotation
            mCropImageView.resetCropRect() // reset rotate rectangle marker
            mCropImageView.setImageBitmap(this.resetChosenBitmap) // reset initial image

            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    // Remove photo from internal memory when activity is finished
    override fun onDestroy() {
        super.onDestroy()

        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Remove all files from directory. It will free the space and facilitates process of managing photos during rotation
        storageDir?.listFiles()?.forEach { item ->
            item.delete()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        if (newConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "Układ poziomy", Toast.LENGTH_SHORT).show()
        } else if (newConfig?.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "Układ pionowy", Toast.LENGTH_SHORT).show()
        }
    }

    private fun persistImage(bitmap: Bitmap, name: String) : File {
        val filesDir = baseContext.filesDir
        val imageFile = File(filesDir, "$name.jpg")

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
                this.imagePath = photoFile.path // remember path to this image in string variable so later it can be read for rotating if photo was taken in landscape orientation
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Toast.makeText(applicationContext, "Błąd. Nie można utworzyć pliku obrazu", Toast.LENGTH_SHORT).show()
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    photoURI = FileProvider.getUriForFile(applicationContext,"com.example.android.fileprovider", photoFile)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, requestImageFromCamera)
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

        // Remove all files from directory. It will free the space and facilitates process of managing photos during rotation
        storageDir?.listFiles()?.forEach { item ->
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
        intent.putExtra("return-data", true)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), requestPickImageFromGallery)
    }

    private fun sendPhotoToServer(){
        val alertDialog = setProgressDialog("Wysyłanie...")
        try {
            ServerConnect(AppConfigurator.server_domain, this.selectedAlgorithm, alertDialog, WeakReference(this)).execute(persistImage(this.chosenBitmap!!, "output"))
        }catch (e: Exception){
            alertDialog.dismiss()
            Toast.makeText(applicationContext, "Najpierw wybierz zdjęcie", Toast.LENGTH_SHORT).show()
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
            layoutParams.copyFrom(dialog.window!!.attributes)
            layoutParams.width = 756
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog.window!!.attributes = layoutParams
        }

        return dialog
    }
}
