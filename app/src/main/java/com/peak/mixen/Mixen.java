package com.peak.mixen;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.aboutlibraries.Libs;
import com.parse.ParseObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;

/**
 * Created by markrjr on 11/24/14.
 */
public class Mixen {

    public static final int MIXEN_SERVICE_PORT = 35700;

    public static final int MIXEN_NOTIFY_CODE = 11;

    public static final String TAG = "Mixen";

    public static boolean isHost;

    public static int[] appColors;

    public static final String COVER_ART_URL = "http://images.gs-cdn.net/static/albums/";

    public static final String MIXEN_PREF_FILE = "com.peak.mixen.preferences";

    public static SharedPreferences sharedPref;

    //Song and current session related data.

    public static ParseObject thisUser;

    public static SpotifyApi spotifyAPI;

    public static SpotifyService spotify;

    public static String spotifyToken;

    public static long spotifyTokenExpiry;

    private static final String CLIENT_ID = "fb5c429de70a4aa184ea97dbaa5e8b98";

    private static final String REDIRECT_URI = "mixen://spotify-auth-callback";

    private static final String SDK_SECRET = "tzQx7qKVRn-vUjQyesGeRA";

    private static final String PARSE_SECRET = "IBGVNAmzJWvgXxvmSe2Uv6OT2ScabU3a4XudCiOC";

    private static final String PARSE_KEY = "q3tVE8UUfuIJuc0b4XDRDv0ebevcMGWbvkF6gF2J";

    public static Context currentContext;

    public static String username;

    public static boolean hasSeenTutorial;

    public static boolean amoledMode;

    public static String getClientId() {
        return CLIENT_ID;
    }

    public static String getRedirectUri() {
        return REDIRECT_URI;
    }

    public static String getParseSecret() {
        return PARSE_SECRET;
    }

    public static String getParseKey() {
        return PARSE_KEY;
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
                return urlc.getResponseCode() == 200;
            } catch (IOException e) {
                Log.i("warning", "Error checking internet connection", e);
                return false;
            }
        }

        return false;
    }

    public static boolean partyCreated()
    {
        return(thisUser != null && thisUser.getBoolean("partyCreated"));
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
