package com.peak.mixen;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import co.arcs.groove.thresher.GroovesharkException;
import co.arcs.groove.thresher.Song;



public class GrooveSharkRequests {
    //Nice helper functions for actual GrooveShark Async requests.

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

class querySongs extends AsyncTask<Void, Void, Integer> {
    String songName;
    ArrayList<Song> dupesRemoved = new ArrayList<>(10);
    public static final Integer REQUEST_SUCCESSFUL = 0;
    public static final Integer REQUEST_FAILED = -1;
    Integer requestStatus;

    public querySongs(String songName) {
        this.songName = songName;
    }

    //Try to search for songs asynchronously.
    @Override
    protected Integer doInBackground(Void... params) {

        try {
            List songs = Mixen.grooveSharkSession.searchSongs(songName);
            dupesRemoved.addAll(GrooveSharkRequests.removeDups(songs));
            return REQUEST_SUCCESSFUL;

        } catch (Exception ex) {
            Log.e(Mixen.TAG, "There was an error while attempting to retrieve the data.");
            ex.printStackTrace();
            return REQUEST_FAILED;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {

        SearchSongs.instance.postHandleSearchTask(dupesRemoved, result);

        super.onPostExecute(result);
    }
}


class DownloadAlbumArt extends AsyncTask<Void, Void, Bitmap>{
    ImageView imageView;
    View v;

    public DownloadAlbumArt(ImageView imageView, View v) {
        this.imageView = imageView;
        this.v = v;
    }

    protected Bitmap doInBackground(Void... params) {


        String coverArt = Mixen.COVER_ART_URL + MixenPlayerService.instance.currentSong.getCoverArtFilename();
        MixenPlayerService.instance.currentAlbumArtURL = coverArt;
        Log.d(Mixen.TAG, "Current album art was found at " + coverArt);

        Bitmap art = null;
        try {
            InputStream in = new java.net.URL(coverArt).openStream();
            art = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e(Mixen.TAG, "Failed to get album art. It will not be available for this song.");
            e.printStackTrace();
        }
        return art;
    }

    protected void onPostExecute(Bitmap result) {
        if(result != null)
        {
            imageView.setImageBitmap(result);
            MixenPlayerService.instance.currentAlbumArt = result;
            MixenBase.mixenPlayerFrag.generateAlbumArtPalette();
            MixenPlayerService.instance.updateAlbumArtCache();
        }

    }
}

class getTopSongs extends AsyncTask<Void, Void, List<Song>>
{
    List<Song> songs;


    @Override
    protected List<Song> doInBackground(Void... params) {
        try{
            return Mixen.grooveSharkSession.searchPopularSongs();
        }
        catch (Exception ex)
        {
            Log.e(Mixen.TAG, "There was a network error while attempting to retrieving the data.");
        }
    return null;
    }
}


class getStreamURLAsync extends AsyncTask<Void, Void, URL>
{
    Song song;
    //Get the actual stream URL for the song asynchronously.
    public getStreamURLAsync(Song song)
    {
        this.song = song;
    }

    @Override
    protected URL doInBackground(Void... params) {

        try {
            return Mixen.grooveSharkSession.getStreamUrl(song);
        } catch (Exception ex)
        {
            Log.e(Mixen.TAG, "There was a network error while attempting to retrieving the data.");
        }
        return null;

    }

    @Override
    protected void onPostExecute(URL url) {
        super.onPostExecute(url);
        MixenPlayerService.instance.currentStreamURL = url;
        MixenPlayerService.doAction(Mixen.currentContext, MixenPlayerService.preparePlayback);
    }
}



