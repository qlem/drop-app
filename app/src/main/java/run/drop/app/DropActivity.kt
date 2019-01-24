package run.drop.app

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.*
import com.google.ar.sceneform.ux.ArFragment
import run.drop.app.apollo.Apollo
import run.drop.app.rendering.Message
import run.drop.app.rendering.DropRenderer
import run.drop.app.location.LocationHandler
import run.drop.app.apollo.TokenHandler
import run.drop.app.location.OnLocationUpdateListener
import run.drop.app.rendering.Drop
import run.drop.app.utils.colorHexStringToInt
import run.drop.app.utils.colorIntToHexString
import run.drop.app.utils.setStatusBarColor
import kotlin.collections.ArrayList

class DropActivity : AppCompatActivity() {

    companion object {
        var drops: MutableList<Drop> = ArrayList()
    }

    private var locationHandler: LocationHandler? = null
    private var arFragment: ArFragment? = null

    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            val currentLocation = LocationHandler.lastLocation
            if (currentLocation != null) {
                updateDropList(currentLocation.latitude, currentLocation.longitude, 10)
            }
            handler.postDelayed(this, 2000)
        }
    }

    private fun initLocationHandler() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationHandler = LocationHandler(this)
            locationHandler?.setOnLocationUpdateListener(object : OnLocationUpdateListener {
                override fun onLocationUpdateListener(location: Location) {
                    val latitudeView: TextView = findViewById(R.id.latitude)
                    val longitudeView: TextView = findViewById(R.id.longitude)
                    val altitudeView: TextView = findViewById(R.id.altitude)
                    latitudeView.text = location.latitude.toString()
                    longitudeView.text = location.longitude.toString()
                    altitudeView.text = location.altitude.toString()
                }
            })
        }
    }

    private fun initArScene() {
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        arFragment?.setOnTapArPlaneListener { _: HitResult, _: Plane, _: MotionEvent ->
            touchEvent()
        }
        arFragment!!.arSceneView.scene.addOnUpdateListener {
            var plane: Plane? = null
            var anchor: Anchor? = null
            val frame: Frame = arFragment!!.arSceneView.session.update()
            // TODO increase precision of hitTest
            val hitResults: MutableList<HitResult> = frame.hitTest(500f, 1000f)
            for (hitResult in hitResults) {
                if (hitResult.trackable is Plane) {
                    plane = hitResult.trackable as Plane
                    anchor = hitResults.first().createAnchor()
                    break
                }
            }

            if (plane == null || anchor == null || plane.trackingState != TrackingState.TRACKING) {
                return@addOnUpdateListener
            }

            for (drop in drops) {
                if (!drop.isDisplayed) {
                    drop.isDisplayed = true
                    DropRenderer(this, anchor, drop.message, arFragment!!, plane)
                    break
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop)
        setStatusBarColor(window, this)

        // init location handler
        initLocationHandler()

        // init ar scene
        initArScene()

        // init button for disconnect
        val quitButton: Button = findViewById(R.id.quit_btn)
        quitButton.setOnClickListener {
            TokenHandler.clearToken(this)
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun touchEvent() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.drop_dialog)

        val dropTextInput = dialog.findViewById<EditText>(R.id.dropTextInput)
        val dropSubmit = dialog.findViewById<Button>(R.id.dropSubmit)
        // TODO save text size too
        // val textSize = dialog.findViewById<SeekBar>(R.id.seekBarSize)
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
            val location = LocationHandler.lastLocation
            if (location != null) {
                saveDrop(dropTextInput.text.toString(), colorIntToHexString(color.color), location.latitude,
                        location.longitude, location.altitude)
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateDropList(latitude: Double, longitude: Double, radius: Int) {
        Apollo.client.query(
                DroppedAroundQuery.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .radius(radius)
                        .build()).enqueue(object : ApolloCall.Callback<DroppedAroundQuery.Data>() {

            override fun onResponse(response: Response<DroppedAroundQuery.Data>) {
                val data = response.data()!!.droppedAround
                // TODO optimize here
                if (!data.isEmpty()) {
                    data.forEach { item ->
                        var isPresent = false
                        drops.forEach { drop ->
                            if (drop.id == item.id) {
                                isPresent = true
                            }
                        }
                        if (!isPresent) {
                            val message = Message(item.text, 50f, colorHexStringToInt(item.color))
                            drops.add(Drop(message, item.id, false, item.location.latitude,
                                    item.location.longitude, item.location.altitude))
                        }
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message)
                e.printStackTrace()
            }

        })
    }

    private fun saveDrop(text: String, color: String, latitude: Double, longitude: Double, altitude: Double) {
        Apollo.client.mutate(CreateDropMutation.Builder()
                .text(text)
                .color(color)
                .latitude(latitude)
                .longitude(longitude)
                .altitude(altitude)
                .build()).enqueue(object : ApolloCall.Callback<CreateDropMutation.Data>() {

            override fun onResponse(response: Response<CreateDropMutation.Data>) {
                Log.i("APOLLO", response.data()!!.createDrop.id)
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message)
                e.printStackTrace()
            }
        })
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Drop needs camera and location access", Toast.LENGTH_SHORT).show()
            finish()
        }
        initLocationHandler()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LocationHandler.REQUEST_SETTINGS_CODE -> when (resultCode) {
                Activity.RESULT_OK -> initLocationHandler()
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, "Drop needs device location enabled", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        handler.post(runnable)
    }

    override fun onResume() {
        super.onResume()
        checkAuthentication()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable)
        locationHandler?.removeLocationUpdates()
    }
}
