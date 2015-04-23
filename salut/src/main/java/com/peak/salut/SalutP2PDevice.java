package com.peak.salut;

import android.net.wifi.p2p.WifiP2pDevice;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.net.InetAddress;
import java.util.Map;

@JsonObject
public class SalutP2PDevice {

    @JsonField
    public Map<String, String> txtRecord;
    @JsonField
    public String macAddress;
    @JsonField
    public String readableName;
    @JsonField
    public boolean isSynced = false;
    @JsonField
    public boolean isRegistered = false;
    @JsonField
    public String deviceName;

    public WifiP2pDevice device;
    public InetAddress actualDeviceAddress;

    public SalutP2PDevice(){}

    public SalutP2PDevice(WifiP2pDevice device, Map<String, String> txtRecord) {
        this.device = device;
        this.txtRecord = txtRecord;
    }
}
