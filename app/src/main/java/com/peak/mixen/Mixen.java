package com.peak.mixen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import co.arcs.groove.thresher.GroovesharkException;
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

}




class queryPopSongs extends AsyncTask<Void, Void, Void>
{
    //Try to get the top twenty songs from GrooveShark asynchronously.

    @Override
    protected Void doInBackground(Void... params) {

        try {

            List songs = MixenPlayer.grooveSharkSession.searchPopularSongs().subList(0,20);
            SearchSongs.foundSongs = new ArrayList<Song>(songs.size()); //Convert to an ArrayList.
            SearchSongs.foundSongs.addAll(songs);
            return null;

        } catch (IOException IOError) {
            Log.e(Mixen.TAG, "IOError");
        } catch (GroovesharkException GSExcep) {
            Log.e(Mixen.TAG, "Grooveshark Exception");
        } catch (NullPointerException nullReturn)
        {
            Log.e(Mixen.TAG, "There was an unknown network error while attempting to retrieving the search results.");
        }

        return null;

    }

}

class querySongs extends AsyncTask<String, Void, Void>
{
    //Try to search for songs asynchronously.
    @Override
    protected Void doInBackground(String... params) {

        try {
            List songs = MixenPlayer.grooveSharkSession.searchSongs(params[0]);
            SearchSongs.foundSongs = new ArrayList<Song>(songs.size()); //Convert to an ArrayList.
            SearchSongs.foundSongs.addAll(songs);
            return null;

        } catch (IOException IOError) {
            Log.e(Mixen.TAG, "IOError");
        } catch (GroovesharkException GSExcep) {
            Log.e(Mixen.TAG, "Grooveshark Exception");
        } catch (NullPointerException nullReturn)
        {
            Log.e(Mixen.TAG, "There was a network error while attempting to retrieving the data.");
        }

        return null;

    }


}


class DownloadAlbumArt extends AsyncTask<String, Void, Bitmap> {
    ImageView imageView;

    public DownloadAlbumArt(ImageView imageView) {
        this.imageView = imageView;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap art = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            art = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return art;
    }

    protected void onPostExecute(Bitmap result) {
        imageView.setImageBitmap(result);
    }
}


class DownloadArtistArt extends AsyncTask<String, Void, BitmapDrawable> {
    RelativeLayout relativeLayout;

    public DownloadArtistArt(RelativeLayout relativeLayout) {
        this.relativeLayout = relativeLayout;
    }

    protected BitmapDrawable doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap art = null;
        BitmapDrawable background = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            art = BitmapFactory.decodeStream(in);
            background = new BitmapDrawable(art);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return background;
    }

    protected void onPostExecute(BitmapDrawable result) {
        relativeLayout.setBackground(result);
    }
}


class getStreamURLAsync extends AsyncTask<Song, Void, URL>
{
    //Get the actual stream URL for the song asynchronously.

    @Override
    protected URL doInBackground(Song... params) {

        try {
            URL streamURL = MixenPlayer.grooveSharkSession.getStreamUrl(params[0]);
            return streamURL;

        } catch (IOException IOError) {
            Log.e(Mixen.TAG, "IOError");
        } catch (GroovesharkException GSExcep) {
            Log.e(Mixen.TAG, "Grooveshark Exception");
        } catch (NullPointerException nullReturn)
        {
            Log.e(Mixen.TAG, "There was a network error while attempting to retrieving the data.");
        }

        return null;

    }

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
                urlc.setRequestProperty("User-Agent", "test");
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


class checkNetworkConnection extends AsyncTask<Void, Void, Boolean>
{
    //A simple wrapper for the internet state class.

    @Override
    protected Boolean doInBackground(Void... params) {

        return InternetState.isConnected(Mixen.currentContext);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        //After execution of the async task to check the network, set the result out.
        Mixen.networkisReachableAsync = result;
    }
}

