package com.example.grycz.imageprocessor

import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.json.JSONObject
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.URL
import android.widget.TextView
import java.io.*


class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
    }

    override fun onResume() {
        super.onResume()

//        var outpou: String = LoginInfoOnStartup().execute("dsf").get()
        var output: String? = null
        val postData = JSONObject()
        try {
            postData.put("Email", "test@test.com")
            postData.put("token", "sadjifhh08934242utrrhhfgds8v034775q29t9ftfjhds8gvb")

            output = LoginInfoOnStartup().execute("http://192.168.0.3:62000/serwer/MobileDevices/checkIfLoggedIn", postData.toString()).get()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val intent = Intent(this, NavActivity::class.java)
        intent.putExtra("napis", output)
        startActivity(intent)

        finish()
        }
}

class LoginInfoOnStartup : AsyncTask<String, Void, String>(){

    private var inputStream: InputStream? = null
    private var result: String  = ""
    private var jsonObject: JSONObject? = null

    override fun doInBackground(vararg urlContent: String?): String {

        var httpURLConnection: HttpURLConnection? = null
        var data: String? = ""

        httpURLConnection = URL(urlContent[0]).openConnection() as HttpURLConnection
        httpURLConnection.requestMethod = "POST"
        httpURLConnection.setRequestProperty("Content-Type", "application/json")
        httpURLConnection.doOutput = true

//        val wr = DataOutputStream(httpURLConnection.outputStream)
//        wr.writeBytes(URLEncoder.encode(urlContent[1].toString(),"UTF-8"))
//        wr.flush()
//        wr.close()

        val os = httpURLConnection.getOutputStream()
        val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
        writer.write(urlContent[1])

        writer.flush()
        writer.close()
        os.close()

        inputStream = httpURLConnection.inputStream
        val inputStreamReader = InputStreamReader(inputStream)

        var inputStreamData = inputStreamReader.read()
        while (inputStreamData != -1) {
            val current = inputStreamData.toChar()
            inputStreamData = inputStreamReader.read()
            data += current
        }

        return data.toString()
    }
}
