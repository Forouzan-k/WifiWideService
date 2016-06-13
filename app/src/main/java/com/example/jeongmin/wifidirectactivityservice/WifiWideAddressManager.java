package com.example.jeongmin.wifidirectactivityservice;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by Jeongmin on 2016-05-24.
 */
public class WifiWideAddressManager {
    public String ownerAddress;
    public ArrayList<String> peerAddressList;

    public void WifiDirectAddressManager(){
        this.ownerAddress = null;
        this.peerAddressList = null;
    }

    public void initPeerAddressList(){
        if(peerAddressList == null)
            peerAddressList = new ArrayList<String>();
    }

    public void setOwnerAddress(String ownerAddress){
        this.ownerAddress = ownerAddress;
    }

    public Boolean addPeerAddress(String peerAddress){
        if(peerAddressList == null || peerAddress == null)
            return false;
        return peerAddressList.add(peerAddress);
    }

    public void deletePeerAddress(int index){
        if(peerAddressList != null){
            if(peerAddressList.isEmpty()) {
                return;
            }
            peerAddressList.remove(index);
        }else
            return;
    }

    public int getPeerAddressListSize(){
        if(peerAddressList != null) {
            if (!peerAddressList.isEmpty()) {
                return peerAddressList.size();
            }
        }
        return 0;
    }

    public String getOwnerAddress(){
        return ownerAddress;
    }

    public String getPeerDeviceAddress(int index){
        if(peerAddressList != null){
            if(peerAddressList.isEmpty())
                return null;
            StringTokenizer st = new StringTokenizer(peerAddressList.get(index), "-");
            String device = null;
            if(st.hasMoreTokens()){
                device = st.nextToken();
            }
            return device;
        }else
            return null;
    }

    public String getPeerIp(int index){
        if(peerAddressList != null){
            if(peerAddressList.isEmpty())
                return null;
            StringTokenizer st = new StringTokenizer(peerAddressList.get(index), "-");
            String ip = null;
            if(st.hasMoreTokens()){
                st.nextToken();
                if(st.hasMoreTokens()){
                    ip = st.nextToken();
                }
            }
            return ip;
        }else
            return null;
    }

    public String getPeerAddress(int index){
        if(peerAddressList != null){
            if(peerAddressList.isEmpty())
                return null;
            return peerAddressList.get(index);
        }else
            return null;
    }

    public Boolean isEmpty(){
        return peerAddressList.isEmpty();
    }

    public void clear(){
        peerAddressList.clear();
    }
}