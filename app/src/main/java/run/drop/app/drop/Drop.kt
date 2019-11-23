package run.drop.app.drop

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.UserBuilder
import run.drop.app.*
import run.drop.app.R
import run.drop.app.apollo.Apollo
import run.drop.app.apollo.IsAuth


class Drop(private val context: Context, val id: String, val dLocation: DLocation,
           val message: Message, private var social: Social) {

    enum class State {
        INACTIVE, ATTACHED, DISPLAYED
    }

    var state: State = State.INACTIVE
    val node: Node = Node()
    private var anchorNode: AnchorNode? = null

    private lateinit var likeButton: ImageButton
    private lateinit var dislikeButton: ImageButton
    private lateinit var likeCountView: TextView
    private lateinit var dislikeCountView: TextView

    init {
        buildCube()
    }

    fun attach(arFragment: ArFragment, anchor: Anchor?) {
        state = State.ATTACHED
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)
        node.setParent(anchorNode)
        this.anchorNode = anchorNode
    }

    fun detach() {
        state = State.INACTIVE
        node.setParent(null)
        anchorNode!!.anchor!!.detach()
    }

    fun update(social: Social) {
        this.social = social
        buildCube()
    }

    private fun buildCube() {
        MaterialFactory.makeTransparentWithColor(context, Color(0.8f, 0.8f, 0.8f, 0f))
                .thenAccept { material ->
                    val cube = ShapeFactory.makeCube(Vector3(0.8f, 0.4f, 0.3f), Vector3(0f, 0.2f, 0f), material)
                    cube.isShadowCaster = false
                    cube.isShadowReceiver = false
                    node.renderable = cube

                    val dropNode = Node()
                    dropNode.setParent(node)
                    dropNode.localPosition = Vector3(0f, 0f, 0.15f)
                    buildRenderable(dropNode)
                }
    }

    private fun buildRenderable(node: Node) {
        ViewRenderable.builder()
                .setView(context, R.layout.drop_layout)
                .build()
                .thenAccept { model ->
                    val dropMessage = model.view.findViewById<TextView>(R.id.drop_message)
                    likeButton = model.view.findViewById(R.id.like_button)
                    dislikeButton = model.view.findViewById(R.id.dislike_button)
                    likeCountView = model.view.findViewById(R.id.like_total)
                    dislikeCountView = model.view.findViewById(R.id.dislike_total)

                    dropMessage.text = message.text
                    dropMessage.setTextColor(message.color)
                    dropMessage.textSize = message.size

                    setSocialView(social.state, social.likeCount, social.dislikeCount)
                    likeButtonSetListener()
                    dislikeButtonSetListener()

                    model.isShadowCaster = false
                    model.isShadowReceiver = false
                    model.setSizer {
                        Vector3(0.8f, 0.4f, 0f)
                    }
                    node.renderable = model
                    buildBoundingBox()
                }
    }

    private fun buildBoundingBox() {
        val boundsNode = Node()
        boundsNode.setParent(node)
        MaterialFactory.makeTransparentWithColor(context, Color(1f, 0f, 0f, 0.3f))
                .thenAccept { material ->
                    val box = this.node.collisionShape as Box
                    val model = ShapeFactory.makeCube(box.size, box.center, material)
                    model.isShadowCaster = false
                    model.isShadowReceiver = false
                    model.collisionShape = null
                    boundsNode.renderable = model
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
            Social.State.LIKED -> social.state = Social.State.LIKED
            Social.State.DISLIKED -> social.state = Social.State.DISLIKED
            else -> social.state = Social.State.BLANK
        }
        social.likeCount = likeCount
        social.dislikeCount = dislikeCount
    }

    private fun likeButtonSetListener() {
        likeButton.setOnClickListener {
            Apollo.client.mutate(LikeMutation.builder()
                    .id(id).build()).enqueue(object : ApolloCall.Callback<LikeMutation.Data>() {

                override fun onResponse(response: Response<LikeMutation.Data>) {
                    if (!IsAuth.state) {
                        (context as DropActivity).runOnUiThread {
                            Toast.makeText(context, "You need to be connected to access this feature", Toast.LENGTH_LONG).show()
                        }
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
                    .id(id).build()).enqueue(object : ApolloCall.Callback<DislikeMutation.Data>() {
                override fun onResponse(response: Response<DislikeMutation.Data>) {
                    if (!IsAuth.state) {
                        (context as DropActivity).runOnUiThread {
                            Toast.makeText(context, "You need to be connected to access this feature", Toast.LENGTH_LONG).show()
                        }
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
