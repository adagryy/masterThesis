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
import android.R.attr.data
import java.io.IOException


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

            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                photo_preview.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }

//            val extras = data?.extras
//            if (extras != null) {
//                //Get image
//                val newProfilePic = extras.getParcelable<Bitmap>("data")
//                photo_preview.setImageBitmap(newProfilePic)
//            }
        }
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
//        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
//        intent.type = "image/*"
//        intent.putExtra("crop", "true")
//        intent.putExtra("scale", true)
//        intent.putExtra("outputX", 256)
//        intent.putExtra("outputY", 256)
//        intent.putExtra("aspectX", 1)
//        intent.putExtra("aspectY", 1)
//        intent.putExtra("return-data", true)
//        startActivityForResult(intent, 1)
    }
}
