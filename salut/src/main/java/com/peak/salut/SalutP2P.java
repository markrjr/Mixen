package com.peak.salut;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.Handler;
import android.util.Log;

import com.arasthel.asyncjob.AsyncJob;
import com.bluelinelabs.logansquare.LoganSquare;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class SalutP2P implements WifiP2pManager.ConnectionInfoListener{


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
    private SalutP2PDevice hostDevice;
    private WifiP2pDevice lastConnectedDevice;
    protected SalutP2PDevice thisDevice;
    public ArrayList<SalutP2PDevice> foundDevices;
    public ArrayList<SalutP2PDevice> serviceDevices;
    public IntentFilter intentFilter = new IntentFilter();
    public BroadcastReceiver receiver = null;
    private static Context currentContext;

    //Connection Objects
    public boolean isConnectedToAnotherDevice = false;
    private boolean isHost = false;
    private ServerSocket serverSocket;
    private Socket clientSocket;

    //Service Data
    protected String serviceName;
    protected String instanceName;
    protected Map<String, String> serviceData = new HashMap(); //Information other devices will want once they connect to this one.

    //Protocol and Transport Layers
    private String TTP = "._tcp";
    protected static int connectionTimeout = 2000;
    protected static int serverPort;

    public SalutP2P(Context currentContext, String instanceName, String serviceName, Map<String, String> serviceData, SalutCallback deviceNotSupported)
    {
        this.currentContext = currentContext;
        this.serviceName = serviceName;
        this.instanceName = instanceName;
        this.serviceData = serviceData;
        this.deviceNotSupported = deviceNotSupported;
        TTP = serviceName + TTP;

        foundDevices = new ArrayList<>();
        serviceDevices = new ArrayList<>();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) currentContext.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(currentContext, currentContext.getMainLooper(), null);

        receiver = new SalutBroadcastReceiver(this, manager, channel);
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    private void findRandomOpenPort()
    {
        try {
            serverSocket = new ServerSocket(0);
            serverSocket.setReuseAddress(true);
            serverPort = serverSocket.getLocalPort();
        }
        catch(IOException ex)
        {
            Log.e(TAG, "Failed to get an open port, standard will be used instead.");
            serverPort = 495050;
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        /* This method is automatically called when we connect to a device.
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by the RecieveClientIP AsyncTask;
         */

        if(isHost && !info.isGroupOwner)
        {
            Log.d(TAG, "Connected, but we're not the group owner, reestablishing connection.");
            disconnectFromDeviceToChangeOwner();
        }
        else if(!isHost && info.isGroupOwner)
        {
            Log.d(TAG, "Connected as group owner, but we're not the host, disconnecting...");
            disconnectFromDevice();
        }
        else
        {
            if (info.isGroupOwner) {

                Log.d(TAG, "Connected as group owner.");
                try {
                    startGroupOwnerServer();
                } catch (Exception ex) {
                    Log.e(TAG,"Failed to create a server thread - " + ex.getMessage());
                }

            }
            else {

                if(!thisDevice.isRegistered)
                {
                    Log.d(TAG, "Connected as peer.");
                    //hostDevice.actualDeviceAddress = info.groupOwnerAddress;
                    InetSocketAddress groupOwnerAddress = new InetSocketAddress(info.groupOwnerAddress, serverPort);
                    registerClient(groupOwnerAddress);
                }
            }
        }
    }

    public static void enableWiFi(Context context)
    {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
    }

    public static boolean isWiFiEnabled(Context context)
    {
        if(hotspotIsEnabled(context))
        {
            return true;
        }

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static void disableWiFi(Context context)
    {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
    }

    private void commitClientToService(final SalutP2PDevice clientDevice, final InetAddress clientAddress)
    {
        manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                for(WifiP2pDevice foundDevice : peers.getDeviceList())
                {
                    //TODO Prevent duplicates.
                    if(foundDevice.deviceName.equals(clientDevice.deviceName))
                    {
                        SalutP2PDevice deviceRunningOurService = new SalutP2PDevice();
                        deviceRunningOurService.device = foundDevice;
                        deviceRunningOurService.actualDeviceAddress = clientAddress;
                        serviceDevices.add(deviceRunningOurService);
                        Log.d(TAG, "Device " + clientDevice.deviceName + " has been added to the " + serviceName + " service successfully.");

                    }
                }
            }
        });


    }

    private void startGroupOwnerServer()
    {

        AsyncJob.OnBackgroundJob serviceServer = new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                Log.d(TAG, "Creating a server thread.");
                try
                {
                    //Create a server socket and wait for client connections. This
                    //call blocks until a connection is accepted from a client.


                    clientSocket = serverSocket.accept();


                    //If this code is reached, a client has connected and transferred data.
                    Log.d(TAG, "A device has connected to the server, transferring data...");
                    InputStream inputStream = clientSocket.getInputStream();
                    SalutP2PDevice clientDevice = LoganSquare.parse(inputStream, SalutP2PDevice.class);
                    Log.d(TAG, "Connected device " + clientDevice.macAddress);
                    commitClientToService(clientDevice, clientSocket.getInetAddress());
                    serverSocket.close();
                    clientSocket.close();
                    disconnectFromDevice();
                }
                catch (IOException ex)
                {
                    Log.d(TAG, "An error occurred while executing a server thread.");
                    ex.printStackTrace();
                }
            }
        };

        AsyncJob.doInBackground(serviceServer);
    }

    private void registerClient(final InetSocketAddress groupOwnerAddress)
    {
        //TODO Create handshake hash using device name, mac address and some random secret key.
        AsyncJob.OnBackgroundJob clientServer = new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {

                try
                {
                    /**
                     * Create a client socket with the host,
                     * port, and timeout information.
                     */
                    Log.d(TAG, "Attempting to register client...");
                    Socket client = new Socket();
                    client.bind(null);
                    client.connect(groupOwnerAddress, serverPort);


                    //If this code is reached, we've connected to the server and will transfer data.
                    Log.d(TAG, "We're connected to the server, receiving data...");
                    OutputStream outputStream = client.getOutputStream();
                    LoganSquare.serialize(thisDevice, outputStream);
                    client.close();
                    disconnectFromDevice();
                    Log.d(TAG, "Device " + thisDevice.deviceName + " has been added to the " + serviceName + " service successfully.");
                    thisDevice.isRegistered = true;
                }
                catch (IOException ex)
                {
                    Log.d(TAG, "An error occurred while executing a server thread.");
                }
            }
        };

        AsyncJob.doInBackground(clientServer);
    }

    public void sendClientData(Object data)
    {
        //TODO Create handshake hash using device name, mac address and some random secret key.
        AsyncJob.OnBackgroundJob clientServer = new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {

                try
                {
                    /**
                     * Create a client socket with the host,
                     * port, and timeout information.
                     */
                    Log.d(TAG, "Sending client data...");
                    Socket client = new Socket();
                    client.bind(null);
                    client.connect(new InetSocketAddress(hostDevice.actualDeviceAddress, serverPort));


                    //If this code is reached, we've connected to the server and will transfer data.
                    Log.d(TAG, "We're connected to the server, receiving data...");
                    OutputStream outputStream = client.getOutputStream();
                    LoganSquare.serialize(thisDevice, outputStream);
                    client.close();
                    disconnectFromDevice();
                    Log.d(TAG, "Device " + thisDevice.deviceName + " has been added to the " + serviceName + " service successfully.");
                    thisDevice.isRegistered = true;
                }
                catch (IOException ex)
                {
                    Log.d(TAG, "An error occurred while executing a server thread.");
                }
            }
        };

        AsyncJob.doInBackground(clientServer);
    }

    public void connectToDevice(final WifiP2pDevice device, final SalutCallback onSuccess, final SalutCallback onFailure)
    {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        if(isHost)
        {
            //This device will become the WiFi P2P group owner.
            config.groupOwnerIntent = 15;
        }
        else
        {
            config.groupOwnerIntent = 0;
        }
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully connected to another device.");
                lastConnectedDevice = device;
                onSuccess.call();
            }

            @Override
            public void onFailure(int reason) {
                onFailure.call();
                Log.e(TAG, "Failed to connect to device. ");
            }
        });

    }
    public void disconnectFromDevice()
    {
        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if(group != null && group.isGroupOwner())
                {
                    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            isConnectedToAnotherDevice = false;
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.e(TAG, "Failed to disconnect from device. Reason: " + reason);
                        }
                    });
                }
            }
        });

