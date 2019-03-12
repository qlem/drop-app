package run.drop.app.rendering

import android.content.Context
import android.widget.ImageButton
import android.widget.TextView
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import run.drop.app.R

class DropRenderer(context: Context, anchor: Anchor, message: Message, arFragment: ArFragment, plane: Plane) {
    init {
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
            .setView(context, R.layout.drop_layout)
            .build()
            .thenAccept { model ->
                val dropMessage = model.view.findViewById<TextView>(R.id.drop_message)
                val likeButton = model.view.findViewById<ImageButton>(R.id.like_button)
                val dislikeButton = model.view.findViewById<ImageButton>(R.id.dislike_button)

                dropMessage.text = message.text
                dropMessage.setTextColor(message.color!!)
                dropMessage.textSize = message.size!!
                likeButton.setOnClickListener {
                    likeButton.setImageResource(R.drawable.like_filled)
                    dislikeButton.setImageResource(R.drawable.dislike_transparent)
                }
                dislikeButton.setOnClickListener {
                    dislikeButton.setImageResource(R.drawable.dislike_filled)
                    likeButton.setImageResource(R.drawable.like_transparent)
                }
                model.isShadowCaster = false
                model.isShadowReceiver = false
                textNode.setParent(anchorNode)
                textNode.renderable = model
                textNode.select()
            }
    }
}