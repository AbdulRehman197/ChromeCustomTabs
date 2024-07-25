package com.example.chromecustomtabs


//import androidx.browser.customtabs.CustomTabsService.FilePurpose
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsCallback.NAVIGATION_FINISHED
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
import androidx.core.content.ContextCompat
import com.google.androidbrowserhelper.trusted.TwaLauncher
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL

import kotlin.io.encoding.ExperimentalEncodingApi

class MainActivity : AppCompatActivity() {

    var mClient: CustomTabsClient? = null
    var mSession: CustomTabsSession? = null
    val uRL = Uri.parse("https://twa-app-741b9.web.app")
    private val url2 = "https://twa-sample-app.web.app"

    // This origin is going to be validated via DAL, please see
// (https://developer.chrome.com/docs/android/post-message-twa#add_the_app_to_web_validation),
// it has to either start with http or https.
//     val SOURCE_ORIGIN = Uri.parse("https://sayedelabady.github.io/")
    val targetOrigin = Uri.parse("https://twa-app-741b9.web.app")
    var mValidated = false

    val tAG = "PostMessageDemo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // No need to ask for permission as the compileSDK is 31.
        MessageNotificationHandler.createNotificationChannelIfNeeded(this)
        val chromeCustomTab = findViewById<Button>(/* id = */ R.id.chromeTab)
        val chromeCustomTab1 = findViewById<Button>(/* id = */ R.id.chromeTab1)

        val mt = TwaLauncher(this)
        chromeCustomTab.setOnClickListener {
            bindCustomTabsService()
        }
        chromeCustomTab1.setOnClickListener {
            val uri = Uri.parse(url2)
            mt.launch(uri)
        }
    }


    @OptIn(ExperimentalEncodingApi::class)
    private val customTabsCallback = object : CustomTabsCallback() {


        override fun onRelationshipValidationResult(
            relation: Int,
            requestedOrigin: Uri,
            result: Boolean,
            extras: Bundle?
        ) {
            // If this fails:
            // - Have you called warmup?
            // - Have you set up Digital Asset Links correctly?
            // - Double check what browser you're using.
            Log.d(tAG, "Relationship result: $result")
            mValidated = result
        }

        // Listens for navigation, requests the postMessage channel when one completes.
        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
            if (navigationEvent != NAVIGATION_FINISHED) {
                return
            }

            if (!mValidated) {
                Log.d(tAG, "Not starting PostMessage as validation didn't succeed.")
            }

            // If this fails:
            // - Have you included PostMessageService in your AndroidManifest.xml?
            val result = mSession?.requestPostMessageChannel(targetOrigin)
            Log.d(tAG, "Requested Post Message Channel: $result")
        }

        //        @SuppressLint("WrongConstant")
        override fun onMessageChannelReady(extras: Bundle?) {

            Log.d(tAG, "Message channel ready.")
//

            val result = mSession?.postMessage("First message", null)



        }

        override fun onPostMessage(message: String, extras: Bundle?) {
            var string: String? = ""
            super.onPostMessage(message, extras)
            if (message.contains("ACK")) {
                return
            }
            MessageNotificationHandler.showNotificationWithMessage(this@MainActivity, message)
            try {

                val inputStream: InputStream =
                    assets.open("demo.txt")
//                var mImage = findViewById<ImageView>(R.id.image)
//                val d = Drawable.createFromStream(inputStream, null)
                // set image to ImageView
//                mImage.setImageDrawable(d);
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
//              val result   = Base64.encodeToString(buffer, Base64.DEFAULT);

                string = String(buffer)
                mSession?.postMessage(string,null)
                Log.d(tAG, "Got message: ${buffer.toString().length}")
            } catch (e: IOException) {
                e.printStackTrace()
            }
//            File("demofile").writeText(string.toString())

        }
    }

    private fun bindCustomTabsService() {
        val packageName = CustomTabsClient.getPackageName(this, null)
        Toast.makeText(this, "Binding to $packageName", Toast.LENGTH_SHORT).show()
        CustomTabsClient.bindCustomTabsService(this, packageName,
            object : CustomTabsServiceConnection() {
                override fun onCustomTabsServiceConnected(
                    name: ComponentName,
                    client: CustomTabsClient
                ) {
                    mClient = client

                    // Note: validateRelationship requires warmup to have been called.
                    client.warmup(0L)

                    mSession = mClient?.newSession(customTabsCallback)

                    launch(uRL)
                    registerBroadcastReceiver()
                }

                override fun onServiceDisconnected(componentName: ComponentName) {
                    mClient = null
                }
            })
    }

    // The demo should work for both CCT and TWA but here we are using TWA.
    private fun launch(addr: Uri) {
        TrustedWebActivityIntentBuilder(addr).build(mSession!!)
            .launchTrustedWebActivity(this@MainActivity)
    }

    @SuppressLint("WrongConstant")
    private fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(PostMessageBroadcastReceiver.POST_MESSAGE_ACTION)
        }
        ContextCompat.registerReceiver(
            this,
            PostMessageBroadcastReceiver(mSession),
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun readFileAsTextUsingInputStream(fileName: String) =
        File(fileName).inputStream().readBytes().toString(Charsets.UTF_8)
}

