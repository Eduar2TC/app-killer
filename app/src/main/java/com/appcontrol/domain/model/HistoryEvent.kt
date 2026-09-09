package com.appcontrol.domain.model

data class HistoryEvent(
    val id: Long = 0L,
    val timestamp: Long,
    val eventType: HistoryEventType,
    val title: String,
    val description: String,
    val packageName: String? = null,
    val profileId: Long? = null
)
