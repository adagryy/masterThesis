package com.example.grycz.imageprocessor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_send_image.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class SendImageActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val RequestImageFromCamera = 1
    private val RequestPickImageFromGallery = 2
    private var chosenBitmap: Bitmap? = null
    private var selectedAlgorithm = ""

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
            sendPhotoToServer()
        }

        val spinner: Spinner = findViewById(R.id.spinner_algorithms)
        spinner.onItemSelectedListener = this

        AlgorithmList(getString(R.string.server_domain) + "serwer/MobileDevices/getAlgorithms", spinner, applicationContext).execute()

    }

    private class AlgorithmList(private val url: String, private val spinner: Spinner, private val context: Context) : AsyncTask<String, Void, Unit>(){
        private var response: List<String>? = null
        private var algorithms: MutableList<String> = ArrayList()
        override fun doInBackground(vararg params: String?) {
            val mu = MultipartUtility(url, "UTF-8")

            response = mu.finish()

            val json = JSONObject(response!![0])

            val keys: Iterator<String> = json.keys()
            while (keys.hasNext()){
                algorithms.add(json.get(keys.next()) as String)
            }

            println(json)
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)
            var adapter: ArrayAdapter<String> = ArrayAdapter(context, android.R.layout.simple_list_item_1, algorithms)
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
        if ( requestCode == RequestImageFromCamera && resultCode == RESULT_OK) {
//            val extras = data?.extras
//            val imageBitmap = extras?.get("data") as Bitmap
        }

        if (resultCode == Activity.RESULT_OK && requestCode == RequestPickImageFromGallery) {
            val uri = data?.data
            var postImageSend = ""
            try {
                chosenBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                photo_preview.setImageBitmap(chosenBitmap)
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
        Log.i("INFO: ", "Entering camera")
        val takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent?.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, RequestImageFromCamera)
        }
        Log.i("INFO: ", "Exitting camera")
    }

    private fun pickGalleryImage(){
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra("return-data", true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RequestPickImageFromGallery)
    }

    private fun sendPhotoToServer(){
        var postImageSend = ""
        try {
            postImageSend = ServerConnect(getString(R.string.server_domain), this.selectedAlgorithm).execute(persistImage(this.chosenBitmap!!, "output")).get()
            Log.i("1. INFO:  ", postImageSend)
        }catch (e: Exception){
            var toast: Toast = Toast.makeText(applicationContext, "Najpierw wybierz zdjęcie", Toast.LENGTH_SHORT)
            toast.show()
        }
    }
}
