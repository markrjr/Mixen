package com.peak.mixen;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.Preference;
import android.support.v7.graphics.Palette;
import android.util.Log;

import com.nispok.snackbar.SnackbarManager;
import com.peak.salut.Salut;
import com.squareup.okhttp.internal.Util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;

import static android.support.v4.app.ActivityCompat.startActivity;

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

    public static AudioManager audioManager;

    public static final String COVER_ART_URL = "http://images.gs-cdn.net/static/albums/";

    public static final String MIXEN_PREF_FILE = "com.peak.mixen.preferences";

    public static SharedPreferences sharedPref;

    //Song and current session related data.


    public static Client grooveSharkSession;


    public static Context currentContext;


    public static String username;


    public static Intent prepareErrorHandlerActivity(Activity currentActivity)
    {
        Intent provideErrorInfo = new Intent(currentActivity, MoreInfo.class); //Totally lazy, but really easy.
        provideErrorInfo.putExtra("START_REASON", Mixen.GENERIC_STREAMING_ERROR);
        return (provideErrorInfo);
    }

    public static void setupAudioManager()
    {
        Mixen.audioManager = (AudioManager)currentContext.getSystemService(Context.AUDIO_SERVICE);
    }
    public static boolean requestAudioFocus() {
        // Request audio focus for playback
        int result = audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
                                                        @Override
                                                        public void onAudioFocusChange(int audioChange) {

                                                            if(audioChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
                                                            {
                                                                if(MixenPlayerService.playerIsPlaying())
                                                                {
                                                                    Mixen.currentContext.startActivity(MixenPlayerService.pause);
                                                                }

                                                            }
                                                            else if(audioChange == AudioManager.AUDIOFOCUS_GAIN)
                                                            {
                                                                if(!MixenPlayerService.playerIsPlaying() && MixenPlayerFrag.playerHasTrack())
                                                                {
                                                                    Mixen.currentContext.startActivity(MixenPlayerService.play);
                                                                }
                                                            }
                                                            else if(audioChange == AudioManager.AUDIOFOCUS_LOSS);
                                                            {
                                                                if(MixenPlayerService.playerIsPlaying())
                                                                {
                                                                    Mixen.currentContext.startActivity(MixenPlayerService.pause);
                                                                    audioManager.abandonAudioFocus(this);
                                                                }
                                                            }
                                                        }
                                                    },
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
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