//        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
//            @Override
//            public void onSuccess() {
//                isConnectedToAnotherDevice = false;
//            }
//
//            @Override
//            public void onFailure(int reason) {
//                Log.e(TAG, "Failed to disconnect from device. Reason: " + reason);
//            }
//        });
    }

    private void disconnectFromDeviceToChangeOwner()
    {
        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if(group != null && group.isGroupOwner())
                {
                    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            isConnectedToAnotherDevice = false;
                            connectToDevice(lastConnectedDevice, new SalutCallback() {
                                @Override
                                public void call() {
                                    Log.d(TAG, "Reestablished connection.");
                                }
                            }, new SalutCallback() {
                                @Override
                                public void call() {
                                    //Do nothing.
                                }
                            });
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.e(TAG, "Failed to disconnect from device. Reason: " + reason);
                        }
                    });
                }
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

        //Inject the listening port along with whatever else data that is going to be sent.
        this.serviceData.put("LISTEN_PORT", String.valueOf(serverPort));

        //Create a service info object will android will actually hand out to the clients.
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(instanceName, TTP , this.serviceData);

        //Register our service. The callbacks here just let us know if the service was registered correctly,
        //not necessarily whether or not we connected to a device.
        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully created " + SalutP2P.this.serviceName + " service running on port " + serverPort);
                serviceIsRunning = true;
            }

            @Override
            public void onFailure(int error) {
                Log.d(TAG, "Failed to create " + SalutP2P.this.serviceName + " : Error Code: " + error);
            }
        });
    }


    public void startNetworkService()
    {
        //In order to have a service that you create be seen, you must also actively look for other services. This is an Android bug.
        //For more information, read here. https://code.google.com/p/android/issues/detail?id=37425
        //We do not need to setup DNS responders.
        isHost = true;
        hostDevice = thisDevice;
        createService();
        discoverNetworkServices(deviceNotSupported);
        findRandomOpenPort();
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

                foundDevices.add(new SalutP2PDevice(device, record));
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

                foundDevices.add(new SalutP2PDevice(device, record));
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

                foundDevices.add(new SalutP2PDevice(device, record));
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

        if(!respondersAlreadySet)
        {
            setupDNSResponders();
        }

        discoverNetworkServices(deviceNotSupported);
        devicesNotFoundInTime(onDevicesNotFound, onDevicesFound, timeout);
    }

    public void stopNetworkService(final boolean disableWiFi)
    {
        isHost = false;
        hostDevice = null;
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