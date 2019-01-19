package run.drop.app

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.view.MotionEvent
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.ux.ArFragment


class DropActivity : AppCompatActivity(), LocationProviderDialog.OpenSettingsListener {

    private var requestPermissionCode: Int = 42

    private var locationHandler: LocationHandler? = null

    private var arFragment: ArFragment? = null


    private fun checkPermissions() {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        requestPermissionCode)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop)
        setStatusBarColor(window, this)

        // views for test only
        val quitButton: Button = findViewById(R.id.quit_btn)
        val locationButton: Button = findViewById(R.id.location_btn)
        val latitudeView: TextView = findViewById(R.id.latitude)
        val longitudeView: TextView = findViewById(R.id.longitude)
        val altitudeView: TextView = findViewById(R.id.altitude)

        // test authentication
        testTokenAuth()

        // check permissions
        checkPermissions()

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        arFragment?.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, _: MotionEvent ->
            dropButtonEvent(hitResult, plane)
        }

        // check device location
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isLocationEnabled) {
            val dialog = LocationProviderDialog()
            dialog.show(supportFragmentManager, "LocationProviderDialog")
        }

        // init location handler if has permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationHandler = LocationHandler(this)
        }

        quitButton.setOnClickListener {
            TokenStore.clearToken(this)
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        locationButton.setOnClickListener {
            val location: Location = locationHandler!!.lastLocation!!

            latitudeView.text = location.latitude.toString()
            longitudeView.text = location.longitude.toString()
            altitudeView.text = location.altitude.toString()
        }
    }

    private fun dropButtonEvent(hitResult: HitResult, plane: Plane) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.drop_text_dialog)

        val dropTextInput = dialog.findViewById<EditText>(R.id.dropTextInput)
        val dropSubmit = dialog.findViewById<Button>(R.id.dropSubmit)
        val textSize = dialog.findViewById<SeekBar>(R.id.seekBarSize)
        val colorPicker = dialog.findViewById<LinearLayout>(R.id.colorPicker)
        val colorPickerButton = dialog.findViewById<Button>(R.id.colorPickerButton)

        // only for presentation
        colorPickerButton.setOnClickListener {
            val color = colorPicker.background as ColorDrawable
            when (color.color) {
                ContextCompat.getColor(this, R.color.textColor1) -> colorPicker.setBackgroundResource(R.color.textColor2)
                ContextCompat.getColor(this, R.color.textColor2) -> colorPicker.setBackgroundResource(R.color.textColor3)
                ContextCompat.getColor(this, R.color.textColor3) -> colorPicker.setBackgroundResource(R.color.textColor4)
                ContextCompat.getColor(this, R.color.textColor4) -> colorPicker.setBackgroundResource(R.color.textColor5)
                ContextCompat.getColor(this, R.color.textColor5) -> colorPicker.setBackgroundResource(R.color.textColor1)
            }
        }

        dropSubmit.setOnClickListener {
            val color = colorPicker.background as ColorDrawable

            DropText(this,
                    hitResult.createAnchor(),
                    TextEntity(dropTextInput.text.toString(), textSize.progress.toFloat(), color.color),
                    this.arFragment!!,
                    plane)
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroy() {
        if (locationHandler != null) {
            locationHandler!!.removeLocationUpdates()
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            requestPermissionCode -> {
                when (grantResults[0]) {
                    PackageManager.PERMISSION_DENIED -> {
                        Toast.makeText(this, "Drop cannot access to location, permission denied",
                                Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    PackageManager.PERMISSION_GRANTED -> locationHandler = LocationHandler(this)
                }
            }
        }
    }

    private fun testTokenAuth() {
        Apollo.client.query(
                AmIAuthQuery.builder().build()).enqueue(object : ApolloCall.Callback<AmIAuthQuery.Data>() {

            override fun onResponse(response: Response<AmIAuthQuery.Data>) {
                if (!response.data()!!.amIAuth().isAuth()) {
                    startActivity(Intent(this@DropActivity, SignInActivity::class.java))
                    finish()
                }
            }

            override fun onFailure(e: ApolloException) {}
        })
    }

    override fun onOpenSettingsClick(dialog: DialogFragment) {
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
}
