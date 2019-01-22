package run.drop.app.rendering

import android.content.Context
import android.widget.TextView
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class DropRenderer(context: Context, anchor: Anchor, message: Message, arFragment: ArFragment, plane: Plane) {

    init {
        val textView = TextView(context)
        textView.text = message.text
        textView.setTextColor(message.color!!)
        textView.textSize = message.size!!

        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)

        val textNode = TransformableNode(arFragment.transformationSystem)
        val scaleController = textNode.scaleController
        scaleController.minScale = 0.01f
        scaleController.maxScale = 40f
        scaleController.sensitivity = 0.4f

        if (plane.type === Plane.Type.VERTICAL) {
            val yAxis = plane.centerPose.yAxis
            val planeNormal = Vector3(yAxis[0], yAxis[1], yAxis[2])
            val quaternion = Quaternion.lookRotation(Vector3.up(), planeNormal)
            textNode.worldRotation = quaternion
        }

        ViewRenderable.builder()
            .setView(context, textView)
            .build()
            .thenAccept { model ->
                model.isShadowCaster = false
                model.isShadowReceiver = false
                textNode.setParent(anchorNode)
                textNode.renderable = model
                textNode.select()
            }
    }
}