package com.peak.mixen;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pGroup;
import android.nfc.Tag;
import android.util.Log;

import com.albertcbraun.wifidlite.CreateGroupListener;
import com.albertcbraun.wifidlite.WifiDLite;
import com.albertcbraun.wifidlite.impl.DefaultConfiguration;

/**
 * Created by markrjr on 2/2/15.
 */
public class MixenNetwork {

    WifiDLite p2pClient;
    WifiP2pGroup mixenService;


    public MixenNetwork(Context context)
    {
        p2pClient = WifiDLite.getInstance();
        p2pClient.initialize(context, new DefaultConfiguration());
    }

    public void setupWiFiP2P()
    {
        if (p2pClient.isWifiP2pEnabled())
        {
            return;
        }

        p2pClient.openWifiSettings();
    }

    public void createMixenNetwork()
    {
        p2pClient.createGroup(new CreateGroupListener() {
            @Override
            public void onCreateGroupSuccess(WifiP2pGroup wifiP2pGroup) {
                mixenService = wifiP2pGroup;
                Log.d(Mixen.TAG, "Mixen network service successfully initialized.");
                setupWiFiP2P();
            }

            @Override
            public void onCreateGroupFailure(int i) {
                Log.e(Mixen.TAG, "There was an error creating the P2P group, this functionality will be disabled.");
            }
        });
    }

    public void getMixenServiceName()
    {
        Log.d(Mixen.TAG, mixenService.getNetworkName());

    }


}
