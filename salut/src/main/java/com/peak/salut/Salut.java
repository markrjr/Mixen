package com.peak.salut;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;


public class Salut{


    private final static String TAG = "Salut";

    private static boolean wifiWasEnabledBefore = false;
    private static WifiManager wifiManager;

    public boolean serviceIsRunning;

    //WiFi P2P Objects
    private WifiP2pServiceInfo serviceInfo;
    public HashMap<String, WifiP2pDevice> foundDevices;
    public IntentFilter intentFilter = new IntentFilter();
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    public BroadcastReceiver receiver = null;
    private static Context currentContext;

    //Service Data
    public static String serviceName;
    private Map<String, String> serviceData = new HashMap(); //Information other devices will want once they connect to this one.

    //Protocol and Transport Layers
    public String TTP = "._tcp";
    private final String SERVER_PORT = "25400"; //TODO Should not be hardcoded, instead should be an available port handed by Android.

    //TODO For the love of God, please user super here in these constructors to bring it down to just one.

    public Salut(Context currentContext, String serviceName, Map<String, String> serviceData)
    {
        this.currentContext = currentContext;
        this.serviceName = serviceName;
        this.serviceData = serviceData;
        TTP = serviceName + TTP;

        foundDevices = new HashMap<>();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) currentContext.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(currentContext, currentContext.getMainLooper(), null);

        receiver = new SalutBroadcastReceiver(manager, channel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                //Once a general list of devices is discovered.
                Log.d(TAG, "" + wifiP2pDeviceList.getDeviceList().size());
            }
        });
    }


    public static void checkIfIsWifiEnabled(Context context)
    {
        //TODO Create boolean to remember if WiFi was already enabled before we enable it.

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(true);
        }
        else
        {
            wifiWasEnabledBefore = true;
        }
    }

    public static void disableWiFi(Context context)
    {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled() && !wifiWasEnabledBefore)
        {
            wifiManager.setWifiEnabled(false);
        }
    }


    public void startNetworkService(SalutCallback function) {
        this.serviceData.put("LISTEN_PORT", String.valueOf(SERVER_PORT));
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(serviceName, TTP , this.serviceData);

        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully created " + Salut.serviceName + " service running on port " + SERVER_PORT);
                serviceIsRunning = true;
            }

            @Override
            public void onFailure(int error) {
                Log.d(TAG, "Failed to create " + Salut.serviceName + " : Error Code: " + error);
            }
        });

        setupDNSResponders(function);

    }


    private void setupDNSResponders(final SalutCallback function)
    {
         /*
         *Here, we register a listener for services. Each time a service is found we simply log.
         */
        WifiP2pManager.DnsSdServiceResponseListener serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String transportProtocol, WifiP2pDevice sourceDevice) {

                if (instanceName.equalsIgnoreCase(serviceName))
                {
                    Log.v(TAG, "Found a service named " + instanceName + " running on " + sourceDevice.deviceName + " registered as " + transportProtocol);
                }

            }
        };


        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String serviceFullDomainName, Map<String, String> record, WifiP2pDevice device) {

                Log.d(TAG, "Found " + device.deviceName +  " " + record.values().toString());

                String username = record.get("username");

                Toast.makeText(currentContext, "Found " + device.deviceName + "  :  User is " + record.get("username"), Toast.LENGTH_SHORT).show();
                if (!foundDevices.containsValue(device) && record.get("username") != null)
                {
                    foundDevices.put(username, device);
                    function.call();
                }

            }
        };


        manager.setDnsSdResponseListeners(channel, serviceListener, txtRecordListener);


        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        manager.addServiceRequest(channel, serviceRequest,
                //Look for exactly this service.
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "Service discovery request acknowledged.");
                    }
                    @Override
                    public void onFailure(int arg0) {
                        Log.i(TAG, "Failed adding service discovery request.");
                    }
                });

        discoverNetworkService(new SalutCallback() {
            @Override
            public void call() {
                //
            }
        });

    }


    public void discoverNetworkService(final SalutCallback function)
    {
        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discovery initiated.");
                function.call();
            }
            @Override
            public void onFailure(int arg0) {
                Log.d(TAG, "Service discovery has failed." );
            }
        });
    }

    public void disposeNetworkService()
    {
        if (manager != null && channel != null && serviceInfo != null) {
            manager.removeLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Could not end the service. Reason : " + reasonCode);
                }
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully shutdown service.");
                    serviceIsRunning = false;
                }
            });
        }
    }
}





