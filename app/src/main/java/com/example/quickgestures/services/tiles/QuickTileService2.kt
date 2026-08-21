package com.example.quickgestures.services.tiles

import android.service.quicksettings.TileService
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionCatalog
import com.example.quickgestures.utils.ActionExecutor

class QuickTileService2 : TileService() {

    override fun onClick() {
        super.onClick()
        val prefs = AppPreferences(applicationContext)
        val actionId = prefs.quickTileAction2Id ?: return
        GestureActionCatalog.byId(actionId)?.let { ActionExecutor(applicationContext).execute(it) }
    }

    override fun onStartListening() {
        super.onStartListening()
        val prefs = AppPreferences(applicationContext)
        val action = GestureActionCatalog.byId(prefs.quickTileAction2Id ?: "")
        qsTile?.label = action?.displayLabel ?: "Quick Touch 2"
        qsTile?.updateTile()
    }
}
