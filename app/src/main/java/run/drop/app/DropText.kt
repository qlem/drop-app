package run.drop.app

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

class DropText(context: Context, anchor: Anchor, textEntity: TextEntity, arFragment: ArFragment, plane: Plane) {
    private val anchor:Anchor? = anchor
    private val arFragment:ArFragment? = arFragment
    private val plane:Plane? = plane

    init {
        val textView = TextView(context)
        textView.text = textEntity.text
        textView.setTextColor(textEntity.color!!)
        textView.textSize = textEntity.size!!


        val textNode = TransformableNode(arFragment.transformationSystem)
        val scaleController = textNode.scaleController
        scaleController.minScale = 0.01f
        scaleController.maxScale = 40f
        scaleController.sensitivity = 0.4f

        ViewRenderable.builder()
            .setView(context, textView)
            .build()
            .thenAccept { model ->
                model.isShadowCaster = false
                model.isShadowReceiver = false
                onAfterLoad(model, textNode)
            }
    }

    private fun onAfterLoad(model:ViewRenderable, textNode:TransformableNode) {
        textNode.renderable = model

        if (plane?.type === Plane.Type.VERTICAL) {
            val yAxis = plane.centerPose.yAxis
            val planeNormal = Vector3(yAxis[0], yAxis[1], yAxis[2])
            val quaternion = Quaternion.lookRotation(Vector3.up(), planeNormal)
            textNode.worldRotation = quaternion
        }

        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment?.arSceneView?.scene)
        textNode.setParent(anchorNode)
        textNode.select()
    }
}