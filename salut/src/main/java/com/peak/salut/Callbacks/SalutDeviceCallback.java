package com.peak.salut.Callbacks;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Map;

/**
 * Created by Mark on 4/5/2015.
 */
public interface SalutDeviceCallback {
    void call(Map<String, String> serviceData, WifiP2pDevice foundDevice);
}
