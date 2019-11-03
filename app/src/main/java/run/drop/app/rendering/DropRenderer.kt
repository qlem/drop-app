package run.drop.app.rendering

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.UserBuilder
import run.drop.app.*
import run.drop.app.apollo.Apollo
import run.drop.app.apollo.IsAuth
import run.drop.app.dropObject.Drop
import run.drop.app.dropObject.Social


class DropRenderer(private val context: Context, arFragment: ArFragment, anchor: Anchor, plane: Plane, private val drop: Drop) {

    private lateinit var likeButton: ImageButton
    private lateinit var dislikeButton: ImageButton
    private lateinit var likeCountView: TextView
    private lateinit var dislikeCountView: TextView

    init {
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)

        drop.anchorNode = anchorNode

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
            likeButton = model.view.findViewById(R.id.like_button)
            dislikeButton = model.view.findViewById(R.id.dislike_button)
            likeCountView = model.view.findViewById(R.id.like_total)
            dislikeCountView = model.view.findViewById(R.id.dislike_total)

            dropMessage.text = drop.message.text
            dropMessage.setTextColor(drop.message.color)
            dropMessage.textSize = drop.message.size

            setSocialView(drop.social.state, drop.social.likeCount, drop.social.dislikeCount)
            likeButtonSetListener()
            dislikeButtonSetListener()

            model.isShadowCaster = false
            model.isShadowReceiver = false
            textNode.setParent(anchorNode)
            textNode.renderable = model
            textNode.select()
        }
    }

    private fun setSocialView(state: Social.State, likeCount: Int, dislikeCount: Int) {
        when (state) {
            Social.State.LIKED -> {
                likeButton.setImageResource(R.drawable.like_filled)
                dislikeButton.setImageResource(R.drawable.dislike_transparent)
            }
            Social.State.DISLIKED -> {
                likeButton.setImageResource(R.drawable.like_transparent)
                dislikeButton.setImageResource(R.drawable.dislike_filled)
            }
            else -> {
                likeButton.setImageResource(R.drawable.like_transparent)
                dislikeButton.setImageResource(R.drawable.dislike_transparent)
            }
        }
        likeCountView.text = likeCount.toString()
        dislikeCountView.text = dislikeCount.toString()
    }

    private fun updateSocialDrop(state: Social.State, likeCount: Int, dislikeCount: Int) {
        when (state) {
            Social.State.LIKED -> drop.social.state = Social.State.LIKED
            Social.State.DISLIKED -> drop.social.state = Social.State.DISLIKED
            else -> drop.social.state = Social.State.BLANK
        }
        drop.social.likeCount = likeCount
        drop.social.dislikeCount = dislikeCount
    }

    private fun likeButtonSetListener() {
        likeButton.setOnClickListener {
            Apollo.client.mutate(LikeMutation.builder()
                    .id(drop.id).build()).enqueue(object : ApolloCall.Callback<LikeMutation.Data>() {

                override fun onResponse(response: Response<LikeMutation.Data>) {
                    if (!IsAuth.state) {
                        context.startActivity(Intent(context, AuthActivity::class.java))
                    } else {
                        Log.i("APOLLO", response.errors().toString())
                        val state: Social.State = when (response.data()?.like()?.likeState()) {
                            "LIKED" -> Social.State.LIKED
                            "DISLIKED" -> Social.State.DISLIKED
                            else -> Social.State.BLANK
                        }

                        val likeCount: Int = response.data()?.like()?.likeCount()!!
                        val dislikeCount: Int = response.data()?.like()?.dislikeCount()!!
                        updateSocialDrop(state, likeCount, dislikeCount)
                        (context as DropActivity).runOnUiThread {
                            setSocialView(state, likeCount, dislikeCount)
                        }
                    }
                }

                override fun onFailure(e: ApolloException) {
                    Log.e("APOLLO", e.message ?: "apollo error: LikeMutation")
                    Sentry.getContext().recordBreadcrumb(
                            BreadcrumbBuilder().setMessage("Like Button failed Apollo").build()
                    )

                    val email = context.getSharedPreferences("Drop", Context.MODE_PRIVATE).getString("email", "")
                    Sentry.getContext().user = UserBuilder().setEmail(email).build()

                    Sentry.capture(e)
                    Sentry.getContext().clear()


                    e.printStackTrace()
                }
            })
        }
    }

    private fun dislikeButtonSetListener() {
        dislikeButton.setOnClickListener {
            Apollo.client.mutate(DislikeMutation.builder()
                    .id(drop.id).build()).enqueue(object : ApolloCall.Callback<DislikeMutation.Data>() {
                override fun onResponse(response: Response<DislikeMutation.Data>) {
                    if (!IsAuth.state) {
                        context.startActivity(Intent(context, AuthActivity::class.java))
                    } else {
                        val state: Social.State = when (response.data()?.dislike()?.likeState()) {
                            "LIKED" -> Social.State.LIKED
                            "DISLIKED" -> Social.State.DISLIKED
                            else -> Social.State.BLANK
                        }
                        val likeCount: Int = response.data()?.dislike()?.likeCount()!!
                        val dislikeCount: Int = response.data()?.dislike()?.dislikeCount()!!
                        updateSocialDrop(state, likeCount, dislikeCount)
                        (context as DropActivity).runOnUiThread {
                            setSocialView(state, likeCount, dislikeCount)
                        }
                    }
                }

                override fun onFailure(e: ApolloException) {
                    Log.e("APOLLO", e.message ?: "apollo error: DislikeMutation")
                    Sentry.getContext().recordBreadcrumb(
                            BreadcrumbBuilder().setMessage("Dislike Button failed Apollo").build()
                    )

                    val email = context.getSharedPreferences("Drop", Context.MODE_PRIVATE).getString("email", "")
                    Sentry.getContext().user = UserBuilder().setEmail(email).build()

                    Sentry.capture(e)
                    Sentry.getContext().clear()

                    e.printStackTrace()
                }
            })
        }
    }
}