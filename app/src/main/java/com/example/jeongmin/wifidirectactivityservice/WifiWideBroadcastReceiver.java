package com.example.jeongmin.wifidirectactivityservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Jeongmin on 2016-05-21.
 */
public class WifiWideBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WifiWideService service;
    public static final ArrayList mPeers = new ArrayList();

    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param service Service associated with the receiver
     */
    public WifiWideBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       WifiWideService service) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.service = service;
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                service.setIsWifiP2pEnabled(true);
            } else {
                service.setIsWifiP2pEnabled(false);
            }
            Log.d(WifiWideService.TAG, "WifiP2pState");
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            Log.d(WifiWideService.TAG, "WifiP2pPeers");
            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                Log.d(WifiWideService.TAG, "request peers");
                manager.requestPeers(channel, (WifiP2pManager.PeerListListener) service);
            }else{
                Log.d(WifiWideService.TAG, "manager null");
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (manager == null) {
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP
                Log.d(WifiWideService.TAG, "connection changed");
                manager.requestConnectionInfo(channel, service);
            } else {
                // It's a disconnect
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            service.setThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }
    }
}
