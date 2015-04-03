package com.peak.mixen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.peak.salut.Salut;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import co.arcs.groove.thresher.Client;
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

    public static final int MIXEN_NOTIFY_CODE = 11;

    //Misc

    public static final String TAG = "Mixen";

    public static boolean isHost;

    public static int[] appColors;

    public static Salut network;

    public static boolean networkisReachableAsync;

    public static final String COVER_ART_URL = "http://images.gs-cdn.net/static/albums/";

    public static final String MIXEN_PREF_FILE = "com.peak.mixen.preferences";

    public static SharedPreferences sharedPref;

    //Song and current session related data.

    public static Client grooveSharkSession;

    public static Context currentContext;

    public static String username;


    public static Intent moreInfoDialog(Context context, int errorCode)
    {
        Intent provideErrorInfo = new Intent(context, MoreInfo.class); //Totally lazy, but really easy.
        provideErrorInfo.putExtra("START_REASON", errorCode);
        return (provideErrorInfo);
    }

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
        Mixen.networkisReachableAsync = result;

    }
}


