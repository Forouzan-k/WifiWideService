package com.example.jeongmin.wifidirectactivityservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
/**
 * Created by Jeongmin on 2016-05-21.
 */
public class WifiWideService extends Service implements WifiP2pManager.ChannelListener,
        WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener{

    public static final String TAG = "WifiWideService";

    private WifiP2pManager manager;
    private WifiWideAddressManager addressManager;
    private List<WifiP2pDevice> deviceList = new ArrayList<WifiP2pDevice>();
    private List<WifiP2pDevice> groupList = new ArrayList<WifiP2pDevice>();
    private WifiP2pDevice myDevice = new WifiP2pDevice();
    private WifiP2pInfo wifiP2pInfo;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private Context context;

    private final IntentFilter intentFilter = new IntentFilter();

    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;

    private MessageHandler messageHandler;

    private WifiWideServerThread wifiWideServerThread = null;

    public WifiWideService(){

    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(WifiWideService.TAG, "Service start");
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        context = getApplicationContext();
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        receiver = new WifiWideBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);

        addressManager = new WifiWideAddressManager();
        addressManager.initPeerAddressList();

        messageHandler = new MessageHandler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
        Log.d(WifiWideService.TAG, "Service End");
    }

    public void setThisDevice(WifiP2pDevice device){
        this.myDevice = device;
    }
    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
        Log.d(WifiWideService.TAG, "WifiP2pEnabled : "+ isWifiP2pEnabled);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        deviceList.clear();
        deviceList.addAll(peerList.getDeviceList());
        if (deviceList.size() == 0) {
            Log.d(WifiWideService.TAG, "No devices found");
            doServiceCallback(WifiWideConstants.FAIL_CODE, WifiWideConstants.WIFI_WIDE_DISCOVER_CALLBACK,
                    null, null, null, null);
        }else{
            Log.d(WifiWideService.TAG, "Devices found");
            doServiceCallback(WifiWideConstants.SUCCESS_CODE, WifiWideConstants.WIFI_WIDE_DISCOVER_CALLBACK,
                    deviceList, null, null, null);
        }
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(context, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            retryChannel = true;
            manager.initialize(context, getMainLooper(), this);
        } else {
            Toast.makeText(context,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(WifiWideService.TAG, "on Connection");
        this.wifiP2pInfo = info;

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        manager.requestGroupInfo(channel, this);
        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            Log.d(WifiWideService.TAG, "I'm owner");
            if(deviceList.size() != 0) {
                if (deviceList.size() != addressManager.getPeerAddressListSize()) {
                    addressManager.setOwnerAddress(wifiP2pInfo.groupOwnerAddress.getHostAddress());
                    if(wifiWideServerThread == null) {
                        wifiWideServerThread = new WifiWideServerThread(context, new MessageHandler(), WifiWideConstants.INIT_OWNER_SOCKET_PORT, WifiWideConstants.SERVER_THREAD_LONG_TYPE);
                        wifiWideServerThread.start();
                    }
                }
            }
        }
        if (wifiP2pInfo.groupFormed) {
            if (!wifiP2pInfo.isGroupOwner){
                Log.d(WifiWideService.TAG, "I'm peer");
                if(deviceList.size() != 0) {
                    if (deviceList.size() != addressManager.getPeerAddressListSize()) {
                        addressManager.setOwnerAddress(wifiP2pInfo.groupOwnerAddress.getHostAddress());

                        Log.d(WifiWideService.TAG, "Here Address send");

                        new WifiWideClientThread(context, messageHandler, myDevice, WifiWideConstants.INIT_OWNER_SOCKET_PORT,
                                info.groupOwnerAddress.getHostAddress(), WifiWideConstants.WIFI_WIDE_ADDRESS_TYPE, null).start();
                    }
                }
            }
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        groupList.clear();
        groupList.addAll(group.getClientList());
        for(int i = 0;i < groupList.size(); ++i){
            Log.d(WifiWideService.TAG, "group member " + i + " : " + groupList.get(i).deviceAddress);
        }
        refreshAddress();
        sendFriendsAddress();
    }

    public void refreshAddress(){
        if(addressManager != null){
            if(addressManager.getPeerAddressListSize() >= 1){
                for(int i = 0; i < addressManager.getPeerAddressListSize(); ++i){
                    boolean exist = false;
                    for(int j = 0; j < groupList.size(); ++j){
                        if(addressManager.getPeerDeviceAddress(i).equals(groupList.get(j).deviceAddress)){
                            exist = true;
                            break;
                        }
                    }
                    if(!exist){
                        addressManager.deletePeerAddress(i);
                    }
                }
            }
        }
    }

    public void sendFriendsAddress(){
        if(wifiP2pInfo != null){
            if(wifiP2pInfo.isGroupOwner){
                if(groupList.size() >= 1){
                    for(int i = 0; i < groupList.size(); ++i){
                        String address = groupList.get(i).deviceAddress;
                        String ip = null;
                        int count = 0;
                        if(addressManager != null){
                            if(addressManager.getPeerAddressListSize() >= 1){
                                for(int j = 0; j < addressManager.getPeerAddressListSize(); ++j){
                                    if(addressManager.getPeerDeviceAddress(j).equals(address)){
                                        ip = addressManager.getPeerIp(j);
                                    }
                                }
                                if(ip != null){
                                    String friends = "";
                                    for(int j = 0; j < addressManager.getPeerAddressListSize(); ++j){
                                        if(!addressManager.getPeerDeviceAddress(j).equals(address)){
                                            friends += addressManager.getPeerAddress(j);
                                            count++;
                                            if(count < addressManager.getPeerAddressListSize() - 1){
                                                friends += ",";
                                            }
                                        }
                                    }
                                    Log.d(WifiWideService.TAG, "address :" + ip + ", frnd : " + friends);
                                    transferData(myDevice, getPortByAddress(ip), ip, WifiWideConstants.WIFI_WIDE_FRIENDS_TYPE, friends);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean transferData(WifiP2pDevice sender, int port, String address, String type, String data){
        Log.d(WifiWideService.TAG, "transfer Data");
        if(type == null)
            return false;
        new WifiWideClientThread(context, messageHandler, sender, port, address, type, data).start();
        return true;
    }

    public Boolean refreshAddressList(String addressList){
        addressManager.clear();
        groupList.clear();
        Log.d(WifiWideService.TAG, addressList);
        StringTokenizer st = new StringTokenizer(addressList, ",");
        while(st.hasMoreTokens()){
            String peerAddress = st.nextToken();
            Log.d(WifiWideService.TAG, peerAddress);
            StringTokenizer st2 = new StringTokenizer(peerAddress, "-");
            if(st2.hasMoreTokens()) {
                String deviceAddress = st2.nextToken();
                WifiP2pDevice device = new WifiP2pDevice();
                device.deviceAddress = deviceAddress;
                groupList.add(device);
            }
            addressManager.addPeerAddress(peerAddress);
        }
        return !addressManager.isEmpty();
    }

    public int getPortByAddress(String address){
        StringTokenizer st = new StringTokenizer(address, ".");
        String tmpPort = "0";
        int port = WifiWideConstants.INIT_PEER_SOCKET_PORT;
        while(st.hasMoreTokens()){
            tmpPort = st.nextToken();
        }
        port = port + Integer.parseInt(tmpPort);
        return port;
    }

    public WifiP2pDevice getWifiP2pDeviceFromData(String data){
        WifiP2pDevice sender = new WifiP2pDevice();
        StringTokenizer st = new StringTokenizer(data, "-");
        if(st.hasMoreTokens()){
            sender.deviceAddress = st.nextToken();
        }
        return sender;
    }

    class MessageHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch(msg.what){
                case WifiWideConstants.RECEIVE_THREAD_STRING_MESSAGE:
                    if (msg.obj != null) {
                        String data = msg.obj.toString();
                        WifiP2pDevice sender = getWifiP2pDeviceFromData(data);
                        Log.d(WifiWideService.TAG, "receive : string = " + data);
                        doServiceCallback(WifiWideConstants.SUCCESS_CODE, WifiWideConstants.WIFI_WIDE_TRANSFER_CALLBACK,
                                null, sender, WifiWideConstants.WIFI_WIDE_STRING_TYPE, data);
                    }else{
                        doServiceCallback(WifiWideConstants.FAIL_CODE, WifiWideConstants.WIFI_WIDE_TRANSFER_CALLBACK,
                                null, null, null, null);
                    }
                    break;
                case WifiWideConstants.RECEIVE_THREAD_FILE_MESSAGE:
                    if (msg.obj != null) {
                        String data = msg.obj.toString();
                        Log.d(WifiWideService.TAG, "receive : file = " + msg.obj.toString());
                        doServiceCallback(WifiWideConstants.SUCCESS_CODE, WifiWideConstants.WIFI_WIDE_TRANSFER_CALLBACK,
                                null, null, WifiWideConstants.WIFI_WIDE_FILE_TYPE, data);
                    }else{
                        doServiceCallback(WifiWideConstants.FAIL_CODE, WifiWideConstants.WIFI_WIDE_TRANSFER_CALLBACK,
                                null, null, null, null);
                    }
                    break;
                case WifiWideConstants.RECEIVE_THREAD_ADDRESS_MESSAGE:
                    if (msg.obj != null) {
                        String peerAddress = msg.obj.toString();
                        if(addressManager.addPeerAddress(peerAddress)) {
                            sendFriendsAddress();
                            doServiceCallback(WifiWideConstants.SUCCESS_CODE, WifiWideConstants.WIFI_WIDE_CONNECT_CALLBACK,
                                    groupList, null, null, null);
                            Log.d(WifiWideService.TAG, "add : " + peerAddress);
                            Log.d(WifiWideService.TAG, "size : " + addressManager.getPeerAddressListSize());
                        }else{
                            Log.d(WifiWideService.TAG, "connect fail");
                            doServiceCallback(WifiWideConstants.FAIL_CODE, WifiWideConstants.WIFI_WIDE_CONNECT_CALLBACK,
                                    null, null, null, null);
                        }
                    }
                    break;
                case WifiWideConstants.RECEIVE_THREAD_STOP_MESSAGE:
                    Log.d(WifiWideService.TAG, "wtf");
                    break;
                case WifiWideConstants.RECEIVE_THREAD_FRIENDS_MESSAGE:
                    Log.d(WifiWideService.TAG, "friends");
                    if(refreshAddressList(msg.obj.toString())){
                        doServiceCallback(WifiWideConstants.SUCCESS_CODE, WifiWideConstants.WIFI_WIDE_CONNECT_CALLBACK,
                                groupList, null, null, null);
                    }else{
                        Log.d(WifiWideService.TAG, "connect fail");
                        doServiceCallback(WifiWideConstants.FAIL_CODE, WifiWideConstants.WIFI_WIDE_CONNECT_CALLBACK,
                                groupList, null, null, null);
                    }
                    break;
                case WifiWideConstants.SEND_WIFI_WIDE_ADDRESS_MESSAGE:
                    Log.d(WifiWideService.TAG, "address send success");
                    String address = msg.obj.toString();
                    int port = getPortByAddress(address);
                    if(wifiWideServerThread == null){
                        wifiWideServerThread = new WifiWideServerThread(context, messageHandler, port,
                                WifiWideConstants.SERVER_THREAD_LONG_TYPE);
                        wifiWideServerThread.start();
                    }
                    break;
                default:
                    Log.d(WifiWideService.TAG, "default");
                    break;
            }
        }
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WifiWideService.TAG, e.toString());
            return false;
        }
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    final RemoteCallbackList<WifiWideServiceCallback> callbackList = new RemoteCallbackList<WifiWideServiceCallback>();

    public void doServiceCallback(int resultCode, String callbackType, List<WifiP2pDevice> list,
                                  WifiP2pDevice sender, String type, String data){
        int cnt = callbackList.beginBroadcast();
        for(int i = 0; i < cnt; ++i){
            WifiWideServiceCallback callback = callbackList.getBroadcastItem(i);
            try {
                switch (callbackType) {
                    case WifiWideConstants.WIFI_WIDE_BEOWNER_CALLBACK:
                        callback.onCreateGroupAsOwner(resultCode);
                        break;
                    case WifiWideConstants.WIFI_WIDE_DISCOVER_CALLBACK:
                        callback.onDiscoverWifiWidePeers(resultCode, list);
                        break;
                    case WifiWideConstants.WIFI_WIDE_CONNECT_CALLBACK:
                        callback.onConnectChanged(resultCode, list);
                        break;
                    case WifiWideConstants.WIFI_WIDE_DISCONNECT_CALLBACK:
                        callback.onDisconnect(resultCode);
                        break;
                    case WifiWideConstants.WIFI_WIDE_TRANSFER_CALLBACK:
                        callback.onReceivedData(sender, resultCode, type, data);
                        break;
                }
            }catch (RemoteException e){
                e.printStackTrace();
            }
        }
        callbackList.finishBroadcast();
    }
    WifiWideServiceBinder.Stub mBinder = new WifiWideServiceBinder.Stub(){
        @Override
        public boolean registerWifiWideServiceCallback(WifiWideServiceCallback callback) throws RemoteException {
            return callbackList.register(callback);
        }

        @Override
        public boolean unregisterWifiWideServiceCallback(WifiWideServiceCallback callback) throws RemoteException {
            return callbackList.unregister(callback);
        }

        @Override
        public WifiP2pDevice getThisDevice() throws RemoteException {
            return myDevice;
        }

        @Override
        public void createGroupAsOwner() throws RemoteException {
            manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(WifiWideService.TAG, "create group as Owner");
                    doServiceCallback(WifiWideConstants.SUCCESS_CODE, WifiWideConstants.WIFI_WIDE_BEOWNER_CALLBACK,
                            null, null, null, null);
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(WifiWideService.TAG, "create group failed");
                    doServiceCallback(WifiWideConstants.FAIL_CODE, WifiWideConstants.WIFI_WIDE_BEOWNER_CALLBACK,
                            null, null, null, null);
                }
            });
        }

        @Override
        public void disconnect() throws RemoteException {
            if(wifiWideServerThread!= null){
                if(wifiWideServerThread.isAlive()){
                    wifiWideServerThread.interrupt();
                    wifiWideServerThread = null;
                }
            }
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                    doServiceCallback(WifiWideConstants.FAIL_CODE, WifiWideConstants.WIFI_WIDE_DISCONNECT_CALLBACK,
                            null, null, null, null);
                }
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Disconnect success");
                    doServiceCallback(WifiWideConstants.SUCCESS_CODE, WifiWideConstants.WIFI_WIDE_DISCONNECT_CALLBACK,
                            null, null, null, null);
                }
            });

        }

        @Override
        public void connect(WifiP2pDevice device) throws RemoteException {
            Log.d(WifiWideService.TAG, "Connect");
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            config.wps.setup = WpsInfo.PBC;

            Log.d(WifiWideService.TAG, "device : " + config.deviceAddress);
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                    Log.d(WifiWideService.TAG, "Connect success");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(WifiWideService.TAG, "Connect failed. retry");
                }
            });
        }

        @Override
        public void discoverWifiWidePeers() throws RemoteException {
            Log.d(WifiWideService.TAG, "Discover");
            if (!isWifiP2pEnabled) {
                Toast.makeText(context, "Set on the Wifi", Toast.LENGTH_SHORT).show();
                return ;
            }
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(WifiWideService.TAG, "Discovery Initiated");
                }

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(WifiWideService.TAG, "Discovery Failed : " + reasonCode);
                }
            });

        }

        @Override
        public boolean transferDataToOwnerDevice(WifiP2pDevice sender, String type, String data) throws RemoteException {
            if(!wifiP2pInfo.isGroupOwner)
                return transferData(sender, WifiWideConstants.INIT_OWNER_SOCKET_PORT,
                        wifiP2pInfo.groupOwnerAddress.getHostAddress(), type, data);
            else
                return false;
        }

        @Override
        public boolean transferDataToPeerDevice(WifiP2pDevice sender, WifiP2pDevice receiver,
                                                String type, String data) throws RemoteException {
            String address = null;
            for(int i = 0; i < addressManager.getPeerAddressListSize(); ++i){
                if(receiver.deviceAddress.equals(addressManager.getPeerDeviceAddress(i))){
                    address = addressManager.getPeerIp(i);
                    break;
                }
            }

            if(address != null) {
                int port = getPortByAddress(address);
                return transferData(sender, port, address, type, data);
            }else{
                return false;
            }
        }
    };
}
