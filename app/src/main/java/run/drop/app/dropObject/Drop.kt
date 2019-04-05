package run.drop.app.dropObject

import com.google.ar.sceneform.AnchorNode

class Drop(val id: String, val message: Message, val dLocation: DLocation, val social: Social, var isDisplayed: Boolean) {
    var anchorNode: AnchorNode? = null
}
