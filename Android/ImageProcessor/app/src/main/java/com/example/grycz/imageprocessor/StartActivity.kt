package com.example.grycz.imageprocessor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import org.json.JSONObject
import org.json.JSONException
import android.widget.TextView
import java.io.*
import java.lang.Exception
import java.net.*

private val cookieManager = CookieManager()

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(cookieManager)

        TestLogging("http://192.168.0.3:62000/serwer/Account/Login", cookieManager).execute()
        TestLogging("http://192.168.0.3:62000/serwer/MobileDevices/testToken", cookieManager).execute()
    }

    override fun onResume() {
        super.onResume()

        val postData = JSONObject()
        try {
            postData.put("Email", "test@test.com")
            postData.put("Token", "sadjifhh08934242utrrhhfgds8v034775q29t9ftfjhds8gvb")

            LoginInfoOnStartup(applicationContext, this).execute(getString(R.string.server_url_login), postData.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private class TestLogging(val url: String, val cookieManager: CookieManager) : AsyncTask<String, Unit, Unit>(){
        private var response: List<String>? = null
        override fun doInBackground(vararg params: String?) : Unit {
            val mu: MultipartUtility = MultipartUtility(url, "UTF-8")
            mu.addFormField("Email", "a@b.d")
            mu.addFormField("Password", "RedKon,123")
            mu.addFormField("RememberMe", "true")

            response = mu.finish()
            return Unit
        }


        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)

            AppConfigurator.cookieManager = cookieManager
            }
    }
}

private class LoginInfoOnStartup(val context: Context, val activity: Activity) : AsyncTask<String, Void, String?>() {

    private var inputStream: InputStream? = null
    private var data: String? = ""

    override fun doInBackground(vararg urlContent: String?): String? {

        var httpURLConnection: HttpURLConnection? = null

        try {
            httpURLConnection = URL(urlContent[0]).openConnection() as HttpURLConnection
            httpURLConnection.requestMethod = "POST"
            httpURLConnection.setRequestProperty("Content-Type", "application/json")
            httpURLConnection.doOutput = true
            httpURLConnection.connectTimeout = 5000
            httpURLConnection.connect()

            val os = httpURLConnection.outputStream
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
        } catch (socketException: SocketTimeoutException) {
            return "Connection error"
        } catch (connectException: ConnectException) {
            return "Connection error"
        } catch (exception: Exception) {
            return "Unknown error"
        }
        return data.toString()
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)

        var redirectIntent = Intent(context, NavActivity::class.java)
        context.startActivity(redirectIntent)
        activity.finish()
    }
}