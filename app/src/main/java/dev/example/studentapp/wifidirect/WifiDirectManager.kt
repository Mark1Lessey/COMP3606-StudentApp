// 816032089

package dev.example.studentapp.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Build
import android.util.Log
import android.widget.EditText
import dev.example.studentapp.models.ContentModel

class WifiDirectManager (
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val iFaceImpl: WifiDirectInterface
):BroadcastReceiver() {
    var groupInfo: WifiP2pGroup? = null

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                iFaceImpl.onWifiDirectStateChanged(isWifiP2pEnabled)
                Log.e(
                    "WifiDirectManager",
                    "The Wifi direct adapter state has been changed to $isWifiP2pEnabled"
                )
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel) { peers: WifiP2pDeviceList ->
                    peers?.deviceList?.let { iFaceImpl.onPeerListUpdated(it) }
                    Log.e("WifiDirectManager", "The list of available peers has been updated")
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val wifiP2pInfo = when {
                    Build.VERSION.SDK_INT >= 33 -> intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO, WifiP2pInfo::class.java)!!
                    else -> @Suppress("DEPRECATION") intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)!!
                }
                val tmpGroupInfo = when {
                    !(wifiP2pInfo.groupFormed)->null
                    Build.VERSION.SDK_INT >= 33 -> intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP, WifiP2pGroup::class.java)!!
                    else -> @Suppress("DEPRECATION") intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)!!
                }
                if (groupInfo != tmpGroupInfo) {
                    groupInfo = tmpGroupInfo
                    Log.e("WifiDirectManager", "The group states has changed")
                    iFaceImpl.onGroupStatusChanged(groupInfo)
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val thisDevice = when {
                    Build.VERSION.SDK_INT >= 33 -> intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)!!
                    else -> @Suppress("DEPRECATION") intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)!!
                }
                Log.e("WifiDirectManager", "The device status has changed")

                iFaceImpl.onDeviceStatusChanged(thisDevice)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(peer: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = peer.deviceAddress
        manager.connect(channel, config, object : ActionListener{
            override fun onSuccess() {
                Log.e("WifiDirectManager", "Connected to '${peer.deviceName}'")
            }

            override fun onFailure(reason: Int) {
                Log.e("WifiDirectManager", "Failed to connect to '${peer.deviceName}'")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers(studentId: EditText) {
        val studentID = studentId.text.toString()
        val studentIdArr: Array<String> = studentID.toCharArray().map { it.toString() }.toTypedArray()
        if (studentIdArr[0] == "8" && studentIdArr[1] == "1" && studentIdArr[2] == "6") {
        manager.discoverPeers(channel, object : ActionListener {
            override fun onSuccess() {
                Log.e("WFDManager", "Successfully attempted to discover peers")
            }

            override fun onFailure(reason: Int) {
                Log.e("WFDManager", "An error occurred while trying to discover peers")
            }
        })
        } else {
            Log.e("WFDManager", "Invalid student ID")
        }
    }

    fun disconnect() {
        manager.removeGroup(channel, object : ActionListener{
            override fun onSuccess() {
                Log.e("WifiDirectManager", "Disconnected from the group")
            }
            override fun onFailure(reason: Int) {
                Log.e("WifiDirectManager", "Failed to disconnect from the group")
            }
        })
    }
}