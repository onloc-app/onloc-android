package ca.kebs.onloc.android.permissions

import android.app.Activity
import android.content.Context

abstract class Permission {
    abstract fun isGranted(context: Context): Boolean
    abstract fun request(activity: Activity)
}