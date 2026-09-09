package com.appcontrol.data.system

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.appcontrol.domain.model.StopResult
import com.appcontrol.shizuku.IAppControlService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

class ShizukuManager(private val context: Context) : ShizukuGateway {

    companion object {
        private const val BIND_TIMEOUT_MS = 5000L
    }

    @Volatile
    private var boundService: IAppControlService? = null

    @Volatile
    private var currentConnection: ServiceConnection? = null

    override val isAvailable: Boolean
        get() = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }

    override val isPermissionGranted: Boolean
        get() = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

    val shizukuVersion: Int
        get() = try {
            if (Shizuku.isPreV11()) 1 else Shizuku.getVersion()
        } catch (_: Throwable) {
            0
        }

    fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (_: Throwable) {
        }
    }

    fun addPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.addRequestPermissionResultListener(listener)
        } catch (_: Throwable) {
        }
    }

    fun removePermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.removeRequestPermissionResultListener(listener)
        } catch (_: Throwable) {
        }
    }

    override fun forceStop(packageName: String): StopResult {
        return runBlocking {
            val service = withContext(Dispatchers.IO) { ensureBound() }
                ?: return@runBlocking StopResult.FAILED
            try {
                val exitCode = withContext(Dispatchers.IO) { service.forceStop(packageName) }
                if (exitCode == 0) StopResult.STOPPED else StopResult.FAILED
            } catch (_: Exception) {
                boundService = null
                StopResult.FAILED
            }
        }
    }

    fun release() {
        runBlocking {
            currentConnection?.let { conn ->
                runCatching { Shizuku.unbindUserService(userServiceArgs(), conn, true) }
            }
        }
        currentConnection = null
        boundService = null
    }

    private suspend fun ensureBound(): IAppControlService? {
        boundService?.let { return it }
        if (!isAvailable || !isPermissionGranted) return null

        val deferred = CompletableDeferred<IAppControlService?>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val impl = service?.let { IAppControlService.Stub.asInterface(it) }
                if (!deferred.isCompleted) deferred.complete(impl)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                boundService = null
                if (!deferred.isCompleted) deferred.complete(null)
            }
        }

        val args = userServiceArgs()
        var bound = false
        try {
            Shizuku.bindUserService(args, connection)
            bound = true
            currentConnection = connection
        } catch (_: Throwable) {
            if (!deferred.isCompleted) deferred.complete(null)
        }

        val impl = withTimeoutOrNull(BIND_TIMEOUT_MS) { deferred.await() }
        if (impl == null && bound) {
            runCatching { Shizuku.unbindUserService(args, connection, true) }
            currentConnection = null
        }
        boundService = impl
        return impl
    }

    private fun userServiceArgs(): Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(ComponentName(context.packageName, ShizukuForceStopService::class.java.name))
            .daemon(false)
}