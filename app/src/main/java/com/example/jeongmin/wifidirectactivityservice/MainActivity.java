package com.example.jeongmin.wifidirectactivityservice;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener{

    private List<WifiP2pDevice> mPeers;
    private List<WifiP2pDevice> mGroup;
    private Boolean mBound = false;

    private WifiWideServiceBinder wifiWideServiceBinder = null;
    private WifiWideServiceCallback wifiWideServiceCallback = null;

    private ListView listView;
    public WifiP2pDevice device;
    public WifiP2pDevice myDevice;
    public ArrayList<String> values;
    public ArrayAdapter<String> adapter;
    private IntentFilter intentFilter;
    ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCreateOwner = (Button) findViewById(R.id.btnCreateOwner);
        Button btnServiceStart = (Button) findViewById(R.id.btnDiscover);
        Button btnServiceEnd = (Button) findViewById(R.id.btnServiceEnd);
        Button btnConnect = (Button) findViewById(R.id.btnConnect);
        Button btnDisconnet = (Button) findViewById(R.id.btnDisconnect);
        Button btnSendFile = (Button) findViewById(R.id.btnSendFile);
        btnCreateOwner.setOnClickListener(this);
        btnServiceStart.setOnClickListener(this);
        btnServiceEnd.setOnClickListener(this);
        btnConnect.setOnClickListener(this);
        btnDisconnet.setOnClickListener(this);
        btnSendFile.setOnClickListener(this);

        mPeers = new ArrayList<WifiP2pDevice>();
        mGroup = new ArrayList<WifiP2pDevice>();
        listView = (ListView) findViewById(R.id.listView);

        // Defined Array values to show in ListView
        values = new ArrayList<String>();
        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);


        // Assign adapter to ListView
        listView.setAdapter(adapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition     = position;
                // ListView Clicked item value
                String  itemValue    = (String) listView.getItemAtPosition(position);
                // Show Alert
                device = mPeers.get(position);
            }
        });

        wifiWideServiceCallback = new WifiWideServiceCallback.Stub(){

            @Override
            public void onCreateGroupAsOwner(int resultCode) throws RemoteException {

            }

            @Override
            public void onDiscoverWifiWidePeers(int resultCode, List<WifiP2pDevice> peerList) throws RemoteException {
                if(resultCode == WifiWideConstants.SUCCESS_CODE) {
                    mPeers = peerList;
                    Log.d(WifiWideService.TAG, "Receive success");
                    values.clear();
                    for (int i = 0; i < mPeers.size(); ++i) {
                        values.add(mPeers.get(i).deviceName);
                    }
                    adapter.notifyDataSetChanged();
                }else{
                    Log.d(WifiWideService.TAG, "Receive fail");
                }
            }

            @Override
            public void onConnectChanged(int resultCode, List<WifiP2pDevice> groupMembers) throws RemoteException {
                mGroup = groupMembers;
            }

            @Override
            public void onDisconnect(int resultCode) throws RemoteException {

            }

            @Override
            public void onReceivedData(WifiP2pDevice sender, int resultCode, String type, String data) throws RemoteException {
                Log.d(WifiWideService.TAG, "sender : " + sender.deviceAddress);
                Log.d(WifiWideService.TAG, "dataType : " + type);
                Log.d(WifiWideService.TAG, "data : " + data);
            }
        };
    }

    public void onClick(View v){
        try {
            switch (v.getId()) {
                case R.id.btnCreateOwner:
                    if (mBound)
                        wifiWideServiceBinder.createGroupAsOwner();
                    break;
                case R.id.btnDiscover:
                    if (mBound)
                        wifiWideServiceBinder.discoverWifiWidePeers();
                    break;
                case R.id.btnServiceEnd:
                    resetData();
                    break;
                case R.id.btnConnect:
                    if (mBound)
                        wifiWideServiceBinder.connect(device);
                    break;
                case R.id.btnDisconnect:
                    if (mBound) {
                        wifiWideServiceBinder.disconnect();
                    }
                    break;
                case R.id.btnSendFile:
                    if (mBound) {
                        Log.d(WifiWideService.TAG, "group : " + mGroup.size());
                        for (int i = 0; i < mGroup.size(); ++i) {
                            wifiWideServiceBinder.transferDataToPeerDevice(myDevice, mGroup.get(i), WifiWideConstants.WIFI_WIDE_STRING_TYPE,
                                    "Hello Peer number " + i);
                        }
                        wifiWideServiceBinder.transferDataToOwnerDevice(myDevice, WifiWideConstants.WIFI_WIDE_STRING_TYPE, "Hello Owner!");
                    }
                    break;
            }
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }
    
    private void resetData(){
        values.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(this, WifiWideService.class);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onStop();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            wifiWideServiceBinder = WifiWideServiceBinder.Stub.asInterface(service);
            try{
                if(wifiWideServiceBinder.registerWifiWideServiceCallback(wifiWideServiceCallback)){
                    Log.d(WifiWideService.TAG, "callback bind success");
                    myDevice = wifiWideServiceBinder.getThisDevice();
                    Log.d(WifiWideService.TAG, "myDevice : " + myDevice.deviceAddress);
                }else{
                    Log.d(WifiWideService.TAG, "callback bind fail");
                }
            }catch (RemoteException e){
                e.printStackTrace();
            }
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            wifiWideServiceBinder = null;
            mBound = false;
        }
    };
}
