package com.example.jeongmin.wifidirectactivityservice;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Jeongmin on 2016-05-24.
 */
public class WifiWideServerThread extends Thread{
    private Context context;
    private Handler mHandler;
    private Boolean messageHandler = true;
    private int port;
    private int type;
    private ServerSocket serverSocket = null;
    private Socket client = null;

    public WifiWideServerThread(Context context,Handler handler, int port, int type){
        this.context = context;
        this.mHandler = handler;
        this.port = port;
        this.type = type;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        if(client != null){
            if(client.isBound()){
                try {
                    client.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        if(serverSocket != null){
            if(serverSocket.isBound()){
                try {
                    type = WifiWideConstants.SERVER_THREAD_SHORT_TYPE;
                    serverSocket.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run(){
        super.run();
        Message message = null;
        DataInputStream inputStream = null;
        DataOutputStream outputStream = null;
        try{
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            Log.d(WifiWideService.TAG, "Server: Socket opened, port : " + port);
            do {
                message = Message.obtain();
                try {
                    if(!serverSocket.isClosed()){
                        client = serverSocket.accept();
                    }else
                        break;
                    Log.d(WifiWideService.TAG, "Server: Socket accept");
                    inputStream = new DataInputStream(client.getInputStream());
                    outputStream = new DataOutputStream(client.getOutputStream());
                    Log.d(WifiWideService.TAG, "Server: read");
                    String dataType = inputStream.readUTF();
                    Boolean dataFlag = false;
                    if(dataType.length() == 4) {
                        Log.d(WifiWideService.TAG, "rcv : " + dataType);
                        switch (dataType) {
                            case WifiWideConstants.WIFI_WIDE_STRING_TYPE:
                                outputStream.writeUTF(String.valueOf(WifiWideConstants.SUCCESS_CODE));
                                outputStream.flush();
                                dataFlag = true;
                                break;
                            case WifiWideConstants.WIFI_WIDE_ADDRESS_TYPE:
                                outputStream.writeUTF(String.valueOf(WifiWideConstants.SUCCESS_CODE));
                                outputStream.flush();
                                dataFlag = true;
                                break;
                            case WifiWideConstants.WIFI_WIDE_FILE_TYPE:
                                outputStream.writeUTF(String.valueOf(WifiWideConstants.SUCCESS_CODE));
                                outputStream.flush();
                                dataFlag = true;
                                break;
                            case WifiWideConstants.WIFI_WIDE_FRIENDS_TYPE:
                                outputStream.writeUTF(String.valueOf(WifiWideConstants.SUCCESS_CODE));
                                outputStream.flush();
                                dataFlag = true;
                                break;
                            default:
                                outputStream.writeUTF(String.valueOf(WifiWideConstants.FAIL_CODE));
                                outputStream.flush();
                                dataFlag = false;
                                break;
                        }
                        if(dataFlag){
                            inputStream = new DataInputStream(client.getInputStream());
                            String data;
                            switch (dataType) {
                                case WifiWideConstants.WIFI_WIDE_STRING_TYPE:
                                    message.what = WifiWideConstants.RECEIVE_THREAD_STRING_MESSAGE;
                                    data = inputStream.readUTF();
                                    if(data != null)
                                        Log.d(WifiWideService.TAG, "rcv data : " + data);
                                    message.obj = data;
                                    break;
                                case WifiWideConstants.WIFI_WIDE_ADDRESS_TYPE:
                                    message.what = WifiWideConstants.RECEIVE_THREAD_ADDRESS_MESSAGE;
                                    data = inputStream.readUTF();
                                    if(data != null)
                                        Log.d(WifiWideService.TAG, "rcv data : " + data);
                                    message.obj = data;
                                    break;
                                case WifiWideConstants.WIFI_WIDE_FRIENDS_TYPE:
                                    message.what = WifiWideConstants.RECEIVE_THREAD_FRIENDS_MESSAGE;
                                    data = inputStream.readUTF();
                                    if(data != null)
                                        Log.d(WifiWideService.TAG, "rcv data : " + data);
                                    message.obj = data;
                                    break;
                                case WifiWideConstants.WIFI_WIDE_FILE_TYPE:
                                    message.what = WifiWideConstants.RECEIVE_THREAD_FILE_MESSAGE;
                                    final File f = new File(Environment.getExternalStorageDirectory() + "/wifip2pshared-" + "hello.apk");
                                    File dirs = new File(f.getParent());
                                    if (!dirs.exists()) {
                                        if(!dirs.mkdirs()){
                                            Log.d(WifiWideService.TAG, "dir fail");
                                        }
                                    }
                                    f.createNewFile();
                                    Log.d(WifiWideService.TAG, "file : "+ f.getAbsolutePath());
                                    message.obj = f.getAbsolutePath();
                                    InputStream inputstream = client.getInputStream();
                                    WifiWideService.copyFile(inputstream, new FileOutputStream(f));
                                    break;
                                default:
                                    message.what = WifiWideConstants.RECEIVE_THREAD_STOP_MESSAGE;
                                    type = WifiWideConstants.SERVER_THREAD_SHORT_TYPE;
                                    messageHandler = false;
                                    break;
                            }
                            if(messageHandler)
                                mHandler.sendMessage(message);
                        }
                    } else {
                        Log.d(WifiWideService.TAG, "DataType : null");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                            Log.d(WifiWideService.TAG, "input stream close");
                        } catch (IOException e) {
                            Log.e(WifiWideService.TAG, e.getMessage());
                        }
                    }
                    if(outputStream != null){
                        try {
                            outputStream.close();
                            Log.d(WifiWideService.TAG, "output stream close");
                        } catch (IOException e) {
                            Log.e(WifiWideService.TAG, e.getMessage());
                        }
                    }
                    if (client != null) {
                        try {
                            client.close();
                            Log.d(WifiWideService.TAG, "client close");
                        } catch (IOException e) {
                            Log.e(WifiWideService.TAG, e.getMessage());
                        }
                    }
                }
            } while (type == WifiWideConstants.SERVER_THREAD_LONG_TYPE);
            message.what = WifiWideConstants.SEND_WIFI_WIDE_STOP_MESSAGE;
            mHandler.sendMessage(message);
        }catch(IOException e){
            e.printStackTrace();
        }finally {
            if(serverSocket != null){
                try{
                    serverSocket.close();
                    Log.d(WifiWideService.TAG, "server socket close");
                } catch (IOException e) {
                    Log.e(WifiWideService.TAG, e.getMessage());
                }
            }
        }
    }
}

