package com.example.jeongmin.wifidirectactivityservice;

import java.util.ArrayList;

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

    public void deletePeerAddress(String peerAddress){
        if(peerAddressList != null){
            if(peerAddressList.isEmpty()) {
                return;
            }
            int select = 0;
            for(int i = 0; i < peerAddressList.size(); i++){
                if(peerAddressList.get(i).equals(peerAddress)){
                    select = i;
                    break;
                }
            }
            peerAddressList.remove(select);
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