package com.vortex.launcher.system
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class AdminReceiver : DeviceAdminReceiver()

/** Перезагрузка — только Device Owner (adb shell dpm set-device-owner com.vortex.launcher/.system.AdminReceiver).
 *  Сон — через lockNow, если выдан «Администратор устройства». Выключение — только системные приложения. */
object PowerActions {
    fun admin(ctx: Context) = ComponentName(ctx, AdminReceiver::class.java)
    fun reboot(ctx: Context): Boolean = runCatching {
        ctx.getSystemService(DevicePolicyManager::class.java).reboot(admin(ctx)); true
    }.getOrDefault(false)
    fun shutdown(ctx: Context): Boolean = runCatching {
        ctx.startActivity(Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN")
            .putExtra("android.intent.extra.KEY_CONFIRM", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    }.getOrDefault(false)
    fun sleep(ctx: Context): Boolean {
        val locked = runCatching {
            val d = ctx.getSystemService(DevicePolicyManager::class.java)
            if (d.isAdminActive(admin(ctx))) { d.lockNow(); true } else false
        }.getOrDefault(false)
        return locked || shutdown(ctx)
    }
}
