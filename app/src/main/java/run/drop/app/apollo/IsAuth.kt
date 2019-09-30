package run.drop.app.apollo

object IsAuth {
    private var state = false

    fun getState(): Boolean {
        return state
    }

    fun setSate(state: Boolean) {
        this.state = state
    }
}