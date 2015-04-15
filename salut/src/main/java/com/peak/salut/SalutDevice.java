package com.peak.salut;

import android.net.wifi.p2p.WifiP2pDevice;

import java.net.InetAddress;
import java.util.Map;

/**
 * Created by Mark on 4/7/2015.
 */
public class SalutDevice {

    public WifiP2pDevice device;
    public Map<String, String> txtRecord;
    public boolean deviceSynced = false;
    public InetAddress actualDeviceAddress;


    public SalutDevice(WifiP2pDevice device, Map<String, String> txtRecord) {
        this.device = device;
        this.txtRecord = txtRecord;
    }
}
