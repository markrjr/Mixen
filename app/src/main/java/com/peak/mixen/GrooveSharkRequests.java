package com.peak.mixen;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import co.arcs.groove.thresher.GroovesharkException;
import co.arcs.groove.thresher.Song;

/**
 * Created by markrjr on 1/28/15.
 */


public class GrooveSharkRequests {
    //Nice helper functions for actual GrooveShark Async requests.

    public static int searchResultCode;

    public static void findSong(String songName, SimpleCallback functionToCallOnCompletion)
    {
        new querySongs(songName, functionToCallOnCompletion).execute();
    }

    public static List<Song> removeDups(List<Song> songs) {

        List<Song> listOfSongs = songs;

        for (int i = 1; i < listOfSongs.size(); i++) {
            if (listOfSongs.get(i).getCoverArtFilename() == null || listOfSongs.get(i).getCoverArtFilename().equals("")) {
                listOfSongs.remove(i);
            }
        }

        return listOfSongs;
    }


}

class querySongs extends AsyncTask<String, Void, Void> {
    String songName;
    SimpleCallback functionToCallOnCompletion;

    public querySongs(String songName, SimpleCallback function) {
        this.songName = songName;
        this.functionToCallOnCompletion = function;
    }


    //Try to search for songs asynchronously.
    @Override
    protected Void doInBackground(String... params) {

        try {
            List songs = MixenPlayer.grooveSharkSession.searchSongs(songName);
            SearchSongs.foundSongs = new ArrayList<Song>(10);
            SearchSongs.foundSongs.addAll(GrooveSharkRequests.removeDups(songs));

        } catch (IOException IOError) {
            Log.e(Mixen.TAG, "IOError");
            GrooveSharkRequests.searchResultCode = Mixen.GENERIC_NETWORK_ERROR;
            return null;
        } catch (GroovesharkException GSExcep) {
            Log.e(Mixen.TAG, "Grooveshark Exception");
            GrooveSharkRequests.searchResultCode = Mixen.SONG_NOT_FOUND;
            return null;
        } catch (NullPointerException nullReturn) {
            Log.e(Mixen.TAG, "There was a network error while attempting to retrieving the data.");
            GrooveSharkRequests.searchResultCode = Mixen.GENERIC_NETWORK_ERROR;
            return null;
        }

        GrooveSharkRequests.searchResultCode = 99; //Success
        return null;

    }

    @Override
    protected void onPostExecute(Void result) {

        functionToCallOnCompletion.call();

        super.onPostExecute(result);
    }
}


class DownloadAlbumArt extends AsyncTask<Void, Void, Bitmap> {
    ImageView imageView;

    public DownloadAlbumArt(ImageView imageView) {
        this.imageView = imageView;
    }

    protected Bitmap doInBackground(Void... params) {


        String coverArt = Mixen.COVER_ART_URL + Mixen.currentSong.getCoverArtFilename();

        Bitmap art = null;
        try {
            InputStream in = new java.net.URL(coverArt).openStream();
            art = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e(Mixen.TAG, e.getMessage());
            e.printStackTrace();
        }
        return art;
    }

    protected void onPostExecute(Bitmap result) {
        imageView.setImageBitmap(result);
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

    @Override
    protected void onPostExecute(URL url) {

        MixenPlayer.beginPlayback();

        super.onPostExecute(url);
    }
}


