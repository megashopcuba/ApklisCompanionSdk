package cu.apkuba.sdk.license.models

import cu.apkuba.sdk.license.TaskState

data class Task(
    val name: String,
    val errorMessage: String? = null,
    val state: TaskState = TaskState.Loading,
)