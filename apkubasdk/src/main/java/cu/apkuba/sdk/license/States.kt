package cu.apkuba.sdk.license


sealed class TaskState {
    data object InitializeProcess : TaskState()
    data object Loading : TaskState()
    data object Success : TaskState()
    data class Error(val message: String, val critical: Boolean = false) : TaskState()
}

sealed class LoadingState {
    data object Loading : LoadingState()
    data object Success : LoadingState()
    data class Error(val message: String) : LoadingState()
}
