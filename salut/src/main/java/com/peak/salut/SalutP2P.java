package com.peak.salut;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
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
import android.support.annotation.Nullable;
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

    protected static final String TAG = "Salut";
    private static final int SALUT_SERVER_PORT = 37500;
    private static final int MAX_CLIENT_CONNECTIONS = 5;
    private static final int MAX_SERVER_CONNECTIONS = 25;
    private String TTP = "._tcp";
    protected static int connectionTimeout = 2000;
    private Activity currentActivity;

    private static WifiManager wifiManager;
    private boolean respondersAlreadySet = false;
    private boolean firstDeviceAlreadyFound = false;
    private SalutCallback deviceNotSupported;
    private SalutCallback onRegistered;
    private SalutCallback onRegistrationFail;

    //Service Objects
    public boolean serviceIsRunning = false;
    private boolean isRunningAsHost = false;
    public SalutDevice thisDevice;
    private SalutDevice registeredHost;


    //WiFi P2P Objects
    private WifiP2pServiceInfo serviceInfo;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    public IntentFilter intentFilter = new IntentFilter();
    public BroadcastReceiver receiver = null;

    //Connection Objects
    public boolean isConnectedToAnotherDevice = false;
    private ServerSocket serverSocket;
    private Socket clientSocket;

    //Found Service Objects
    private SalutDevice lastConnectedDevice;
    public ArrayList<SalutDevice> foundDevices;
    public ArrayList<SalutDevice> registeredClients;



    public SalutP2P(Activity activity, Map<String, String> serviceData, SalutCallback deviceNotSupported)
    {
        WifiManager wifiMan = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMan.getConnectionInfo();

        this.currentActivity = activity;
        thisDevice = new SalutDevice();
        thisDevice.serviceName = serviceData.get("SERVICE_NAME");
        thisDevice.readableName = serviceData.get("INSTANCE_NAME");
        thisDevice.instanceName = serviceData.get("INSTANCE_NAME") + "-" + wifiInfo.getMacAddress().hashCode();
        thisDevice.macAddress = wifiInfo.getMacAddress();
        thisDevice.TTP = thisDevice.serviceName + TTP;
        thisDevice.servicePort = Integer.valueOf(serviceData.get("SERVICE_PORT"));
        thisDevice.txtRecord = serviceData;
        this.deviceNotSupported = deviceNotSupported;
        TTP = serviceData.get("SERVICE_NAME") + TTP;

        foundDevices = new ArrayList<>();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(activity, activity.getMainLooper(), null);

        receiver = new SalutBroadcastReciever(this, manager, channel);
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    private void findRandomOpenPort()
    {
        try {
            serverSocket = new ServerSocket(SALUT_SERVER_PORT, MAX_SERVER_CONNECTIONS);
            serverSocket.setReuseAddress(true);
        }
        catch(IOException ex)
        {
            Log.e(TAG, "Failed to use standard port, another will be used instead.");
        }
    }


    public ArrayList<String> getReadableFoundNames()
    {
        ArrayList<String> foundHostNames = new ArrayList<>(foundDevices.size());
        for(SalutDevice device : foundDevices)
        {
            foundHostNames.add(device.readableName);
        }

        return foundHostNames;
    }

    public ArrayList<String> getReadableRegisteredNames()
    {
        ArrayList<String> registeredNames = new ArrayList<>(registeredClients.size());
        for(SalutDevice device : registeredClients)
        {
            registeredNames.add(device.readableName);
        }

        return registeredNames;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        /* This method is automatically called when we connect to a device.
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by the RecieveClientIP AsyncTask;
         */

        if(isRunningAsHost && !info.isGroupOwner)
        {
            Log.d(TAG, "Connected, but we're not the group owner, reestablishing connection.");
            disconnectFromDeviceToChangeOwner();
        }
        else if(!isRunningAsHost && info.isGroupOwner)
        {
            Log.d(TAG, "Connected as group owner, but we're not the host, disconnecting...");
            disconnectFromDevice();
        }
        else
        {
            if (info.isGroupOwner) {

                Log.d(TAG, "Connected as group owner.");
                try {
                    startHostServer();
                } catch (Exception ex) {
                    Log.e(TAG,"Failed to create a server thread - " + ex.getMessage());
                }

            }
            else {

                if(!thisDevice.isRegistered)
                {
                    Log.d(TAG, "Connected as peer.");
                    //hostDevice.actualDeviceAddress = info.groupOwnerAddress;
                    InetSocketAddress groupOwnerAddress = new InetSocketAddress(info.groupOwnerAddress, SALUT_SERVER_PORT);
                    startRegistrationForClient(groupOwnerAddress);
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


    private void startHostServer()
    {

        AsyncJob.OnBackgroundJob serviceServer = new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                Log.d(TAG, "Creating a server thread.");
                try
                {
                    //Create a server socket and wait for client connections. This
                    //call blocks until a connection is accepted from a client.

                    while(isRunningAsHost)
                    {
                        clientSocket = serverSocket.accept();

                        //If this code is reached, a client has connected and transferred data.
                        Log.d(TAG, "A device has connected to the server, transferring data...");
                        InputStream inputStream = clientSocket.getInputStream();
                        SalutDevice clientDevice = LoganSquare.parse(inputStream, SalutDevice.class);
                        clientDevice.deviceAddress = clientSocket.getInetAddress();
                        if(!clientDevice.isRegistered)
                        {
                            Log.d(TAG, "Registered device and user: " + clientDevice.instanceName);
                            registeredClients.add(clientDevice);
                        }
                        else
                        {
                            Log.d(TAG, "Unregistering device and user: " + clientDevice.instanceName);
                            clientDevice.deviceAddress = clientSocket.getInetAddress();
                            registeredClients.remove(clientDevice);
                        }
                    }

                }
                catch (Exception ex)
                {
                    Log.d(TAG, "An error occurred while executing a server thread.");
                    ex.printStackTrace();
                }
            }
        };

        AsyncJob.doInBackground(serviceServer);
    }

    private void startRegistrationForClient(final InetSocketAddress hostDeviceAddress)
    {
        AsyncJob.OnBackgroundJob clientServer = new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {

                try
                {
                    /**
                     * Create a client socket with the host,
                     * port, and timeout information.
                     */
                    Log.d(TAG, "Attempting to register this client with the server...");
                    Socket client = new Socket();
                    client.bind(null);
                    client.connect(hostDeviceAddress);


                    //If this code is reached, we've connected to the server and will transfer data.
                    Log.d(TAG, "We're connected to the server, receiving data...");
                    OutputStream outputStream = client.getOutputStream();
                    LoganSquare.serialize(thisDevice, outputStream);
                    //TODO Set registered host.
                    //registeredHost = hostDevice;
                    //Log.d(TAG, hostDevice.deviceName.toString());
                    client.close();
                    Log.d(TAG, "This service has been successfully registered with the host.");
                    thisDevice.isRegistered = true;
                    currentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(onRegistered != null)
                                onRegistered.call();
                        }
                    });
                }
                catch (IOException ex)
                {
                    Log.d(TAG, "An error occurred while attempting to register.");
                    currentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (onRegistrationFail != null)
                                onRegistrationFail.call();
                        }
                    });
                    ex.printStackTrace();
                }
            }
        };

        AsyncJob.doInBackground(clientServer);
    }

    public void sendDataToClient(final SalutDevice device, final Object data)
    {
        AsyncJob.OnBackgroundJob connectToDevice = new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                try
                {
                    //Create a server socket and wait for client connections. This
                    //call blocks until a connection is accepted from a client.


                    Socket socket = new Socket();
                    socket.bind(null);
                    socket.connect(new InetSocketAddress(device.deviceAddress, device.servicePort));


                    //If this code is reached, a client has connected and transferred data.
                    Log.d(TAG, "Connected, transferring data...");
                    OutputStream outputStream = socket.getOutputStream();
                    LoganSquare.serialize(data, outputStream);
                    socket.close();
                    Log.d(TAG, "Successfully sent data.");
                }
                catch (IOException ex)
                {
                    Log.d(TAG, "An error occurred while sending data to a device.");
                    ex.printStackTrace();
                }
            }
        };

        AsyncJob.doInBackground(connectToDevice);
    }

    public void connectToDevice(final SalutDevice device, final SalutCallback onSuccess, final SalutCallback onFailure)
    {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress.toString();
        if(isRunningAsHost)
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

        Log.d(TAG, "Starting " + thisDevice.serviceName + " Transport Protocol " + TTP);

        //Inject the listening port along with whatever else data that is going to be sent.
        thisDevice.txtRecord.put("LISTEN_PORT", String.valueOf(thisDevice.servicePort));

        //Create a service info object will android will actually hand out to the clients.
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(thisDevice.instanceName, TTP , thisDevice.txtRecord);

        //Register our service. The callbacks here just let us know if the service was registered correctly,
        //not necessarily whether or not we connected to a device.
        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully created " + thisDevice.serviceName + " service running on port " + thisDevice.servicePort);
                serviceIsRunning = true;
                isRunningAsHost = true;
            }

            @Override
            public void onFailure(int error) {
                Log.d(TAG, "Failed to create " + thisDevice.serviceName + " : Error Code: " + error);
            }
        });
    }


    public void startNetworkService()
    {
        //In order to have a service that you create be seen, you must also actively look for other services. This is an Android bug.
        //For more information, read here. https://code.google.com/p/android/issues/detail?id=37425
        //We do not need to setup DNS responders.
        registeredClients = new ArrayList<>();
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

                foundDevices.add(new SalutDevice(device, record));
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

                foundDevices.add(new SalutDevice(device, record));
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

                foundDevices.add(new SalutDevice(device, record));
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
                Log.d(TAG, "Service discovery has failed.");
                if (arg0 == WifiP2pManager.P2P_UNSUPPORTED)
                    deviceNotSupported.call();
            }
        });

    }

    public void connectToHostDevice(@Nullable SalutDevice hostDevice, SalutCallback onRegistered, SalutCallback onRegistrationFail)
    {
        SalutDevice hostToConnectTo;

        if(hostDevice != null)
        {
            hostToConnectTo = hostDevice;

        }
        else
        {
            hostToConnectTo = registeredHost;
        }

        this.onRegistered = onRegistered;
        this.onRegistrationFail = onRegistrationFail;

        connectToDevice(hostToConnectTo, onRegistered, onRegistrationFail);

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
        isRunningAsHost = false;
        registeredHost = null;
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
                        disableWiFi(currentActivity); //To give time for the requests to be disposed.
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