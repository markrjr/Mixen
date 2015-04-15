package com.peak.salut;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.Handler;
import android.util.Log;

import com.arasthel.asyncjob.AsyncJob;

import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Salut implements WifiP2pManager.ConnectionInfoListener{


    public final static String TAG = "Salut";

    private static WifiManager wifiManager;
    private boolean respondersAlreadySet = false;
    private boolean firstDeviceAlreadyFound = false;
    private SalutCallback deviceNotSupported;

    public boolean serviceIsRunning = false;

    //WiFi P2P Objects
    private WifiP2pServiceInfo serviceInfo;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    public ArrayList<SalutDevice> foundDevices;
    public IntentFilter intentFilter = new IntentFilter();
    public BroadcastReceiver receiver = null;
    private static Context currentContext;

    //Connection Objects
    private ServerSocket server;
    private Socket client;
    public boolean isConnectedToAnotherDevice = false;

    //Service Data
    public static String serviceName;
    public static String instanceName;
    private Map<String, String> serviceData = new HashMap(); //Information other devices will want once they connect to this one.

    //Protocol and Transport Layers
    public String TTP = "._tcp";
    private int SERVER_PORT;

    public Salut(Context currentContext, String instanceName, String serviceName, Map<String, String> serviceData, SalutCallback deviceNotSupported)
    {
        this.currentContext = currentContext;
        this.serviceName = serviceName;
        this.instanceName = instanceName;
        this.serviceData = serviceData;
        this.deviceNotSupported = deviceNotSupported;
        TTP = serviceName + TTP;

        foundDevices = new ArrayList<>();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) currentContext.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(currentContext, currentContext.getMainLooper(), null);

        receiver = new SalutBroadcastReceiver(this, manager, channel);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by the RecieveClientIP AsyncTask;
         */

        if (info.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                //Create a server thread
            } catch (Exception ex) {
                Log.e(TAG,
                        "Failed to create a server thread - " + ex.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            //Send IP to group owner.
        }
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

    public void startGroupOwnerServer()
    {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        AsyncJob groupOwnerHandShakeServer = new AsyncJob.AsyncJobBuilder<>().doInBackground(new AsyncJob.AsyncAction<Object>() {
            @Override
            public Object doAsync() {
                return null;
            }
        })
        .doWhenFinished(new AsyncJob.AsyncResultAction() {
            @Override
            public void onResult(Object o) {

            }
        })
        .withExecutor(executorService)
        .create();

        groupOwnerHandShakeServer.start();

    }

    public void connectToDevice(final WifiP2pDevice device, final SalutCallback onSuccess, final SalutCallback onFailure)
    {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully connected to another device.");
                onSuccess.call();
            }

            @Override
            public void onFailure(int reason) {
                onFailure.call();
                Log.e(TAG, "Failed to connect to device. ");
            }
        });

    }

    public void disconnectFromDevice(final SalutCallback onSuccess, final SalutCallback onFailure)
    {
        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                onSuccess.call();
            }

            @Override
            public void onFailure(int reason) {
                onFailure.call();
                Log.e(TAG, "Failed to disconnect from device. ");
            }
        });
    }


    public void disconnectFromDevice()
    {
        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isConnectedToAnotherDevice = false;
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failed to disconnect from device. ");
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

    private void addServicesToLists(Map<String, String> record, WifiP2pDevice device)
    {
        foundDevices.add(new SalutDevice(device, record));
    }

    private void createService() {

        Log.d(TAG, "Starting " + serviceName + " Transport Protocol " + TTP);

        //Inject the listening port along with whatever else data that is going to be sent.
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

    private void startNetworkService()
    {
        createService();
        //In order to have a service that you create be seen, you must also actively look for other services. This is an Android bug.
        //For more information, read here. https://code.google.com/p/android/issues/detail?id=37425
        //We do not need to setup DNS responders.
        discoverNetworkServices(deviceNotSupported);
    }

    public void startNetworkService(SalutCallback onDeviceFound, final boolean callContinously)
    {
        createService();
        discoverNetworkServices(onDeviceFound, callContinously);
    }

    private void setupDNSResponders()
    {
         /*Here, we register a listener for when services are actually found. The WiFi P2P specification notes that we need two types of
         *listeners, one for a DNS service and one for a TXT record. The DNS service listener is invoked whenever a service is found, regardless
         *of whether or not it is yours. To that determine if it is, we must compare our service name with the service name. If it is our service,
         * we simply log.*/

        WifiP2pManager.DnsSdServiceResponseListener serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String serviceNameAndTP, WifiP2pDevice sourceDevice) {

                Log.d(TAG, "Found " + instanceName +  " " + serviceNameAndTP);

            }
        };

        /*The TXT record contains specific information about a service and it's listener can also be invoked regardless of the device. Here, we
        *double check if the device is ours, and then we go ahead and pull that specific information from it and put it into an Map. The function
        *that was passed in early is also called.*/
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String serviceFullDomainName, Map<String, String> record, WifiP2pDevice device) {

                Log.d(TAG, "Found " + device.deviceName +  " " + record.values().toString());

                addServicesToLists(record, device);
            }
        };

        manager.setDnsSdResponseListeners(channel, serviceListener, txtRecordListener);
        respondersAlreadySet = true;
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

            }
        };

        /*The TXT record contains specific information about a service and it's listener can also be invoked regardless of the device. Here, we
        *double check if the device is ours, and then we go ahead and pull that specific information from it and put it into an Map. The function
        *that was passed in early is also called.*/
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String serviceFullDomainName, Map<String, String> record, WifiP2pDevice device) {

                Log.d(TAG, "Found " + device.deviceName +  " " + record.values().toString());

                addServicesToLists(record, device);
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
            }
        };

        /*The TXT record contains specific information about a service and it's listener can also be invoked regardless of the device. Here, we
        *double check if the device is ours, and then we go ahead and pull that specific information from it and put it into an Map. The function
        *that was passed in early is also called.*/
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String serviceFullDomainName, Map<String, String> record, WifiP2pDevice device) {

                Log.d(TAG, "Found " + device.deviceName +  " " + record.values().toString());

                    addServicesToLists(record, device);
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
        };

        manager.setDnsSdResponseListeners(channel, serviceListener, txtRecordListener);
        respondersAlreadySet = true;
    }

    public void clearFoundDevices()
    {
        foundDevices = new ArrayList<>();
    }

    private void devicesNotFoundInTime(final SalutCallback cleanUpFunction, final SalutCallback devicesFound, int timeout)
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (foundDevices.isEmpty()) {
                    stopServiceDiscovery();
                    cleanUpFunction.call();
                }
                else
                {
                    devicesFound.call();
                }
            }
        }, timeout);
    }


    private void discoverNetworkServices(final SalutCallback deviceNotSupported)
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
                if (arg0 == WifiP2pManager.P2P_UNSUPPORTED)
                    deviceNotSupported.call();
            }
        });

    }

    public void discoverNetworkServices(SalutDeviceCallback onDeviceFound, boolean callContinously)
    {
        //clearFoundDevices();

        if(!respondersAlreadySet)
        {
            setupDNSRespondersWithDevice(onDeviceFound, callContinously);
        }

        discoverNetworkServices(deviceNotSupported);
    }

    public void discoverNetworkServices(SalutCallback onDeviceFound, boolean callContinously)
    {
        //clearFoundDevices();

        if(!respondersAlreadySet)
        {
            setupDNSResponders(onDeviceFound, callContinously);
        }
        discoverNetworkServices(deviceNotSupported);
    }

    public void discoverNetworkServicesWithTimeout(SalutCallback onDevicesFound, SalutCallback onDevicesNotFound, int timeout)
    {
        //clearFoundDevices();

        //TODO Use nullable to set flags to reduce method overloading.
        if(!respondersAlreadySet)
        {
            setupDNSResponders();
        }

        discoverNetworkServices(deviceNotSupported);
        devicesNotFoundInTime(onDevicesNotFound, onDevicesFound, timeout);
    }



    public void stopNetworkService(final boolean disableWiFi)
    {

        stopServiceDiscovery();

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

    public void stopServiceDiscovery()
    {
        if(isConnectedToAnotherDevice)
            disconnectFromDevice();

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