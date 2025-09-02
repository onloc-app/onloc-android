package ca.kebs.onloc.android

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension

private const val SERVICE_TYPE = "_http._tcp"
private const val SERVICE_NAME = "onloc"

class ServerDiscovery(context: Context, callback: (service: Pair<String, Int>) -> Unit) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val multicastLock = wifiManager.createMulticastLock("Onloc-Bonjour")

    private val pendingResolves = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var registered = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.i("ServerDiscovery", "Discovery started")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i("ServerDiscovery", "Discovery stopped")
        }

        @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (serviceInfo.serviceName == SERVICE_NAME) {
                queueResolve(serviceInfo)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.e("ServerDiscovery", "Service lost")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("ServerDiscovery", "Start error: $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("ServerDiscovery", "Stop error: $errorCode")
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e("ServerDiscovery", "Resolve failed: $errorCode")
            resolving = false
        }

        @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 12)
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d("ServerDiscovery", "Service resolved: $serviceInfo")
            if (serviceInfo.hostAddresses.isNotEmpty() && serviceInfo.hostAddresses.first() != null) {
                callback(serviceInfo.hostAddresses.first().hostAddress!! to serviceInfo.port)
            }
            resolving = false
        }
    }

    private fun queueResolve(serviceInfo: NsdServiceInfo) {
        pendingResolves.add(serviceInfo)
        if (!resolving) {
            processNextResolve()
        }
    }

    private fun processNextResolve() {
        val next = pendingResolves.removeFirstOrNull() ?: return
        resolving = true
        nsdManager.resolveService(next, resolveListener)
    }

    fun startDiscovery() {
        multicastLock.acquire()
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        registered = true
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
    fun stopDiscovery() {
        try {
            if (registered) {
                try {
                    nsdManager.stopServiceResolution(resolveListener)
                } catch (_: Exception) {
                    Log.w("ServerDiscovery", "resolveListener not registered, ignoring")
                }

                try {
                    nsdManager.stopServiceDiscovery(discoveryListener)
                } catch (_: Exception) {
                    Log.w("ServerDiscovery", "discoveryListener not registered, ignoring")
                }
            }
        } finally {
            if (multicastLock.isHeld) {
                multicastLock.release()
            }
            registered = false
        }
    }
}
