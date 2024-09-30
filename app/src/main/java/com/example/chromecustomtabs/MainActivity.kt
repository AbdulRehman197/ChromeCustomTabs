package com.example.chromecustomtabs


//import androidx.browser.customtabs.CustomTabsService.FilePurpose


//import kotlin.io.encoding.Base64

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.androidbrowserhelper.trusted.TwaLauncher
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Comparator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    var mClient: CustomTabsClient? = null
    var mSession: CustomTabsSession? = null
    val uRL = Uri.parse("https://twa-app-741b9.web.app")
    private val url2 = "https://twa-sample-app.web.app"
    var startTime: Long? = null
    var endTime: Long? = null

    private lateinit var mDrive: Drive
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FileDatabase
     var fileBase64Decoded =  byteArrayOf()

    var setString = mutableListOf<String>()
    var fileChunks = mutableMapOf<String, MutableMap<String,String>>()
    var unCompletedSha = mutableListOf<String>()
    var corruptedSha = mutableListOf<String>()
    var missingPakects = mutableListOf<String>()
    // This origin is going to be validated via DAL, please see
// (https://developer.chrome.com/docs/android/post-message-twa#add_the_app_to_web_validation),
// it has to either start with http or https.
//     val SOURCE_ORIGIN = Uri.parse("https://sayedelabady.github.io/")
    val targetOrigin = Uri.parse("https://twa-app-741b9.web.app")
    var mValidated = false
    private lateinit var googleSignInClient: GoogleSignInClient
    val tAG = "PostMessageDemo"
    var isMissing = false
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // No need to ask for permission as the compileSDK is 31.
        MessageNotificationHandler.createNotificationChannelIfNeeded(this)
        checkStoragePermission(this@MainActivity)
        val chromeCustomTab = findViewById<Button>(/* id = */ R.id.chromeTab)
        val chromeCustomTab1 = findViewById<Button>(/* id = */ R.id.chromeTab1)

        val googleSignOutBtn = findViewById<Button>(R.id.googleSignout)
        val uploadBtn = findViewById<Button>(R.id.upload)
        val loadBtn = findViewById<Button>(R.id.loadImage)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()
        mDrive = getDriveService(this)!!

        db = Room.databaseBuilder(applicationContext,FileDatabase::class.java, "filesshasDB").build()

        val mt = TwaLauncher(this)
        chromeCustomTab.setOnClickListener {

            bindCustomTabsService()


        }
        chromeCustomTab1.setOnClickListener {
            val uri = Uri.parse(url2)
            mt.launch(uri)
        }

        googleSignOutBtn.setOnClickListener {
            googleSignOut()
        }

        uploadBtn.setOnClickListener {
            GlobalScope.async(Dispatchers.IO) {
                checkStoragePermission(this@MainActivity)
            }
        }
        loadBtn.setOnClickListener {
//            checkTextStoragePermission(this@MainActivity)


//            val decodedString = setString.joinToString(separator = "")
//               val image = findViewById<ImageView>(R.id.imageView)
//                val bimapImage: Bitmap =  BitmapFactory.decodeByteArray(decodedString.toByteArray(), 0 , decodedString.length)
//                image.setImageBitmap(bimapImage)
        }


    }


    @OptIn(ExperimentalEncodingApi::class, ExperimentalUnsignedTypes::class)
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
            val result = mSession?.requestPostMessageChannel(uRL)
            Log.d(tAG, "Requested Post Message Channel: $result")
        }

        //        @SuppressLint("WrongConstant")
        override fun onMessageChannelReady(extras: Bundle?) {

            Log.d(tAG, "Message channel ready.")
//


            val result = mSession?.postMessage("First message", null)



        }

        @SuppressLint("Range")
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPostMessage(message: String, extras: Bundle?) {
            super.onPostMessage(message, extras)
            val messagesList =   splitMessage(message)
            val type = messagesList[0]
            when (type) {
                "sha256" -> {
                    Log.d(tAG +  "sha256", messagesList.toString())
                   lifecycleScope.launch {
                      try{
                          val filesha: FIleSHA =  db.fileshaDao().getFileSha(messagesList[1])
                          Log.d(tAG + "filesha", filesha.id.toString())
                          mSession?.postMessage("sha256Exist|true",null)
                      }catch (e: Exception){
                          Log.d(tAG + "value not found",  e.toString())
                          db.fileshaDao().insertSha(FIleSHA(0,"filedemo", messagesList[1]))
                          mSession?.postMessage("sha256Exist|${messagesList[1]}|false",null)
                      }


//                       db.fileshaDao().insertSha(FIleSHA(0,"demofile", messagesList[1]))
                   }

//                    mSession?.postMessage("false|",null)
                }
                "startsending" -> {
                    val singleChunkData = mutableMapOf<String,String>()
                    fileChunks[messagesList[1]] = singleChunkData
                    mSession?.postMessage("isListReady|${messagesList[1]}|true",null)


                }
                "packet" -> {

                    Log.d(tAG+"packet", messagesList[3])
                    Log.d(tAG+"packet", messagesList[4])

//                        lifecycleScope.launch(Dispatchers.IO){
                          val isExistElement = fileChunks[messagesList[5]]?.containsKey(messagesList[3])
                                if(isExistElement == false) {
                                    fileChunks[messagesList[5]]?.put(messagesList[3], messagesList[6])
                                }

//                    }


                }
                "completesending" -> {
                    Log.d(tAG+"currentchunkno", messagesList[1])
                    lifecycleScope.launch(Dispatchers.IO) {
                        checkPackets(messagesList[1],messagesList[2])
                    }
                    mSession?.postMessage("completesending|true",null)



                }
                "filename" -> {
                    Log.d(tAG+"filename", messagesList[1])
//                    mSession?.postMessage("filecompleted|",null)
                    Log.d(tAG + "packetList",fileChunks.size.toString())
                    fileChunks.clear()
                }
                "deleteDB" -> {
                    Log.d(tAG + "delete", "Deleted")
                    lifecycleScope.launch {
                        db.fileshaDao().deleteAll()
                    }
                }
                "demo" -> {
                    Log.d(tAG, "demo message for cheking port")
                }
                else -> { // Note the block
                    print("x is neither 1 nor 2")
                }
            }


//                    if (message == "data") {
//                        MessageNotificationHandler.showNotificationWithMessage(
//                            this@MainActivity,
//                            message
//                        )
//                        mSession?.postMessage("confirm", null)
//                        startTime = System.currentTimeMillis()
//
//                    } else

//                        if (message.contains(".")) {
//
//                            Log.d(tAG + "File Name", message.contains(".").toString())
//                        try {
////                          lifecycleScope.launch(Dispatchers.Default){
////                              delay(2000)
//                              val reconstructedData = processChunks(setString)
//                              Log.d("final result", reconstructedData.size.toString())
//                          }

//                            endTime = System.currentTimeMillis()
//                            Log.d(tAG + "Time Taken in Ms", (endTime!! - startTime!!).toString())

//                                Log.d(tAG + "setsize", setString.size.toString())
//                                Log.d(
//                                    tAG + "set",
//                                    setString.joinToString(separator = "").length.toString()
//                                )

//                                    for (i in setString) {
////                                    val decodedStringBaseString: ByteArray =
////                                        Base64.decode(
////                                            i,
////                                           0
////                                        )
////
////                                    fileBase64Decoded.plus(decodedStringBaseString)
//                                        Log.d(tAG+ "Loop", i.length.toString())
//
//                                    }

//
//                                for (i in singleChunkData) {
//                                    val decodedStringBaseString: ByteArray =
//                                        Base64.decode(
//                                            i.toByteArray(),
//                                           0,
//                                            i.toByteArray().size
//                                        )
//                                    Log.d(tAG+ "Loop value", singleChunkData[15])
//                                    fileBase64Decoded += (decodedStringBaseString)
//
//
//                                }

//                            Log.d(tAG+ "totalBytesArrayLength", fileBase64Decoded.size.toString())
//
//                            val chunkNO = setString.get(30)
//                            val spliteString = chunkNO.split("|")
//
//                            Log.d(tAG + "currentSmallChunkId", spliteString[0].length.toString())
//                            Log.d(tAG + "totalChunkID", spliteString[1].length.toString())
//                            Log.d(tAG + "chunkSHA256", spliteString[2].length.toString())
//                            Log.d(tAG + "Base64SHA256",  spliteString[3].length.toString())
//                            Log.d(tAG + "Base64String", spliteString[4].length.toString())
//                                withContext(Dispatchers.IO){
//                                    val path =
//                                        Environment.getExternalStoragePublicDirectory("AlivaTech")
//                                    path.mkdirs()
//////                                Log.d(tAG + "path", path.toString())
//                                    val file: File = File(path, message)
//                                    val fileOutPutStream = FileOutputStream(file)
//////////////                bimapImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOutPutStream)
////////////
//                                    fileOutPutStream.write(reconstructedData)
//                                    fileOutPutStream.flush();
////
//                                    fileOutPutStream.close()
//                                }


//                                val path =
//                                    Environment.getExternalStoragePublicDirectory("AlivaTech")
//                                path.mkdirs()
////                                Log.d(tAG + "path", path.toString())
//                                val file: File = File(path, message)
//                                val decodedString = setString.joinToString(separator = "")
//                            val value = setString.get(25)
//                            Log.d(tAG + "value", value)
//                            val source = decodedString.toByteArray()
//                                val decodedStringBaseString: ByteArray =
//                                    Base64.decode(
//                                        decodedString,
//                                       0
//                                    )
//                            Log.d(tAG + "base String size", decodedStringBaseString.size.toString())
//               val image = findViewById<ImageView>(R.id.imageView)
//                val bimapImage: Bitmap = BitmapFactory.decodeByteArray(
//                    fileBase64Decoded,
//                    0,
//                    fileBase64Decoded.size
//                )
//                                Log.d(tAG, "Writing...")
//               image.setImageBitmap(bimapImage)

//                             withContext(Dispatchers.IO){
//                                 val fileOutPutStream = FileOutputStream(file)
////////////                bimapImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOutPutStream)
//////////
//                                 fileOutPutStream.write(fileBase64Decoded)
//                                 fileOutPutStream.flush();
//
//                                 fileOutPutStream.close()
//
//                             }

                    //                        val decodedString: ByteArray =
                    //                            Base64.decode(base64String.toByteArray(), 0, base64String.length)
                    //                          launch(Dispatchers.Default){
                    //                              delay(2000)
                    //                              uploadFileToDrive(this@MainActivity,decodedString)
                    //                          }

                    //                          firebaseAuth.getAccessToken(false).addOnSuccessListener {
                    //                              Log.d(tAG + "Token", it.token.toString())
                    //                              val response = httpPost {
                    //                              url("https://www.googleapis.com/upload/drive/v3/files?uploadType=media")
                    //                              header {
                    //                                  "Authorization" to "Bearer ${it.token}"
                    //                                  "mimeType" to "application/vnd.google-apps.photo"
                    //                                  "Content-Type" to "application/jpg"
                    //                                  "name" to "myImage"
                    //                              }
                    //
                    //                              body {
                    //                                  bytes(decodedString)
                    //                              }
                    //                          }
                    //                              Log.d(tAG + "response", response.toString())
                    //                          }


                    //

                    //                        val image = findViewById<ImageView>(R.id.imageView)
                    //                        val bimapImage: Bitmap =
                    //                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
//                            Log.d(tAG, "bimapSuccess")
//                        withContext(Dispatchers.Main) {
////                              image.setImageBitmap(bimapImage)
//                            checkTextStoragePermission(this@MainActivity)
//                        }

//
//                        } catch (e: IOException) {
//                            e.printStackTrace()
//                        }


//                    } else {
//                            lifecycleScope.launch(Dispatchers.Default) {
//                        Log.d(tAG + "ThreadName", Thread.currentThread().name)
//
//                                setString.add(message)
////                                if(setString.size == 115){
//                                    val newMessgae = message.split("|")
//                                    Log.d(tAG+"split message", newMessgae[2].length.toString())
////                                    mSession?.postMessage("finished", null)
////                                }
//                        Log.d(tAG + "List Size", setString.size.toString())
//
//
//
//
////                        mSession?.postMessage("confirmation|${newMessage[1]}", null)
//
//
//                        }
////                               Log.d(tAG,"outofcontext")
//
//
//
//                }



        }

        @SuppressLint("SuspiciousIndentation")
        private fun checkPackets(sha256Base64: String, packetListSize: String) {
          val sha256: String =   parseBase64toSha256(sha256Base64)
          val packetsMap = fileChunks[sha256Base64]?.toSortedMap(compareBy { it.toInt() })

            if(packetsMap?.size == packetListSize.toInt())
//            if(isMissing)
            {

                Log.d(tAG + "List Length",  "List Length is ok ${packetsMap.keys}?.size")
                val packetListBase64String = packetsMap.values.joinToString("")
                Log.d(tAG ,"List Length is packetListBase64String ${packetsMap.keys.indices}?.size")
                val chunkSha256Bytes = parseBase64toByteArray(packetListBase64String)
                val chunkSha256String = sha256hash(chunkSha256Bytes)
                Log.d(tAG + "orginal sha256", sha256)
                Log.d(tAG + "chunk sha256", chunkSha256String)
                if(sha256 == chunkSha256String){
                        Log.d(tAG, "Chunk is ok")
                    mSession?.postMessage("completeChunkPackets|$sha256Base64",null)
//                    mSession?.postMessage("completesending|$sha256Base64",null)

                }else{
                    corruptedSha.add(sha256Base64)
                }

            }else{

//                isMissing = true
           if(packetsMap?.size!! > 0) {
               packetsMap.keys.indices.forEach { index ->
                   if(!packetsMap.containsKey(index.toString())){
                       // for test purpose
//                        if(index == 8){
                       missingPakects.add(index.toString())
                       Log.d(tAG + "check index", index.toString())
                       mSession?.postMessage("missingPacket|$packetListSize|$sha256Base64|${missingPakects}",null)
                   }

               }
           }

                unCompletedSha.add(sha256Base64)
            }

        }

        private fun parseBase64toSha256(base64String: String): String {
            val sha256ByteDecode: ByteArray =   parseBase64toByteArray(base64String)
            return String(sha256ByteDecode, Charsets.UTF_8)

        }

        private fun parseBase64toByteArray(sha256Base64: String): ByteArray {
            val source =  sha256Base64.toByteArray()
             return Base64.decode(source,0,source.size)
        }

        private fun splitMessage(message: String): List<String> {
            return  message.split("|")
        }

        private fun sha256hash(bytes: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }
    }



    // Function to decode Base64 and SHA256 hash check
    @OptIn(ExperimentalEncodingApi::class, ExperimentalUnsignedTypes::class)
    @RequiresApi(Build.VERSION_CODES.O)
