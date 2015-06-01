package com.peak.salut;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.os.Handler;

import com.arasthel.asyncjob.AsyncJob;
import com.bluelinelabs.logansquare.LoganSquare;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.peak.salut.Callbacks.SalutServiceCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Salut{

    protected static final String TAG = "Salut";
    private static final int SALUT_SERVER_PORT = 37500;
    private static final int MAX_CLIENT_CONNECTIONS = 5;
    private static final int MAX_SERVER_CONNECTIONS = 25;
    private Activity currentActivity;

    //Service Objects
    public SalutDevice thisService;
    public ArrayList<SalutDevice> registeredClients;
    public ArrayList<SalutDevice> foundHostServices;
    public ArrayList<Object> newDataForClients;
    private boolean isRunningAsHost = false;
    private boolean foundDeviceInTime = false;
    private boolean timeoutDiscoveryUsed;


    //Connection Objects
    private ServerSocket serverSocket;
    private Socket clientSocket;

    //NSD Objects
    private NsdManager nsdManager;
    private NsdManager.RegistrationListener registrationListener;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.ResolveListener resolveListener;

    //Found Service Objects;
    private SalutDevice registeredHost;
    private SalutServiceCallback onServiceFound;
    private SalutCallback onRegistered;
    private SalutCallback onRegistrationFail;



    public Salut(Activity activity, String serviceName, String instanceName, int servicePort)
    {
        this.currentActivity = activity;
        this.nsdManager = (NsdManager) currentActivity.getSystemService(Context.NSD_SERVICE);
        WifiManager wifiMan = (WifiManager) currentActivity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMan.getConnectionInfo();

        setupDNSListeners();

        thisService = new SalutDevice();
        thisService.serviceName = serviceName;
        thisService.macAddress = wifiInfo.getMacAddress();
        thisService.readableName = instanceName;
        thisService.instanceName = instanceName + "-" + thisService.macAddress.hashCode();
        thisService.TTP = thisService.serviceName + thisService.TTP;
        thisService.servicePort = servicePort;
    }

    public static boolean isWiFiEnabled(Context context)
    {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
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
        ArrayList<String> foundHostNames = new ArrayList<>(foundHostServices.size());
        for(SalutDevice device : foundHostServices)
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

    public void discoverNetworkServices(@Nullable SalutServiceCallback onServiceFound)
    {
        timeoutDiscoveryUsed = false;
        this.onServiceFound = onServiceFound;
        foundHostServices = new ArrayList<>();
        nsdManager.discoverServices(thisService.TTP, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void discoverNetworkServicesWithTimeOut(@Nullable final SalutCallback onDevicesFound, final SalutCallback onDevicesNotFound, int timeout)
    {
        timeoutDiscoveryUsed = true;
        foundHostServices = new ArrayList<>();
        nsdManager.discoverServices(thisService.TTP, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (!foundDeviceInTime) {
                    currentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onDevicesNotFound.call();
                        }
                    });
                } else {
                    if (onDevicesFound != null) {
                        currentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onDevicesFound.call();
                            }
                        });
                    }
                    foundDeviceInTime = false;
                }
                stopNetworkServiceDiscovery();
            }
        }, timeout);
    }


    public void stopNetworkServiceDiscovery()
    {
        timeoutDiscoveryUsed = false;
        nsdManager.stopServiceDiscovery(discoveryListener);
    }

    public void startNetworkService(){
        registeredClients = new ArrayList<>();
        newDataForClients = new ArrayList<>();
        Log.d(TAG, "Starting network service...");
        findRandomOpenPort();
        NsdServiceInfo serviceInfo = thisService.getAsServiceInfo();
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    public void stopNetworkService() {
        try
        {
            nsdManager.unregisterService(registrationListener);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void startListeningForData(final Class<?> classType, final SalutDataCallback onDataRecieved)
    {
        AsyncJob.OnBackgroundJob serviceServer = new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                Log.d(TAG, "Listening for data on port: " + thisService.servicePort);
                    try
                    {
                        //Create a server socket and wait for client connections. This
                        //call blocks until a connection is accepted from a client.
                        ServerSocket listenerServiceSocket = new ServerSocket(thisService.servicePort, MAX_CLIENT_CONNECTIONS);

                        while(isRunningAsHost) {

                            Socket listeningSocket = listenerServiceSocket.accept();

                            //If this code is reached, a client has connected and transferred data.
                            Log.d(TAG, "A device is sending data...");
                            InputStream inputStream = listeningSocket.getInputStream();
                            final Object data = LoganSquare.parse(inputStream, classType);
                            inputStream.close();
                            currentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    onDataRecieved.call(data);
                                }
                            });
                            Log.d(TAG, "Successfully recieved data.");
                        }

                        listenerServiceSocket.close();
                        //listeningSocket.close();
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
                    //socket.connect(new InetSocketAddress(device.serviceAddress, device.servicePort));


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

    public void sendDataToClients(Object data)
    {

        if(registeredClients.isEmpty())
        {
            Log.d(TAG, "There are no registered clients.");
        }
        else
        {
            for(SalutDevice client : registeredClients)
            {
                sendDataToClient(client, data);
            }
        }


    }
    private void updateNewClient(SalutDevice newlyRegisteredDevice)
    {
        if(!newDataForClients.isEmpty())
        {
            for(Object data : newDataForClients)
            {
                sendDataToClient(newlyRegisteredDevice, data);
            }
        }
    }


    public void connectToHostService(@Nullable SalutDevice hostDevice, SalutCallback onRegistered, SalutCallback onRegistrationFail)
    {
        NsdServiceInfo hostToConnectTo;

        if(hostDevice != null)
        {
            hostToConnectTo = hostDevice.getAsServiceInfo();

        }
        else
        {
            hostToConnectTo = registeredHost.getAsServiceInfo();
        }

        this.onRegistered = onRegistered;
        this.onRegistrationFail = onRegistrationFail;

        nsdManager.resolveService(hostToConnectTo, resolveListener);
        Log.d(TAG, "Attempting to resolve service...");
    }

    private void startHostServer()
    {

        AsyncJob.OnBackgroundJob serviceServer = new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
//                Log.d(TAG, "Creating a server thread.");
//                    try
//                    {
//                        //Create a server socket and wait for client connections. This
//                        //call blocks until a connection is accepted from a client.
//
//                        while(isRunningAsHost)
//                        {
//                            clientSocket = serverSocket.accept();
//
//                            //If this code is reached, a client has connected and transferred data.
//                            Log.d(TAG, "A device has connected to the server, transferring data...");
//                            InputStream inputStream = clientSocket.getInputStream();
//                            SalutDevice clientDevice = LoganSquare.parse(inputStream, SalutDevice.class);
//                            clientDevice.serviceAddress = clientSocket.getInetAddress();
//                            if(!clientDevice.isRegistered)
//                            {
//                                Log.d(TAG, "Registered device and user: " + clientDevice.instanceName);
//                                registeredClients.add(clientDevice);
//                                updateNewClient(clientDevice);
//                            }
//                            else
//                            {
//                                Log.d(TAG, "Unregistering device and user: " + clientDevice.instanceName);
//                                clientDevice.serviceAddress = clientSocket.getInetAddress();
//                                registeredClients.remove(clientDevice);
//                            }
//                        }
//
//                    }
//                    catch (Exception ex)
//                    {
//                        Log.d(TAG, "An error occurred while executing a server thread.");
//                        ex.printStackTrace();
//                    }
                }
        };

        AsyncJob.doInBackground(serviceServer);
    }

    private void startRegistrationForClient(final NsdServiceInfo hostService)
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
                    client.connect(new InetSocketAddress(hostService.getHost(), SALUT_SERVER_PORT));


                    //If this code is reached, we've connected to the server and will transfer data.
                    Log.d(TAG, "We're connected to the server, receiving data...");
                    OutputStream outputStream = client.getOutputStream();
                    LoganSquare.serialize(thisService, outputStream);
                    registeredHost = new SalutDevice(hostService);
                    Log.d(TAG, registeredHost.toString());
                    client.close();
                    Log.d(TAG, "This service has been successfully registered with the host.");
                    thisService.isRegistered = true;
                    currentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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
                            onRegistrationFail.call();
                        }
                    });
                    ex.printStackTrace();
                }
            }
        };

        AsyncJob.doInBackground(clientServer);
    }

    public void unregisterClient()
    {
        AsyncJob.OnBackgroundJob unregister = new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {

                try
                {
                    /**
                     * Create a client socket with the host,
                     * port, and timeout information.
                     */
                    Log.d(TAG, "Attempting to unregister client...");
                    Socket client = new Socket();
                    client.bind(null);
                    //client.connect(new InetSocketAddress(registeredHost.serviceAddress, SALUT_SERVER_PORT));


                    //If this code is reached, we've connected to the server and will transfer data.
                    Log.d(TAG, "We're connected to the server, receiving data...");
                    OutputStream outputStream = client.getOutputStream();
                    LoganSquare.serialize(thisService, outputStream);
                    client.close();
                    Log.d(TAG, "This service has been successfully unregistered with the host.");
                    registeredHost = null;
                    thisService.isRegistered = false;
                }
                catch (IOException ex)
                {
                    Log.d(TAG, "An error occurred while attempting to unregister.");
                    ex.printStackTrace();
                }
            }
        };

        AsyncJob.doInBackground(unregister);
    }



    private void setupDNSListeners()
    {
        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Failed to register network service. Reason: " + errorCode);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Failed to shutdown network service. Reason: " + errorCode);
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Successfully registered network service, now starting client registration service.");
                isRunningAsHost = true;
                startHostServer();
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Successfully shutdown network service.");
                isRunningAsHost = false;
                try
                {
                    serverSocket.close();
                    clientSocket.close();
                    //TODO Notify all clients of disconnect.
                }
                catch(Exception ex)
                {
                    Log.d(TAG, "Attempted to close host sockets that may have not been initialized.");
                }

            }

        };

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Failed to start service discovery. Reason: " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Failed to stop service discovery. Reason: " + errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Successfully started discovery for service: " + serviceType);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Successfully stopped discovery for service: " + serviceType);
            }

            @Override
            public void onServiceFound(final NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Found service " + serviceInfo.getServiceType());
                if (!serviceInfo.getServiceType().equals(thisService.TTP))
                {
                    Log.d(TAG, "Unknown Service Type: " + serviceInfo.getServiceName() + " " + serviceInfo.getServiceType());
                }
                else if (serviceInfo.getServiceName().equals(thisService.instanceName))
                {
                    Log.d(TAG, "Ignoring discovery of our device.");
                }
                else
                {
                    if(timeoutDiscoveryUsed)
                    {
                        foundDeviceInTime = true;
                        foundHostServices.add(new SalutDevice(serviceInfo));
                    }
                    else
                    {
                        currentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onServiceFound.call(new SalutDevice(serviceInfo));
                            }
                        });
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service " + serviceInfo.getServiceType() + " is no longer available. ");
            }
        };

        resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Failed to resolve service " + serviceInfo.getServiceName() + " Reason: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                if(!thisService.isRegistered)
                {
                    startRegistrationForClient(serviceInfo);
                }
            }
        };
    }
}
