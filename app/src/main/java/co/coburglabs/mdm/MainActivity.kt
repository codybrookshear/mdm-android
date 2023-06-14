package co.coburglabs.mdm

import android.Manifest
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.coburglabs.mdm.ui.theme.MDMTheme
import java.io.File

class MainActivity() : ComponentActivity() {

    private val TAG = "MainActivity"
    private var isDeviceOwner = false
    private var devicePolicyManager : DevicePolicyManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager

        var doMessage = "App's device owner state is unknown"

        if (savedInstanceState == null) {
            if (devicePolicyManager!!.isDeviceOwnerApp(applicationContext.packageName)) {
                doMessage = "App is device owner"
                isDeviceOwner = true
            } else {
                doMessage = "App is not device owner"
            }
        }

        Log.e(TAG, doMessage)

        setContent {
            MDMTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Status(doMessage)
                    InstallButton()
                }
            }
        }
    }


    @Composable
    @Preview
    fun InstallButton() {
        Column(Modifier.fillMaxWidth().absolutePadding(10.dp, 200.dp, 10.dp, 0.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { InstallSampleApp() },
                modifier = Modifier.height(100.dp).width(200.dp)) {
                    Text(text = "Install app")
            }
        }
    }

    fun InstallSampleApp() {
        if (devicePolicyManager!!.isDeviceOwnerApp(applicationContext.packageName)) {
            Log.e(TAG, "App is device owner")
            isDeviceOwner = true
        }

        if (!isDeviceOwner) {
            Log.e(TAG, "can't install, not device owner")
            return
        }

        val cn = ComponentName(this, DeviceOwnerReceiver::class.java)

        devicePolicyManager!!.setPermissionGrantState(cn, packageName,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED)

        devicePolicyManager!!.setPermissionGrantState(cn, packageName,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED)

        try {
            ///storage/emulated/0/Android/data/co.coburglabs.mdm/files/test.apk

            Log.d(TAG, getExternalFilesDir(null).toString())

            val file = File(getExternalFilesDir(null), "test.apk")

            if(file.exists()){
                install(this@MainActivity, packageName, file.absolutePath)
            }else{
                Toast.makeText(this,"File is not available", Toast.LENGTH_LONG).show()
            }

            //Runtime.getRuntime().exec("dpm set-device-owner com.cnx.silentupdate/.DevAdminReceiver");

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun install(context: Context, packageName: String, apkPath: String) {

        val packageInstaller = context.packageManager.packageInstaller

        // Prepare params for installing one APK file with MODE_FULL_INSTALL
        // We could use MODE_INHERIT_EXISTING to install multiple split APKs
        val params =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        //params.setAppPackageName(packageName)

        // Get a PackageInstaller.Session for performing the actual update
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        // Copy APK file bytes into OutputStream provided by install Session
        val out = session.openWrite("COSU", 0, -1)
        val fis = File(apkPath).inputStream()
        fis.copyTo(out)
        session.fsync(out)
        out.close()
        fis.close()

        // The app gets killed after installation session commit
        session.commit(
            PendingIntent.getBroadcast(
                context, sessionId,
                Intent("co.coburglabs.mdm.INSTALL_COMPLETE"), FLAG_MUTABLE
            ).intentSender
        )
    }

    @Composable
    fun Status(text: String, modifier: Modifier = Modifier) {
        Text(
            text = text,
            modifier = modifier
        )
    }

}

