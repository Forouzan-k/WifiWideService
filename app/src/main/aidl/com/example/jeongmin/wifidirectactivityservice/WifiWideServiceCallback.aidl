// WifiWideServiceCallback.aidl
package com.example.jeongmin.wifidirectactivityservice;

import android.net.wifi.p2p.WifiP2pDevice;
import java.util.List;

interface WifiWideServiceCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onCreateGroupAsOwner(int resultCode);

    void onDiscoverWifiWidePeers(int resultCode, in List<WifiP2pDevice> peers);

    void onConnectChanged(int resultCode, in List<WifiP2pDevice> group);

    void onDisconnect(int resultCode);

    void onReceivedData(int resultCode, String type, String data);
}

