package run.drop.app.drop

class Social(var state: State, var likeCount: Int, var dislikeCount: Int) {
    enum class State {
        LIKED, DISLIKED, BLANK
    }
}
