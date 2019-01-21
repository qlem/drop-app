package run.drop.app.rendering

import android.Manifest
import com.google.ar.sceneform.ux.ArFragment

class SceneFragment: ArFragment() {

    override fun getAdditionalPermissions(): Array<String> {
        return arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}