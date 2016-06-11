package com.example.jeongmin.wifidirectactivityservice;

/**
 * Created by Jeongmin on 2016-05-24.
 */
public class WifiWideConstants {
    public static final String WIFI_WIDE_FILE_TYPE = "FILE";
    public static final String WIFI_WIDE_STRING_TYPE = "STRG";
    public static final String WIFI_WIDE_ADDRESS_TYPE = "ADDR";
    public static final String WIFI_WIDE_FRIENDS_TYPE = "FRND";
    public static final String WIFI_WIDE_EXIT_TYPE = "EXIT";

    public static final int INIT_OWNER_SOCKET_PORT = 8000;
    public static final int INIT_PEER_SOCKET_PORT = 8001;

    public static final int SEND_WIFI_WIDE_FILE_MESSAGE = 1000;
    public static final int SEND_WIFI_WIDE_STRING_MESSAGE = 2000;
    public static final int SEND_WIFI_WIDE_ADDRESS_MESSAGE = 3000;
    public static final int SEND_WIFI_WIDE_STOP_MESSAGE = 4000;

    public static final int RECEIVE_THREAD_FILE_MESSAGE = 1001;
    public static final int RECEIVE_THREAD_STRING_MESSAGE = 2001;
    public static final int RECEIVE_THREAD_ADDRESS_MESSAGE = 3001;
    public static final int RECEIVE_THREAD_STOP_MESSAGE = 4001;
    public static final int RECEIVE_THREAD_FRIENDS_MESSAGE = 5001;

    public static final int SERVER_THREAD_SHORT_TYPE = 10;
    public static final int SERVER_THREAD_LONG_TYPE = 20;

    public static final String WIFI_WIDE_CONNECT_CALLBACK = "WIFI_WIDE_CONNECT_CALLBACK";
    public static final String WIFI_WIDE_DISCONNECT_CALLBACK = "WIFI_WIDE_DISCONNECT_CALLBACK";
    public static final String WIFI_WIDE_DISCOVER_CALLBACK = "WIFI_WIDE_DISCOVER_CALLBACK";
    public static final String WIFI_WIDE_BEOWNER_CALLBACK = "WIFI_WIDE_BEOWNER_CALLBACK";
    public static final String WIFI_WIDE_TRANSFER_CALLBACK = "WIFI_WIDE_TRANSFER_CALLBACK";

    public static final int SUCCESS_CODE = 200;
    public static final int FAIL_CODE = 400;
}
