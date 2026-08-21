package com.example.quickgestures.services.tiles

import android.service.quicksettings.TileService
import com.example.quickgestures.data.AppPreferences
import com.example.quickgestures.data.GestureActionCatalog
import com.example.quickgestures.utils.ActionExecutor

/**
 * بلاطة أولى بمركز التحكم (القائمة اللي بتنزل من فوق الشاشة). المستخدم بضيفها يدوياً
 * مرة وحدة من قائمة تعديل البلاطات بمركز التحكم (قيد إجباري من أندرويد نفسو، ما فيه
 * طريقة لإضافتها تلقائياً لأي تطبيق عادي)، وبعدها بتنفذ أي إجراء يختاره من إعدادات التطبيق.
 */
class QuickTileService1 : TileService() {

    override fun onClick() {
        super.onClick()
        val prefs = AppPreferences(applicationContext)
        val actionId = prefs.quickTileAction1Id ?: return
        GestureActionCatalog.byId(actionId)?.let { ActionExecutor(applicationContext).execute(it) }
    }

    override fun onStartListening() {
        super.onStartListening()
        val prefs = AppPreferences(applicationContext)
        val action = GestureActionCatalog.byId(prefs.quickTileAction1Id ?: "")
        qsTile?.label = action?.displayLabel ?: "Quick Touch 1"
        qsTile?.updateTile()
    }
}
