package run.drop.app

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
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.ar.core.*
import com.google.ar.sceneform.ux.ArFragment
import run.drop.app.apollo.Apollo
import run.drop.app.apollo.TokenHandler
import run.drop.app.drop.Drop
import run.drop.app.drop.Message
import run.drop.app.drop.Social
import run.drop.app.drop.DLocation
import run.drop.app.location.LocationManager
import run.drop.app.location.LocationManager.OnLocationUpdateListener
import run.drop.app.utils.colorHexStringToInt
import run.drop.app.utils.colorIntToHexString
import run.drop.app.utils.setStatusBarColor
import kotlin.collections.ArrayList
import com.thebluealliance.spectrum.SpectrumPalette
import android.content.Context
import android.view.MenuItem
import android.view.View
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.UserBuilder
import run.drop.app.apollo.IsAuth
import run.drop.app.location.LocationIndicatorView
import run.drop.app.orientation.OrientationManager


class DropActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    companion object {
        var drops: MutableList<Drop> = ArrayList()
        var debugMode: Boolean = false
    }

    private lateinit var locationManager: LocationManager
    private lateinit var locationIndicatorView: LocationIndicatorView
    private lateinit var orientationManager: OrientationManager
    private lateinit var dropDialog: DropDialog
    private lateinit var arFragment: ArFragment
    private val handler = Handler()
    private var planeDetection = true

    private val refreshDropsTask = object : Runnable {
        override fun run() {
            val currentLocation = LocationManager.lastLocation
            if (currentLocation != null) {
                refreshDropList(currentLocation.latitude, currentLocation.longitude, 20.0)
            }
            handler.postDelayed(this, 2000)
        }
    }

    private fun checkCollision(drop: Drop): Boolean {
        val colliders = arFragment.arSceneView.scene.overlapTestAll(drop.node)
        colliders.forEach { collider ->
            val matched = drops.find { drop -> drop.node == collider }
            if (matched != null) {
                return true
            }
        }
        return false
    }

    private val renderTask = object : Runnable {
        override fun run() {

            if (drops.isEmpty()) {
                handler.postDelayed(this, 1000)
                return
            }

            val frame: Frame = arFragment.arSceneView.session!!.update()
            val cameraWidth: Float = frame.camera.textureIntrinsics.imageDimensions[0].toFloat()
            val cameraHeight: Float = frame.camera.textureIntrinsics.imageDimensions[1].toFloat()
            val hitResults: MutableList<HitResult> = frame.hitTest(cameraHeight / 2, cameraWidth / 2)

            if (hitResults.isNotEmpty() && hitResults[0].trackable is Plane) {
                val plane = hitResults[0].trackable as Plane
                if (plane.trackingState == TrackingState.TRACKING) {
                    val drop = drops.find { drop -> drop.state == Drop.State.INACTIVE }
                    if (drop != null) {
                        val anchor = hitResults[0].createAnchor()
                        drop.attach(arFragment, anchor)
                        if (checkCollision(drop)) {
                            drop.detach()
                        } else {
                            drop.state = Drop.State.DISPLAYED
                        }
                    }
                }
            }

            handler.postDelayed(this, 1000)
        }
    }

    private fun initLocationManager() {
        val locationListener: OnLocationUpdateListener = object : OnLocationUpdateListener {
            override fun onLocationUpdate(location: Location) {
                locationIndicatorView.update()
            }

            override fun onLocationAvailability(state: Boolean) {
                if (state) {
                    locationIndicatorView.start()
                } else {
                    locationIndicatorView.stop()
                }
            }
        }
        locationManager = LocationManager(this, locationListener)
        locationIndicatorView.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop)
        setStatusBarColor(window, this)

        locationIndicatorView = findViewById(R.id.location_indicator)

        // init sentry (crash report)
        Sentry.getContext().recordBreadcrumb(
                BreadcrumbBuilder().setMessage("Launching Drop Activity").build()
        )
        val email = getSharedPreferences("Drop", Context.MODE_PRIVATE).getString("email", "")
        Sentry.getContext().user = UserBuilder().setEmail(email).build()
        Sentry.capture("User Report")
        Sentry.getContext().clear()

        // init arCore
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment

        // init drop button
        val dropButton: ImageButton = findViewById(R.id.drop_btn)
        dropButton.setOnClickListener {
            createDrop()
        }

        // init drop dialog
        initDropDialog()
    }

    fun showMenu(view: View) {
        val menu = PopupMenu(this, view).apply {
            setOnMenuItemClickListener(this@DropActivity)
            inflate(R.menu.menu)
        }

        var itemMenu = menu.menu.findItem(R.id.menu_plane)
        itemMenu.title = if (planeDetection) resources.getString(R.string.menu_plane_off) else
            resources.getString(R.string.menu_plane_on)

        itemMenu = menu.menu.findItem(R.id.menu_debug)
        itemMenu.title = if (debugMode) resources.getString(R.string.menu_debug_off) else
            resources.getString(R.string.menu_debug_on)

        itemMenu = menu.menu.findItem(R.id.menu_logout)
        itemMenu.title = if (IsAuth.state) resources.getString(R.string.menu_logout) else
            resources.getString(R.string.menu_sign_in)

        menu.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.menu_reload -> {
                clearDrops()
                true
            }
            R.id.menu_plane -> {
                flipPlaneDetection()
                true
            }
            R.id.menu_debug -> {
                flipDebugMode()
                true
            }
            R.id.menu_logout -> {
                TokenHandler.clearToken(this)
                IsAuth.state = false
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
                true
            }
            else -> false
        }
    }

    private fun clearDrops() {
        drops.forEach { drop ->
            if (drop.state == Drop.State.DISPLAYED) {
                drop.detach()
            }
        }
        drops.clear()
    }

    private fun flipPlaneDetection() {
        planeDetection = !planeDetection
        val session = arFragment.arSceneView.session!!
        session.pause()
        val config = session.config
        if (planeDetection) {
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        } else {
            config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        }
        session.configure(config)
        session.resume()
        Toast.makeText(this, "Plane detection turned " +
                if (planeDetection) "ON" else "OFF", Toast.LENGTH_LONG).show()
    }

    private fun flipDebugMode() {
        debugMode = !debugMode
        drops.forEach { drop ->
            if (drop.state == Drop.State.DISPLAYED) {
                drop.boundsNode.isEnabled = debugMode
            }
        }
        Toast.makeText(this, "Debug mode turned " + if (debugMode) "ON" else "OFF",
                Toast.LENGTH_LONG).show()
    }

    private fun initDropDialog() {
        dropDialog = DropDialog(this)
        dropDialog.setContentView(R.layout.drop_dialog)

        val dropTextInput = dropDialog.findViewById<EditText>(R.id.message)

        val dropSubmit = dropDialog.findViewById<Button>(R.id.drop_btn)

        dropTextInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dropDialog.validateForm()
            }
        })

        val colorPalette = dropDialog.findViewById(R.id.palette) as SpectrumPalette

        colorPalette.setOnColorSelectedListener(dropDialog)

        dropSubmit.setOnClickListener {
            val color = dropDialog.color
            val location = LocationManager.lastLocation
            if (location != null) {
                saveDropQuery(dropTextInput.text.toString(), colorIntToHexString(color),
                        location.latitude, location.longitude, location.altitude)
            }
            dropDialog.dismiss()
        }
    }

    private fun createDrop() {
        if (!IsAuth.state) {
            Toast.makeText(this, "You need to be connected to access this feature", Toast.LENGTH_LONG).show()
                this.startActivity(Intent(this, AuthActivity::class.java))
        } else {
            dropDialog.show()
        }
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

                data.forEach { item ->
                    val location = DLocation(item.location.latitude, item.location.longitude, item.location.altitude)
                    val message = Message(item.text, 30f, colorHexStringToInt(item.color))
                    val socialState = when (item.likeState) {
                        "LIKED" -> Social.State.LIKED
                        "DISLIKED" -> Social.State.REPORTED
                        else -> Social.State.BLANK
                    }
                    val social = Social(socialState, item.likeCount, item.dislikeCount)
                    var matched = false
                    drops.forEach { drop ->
                        if (item.id == drop.id) {
                            matched = true
                            this@DropActivity.runOnUiThread {
                                drop.update(social)
                            }
                        }
                    }
                    if (!matched) {
                        this@DropActivity.runOnUiThread {
                            val drop = Drop(this@DropActivity, item.id, location, message, social)
                            drops.add(drop)
                        }
                    }
                }
                // TODO need to be tested
                /* val dataIds = data.map { it.id }
                drops.filter { it.id !in dataIds }.forEach {
                    runOnUiThread {
                        it.detach()
                    }
                }
                drops.removeAll { it.id !in dataIds } */
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message ?: "apollo error: DroppedAroundQuery")

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
                if (response.data() != null)
                    Log.i("APOLLO", response.data()!!.createDrop.id)
                else {
                    runOnUiThread {
                        Toast.makeText(applicationContext, (if (response.hasErrors()) response.errors()[0].message() else "An error occured"), Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.e("APOLLO", e.message ?: "apollo error: CreateDropMutation")
                runOnUiThread {
                    Toast.makeText(applicationContext, "Unable to join server, your drop can't be posted for now", Toast.LENGTH_LONG).show()
                }
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

    private fun checkResults(grantResults: IntArray): Boolean {
        if (grantResults.isEmpty()) {
            return false
        }
        grantResults.forEach { result ->
            if (result == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!checkResults(grantResults)) {
            Toast.makeText(this, "Drop needs access to camera and location", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LocationManager.REQUEST_SETTINGS_CODE -> when (resultCode) {
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, "Drop needs device location enabled", Toast.LENGTH_LONG).show()
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
        handler.post(refreshDropsTask)
        arFragment.arSceneView.resume()
        handler.post(renderTask)
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(renderTask)
        handler.removeCallbacks(refreshDropsTask)
        clearDrops()
        arFragment.arSceneView.pause()
        orientationManager.unregisterLister()
        locationManager.removeLocationUpdates()
        locationIndicatorView.stop()
    }
}
