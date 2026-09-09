package com.appcontrol.domain.model

data class ProcessResult(
    val total: Int,
    val processed: Int,
    val failed: Int,
    val notAllowed: Int,
    val results: Map<String, ProcessOutcome>
)

data class ProcessOutcome(
    val packageName: String,
    val success: Boolean,
    val message: String,
    val actionTaken: Boolean = false,
    val requiresManualIntervention: Boolean = false
)
