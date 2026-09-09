package com.appcontrol.domain.model

data class ActivityEvent(
    val id: Long = 0L,
    val packageName: String,
    val timestamp: Long,
    val eventType: EventType,
    val source: String,
    val profileId: Long? = null
)
