package com.example.grycz.imageprocessor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
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


        TestLogging(getString(R.string.server_domain) + "serwer/Account/Login", cookieManager, applicationContext, this).execute()
//        TestLogging(getString(R.string.server_domain) + "serwer/MobileDevices/testToken", cookieManager, applicationContext, this).execute()
    }

    private class TestLogging(val url: String, val cookieManager: CookieManager, val context: Context, val activity: Activity) : AsyncTask<String, Unit, Unit>(){
        private var response: List<String>? = null
        private var noRouteToHostException: NoRouteToHostException? = null
        private var connectException: ConnectException? = null
        private var exception: Exception? = null

        override fun doInBackground(vararg params: String?) : Unit {
            try {
                val mu = MultipartUtility(url, "UTF-8")

                mu.addFormField("Email", "a@b.d")
                mu.addFormField("Password", "RedKon,123")
                mu.addFormField("RememberMe", "true")

                response = mu.finish()
            } catch (e: NoRouteToHostException) {
                this.noRouteToHostException = e
            }catch (e: ConnectException){
                this.connectException = e
            }catch (e: Exception){
                this.exception = e
            }
            return
        }


        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)

            AppConfigurator.cookieManager = cookieManager

            if(noRouteToHostException == null && exception == null && connectException == null){
                val redirectIntent = Intent(context, NavActivity::class.java)
                context.startActivity(redirectIntent)
                activity.finish()
            }else if (noRouteToHostException != null || connectException != null){
                val toast: Toast = Toast.makeText(context, "Problem z połączniem z serwerem." + System.lineSeparator() + "Zamykanie aplikacji", Toast.LENGTH_SHORT)
                toast.show()
                ExitApplicationOnConnectFailed().execute()
            }else{
                val toast: Toast = Toast.makeText(context, "Nieznany błąd." + System.lineSeparator() + "Zamykanie aplikacji", Toast.LENGTH_SHORT)
                toast.show()
                ExitApplicationOnConnectFailed().execute()
            }
        }
        private class ExitApplicationOnConnectFailed : AsyncTask<Void, Void, Unit>(){
            override fun doInBackground(vararg params: Void?) {
                Thread.sleep(3000)
                System.exit(0)
            }
        }
    }
}