package com.appcontrol.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val message: String, val code: Int = -1) : AppResult<Nothing>
    data object PermissionRequired : AppResult<Nothing>
    data object NotSupported : AppResult<Nothing>
    data object Restricted : AppResult<Nothing>
}

sealed interface ControlResult {
    data object Success : ControlResult
    data object PermissionRequired : ControlResult
    data object NotSupported : ControlResult
    data object Restricted : ControlResult
    data class Error(val message: String) : ControlResult
    data object ManualRequired : ControlResult
    data object Skipped : ControlResult
}
