package com.example.grycz.imageprocessor

import android.app.Dialog
import android.content.Context
import android.support.v7.app.AppCompatActivity

import android.os.AsyncTask
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AlertDialog
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.URLEncoder
import org.json.JSONObject
import android.widget.TextView
import java.lang.ref.WeakReference


/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() //, LoaderCallbacks<Cursor>
{
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Set up the login form.
        btn_login.setOnClickListener{ _ ->
            login()
        }

        val addressEditText: EditText = findViewById(R.id._addressText)

        val allServerPreferences = AppConfigurator.serverPreferences?.all // read from serverPreferences

        addressEditText.setText(allServerPreferences?.get("serveraddress").toString())
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

    private fun login(){
        val editor = AppConfigurator.serverPreferences?.edit() // start editing
        editor?.putString("serveraddress", _addressText.text.toString()) // put address into serverPreferences
        editor?.commit()
        AppConfigurator.server_domain = "https://" +  _addressText.text.toString() + "/"

        btn_login.isEnabled = false

        val ad = setProgressDialog("Logowanie...")

//        val email = _emailText.getText().toString()
//        val password = _passwordText.getText().toString()

        val map: HashMap<String, String> = HashMap<String, String>()
//        map.set("Email", email)
//        map.set("Password", password)
        map["Email"] = "a@b.d"
        map["Password"] = "RedKon,123"
        map["RememberMe"] = "true"
//        map.put("Email", "a@b.d")
//        map.put("Password", "RedKon,123")
//        map.put("RememberMe", "true")

        LoginClass(AppConfigurator.server_domain + "Account/MobileLogin   ", "UTF-8", ad, WeakReference(applicationContext), WeakReference(this)).execute(map)
    }
    companion object {

        class LoginClass(private val url: String, private val charset: String, private val progressDialog: Dialog, private val contextWeak: WeakReference<Context>, private val activityWeak: WeakReference<LoginActivity>) : AsyncTask<HashMap<String, String>, Void, Unit>(){
            private var responseCode: Int? = null

            override fun doInBackground(vararg params: HashMap<String, String>?) {
                try {
                    val mobileMultipartUtility = MobileMultipartUtility(url, charset)
                    val map = params[0]

                    map?.forEach { (k, v) ->
                        // weird situation was here - ghost newline character appeared at "Password" key entry. Don't know why!!
                        mobileMultipartUtility.addFormField(k, v)
                    }
                    mobileMultipartUtility.addFormField("Password", map?.get("Password"))

                    mobileMultipartUtility.mobileFinish()
                    val response = mobileMultipartUtility.finish()

                    this.responseCode = mobileMultipartUtility.getResponseCode()

                    if(response.size > 0){
                        try {
                            val data = response[0] // data in JSON format. Example data is: {"firstName":"Jan","lastName":"Kowalski","email":"test@test.pl"}
                            val jsonObject = JSONObject(data)

                            // Set data in nav_header_nav (firstname, lastname and email)
                            val name = jsonObject.get("firstName").toString() + " " + jsonObject.get("lastName").toString()
                            val email = jsonObject.get("email").toString()

                            val editor = AppConfigurator.serverPreferences?.edit()

                            editor?.putString("username", name)
                            editor?.putString("useremail", email)

                            editor?.commit()

                        }catch (e: JSONException){ }

                    }
                }catch (e: NoRouteToHostException) {
                }catch (e: ConnectException){
                }catch (e: Exception){
                    println()
                }
            }

            override fun onPostExecute(result: Unit?) {
                super.onPostExecute(result)

                // Successfully logged in
                if (responseCode == 200) {
                    val cookies = AppConfigurator.cookieManager?.cookieStore?.cookies

                    val editor = AppConfigurator.loginPreferences?.edit()
                    cookies?.forEach { item ->
                        editor?.putString("name", URLEncoder.encode(item.name, "UTF-8"))
                        editor?.putString("value", URLEncoder.encode(item.value, "UTF-8"))
                        editor?.putString("domain", URLEncoder.encode(item.domain, "UTF-8"))
                    }
                    editor?.commit()
                    try {
                        val redirectIntent = Intent(contextWeak.get()!!, NavActivity::class.java)
                        redirectIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK)
                        contextWeak.get()!!.startActivity(redirectIntent)
                        progressDialog.dismiss()
                        Toast.makeText(contextWeak.get()!!, "Logowanie zakończone sukcesem", Toast.LENGTH_LONG).show()
                        activityWeak.get()!!.finish()
                    }catch (e: NullPointerException){
                        progressDialog.dismiss()
                    }
                } else { // else login failed
                    progressDialog.dismiss()
                    try {
                        activityWeak.get()!!.onLoginFailed()
                    }catch (e: NullPointerException){}
                }
            }
        }
    }


    override fun onBackPressed() {
//        super.onBackPressed()
        moveTaskToBack(true)
    }

    private fun onLoginFailed() {
        Toast.makeText(baseContext, "Błąd logowania", Toast.LENGTH_LONG).show()

        btn_login.isEnabled = (true)
    }
}
