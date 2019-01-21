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
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.ux.ArFragment
import run.drop.app.apollo.Apollo
import run.drop.app.rendering.Message
import run.drop.app.rendering.DropRenderer
import run.drop.app.location.LocationHandler
import run.drop.app.location.LocationProviderDialog
import run.drop.app.apollo.TokenHandler
import run.drop.app.utils.setStatusBarColor


class DropActivity : AppCompatActivity(), LocationProviderDialog.OpenSettingsListener {

    private var locationHandler: LocationHandler? = null

    private var arFragment: ArFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop)
        setStatusBarColor(window, this)

        // init ar scene
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        arFragment?.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, _: MotionEvent ->
            touchEvent(hitResult, plane)
        }

        // init location handler
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationHandler = LocationHandler(this)
        }

        // check device location
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val dialog = LocationProviderDialog()
            dialog.show(supportFragmentManager, "LocationProviderDialog")
        }

        // TODO remove below for release
        val quitButton: Button = findViewById(R.id.quit_btn)
        val locationButton: Button = findViewById(R.id.location_btn)
        val latitudeView: TextView = findViewById(R.id.latitude)
        val longitudeView: TextView = findViewById(R.id.longitude)
        val altitudeView: TextView = findViewById(R.id.altitude)

        quitButton.setOnClickListener {
            TokenHandler.clearToken(this)
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

    private fun touchEvent(hitResult: HitResult, plane: Plane) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.drop_dialog)

        val dropTextInput = dialog.findViewById<EditText>(R.id.dropTextInput)
        val dropSubmit = dialog.findViewById<Button>(R.id.dropSubmit)
        val textSize = dialog.findViewById<SeekBar>(R.id.seekBarSize)
        val colorPicker = dialog.findViewById<LinearLayout>(R.id.colorPicker)
        val colorPickerButton = dialog.findViewById<Button>(R.id.colorPickerButton)

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

            DropRenderer(this, hitResult.createAnchor(),
                    Message(dropTextInput.text.toString(), textSize.progress.toFloat(), color.color),
                    this.arFragment!!, plane)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun checkAuthentication() {
        Apollo.client.query(
                AmIAuthQuery.builder().build()).enqueue(object : ApolloCall.Callback<AmIAuthQuery.Data>() {

            override fun onResponse(response: Response<AmIAuthQuery.Data>) {
                if (!response.data()!!.amIAuth().isAuth()) {
                    startActivity(Intent(this@DropActivity, SignInActivity::class.java))
                    finish()
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message)
                e.printStackTrace()
            }
        })
    }

    override fun onOpenSettingsClick(dialog: DialogFragment) {
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Drop needs camera and location access", Toast.LENGTH_SHORT).show()
            finish()
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationHandler = LocationHandler(this)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAuthentication()
    }

    override fun onDestroy() {
        if (locationHandler != null) {
            locationHandler!!.removeLocationUpdates()
        }
        super.onDestroy()
    }
}
