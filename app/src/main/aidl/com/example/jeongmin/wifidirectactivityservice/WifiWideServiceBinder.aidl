// WifiWideServiceBinder.aidl
package com.example.jeongmin.wifidirectactivityservice;

// Declare any non-default types here with import statements
import com.example.jeongmin.wifidirectactivityservice.WifiWideServiceCallback;

interface WifiWideServiceBinder {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
     boolean registerWifiWideServiceCallback(WifiWideServiceCallback callback);

     boolean unregisterWifiWideServiceCallback(WifiWideServiceCallback callback);

     void createGroupAsOwner();

     void discoverWifiWidePeers();

     void connect(in WifiP2pDevice device);

     void disconnect();

     boolean transferDataToPeerDevice(in WifiP2pDevice device, String type, String data);

     boolean transferDataToOwnerDevice(String type, String data);
}
