package com.peak.salut;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Map;

/**
 * Created by markrjr on 4/3/15.
 */
public interface SalutDeviceCallback {
    void call(Map<String, String> serviceData, WifiP2pDevice foundDevice);
}
