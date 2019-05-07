package run.drop.app

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.ar.core.*
import com.google.ar.sceneform.ux.ArFragment
import run.drop.app.apollo.Apollo
import run.drop.app.apollo.TokenHandler
import run.drop.app.dropObject.Drop
import run.drop.app.dropObject.Message
import run.drop.app.dropObject.Social
import run.drop.app.dropObject.DLocation
import run.drop.app.location.LocationHandler
import run.drop.app.location.OnLocationUpdateListener
import run.drop.app.rendering.DropRenderer
import run.drop.app.utils.colorHexStringToInt
import run.drop.app.utils.colorIntToHexString
import run.drop.app.utils.setStatusBarColor
import java.text.DecimalFormat
import kotlin.collections.ArrayList
import com.thebluealliance.spectrum.SpectrumPalette
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import run.drop.app.rendering.Toaster
import android.hardware.SensorManager
import android.content.Context
import android.hardware.Sensor
import run.drop.app.sensor.SensorListener


class DropActivity : AppCompatActivity() {

    companion object {
        var drops: MutableList<Drop> = ArrayList()
    }

    private val sensorListener = SensorListener()

    private lateinit var locationHandler: LocationHandler
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetic: Sensor
    private lateinit var arFragment: ArFragment

    private var planeDetection = true
    private lateinit var toaster: Toaster

    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            val currentLocation = LocationHandler.lastLocation
            if (currentLocation != null) {
                refreshDropList(currentLocation.latitude, currentLocation.longitude, 10.0)
            }
            handler.postDelayed(this, 2000)
        }
    }

    private fun initSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    private fun initLocationHandler() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            val locationListener: OnLocationUpdateListener = object : OnLocationUpdateListener {
                override fun onLocationUpdateListener(location: Location) {
                    val df = DecimalFormat("#.###")
                    val latitudeView: TextView = findViewById(R.id.latitude)
                    val longitudeView: TextView = findViewById(R.id.longitude)
                    val altitudeView: TextView = findViewById(R.id.altitude)
                    latitudeView.text = df.format(location.latitude).toString()
                    longitudeView.text = df.format(location.longitude).toString()
                    altitudeView.text = df.format(location.altitude).toString()
                }
            }
            locationHandler = LocationHandler(this, locationListener)
        }
    }



    private fun initArScene() {
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        arFragment.setOnTapArPlaneListener { _: HitResult, _: Plane, _: MotionEvent ->
            saveDrop()
        }
        arFragment.arSceneView.scene.addOnUpdateListener {
            var plane: Plane? = null
            var anchor: Anchor? = null
            val frame: Frame = arFragment.arSceneView.session!!.update()

            // TODO increase hit test precision
            val cameraWidth: Float = frame.camera.textureIntrinsics.imageDimensions[0].toFloat()
            val cameraHeight: Float = frame.camera.textureIntrinsics.imageDimensions[1].toFloat()
            val hitResults: MutableList<HitResult> = frame.hitTest(cameraHeight / 2, cameraWidth / 2)
            for (hitResult in hitResults) {
                if (hitResult.trackable is Plane) {
                    plane = hitResult.trackable as Plane
                    anchor = hitResult.createAnchor()
                    break
                }
            }

            if (plane == null || anchor == null || plane.trackingState != TrackingState.TRACKING) {
                return@addOnUpdateListener
            }

            if (drops.isNotEmpty() && !drops[0].isDisplayed) {
                drops[0].isDisplayed = true
                DropRenderer(this, arFragment, anchor, plane, drops[0])
            }
        }
        toaster.show("Loaded AR scene")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop)
        setStatusBarColor(window, this)

        toaster = Toaster(findViewById(R.id.root_layout))

        // init location handler
        initLocationHandler()

        // init sensors
        initSensors()

        // init ar scene
        initArScene()

        // init logout button
        val quitButton: ImageButton = findViewById(R.id.logout_btn)
        quitButton.setOnClickListener {
            TokenHandler.clearToken(this)
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }

        // init magic button
        val magicButton: ImageButton = findViewById(R.id.refresh_btn)
        magicButton.setOnClickListener {
            if (drops.isNotEmpty() && drops[0].isDisplayed) {
                drops[0].anchorNode?.anchor?.detach()
            }
            drops.clear()
            toaster.show("Cleaned all drops")
        }

        // init plane button
        val planeButton: ImageButton = findViewById(R.id.plane_btn)
        planeButton.setOnClickListener {
            planeDetection = !planeDetection
            val session = arFragment.arSceneView.session!!
            session.pause()
            val config = session.config
            config.planeFindingMode =
                    if (planeDetection)
                        Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    else
                        Config.PlaneFindingMode.DISABLED
            session.configure(config)
            session.resume()
            toaster.show(("Plane detection turned " + if (planeDetection) "ON" else "OFF"))
        }

        // init next button
        val nextButton: Button = findViewById(R.id.next_btn)
        nextButton.setOnClickListener {
            if (drops.isNotEmpty() && drops[0].isDisplayed) {
                drops[0].anchorNode?.anchor?.detach()
                val tmp: Drop = drops.removeAt(0)
                tmp.isDisplayed = false
                drops.add(tmp)
            }
        }
    }

    private fun saveDrop() {
        val dialog = DropDialog(this)
        dialog.setContentView(R.layout.drop_dialog)

        val dropTextInput = dialog.findViewById<EditText>(R.id.dropTextInput)

        val dropSubmit = dialog.findViewById<Button>(R.id.dropSubmit)

        dropTextInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialog.validateForm()
            }
        })

        // TODO save text size too
        // val textSize = dialog.findViewById<SeekBar>(R.id.seekBarSize)

        val colorPalette = dialog.findViewById(R.id.palette) as SpectrumPalette

        colorPalette.setOnColorSelectedListener(dialog)

        dropSubmit.setOnClickListener {
            val color = dialog.color
            val location = LocationHandler.lastLocation
            if (location != null) {
                saveDropQuery(dropTextInput.text.toString(), colorIntToHexString(color), location.latitude,
                        location.longitude, location.altitude)
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun setDrop(item: DroppedAroundQuery.DroppedAround, isDisplayed: Boolean): Drop {
        val location = DLocation(item.location.latitude, item.location.longitude, item.location.altitude)
        val message = Message(item.text, 30f, colorHexStringToInt(item.color))
        val socialState = when (item.likeState) {
            "LIKED" -> Social.State.LIKED
            "DISLIKED" -> Social.State.DISLIKED
            else -> Social.State.BLANK
        }
        val social = Social(socialState, item.likeCount, item.dislikeCount)
        return Drop(item.id, message, location, social, isDisplayed)
    }

    private fun refreshDropList(latitude: Double, longitude: Double, radius: Double) {
        Apollo.client.query(
                DroppedAroundQuery.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .radius(radius)
                        .build()).enqueue(object : ApolloCall.Callback<DroppedAroundQuery.Data>() {

            override fun onResponse(response: Response<DroppedAroundQuery.Data>) {
                val data = response.data()!!.droppedAround

                /**
                 * Retrieves and updates the current displayed drop if it stays in the area.
                 */
                var current: Drop? = null
                if (drops.isNotEmpty() && drops[0].isDisplayed) {
                    val matched = data.find { item -> item.id == drops[0].id }
                    if (matched != null) {
                        current = setDrop(matched, true)
                        current.anchorNode = drops[0].anchorNode
                    }
                }

                /**
                 * Removes the displayed drop from the AR scene if it is outside the area.
                 */
                if (current == null && drops.isNotEmpty() && drops[0].isDisplayed) {
                    drops[0].anchorNode?.anchor?.detach()
                }

                /**
                 * Clears the drops list then adds the updated displayed drop if exists.
                 */
                drops.clear()
                if (current != null) {
                    drops.add(current)
                }

                /**
                 * Adds the other drops from the server into the drops list.
                 */
                data.forEach { item ->
                    if (current == null || item.id != current.id) {
                        val drop = setDrop(item, false)
                        drops.add(drop)
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message)
                e.printStackTrace()
            }

        })
    }

    private fun saveDropQuery(text: String, color: String, latitude: Double, longitude: Double, altitude: Double) {
        Apollo.client.mutate(CreateDropMutation.Builder()
                .text(text)
                .color(color)
                .latitude(latitude)
                .longitude(longitude)
                .altitude(altitude)
                .build()).enqueue(object : ApolloCall.Callback<CreateDropMutation.Data>() {

            override fun onResponse(response: Response<CreateDropMutation.Data>) {
                Log.i("APOLLO", response.data()!!.createDrop.id)
                toaster.show("Dropped")
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
                    startActivity(Intent(this@DropActivity, AuthActivity::class.java))
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
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(sensorListener, magnetic, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable)
        locationHandler.removeLocationUpdates()
    }
}
