package com.peak.salut;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class Salut{


    private final static String TAG = "Salut";

    private static WifiManager wifiManager;
    private boolean respondersAlreadySet = false;
    private boolean firstDeviceAlreadyFound = false;

    public boolean serviceIsRunning = false;

    //WiFi P2P Objects
    private WifiP2pServiceInfo serviceInfo;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    public HashMap<String, WifiP2pDevice> foundServiceDevices;
    private Collection foundDevices;
    public IntentFilter intentFilter = new IntentFilter();
    public BroadcastReceiver receiver = null;
    private static Context currentContext;

    //Service Data
    public static String serviceName;
    public static String instanceName;
    private Map<String, String> serviceData = new HashMap(); //Information other devices will want once they connect to this one.

    //Protocol and Transport Layers
    public String TTP = "._tcp";
    private final String SERVER_PORT = "25400"; //TODO Should not be hardcoded, instead should be an available port handed by Android.

    public Salut(Context currentContext, String instanceName, String serviceName, Map<String, String> serviceData)
    {
        this.currentContext = currentContext;
        this.serviceName = serviceName;
        this.instanceName = instanceName;
        this.serviceData = serviceData;
        TTP = serviceName + TTP;

        foundServiceDevices = new HashMap<>();
        foundDevices = new ArrayList();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) currentContext.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(currentContext, currentContext.getMainLooper(), null);

        receiver = new SalutBroadcastReceiver(manager, channel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                foundDevices = wifiP2pDeviceList.getDeviceList();
            }
        });
    }

    public static void enableWiFi(Context context)
    {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
    }

    public static boolean isWiFiEnabled(Context context)
    {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static void disableWiFi(Context context)
    {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
    }


    public void connectToDevice(WifiP2pDevice device, final SalutCallback onSuccess, final SalutCallback onFailure)
    {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                onSuccess.call();
            }

            @Override
            public void onFailure(int reason) {
                onFailure.call();
            }
        });
    }

    public static boolean hotspotIsEnabled(Context context)
    {
        try
        {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);

            return (Boolean) method.invoke(wifiManager, (Object[]) null);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.d(TAG, "Failed to check tethering state, or it is not enabled.");
        }

        return false;
    }

    private void createService() {

        Log.d(TAG, "Starting " + serviceName + " Transport Protocol " + TTP);

        //Inject the listening port along with whatever else data is sent.
        this.serviceData.put("LISTEN_PORT", String.valueOf(SERVER_PORT));

        //Create a service info object will android will actually hand out to the clients.
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(instanceName, TTP , this.serviceData);

        //Register our service. The callbacks here just let us know if the service was registered correctly,
        //not necessarily whether or not we connected to a device.
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


    }

    public void startNetworkService()
    {
        createService();
        //In order to have a service that you create be seen, you must also actively look for other services. This is an Android bug.
        //For more information, read here. https://code.google.com/p/android/issues/detail?id=37425
        //We do not need to setup DNS responders.
        discoverNetworkServices();
    }

    public void startNetworkService(SalutCallback onDeviceFound, final boolean callContinously)
    {
        createService();
        discoverNetworkServices(onDeviceFound, callContinously);
    }

    private void setupDNSResponders(final SalutCallback onDeviceFound, final boolean callContinously)
    {
         /*Here, we register a listener for when services are actually found. The WiFi P2P specification notes that we need two types of
         *listeners, one for a DNS service and one for a TXT record. The DNS service listener is invoked whenever a service is found, regardless
         *of whether or not it is yours. To that determine if it is, we must compare our service name with the service name. If it is our service,
         * we simply log.*/

        WifiP2pManager.DnsSdServiceResponseListener serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String serviceNameAndTP, WifiP2pDevice sourceDevice) {

                Log.d(TAG, "Found " + instanceName +  " " + serviceNameAndTP);

                if (serviceNameAndTP.equalsIgnoreCase(TTP))
                {
                    Log.v(TAG, "Found service " + instanceName + " running on " + sourceDevice.deviceName + " registered as " + serviceNameAndTP);
                }

            }
        };

        /*The TXT record contains specific information about a service and it's listener can also be invoked regardless of the device. Here, we
        *double check if the device is ours, and then we go ahead and pull that specific information from it and put it into an Map. The function
        *that was passed in early is also called.*/
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String serviceFullDomainName, Map<String, String> record, WifiP2pDevice device) {

                Log.d(TAG, "Found " + device.deviceName +  " " + record.values().toString());

                String username = record.get("username");

                if (!foundServiceDevices.containsValue(device) && record.get("username") != null)
                {
                    foundServiceDevices.put(username, device);
                    if(!firstDeviceAlreadyFound && !callContinously)
                    {
                        onDeviceFound.call();
                        firstDeviceAlreadyFound = true;
                    }
                    else if(firstDeviceAlreadyFound && callContinously)
                    {
                        onDeviceFound.call();
                    }
                }
            }
        };
        
        manager.setDnsSdResponseListeners(channel, serviceListener, txtRecordListener);
        respondersAlreadySet = true;
    }
    
    private void setupDNSRespondersWithDevice(final SalutDeviceCallback onDeviceFound, final boolean callContinously)
    {
         /*Here, we register a listener for when services are actually found. The WiFi P2P specification notes that we need two types of
         *listeners, one for a DNS service and one for a TXT record. The DNS service listener is invoked whenever a service is found, regardless
         *of whether or not it is yours. To that determine if it is, we must compare our service name with the service name. If it is our service,
         * we simply log.*/

        WifiP2pManager.DnsSdServiceResponseListener serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String serviceNameAndTP, WifiP2pDevice sourceDevice) {

                Log.d(TAG, "Found " + instanceName +  " " + serviceNameAndTP);

                if (serviceNameAndTP.equalsIgnoreCase(TTP))
                {
                    Log.v(TAG, "Found service " + instanceName + " running on " + sourceDevice.deviceName + " registered as " + serviceNameAndTP);
                }

            }
        };

        /*The TXT record contains specific information about a service and it's listener can also be invoked regardless of the device. Here, we
        *double check if the device is ours, and then we go ahead and pull that specific information from it and put it into an Map. The function
        *that was passed in early is also called.*/
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String serviceFullDomainName, Map<String, String> record, WifiP2pDevice device) {

                Log.d(TAG, "Found " + device.deviceName +  " " + record.values().toString());

                String username = record.get("username");

                if (!foundServiceDevices.containsValue(device) && record.get("username") != null)
                {
                    foundServiceDevices.put(username, device);
                    if(!firstDeviceAlreadyFound && !callContinously)
                    {
                        onDeviceFound.call(record, device);
                        firstDeviceAlreadyFound = true;
                    }
                    else if(firstDeviceAlreadyFound && callContinously)
                    {
                        onDeviceFound.call(record, device);
                    }
                }
            }
        };

        manager.setDnsSdResponseListeners(channel, serviceListener, txtRecordListener);
        respondersAlreadySet = true;
    }

    public void devicesNotFoundInTime(int timeout, final SalutCallback cleanUpFunction)
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!firstDeviceAlreadyFound) {
                    disableWiFi(currentContext);
                    disposeServiceRequests();
                    cleanUpFunction.call();
                }
            }
        }, timeout);
    }


    private void discoverNetworkServices()
    {
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

        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discovery initiated.");
            }
            @Override
            public void onFailure(int arg0) {
                Log.d(TAG, "Service discovery has failed." );
            }
        });

    }

    public void discoverNetworkServices(SalutDeviceCallback onDeviceFound, boolean callContinously)
    {
        if(!respondersAlreadySet)
        {
            setupDNSRespondersWithDevice(onDeviceFound, callContinously);
        }

        discoverNetworkServices();
    }

    public void discoverNetworkServicesWithTimeout(SalutDeviceCallback onDeviceFound, boolean callContinously, SalutCallback onDevicesNotFound, int timeout)
    {
        //TODO Use nullable to set flags to reduce method overloading.
        if(!respondersAlreadySet)
        {
            setupDNSRespondersWithDevice(onDeviceFound, callContinously);
        }

        discoverNetworkServices();
        devicesNotFoundInTime(timeout, onDevicesNotFound);
    }

    public void discoverNetworkServices(SalutCallback onDeviceFound, boolean callContinously)
    {
        if(!respondersAlreadySet)
        {
            setupDNSResponders(onDeviceFound, callContinously);
        }

        discoverNetworkServices();
    }


    public void discoverNetworkServicesWithTimeout(SalutCallback onDeviceFound, boolean callContinously, SalutCallback onDevicesNotFound, int timeout)
    {
        if(!respondersAlreadySet)
        {
            setupDNSResponders(onDeviceFound, callContinously);
        }

        discoverNetworkServices();
        devicesNotFoundInTime(timeout, onDevicesNotFound);
    }

    public void disposeNetworkService(final boolean disableWiFi)
    {

        disposeServiceRequests();

        if (manager != null && channel != null && serviceInfo != null) {

            manager.removeLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Could not end the service. Reason : " + reason);
                }

                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully shutdown service.");
                    if(disableWiFi)
                    {
                        disableWiFi(currentContext); //To give time for the requests to be disposed.
                    }
                    serviceIsRunning = false;
                }
            });

            respondersAlreadySet = false;
        }

    }

    public void disposeServiceRequests()
    {
        if (manager != null && channel != null)
        {
            manager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully removed service discovery request.");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Failed to remove service discovery request. Reason : " + reason);
                }
            });
        }
    }
}