package run.drop.app.rendering

import android.content.Context
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import run.drop.app.*
import run.drop.app.apollo.Apollo


class DropRenderer(context: Context, anchor: Anchor, message: Message, arFragment: ArFragment, plane: Plane, idDrop: String,
                   likeState: String, likeCount: Int, dislikeCount: Int) {
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

        fun likeButtonSetListener(likeButton: ImageButton, dislikeButton: ImageButton, likeTotal: TextView, dislikeTotal: TextView) {
            likeButton.setOnClickListener {
                Apollo.client.mutate(LikeMutation.builder()
                        .id(idDrop).build()).enqueue(object : ApolloCall.Callback<LikeMutation.Data>() {

                    override fun onResponse(response: Response<LikeMutation.Data>) {
                        likeTotal.text = response.data()?.like()?.likeCount().toString()
                        dislikeTotal.text = response.data()?.like()?.dislikeCount().toString()
                        if (response.data()?.like()?.likeState().equals("LIKED")) {
                            (context as DropActivity).runOnUiThread {
                                likeButton.setImageResource(R.drawable.like_filled)
                                dislikeButton.setImageResource(R.drawable.dislike_transparent)
                            }
                        } else {
                            likeButton.setImageResource(R.drawable.like_transparent)
                        }
                    }

                    override fun onFailure(e: ApolloException) {
                        Log.e("APOLLO", e.message)
                        e.printStackTrace()
                    }
                })
            }
        }

        fun dislikeButtonSetListener(likeButton: ImageButton, dislikeButton: ImageButton, likeTotal: TextView, dislikeTotal: TextView) {
            dislikeButton.setOnClickListener {
                Apollo.client.mutate(DislikeMutation.builder()
                        .id(idDrop).build()).enqueue(object : ApolloCall.Callback<DislikeMutation.Data>() {

                    override fun onResponse(response: Response<DislikeMutation.Data>) {
                        likeTotal.text = response.data()?.dislike()?.likeCount().toString()
                        dislikeTotal.text = response.data()?.dislike()?.dislikeCount().toString()
                        if (response.data()?.dislike()?.likeState().equals("DISLIKED")) {
                            (context as DropActivity).runOnUiThread {
                                dislikeButton.setImageResource(R.drawable.dislike_filled)
                                likeButton.setImageResource(R.drawable.like_transparent)
                            }
                        } else {
                            (context as DropActivity).runOnUiThread {
                                dislikeButton.setImageResource(R.drawable.dislike_transparent)
                            }
                        }
                    }

                    override fun onFailure(e: ApolloException) {
                        Log.e("APOLLO", e.message)
                        e.printStackTrace()
                    }
                })
            }
        }

        ViewRenderable.builder()
        .setView(context, R.layout.drop_layout)
        .build()
        .thenAccept { model ->
            val dropMessage = model.view.findViewById<TextView>(R.id.drop_message)
            val likeButton = model.view.findViewById<ImageButton>(R.id.like_button)
            val dislikeButton = model.view.findViewById<ImageButton>(R.id.dislike_button)
            val likeTotal = model.view.findViewById<TextView>(R.id.like_total)
            val dislikeTotal = model.view.findViewById<TextView>(R.id.dislike_total)

            dropMessage.text = message.text
            dropMessage.setTextColor(message.color!!)
            dropMessage.textSize = message.size!!
            likeTotal.text = likeCount.toString()
            dislikeTotal.text = dislikeCount.toString()

            when (likeState) {
                "LIKED" -> {
                    likeButton.setImageResource(R.drawable.like_filled)
                    dislikeButton.setImageResource(R.drawable.dislike_transparent)
                }
                "DISLIKED" -> {
                    dislikeButton.setImageResource(R.drawable.dislike_filled)
                    likeButton.setImageResource(R.drawable.like_transparent)
                }
                else -> {
                    likeButton.setImageResource(R.drawable.like_transparent)
                    dislikeButton.setImageResource(R.drawable.dislike_transparent)
                }
            }

            likeButtonSetListener(likeButton, dislikeButton, likeTotal, dislikeTotal)
            dislikeButtonSetListener(likeButton, dislikeButton, likeTotal, dislikeTotal)

            model.isShadowCaster = false
            model.isShadowReceiver = false
            textNode.setParent(anchorNode)
            textNode.renderable = model
            textNode.select()
        }
    }
}