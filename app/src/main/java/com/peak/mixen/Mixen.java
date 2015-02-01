package com.peak.mixen;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import co.arcs.groove.thresher.Song;

/**
 * Created by markrjr on 11/24/14.
 */
public class Mixen {

    public static final int NO_NETWORK = 0;
    public static final int GENERIC_NETWORK_ERROR = 1;
    public static final int SONG_NOT_FOUND = 2;
    public static final int GENERIC_STREAMING_ERROR = 3;
    public static final int MORE_INFO = 4;
    public static final int HELP = 5;
    public static final int ABOUT = 6;

    public static int currentSongProgress;

    public static int currentSongAsInt;

    public static int bufferTimes = 0;

    public static String currentAlbumArt;

    public static String currentArtistArt;

    public static final String TAG = "Mixen";

    public static boolean networkisReachableAsync;

    public static Song currentSong;

    public static Context currentContext;

    public static ArrayList<Song> queuedSongs;

    public static ArrayList<Song> proposedSongs;

    public static String username;



}

interface SimpleCallback
{
    void call();
}



class InternetState
{
    //Check if there is a connection to the internet either over mobile data or Wi-Fi.

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            try {
                URL url = new URL("http://www.google.com/");
                HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
                urlc.setRequestProperty("User-Agent", "development");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1000); // mTimeout is in seconds
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
}


class checkNetworkConnection extends AsyncTask<SimpleCallback, Void, Boolean>
{
    //A simple wrapper for the internet state class.
    SimpleCallback functionToCallWhenFinished;

    @Override
    protected Boolean doInBackground(SimpleCallback... params) {
        functionToCallWhenFinished = params[0];
        return InternetState.isConnected(Mixen.currentContext);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        //After execution of the async task to check the network, set the result out.
        Mixen.networkisReachableAsync = result;
        functionToCallWhenFinished.call();

    }
}


