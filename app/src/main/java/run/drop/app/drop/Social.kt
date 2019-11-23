package run.drop.app.drop

class Social(var state: State, var likeCount: Int, var reportCount: Int) {
    enum class State {
        LIKED, REPORTED, BLANK
    }
}
