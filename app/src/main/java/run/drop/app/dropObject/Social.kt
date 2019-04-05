package run.drop.app.dropObject

class Social(var state: State, var likeCount: Int, var dislikeCount: Int) {
    enum class State {
        LIKED, DISLIKED, BLANK
    }
}
