package com.peak.salut;

import android.net.nsd.NsdServiceInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.util.Log;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.net.InetAddress;
import java.util.Map;

/**
 * Created by markrjr on 4/17/15.
 */
@JsonObject
public class SalutDevice {

    public WifiP2pDevice device;

    @JsonField
    public Map<String, String> txtRecord;
    @JsonField
    public String deviceName;
    @JsonField
    public String serviceName;
    @JsonField
    public String instanceName;
    @JsonField
    public String readableName;
    @JsonField
    public boolean isRegistered;
    @JsonField
    public boolean isSynced;
    @JsonField
    protected int servicePort;
    @JsonField
    protected String TTP = "._tcp.";
    protected InetAddress deviceAddress;
    protected String macAddress;



    public SalutDevice(){}

    public SalutDevice(NsdServiceInfo otherService)
    {
        this.instanceName = otherService.getServiceName();
        this.readableName = otherService.getServiceName().split("-")[0];
        this.servicePort = otherService.getPort();
        this.TTP = otherService.getServiceType();
        this.deviceAddress = otherService.getHost(); //Assuming the service has been resolved.
    }

    protected NsdServiceInfo getAsServiceInfo()
    {
        NsdServiceInfo thisDevice = new NsdServiceInfo();
        thisDevice.setServiceName(this.instanceName);
        thisDevice.setPort(this.servicePort);
        thisDevice.setServiceType(this.TTP);

        return thisDevice;
    }

    public SalutDevice(WifiP2pDevice device, Map<String, String> txtRecord) {
        this.device = device;
        this.txtRecord = txtRecord;
    }

    @Override
    public String toString()
    {
        return String.format("Salut Device | Service Name: %s TTP: %s Human-Readable Name: %s", instanceName, TTP, readableName);
    }


}