//    fun processChunks(chunks: MutableList<String>): ByteArray {
//        val byteArrays = mutableListOf<ByteArray>()
//
//        // Process chunks in groups of 16
//        val groupSize = 22
//        for (i in chunks.indices step groupSize) {
//            val group = chunks.subList(i, (i + groupSize).coerceAtMost(chunks.size))
////            Log.d(tAG+ "processChunks fun 1",group.size.toString())
//            val concatenatedBase64 = processGroup(group)
////            Log.d(tAG+ "base64chunksize",concatenatedBase64.length.toString())
////            val decodedBytes = Base64.getDecoder().decode(concatenatedBase64)
//            val decodedBytes = Base64.decode(concatenatedBase64.toByteArray(),0,concatenatedBase64.length)
//
////            Log.d(tAG+ "single array size", decodedBytes.size.toString())
//            byteArrays.add(decodedBytes)
//        }
//        Log.d(tAG+ "final ArrayByte", byteArrays.flatten().size.toString())
//        // Combine all byte arrays into a single byte array
//        return byteArrays.flatten()
//    }

    // Helper function to process a group of chunks
    private fun processGroup(group: MutableList<String>): String {
        val base64Parts = mutableListOf<String>()

        // Extract base64 parts from each chunk in the group and append to the list
        for (chunk in group) {
//            Log.d(tAG+ "processGroup fun 1",group.size.toString())
            val (chunkId, sha256Part, base64Part) = parseChunk(chunk)
//            Log.d(tAG + "part String", base64Part)
//            Log.d(tAG+ "processGroup fun 2",group.size.toString())
            base64Parts.add(base64Part)
        }

        // Concatenate all base64 parts into one string
        return base64Parts.joinToString("")
    }

    // Helper function to parse a chunk string into Base64 part, SHA256 part, and chunk ID
    private fun parseChunk(chunk: String): Triple<String, String, String> {
        // Assuming the chunk format is "<base64>_<sha256>_<chunkid>"
        val parts = chunk.split('|')
//        Log.d(tAG + "parts", parts.toString())
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid chunk format")
        }
        return Triple(parts[0], parts[1], parts[2])
    }



    // Extension function to concatenate a list of ByteArrays
    private fun List<ByteArray>.flatten(): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        for (byteArray in this) {
            outputStream.write(byteArray)
        }
        return outputStream.toByteArray()
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
                    Log.d(tAG, "ProcessEnd")
                    mClient = null
                }
            })
    }

    // The demo should work for both CCT and TWA but here we are using TWA.
    private fun launch(addr: Uri) {
        TrustedWebActivityIntentBuilder(addr).build(mSession!!)
            .launchTrustedWebActivity(this@MainActivity)
    }


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


    fun getDriveService(context: Context): Drive? {
        GoogleSignIn.getLastSignedInAccount(context).let { googleAccount ->
            val credential =
                GoogleAccountCredential.usingOAuth2(this, listOf(DriveScopes.DRIVE_FILE))
            credential.selectedAccount = googleAccount?.account!!


            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(getString(R.string.app_name))
                .build()

        }

        return mDrive

    }

    fun uploadFileToDrive(context: MainActivity, fileBytesArray: ByteArray) {
        Log.d(tAG, "Drive Function Called")
        mDrive.let { googleDriveService ->
            lifecycleScope.launch {
                try {
                    val gFolder = com.google.api.services.drive.model.File()
                    // Set file name and MIME
                    gFolder.name = "My Backup Folder"
                    gFolder.mimeType = "application/vnd.google-apps.folder"

                    // You can also specify where to create the new Google folder
                    // passing a parent Folder Id
                    val parents: MutableList<String> = ArrayList(1)
                    gFolder.parents = parents

//                    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//                    val file  = File(path ,"image.jpg")
                    Log.d(tAG, "create file")

                    var file = File("newFile")
                    file.appendBytes(fileBytesArray)
                    val gfile = com.google.api.services.drive.model.File()
                    gfile.name = "ReactUploadImage"
                    val mimeType = "image/jpg"
                    val fileContent = FileContent(mimeType, file)
                    var fileid = ""
                    withContext(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            launch {

                                var mfolder =
                                    googleDriveService.Files().create(gFolder).setFields("id")
                                        .execute()
                                parents.add(mfolder.id)
                                gfile.parents = parents
                                var mFile =
                                    googleDriveService.Files().create(gfile, fileContent).execute()

                                Log.d("fileupload", mFile.toString())
                            }
                        }
                    }
                } catch (mianActivity: UserRecoverableAuthIOException) {
                    startActivity(mianActivity.intent);
                } catch (e: Exception) {
                    Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
                    Log.d("fileError", e.toString())

                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkStoragePermission(activity: MainActivity) {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    @OptIn(ExperimentalEncodingApi::class)


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
//                    uploadFileToDrive(this)
                } else {
                    Log.d("ERROR STORAGE", "permission denied" + requestCode.toString())
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }

        }

    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
    }

    fun googleSignOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) { task: Task<Void> ->

            firebaseAuth.signOut()
            startActivity(Intent(this, LoginGoogleAuth::class.java))
            finish()
            Toast.makeText(this, "GoogleSignOut Success", Toast.LENGTH_SHORT).show()

        }
    }
}

