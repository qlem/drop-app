package run.drop.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import run.drop.app.location.LocationManager
import run.drop.app.location.OnLocationUpdateListener
import run.drop.app.rendering.DropRenderer
import run.drop.app.utils.colorHexStringToInt
import run.drop.app.utils.colorIntToHexString
import run.drop.app.utils.setStatusBarColor
import java.text.DecimalFormat
import kotlin.collections.ArrayList
import com.thebluealliance.spectrum.SpectrumPalette
import run.drop.app.rendering.Toaster
import android.content.Context
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.UserBuilder
import run.drop.app.orientation.OrientationManager


class DropActivity : AppCompatActivity() {

    companion object {
        var drops: MutableList<Drop> = ArrayList()
    }

    private lateinit var locationManager: LocationManager
    private lateinit var orientationManager: OrientationManager
    private lateinit var arFragment: ArFragment

    private var planeDetection = true
    private lateinit var toaster: Toaster

    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            val currentLocation = LocationManager.lastLocation
            if (currentLocation != null) {
                refreshDropList(currentLocation.latitude, currentLocation.longitude, 10.0)
            }
            handler.postDelayed(this, 2000)
        }
    }

    private fun initLocationManager() {
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
            locationManager = LocationManager(this, locationListener)
        }
    }

    private fun initArScene() {
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
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

    private fun flipPlaneDetection(planeButton: ImageButton) {
        planeDetection = !planeDetection
        val session = arFragment.arSceneView.session!!
        session.pause()
        val config = session.config
        config.planeFindingMode =
                if (planeDetection) {
                    planeButton.background.alpha = 255
                    Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                }
                else {
                    planeButton.background.alpha = 100
                    Config.PlaneFindingMode.DISABLED
                }
        session.configure(config)
        session.resume()
        toaster.show(("Plane detection turned " + if (planeDetection) "ON" else "OFF"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop)
        setStatusBarColor(window, this)

        // init sentry (crash report)
        Sentry.getContext().recordBreadcrumb(
                BreadcrumbBuilder().setMessage("Launching Drop Activity").build()
        )
        val email = getSharedPreferences("Drop", Context.MODE_PRIVATE).getString("email", "")
        Sentry.getContext().user = UserBuilder().setEmail(email).build()
        Sentry.capture("User Report")
        Sentry.getContext().clear()

        toaster = Toaster(findViewById(R.id.root_layout))

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
            flipPlaneDetection(planeButton)
        }

        // init drop button
        val dropButton: Button = findViewById(R.id.drop_btn)
        dropButton.setOnClickListener {
            createDrop()
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

    private fun createDrop() {
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
            val location = LocationManager.lastLocation
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

                Sentry.getContext().recordBreadcrumb(
                        BreadcrumbBuilder().setMessage("Failed to Update Drops List APOLLO").build()
                )

                val email = getSharedPreferences("Drop", Context.MODE_PRIVATE).getString("email", "")
                Sentry.getContext().user = UserBuilder().setEmail(email).build()

                Sentry.capture(e)
                Sentry.getContext().clear()

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
                if (response.hasErrors()) {
                    Log.i("APOLLO", response.errors().toString())
                    response.errors()[0].message()?.let { toaster.error(it) }
                } else {
                    Log.i("APOLLO", response.data()!!.createDrop.id)
                    toaster.show("Added new drop")
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message)

                Sentry.getContext().recordBreadcrumb(
                        BreadcrumbBuilder().setMessage("Failed to save drop APOLLO").build()
                )

                val email = getSharedPreferences("Drop", Context.MODE_PRIVATE).getString("email", "")
                Sentry.getContext().user = UserBuilder().setEmail(email).build()

                Sentry.capture(e)
                Sentry.getContext().clear()

                e.printStackTrace()
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            toaster.show("Drop needs camera and location access")
            finish()
        }
        initLocationManager()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LocationManager.REQUEST_SETTINGS_CODE -> when (resultCode) {
                Activity.RESULT_OK -> initLocationManager()
                Activity.RESULT_CANCELED -> {
                    toaster.show("Drop needs device location enabled")
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        initLocationManager()
        orientationManager = OrientationManager(this)
        orientationManager.registerListener()
        handler.post(runnable)

        // checkAuthentication()
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(runnable)
        orientationManager.unregisterLister()
        locationManager.removeLocationUpdates()
    }

    /* private fun checkAuthentication() {
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

                Sentry.getContext().recordBreadcrumb(
                        BreadcrumbBuilder().setMessage("Failed to check Authentication APOLLO").build()
                )

                val email = getSharedPreferences("Drop", Context.MODE_PRIVATE).getString("email", "")
                Sentry.getContext().user = UserBuilder().setEmail(email).build()

                Sentry.capture(e)
                Sentry.getContext().clear()

                e.printStackTrace()
            }
        })
    } */
}
