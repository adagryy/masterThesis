package com.example.grycz.imageprocessor

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_nav.*
import kotlinx.android.synthetic.main.app_bar_nav.*
import android.provider.MediaStore
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import kotlinx.android.synthetic.main.content_nav.*
import java.security.MessageDigest
import java.io.*


class NavActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val RequestImageFromCamera = 1
    private val RequestPickImageFromGallery = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show()
        }

        new_photo.setOnClickListener{view ->
            dispatchTakePictureIntent()
        }

        send_photo.setOnClickListener { view ->
            pickGalleryImage()
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.title = "Start"

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.nav, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ( requestCode == RequestImageFromCamera && resultCode == RESULT_OK) {
            val extras = data?.extras
            val imageBitmap = extras?.get("data") as Bitmap
            photo_preview.setImageBitmap(imageBitmap)
        }

        if (resultCode == Activity.RESULT_OK && requestCode == RequestPickImageFromGallery) {
            val uri = data?.data
            var postImageSend = ""
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
//                photo_preview.setImageBitmap(bitmap)

//                var byteArrayOutputStream = ByteArrayOutputStream()
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
//                val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
//
                photo_preview.setImageBitmap(bitmap)
//                val encodedImage: String = Base64.encodeToString(byteArray, Base64.DEFAULT)
//
//                var jsonImage = JSONObject()
//
//                jsonImage.put("Email", "test@test.com")
//                jsonImage.put("Token", "sadjifhh08934242utrrhhfgds8v034775q29t9ftfjhds8gvb")
//                jsonImage.put("Image", encodedImage)
//                jsonImage.put("ImageHash", getSHA_256sumOfString(encodedImage).toString())

                // output = LoginInfoOnStartup().execute(getString(R.string.server_url), postData.toString()).get()

                postImageSend = ServerConnect().execute(persistImage(bitmap, "output")).get()
                diag.text = postImageSend
//
//                Log.i("HASH: ", getSHA_256sumOfString(encodedImage).toString())
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

    private fun getSHA_256sumOfString(s: String) : StringBuilder{
        var messageDigest =  MessageDigest.getInstance("SHA-256")

        messageDigest.update(s.toByteArray(Charsets.UTF_8))

        var hash: ByteArray = messageDigest.digest()
        val sb = StringBuilder()

        for (b in hash) {
            sb.append(Integer.toString((b.toInt() and 0xff) + 0x100, 16).substring(1))
        }

        return sb
    }
}
