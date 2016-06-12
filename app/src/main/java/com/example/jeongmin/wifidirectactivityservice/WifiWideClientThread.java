package com.example.jeongmin.wifidirectactivityservice;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Jeongmin on 2016-05-24.
 */
public class WifiWideClientThread extends Thread{

    private int timeout = 5000;
    private Context context;
    private WifiP2pDevice sender;
    private int port;
    private Handler mHandler;
    private String datatype;
    private String host;
    private String data;
    private Boolean success = false;

    public WifiWideClientThread(Context context, Handler handler, WifiP2pDevice sender, int port, String host, String datatype, String data){
        this.context = context;
        this.mHandler = handler;
        this.sender = sender;
        this.port = port;
        this.host = host;
        this.datatype = datatype;
        this.data = data;
    }

    @Override
    public void run() {
        super.run();
        Log.d(WifiWideService.TAG, "address transfer service start");
        Socket socket = new Socket();
        Log.d(WifiWideService.TAG, "host : " + host + "port : " + port);
        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;
        do {
            Message message = Message.obtain();
            try {
                socket.bind(null);
                Log.d(WifiWideService.TAG, "host : " + host + "port : " + port);
                InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                socket.connect(inetSocketAddress, timeout);
                Log.d(WifiWideService.TAG, "host address : " + socket.getInetAddress().getHostAddress());
                outputStream = new DataOutputStream(socket.getOutputStream());
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream.writeUTF(datatype);
                outputStream.flush();
                String rcv = inputStream.readUTF();
                Log.d(WifiWideService.TAG, "rcv : " + rcv);
                if (rcv.equals(String.valueOf(WifiWideConstants.SUCCESS_CODE))) {
                    Log.d(WifiWideService.TAG, "send : " + datatype);
                    switch (datatype) {
                        case WifiWideConstants.WIFI_WIDE_ADDRESS_TYPE:
                            data = socket.getLocalAddress().getHostAddress();
                            message.what = WifiWideConstants.SEND_WIFI_WIDE_ADDRESS_MESSAGE;
                            message.obj = socket.getLocalAddress().getHostAddress();
                            Log.d(WifiWideService.TAG, "peer address : " + data);
                            try {
                                outputStream.writeUTF(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case WifiWideConstants.WIFI_WIDE_STRING_TYPE:
                            message.what = WifiWideConstants.SEND_WIFI_WIDE_STRING_MESSAGE;
                            Log.d(WifiWideService.TAG, "sender address : " + sender.deviceAddress);
                            message.obj = data;
                            data = sender.deviceAddress + "-" + data;
                            try {
                                outputStream.writeUTF(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case WifiWideConstants.WIFI_WIDE_FRIENDS_TYPE:
                            message.obj = data;
                            try {
                                outputStream.writeUTF(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case WifiWideConstants.WIFI_WIDE_FILE_TYPE:
                            Log.d(WifiWideService.TAG, "File URI : " + data);
                            ContentResolver cr = context.getContentResolver();
                            InputStream is = null;
                            String fileUri = data;
                            try {
                                is = cr.openInputStream(Uri.parse(fileUri));
                            } catch (FileNotFoundException e) {
                                Log.d(WifiWideService.TAG, e.toString());
                            }
                            message.what = WifiWideConstants.SEND_WIFI_WIDE_FILE_MESSAGE;
                            message.obj = data;
                            WifiWideService.copyFile(is, outputStream);
                            break;
                        default:
                            message.what = WifiWideConstants.FAIL_CODE;
                            break;
                    }
                }
                success = true;
                mHandler.sendMessage(message);
                Log.d(WifiWideService.TAG, "send message");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
        }while (!success);
    }
}
