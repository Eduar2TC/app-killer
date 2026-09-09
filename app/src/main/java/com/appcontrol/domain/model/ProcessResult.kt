package com.appcontrol.domain.model

data class ProcessResult(
    val total: Int,
    val stopped: Int,
    val inactive: Int,
    val failed: Int,
    val results: Map<String, ProcessOutcome>
)

data class ProcessOutcome(
    val packageName: String,
    val success: Boolean,
    val message: String,
    val actionTaken: Boolean = false
)
