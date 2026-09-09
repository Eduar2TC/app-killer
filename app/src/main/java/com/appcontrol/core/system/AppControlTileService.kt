package com.appcontrol.core.system

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.appcontrol.R
import com.appcontrol.feature.dashboard.MainActivity

class AppControlTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        if (!isLocked()) {
            processApps()
        } else {
            showUnlockMessage()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.label = getString(R.string.tile_label_app_control)
        tile.contentDescription = getString(R.string.tile_label_app_control)
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    private fun processApps() {
        runCatching {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_PROCESS_DASHBOARD, true)
            }
            startActivityAndCollapse(intent)
        }.onFailure {
            openAppInfoSettings()
        }
    }

    private fun openAppInfoSettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
    }

    private fun showUnlockMessage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { startActivityAndCollapse(Intent(this, MainActivity::class.java)) }
        }
    }

    companion object {
        const val EXTRA_PROCESS_DASHBOARD = "extra_process_dashboard"
    }
}