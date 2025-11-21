package com.sparklysparky.tellocontroller.classes

import android.util.Log

actual fun logMsg(message: String) {
    Log.d("TelloController", message)
}