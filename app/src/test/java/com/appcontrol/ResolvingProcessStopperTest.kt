package com.appcontrol

import com.appcontrol.data.system.ProcessStopper
import com.appcontrol.data.system.ResolvingProcessStopper
import com.appcontrol.data.system.ShizukuGateway
import com.appcontrol.domain.model.StopResult
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeShizukuGateway(
    var available: Boolean = true,
    var permissionGranted: Boolean = true,
    var forceStopResult: StopResult = StopResult.STOPPED
) : ShizukuGateway {

    val forceStoppedPackages = mutableListOf<String>()

    override val isAvailable: Boolean get() = available

    override val isPermissionGranted: Boolean get() = permissionGranted

    override fun forceStop(packageName: String): StopResult {
        forceStoppedPackages += packageName
        return forceStopResult
    }
}

class ResolvingProcessStopperTest {

    private val fallback = FakeProcessStopper()

    private fun build(
        enabled: Boolean,
        gateway: FakeShizukuGateway = FakeShizukuGateway()
    ): ResolvingProcessStopper =
        ResolvingProcessStopper(gateway, fallback) { enabled }

    @Test
    fun `when shizuku disabled uses fallback and never calls gateway`() {
        val gateway = FakeShizukuGateway()
        val stopper = build(enabled = false, gateway = gateway)

        val result = stopper.stopPackage("com.example.app")

        assertEquals(StopResult.STOPPED, result)
        assertEquals(listOf("com.example.app"), fallback.stoppedPackages)
        assertEquals(emptyList<String>(), gateway.forceStoppedPackages)
    }

    @Test
    fun `when shizuku enabled available and granted force stops via gateway`() {
        val gateway = FakeShizukuGateway()
        val stopper = build(enabled = true, gateway = gateway)

        val result = stopper.stopPackage("com.example.app")

        assertEquals(StopResult.STOPPED, result)
        assertEquals(listOf("com.example.app"), gateway.forceStoppedPackages)
        assertEquals(emptyList<String>(), fallback.stoppedPackages)
    }

    @Test
    fun `when shizuku enabled but unavailable falls back`() {
        val gateway = FakeShizukuGateway(available = false)
        val stopper = build(enabled = true, gateway = gateway)

        val result = stopper.stopPackage("com.example.app")

        assertEquals(StopResult.STOPPED, result)
        assertEquals(listOf("com.example.app"), fallback.stoppedPackages)
        assertEquals(emptyList<String>(), gateway.forceStoppedPackages)
    }

    @Test
    fun `when shizuku enabled but permission not granted falls back`() {
        val gateway = FakeShizukuGateway(permissionGranted = false)
        val stopper = build(enabled = true, gateway = gateway)

        val result = stopper.stopPackage("com.example.app")

        assertEquals(StopResult.STOPPED, result)
        assertEquals(listOf("com.example.app"), fallback.stoppedPackages)
        assertEquals(emptyList<String>(), gateway.forceStoppedPackages)
    }

    @Test
    fun `when force stop fails falls back to official api`() {
        val gateway = FakeShizukuGateway(forceStopResult = StopResult.FAILED)
        val stopper = build(enabled = true, gateway = gateway)

        val result = stopper.stopPackage("com.example.app")

        assertEquals(StopResult.STOPPED, result)
        assertEquals(listOf("com.example.app"), gateway.forceStoppedPackages)
        assertEquals(listOf("com.example.app"), fallback.stoppedPackages)
    }

    @Test
    fun `when fallback fails the failed result is passed through`() {
        fallback.result = StopResult.FAILED
        val gateway = FakeShizukuGateway(forceStopResult = StopResult.FAILED)
        val stopper = build(enabled = true, gateway = gateway)

        val result = stopper.stopPackage("com.example.app")

        assertEquals(StopResult.FAILED, result)
    }
}