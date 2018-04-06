package com.example.grycz.imageprocessor

import android.app.Dialog
import android.support.v7.app.AppCompatActivity

import android.os.AsyncTask
import android.os.Bundle
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Message
import android.support.v7.app.AlertDialog
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*

import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.progress_dialog.*
import kotlinx.android.synthetic.main.progress_dialog.view.*
import java.net.ConnectException
import java.net.NoRouteToHostException


/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() //, LoaderCallbacks<Cursor>
{
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
//    private var mAuthTask: UserLoginTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Set up the login form.

        btn_login.setOnClickListener{ _ ->
            Login()
        }

        val addressEditText: EditText = findViewById(R.id._addressText)
        var serverAddress = AppConfigurator.readAddressFromFile(applicationContext)

        addressEditText.setText(serverAddress)

//        populateAutoComplete()
//        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
//            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
//                attemptLogin()
//                return@OnEditorActionListener true
//            }
//            false
//        })
//
//        email_sign_in_button.setOnClickListener { attemptLogin() }
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

    private fun Login(){
//        if (!validate()) {
//            onLoginFailed()
//            return
//        }

        AppConfigurator.createOrUpdateServerAddressFile(applicationContext, _addressText.text.toString())
        AppConfigurator.server_domain = "https://" +  _addressText.text.toString() + "/"

        btn_login.setEnabled(false);

//        val progressDialog = ProgressBar(this, null, R.style.AppTheme_Dark_Dialog)
//        progressDialog.isIndeterminate = (true)
//        progressDialog.setMessage = "Authenticating..."

        var ad = setProgressDialog("Logowanie...")

        val email = _emailText.getText().toString();
        val password = _passwordText.getText().toString();

        var map: HashMap<String, String> = HashMap<String, String>()
//        map.set("Email", email)
//        map.set("Password", password)
        map.put("Email", "a@b.d")
        map.put("Password", "RedKon,123")
        map.put("RememberMe", "true")

        LoginClass(AppConfigurator.server_domain + "Account/MobileLogin   ", "UTF-8", ad).execute(map)
    }

    internal inner class LoginClass(val url: String, val charset: String, val progressDialog: Dialog) : AsyncTask<HashMap<String, String>, Void, Unit>(){
        private var responseCode: Int? = null


        override fun doInBackground(vararg params: HashMap<String, String>?) {
            try {
                val mobileMultipartUtility = MobileMultipartUtility(url, charset)
                var map = params[0]
                val c = map?.get("Password")

                map?.forEach { (k, v) ->
                    // weird situation was here - ghost newline character appeared at "Password" key entry. Don't know why!!
                    mobileMultipartUtility.addFormField(k, v)
                }
//            mobileMultipartUtility.addFormField("Email", map?.get("Email"))
                mobileMultipartUtility.addFormField("Password", map?.get("Password"))
//            mobileMultipartUtility.addFormField("RememberMe", map?.get("RememberMe"))

                mobileMultipartUtility.mobileFinish()

                this.responseCode = mobileMultipartUtility.getResponseCode()
            }catch (e: NoRouteToHostException) {
            }catch (e: ConnectException){
            }catch (e: Exception){
            }
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)

            // Successfully logged in
            if (responseCode == 200) {
                val redirectIntent = Intent(applicationContext, NavActivity::class.java)
                applicationContext.startActivity(redirectIntent)
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "Logowanie zakończone sukcesem", Toast.LENGTH_LONG).show()
//                loginActivity.finish()
                this@LoginActivity.finish()
            } else { // else login failed
                progressDialog.dismiss()
                this@LoginActivity.onLoginFailed()
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

    private fun validate(): Boolean {
        var valid = true

        val email = _emailText.text.toString()
        val password = _passwordText.text.toString()

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.error = ("enter a valid email address")
            valid = false
        } else {
            _emailText.setError(null)
        }

        if (password.isEmpty() || password.length < 4 || password.length > 10) {
            _passwordText.error = ("between 4 and 10 alphanumeric characters")
            valid = false
        } else {
            _passwordText.error = (null)
        }

        return valid
    }
//    private fun populateAutoComplete() {
//        if (!mayRequestContacts()) {
//            return
//        }
//
//        loaderManager.initLoader(0, null, this)
//    }
//
//    private fun mayRequestContacts(): Boolean {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            return true
//        }
//        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
//            return true
//        }
//        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
//            Snackbar.make(email, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
//                    .setAction(android.R.string.ok,
//                            { requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS) })
//        } else {
//            requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS)
//        }
//        return false
//    }
//
//    /**
//     * Callback received when a permissions request has been completed.
//     */
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
//                                            grantResults: IntArray) {
//        if (requestCode == REQUEST_READ_CONTACTS) {
//            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                populateAutoComplete()
//            }
//        }
//    }
//
//
//    /**
//     * Attempts to sign in or register the account specified by the login form.
//     * If there are form errors (invalid email, missing fields, etc.), the
//     * errors are presented and no actual login attempt is made.
//     */
//    private fun attemptLogin() {
//        if (mAuthTask != null) {
//            return
//        }
//
//        // Reset errors.
//        email.error = null
//        password.error = null
//
//        // Store values at the time of the login attempt.
//        val emailStr = email.text.toString()
//        val passwordStr = password.text.toString()
//
//        var cancel = false
//        var focusView: View? = null
//
//        // Check for a valid password, if the user entered one.
//        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
//            password.error = getString(R.string.error_invalid_password)
//            focusView = password
//            cancel = true
//        }
//
//        // Check for a valid email address.
//        if (TextUtils.isEmpty(emailStr)) {
//            email.error = getString(R.string.error_field_required)
//            focusView = email
//            cancel = true
//        } else if (!isEmailValid(emailStr)) {
//            email.error = getString(R.string.error_invalid_email)
//            focusView = email
//            cancel = true
//        }
//
//        if (cancel) {
//            // There was an error; don't attempt login and focus the first
//            // form field with an error.
//            focusView?.requestFocus()
//        } else {
//            // Show a progress spinner, and kick off a background task to
//            // perform the user login attempt.
//            showProgress(true)
//            mAuthTask = UserLoginTask(emailStr, passwordStr)
//            mAuthTask!!.execute(null as Void?)
//        }
//    }
//
//    private fun isEmailValid(email: String): Boolean {
//        //TODO: Replace this with your own logic
//        return email.contains("@")
//    }
//
//    private fun isPasswordValid(password: String): Boolean {
//        //TODO: Replace this with your own logic
//        return password.length > 4
//    }
//
//    /**
//     * Shows the progress UI and hides the login form.
//     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
//    private fun showProgress(show: Boolean) {
//        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
//        // for very easy animations. If available, use these APIs to fade-in
//        // the progress spinner.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
//            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
//
//            login_form.visibility = if (show) View.GONE else View.VISIBLE
//            login_form.animate()
//                    .setDuration(shortAnimTime)
//                    .alpha((if (show) 0 else 1).toFloat())
//                    .setListener(object : AnimatorListenerAdapter() {
//                        override fun onAnimationEnd(animation: Animator) {
//                            login_form.visibility = if (show) View.GONE else View.VISIBLE
//                        }
//                    })
//
//            login_progress.visibility = if (show) View.VISIBLE else View.GONE
//            login_progress.animate()
//                    .setDuration(shortAnimTime)
//                    .alpha((if (show) 1 else 0).toFloat())
//                    .setListener(object : AnimatorListenerAdapter() {
//                        override fun onAnimationEnd(animation: Animator) {
//                            login_progress.visibility = if (show) View.VISIBLE else View.GONE
//                        }
//                    })
//        } else {
//            // The ViewPropertyAnimator APIs are not available, so simply show
//            // and hide the relevant UI components.
//            login_progress.visibility = if (show) View.VISIBLE else View.GONE
//            login_form.visibility = if (show) View.GONE else View.VISIBLE
//        }
//    }
//
//    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
//        return CursorLoader(this,
//                // Retrieve data rows for the device user's 'profile' contact.
//                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
//                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,
//
//                // Select only email addresses.
//                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email
//                .CONTENT_ITEM_TYPE),
//
//                // Show primary email addresses first. Note that there won't be
//                // a primary email address if the user hasn't specified one.
//                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC")
//    }
//
//    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
//        val emails = ArrayList<String>()
//        cursor.moveToFirst()
//        while (!cursor.isAfterLast) {
//            emails.add(cursor.getString(ProfileQuery.ADDRESS))
//            cursor.moveToNext()
//        }
//
//        addEmailsToAutoComplete(emails)
//    }
//
//    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
//
//    }
//
//    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
//        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
//        val adapter = ArrayAdapter(this@LoginActivity,
//                android.R.layout.simple_dropdown_item_1line, emailAddressCollection)
//
//        email.setAdapter(adapter)
//    }
//
//    object ProfileQuery {
//        val PROJECTION = arrayOf(
//                ContactsContract.CommonDataKinds.Email.ADDRESS,
//                ContactsContract.CommonDataKinds.Email.IS_PRIMARY)
//        val ADDRESS = 0
//        val IS_PRIMARY = 1
//    }
//
//    /**
//     * Represents an asynchronous login/registration task used to authenticate
//     * the user.
//     */
//    inner class UserLoginTask internal constructor(private val mEmail: String, private val mPassword: String) : AsyncTask<Void, Void, Boolean>() {
//
//        override fun doInBackground(vararg params: Void): Boolean? {
//            // TODO: attempt authentication against a network service.
//
//            try {
//                // Simulate network access.
//                Thread.sleep(2000)
//            } catch (e: InterruptedException) {
//                return false
//            }
//
//            return DUMMY_CREDENTIALS
//                    .map { it.split(":") }
//                    .firstOrNull { it[0] == mEmail }
//                    ?.let {
//                        // Account exists, return true if the password matches.
//                        it[1] == mPassword
//                    }
//                    ?: true
//        }
//
//        override fun onPostExecute(success: Boolean?) {
//            mAuthTask = null
//            showProgress(false)
//
//            if (success!!) {
//                finish()
//            } else {
//                password.error = getString(R.string.error_incorrect_password)
//                password.requestFocus()
//            }
//        }
//
//        override fun onCancelled() {
//            mAuthTask = null
//            showProgress(false)
//        }
//    }
//
//    companion object {
//
//        /**
//         * Id to identity READ_CONTACTS permission request.
//         */
//        private val REQUEST_READ_CONTACTS = 0
//
//        /**
//         * A dummy authentication store containing known user names and passwords.
//         * TODO: remove after connecting to a real authentication system.
//         */
//        private val DUMMY_CREDENTIALS = arrayOf("foo@example.com:hello", "bar@example.com:world")
//    }
}
