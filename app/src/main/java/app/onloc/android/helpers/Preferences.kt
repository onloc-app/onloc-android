package app.onloc.android.helpers

import android.content.SharedPreferences

fun getIP(deviceEncryptedPreferences: SharedPreferences): String? {
    return deviceEncryptedPreferences.getString("ip", null)
}

fun getAccessToken(deviceEncryptedPreferences: SharedPreferences): String? {
    return deviceEncryptedPreferences.getString("accessToken", null)
}

fun getSelectedDeviceId(deviceEncryptedPreferences: SharedPreferences): Int {
    return deviceEncryptedPreferences.getInt("device_id", -1)
}

fun getInterval(deviceEncryptedPreferences: SharedPreferences): Int {
    return deviceEncryptedPreferences.getInt("interval", -1)
}
