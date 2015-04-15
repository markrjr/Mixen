package com.peak.mixen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.arasthel.asyncjob.AsyncJob;
import com.mikepenz.aboutlibraries.Libs;
import com.peak.salut.Salut;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;

/**
 * Created by markrjr on 11/24/14.
 */
public class Mixen {

    public static final int MIXEN_NOTIFY_CODE = 11;

    public static final int SUCCESSFULLY_HOSTING = 0;

    public static final String TAG = "Mixen";

    public static boolean isHost;

    public static int[] appColors;

    public static Salut network;

    public static final String COVER_ART_URL = "http://images.gs-cdn.net/static/albums/";

    public static final String MIXEN_PREF_FILE = "com.peak.mixen.preferences";

    public static SharedPreferences sharedPref;

    //Song and current session related data.

    public static Client grooveSharkSession;

    public static Context currentContext;

    public static String username;

    public static boolean debugFeaturesEnabled = false;

    public static boolean isConnected(Context context, int timeout) {
        ConnectivityManager cm = (ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            try {
                URL url = new URL("http://www.google.com/");
                HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
                urlc.setRequestProperty("User-Agent", "development");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(timeout); // mTimeout is in seconds
                urlc.connect();
                if (urlc.getResponseCode() == 200) {
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                Log.i("warning", "Error checking internet connection", e);
                return false;
            }
        }

        return false;

    }


    public static void showAbout(Activity activity)
    {
        new Libs.Builder()
                .withFields(R.string.class.getFields())
                .start(activity);
    }

    public static MaterialDialog showP2PNotSupported(Context context)
    {
        return new MaterialDialog.Builder(context)
                .title("Bummer ):")
                .content("Mixen isn't supported on your device.")
                .neutralText("Okay")
                .build();

    }


    public static boolean isTablet(Context context) {
        boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }
}


class checkNetworkConnection extends AsyncTask<Void, Void, Boolean>
{
    //A simple wrapper for the internet state class.

    @Override
    protected Boolean doInBackground(Void... params) {
        return Mixen.isConnected(Mixen.currentContext, 1000);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        //After execution of the async task to check the network, set the result out.

    }
}

//class pushMixenServiceInstance extends AsyncTask<Void, Void, Void>
//{
//    WifiP2pDevice pushDevice;
//
//    public pushMixenServiceInstance(WifiP2pDevice device)
//    {
//        pushDevice = device;
//    }
//
//    @Override
//    protected Void doInBackground(Void... params) {
//        try
//        {
//            Log.d(Mixen.TAG, "Attempting to send Mixen Service.");
//
//            //TODO Create a MixenServiceState object that is serializable.
//            Map<String, String> songData = new HashMap<>();
//            Map<String, ArrayList<Song>> songQueues = new HashMap<>();
//
//            songData.put("songName", MixenPlayerService.instance.currentSong.getArtistName());
//            songData.put("songArtist", MixenPlayerService.instance.currentSong.getArtistName());
//            songData.put("songDuration", "" + MixenPlayerService.instance.currentSong.getDuration());
//
//            songQueues.put("queued", MixenPlayerService.instance.queuedSongs);
//            songQueues.put("proposed", MixenPlayerService.instance.proposedSongs);
//
//            Socket client = new Socket();
//            client.bind(null);
//            client.connect((new InetSocketAddress(pushDevice.deviceAddress, Mixen.network.SERVER_PORT)));
//            OutputStream outputStream = client.getOutputStream();
//
//            outputStream.write(Pherialize.serialize(songData).getBytes());
//            outputStream.write(Pherialize.serialize(songQueues).getBytes());
//
//            outputStream.close();
//
//            Log.d(Mixen.TAG, "Sent data");
//
//
//        }
//        catch(Exception ex)
//        {
//            ex.printStackTrace();
//            Log.e(Mixen.TAG, "Failed to send data.");
//        }
//
//
//            return null;
//    }
//    @Override
//    protected void onPostExecute(Void aVoid) {
//        super.onPostExecute(aVoid);
//    }
//}

//class recieveMixenServiceInstance extends AsyncTask<Void, Void, Void>
//{
//    byte[] buf = new byte[2048];
//
//    @Override
//    protected Void doInBackground(Void... params) {
//        try
//        {
//            Log.d(Mixen.TAG, "Attempting to recieve Mixen Service.");
//
//            ServerSocket server = new ServerSocket(Mixen.network.SERVER_PORT);
//            Socket client = server.accept();
//
//            InputStream inputStream = client.getInputStream();
//
//            Log.d(Mixen.TAG, "" + inputStream.read(buf));
//
//            Log.d(Mixen.TAG, "Successfully received Mixen Service instance.");
//        }
//        catch(Exception ex)
//        {
//            Log.e(Mixen.TAG, "Failed to open a socket. One may have to be opened manually.");
//        }
//
//        return null;
//    }
//    @Override
//    protected void onPostExecute(Void aVoid) {
//        super.onPostExecute(aVoid);
//    }
//}

