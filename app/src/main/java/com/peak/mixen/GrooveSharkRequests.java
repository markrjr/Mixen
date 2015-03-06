package com.peak.mixen;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import co.arcs.groove.thresher.GroovesharkException;
import co.arcs.groove.thresher.Song;



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
            List songs = Mixen.grooveSharkSession.searchSongs(songName);
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


class DownloadAlbumArt extends AsyncTask<Void, Void, Bitmap> implements Palette.PaletteAsyncListener{
    ImageView imageView;
    View v;

    public DownloadAlbumArt(ImageView imageView, View v) {
        this.imageView = imageView;
        this.v = v;
    }

    protected Bitmap doInBackground(Void... params) {


        String coverArt = Mixen.COVER_ART_URL + MixenPlayerService.currentSong.getCoverArtFilename();
        MixenPlayerService.currentAlbumArtURL = coverArt;

        Bitmap art = null;
        try {
            InputStream in = new java.net.URL(coverArt).openStream();
            art = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e(Mixen.TAG, "Failed to get album art. It will not be available for this song.");
            //Log.e(Mixen.TAG, e.getMessage());
            //e.printStackTrace();
        }
        return art;
    }

    protected void onPostExecute(Bitmap result) {
        if(result != null)
        {
            imageView.setImageBitmap(result);
            MixenPlayerService.currentAlbumArt = result;
            //Palette.generate(result);
        }

    }

    @Override
    public void onGenerated(Palette palette) {

        //TODO Clean this up for the love of God, please.
        Log.d(Mixen.TAG, "Generated colors.");

        int darkVibrant = palette.getDarkVibrantColor(R.color.Dark_Primary);
        int vibrant = palette.getVibrantColor(R.color.Accent_Color);

        TextView titleTV = (TextView) v.findViewById(R.id.titleTV);
        TextView artistTV = (TextView) v.findViewById(R.id.artistTV);
        TextView upNextTV = (TextView) v.findViewById(R.id.upNextTV);

        ImageButton playPauseButton = (ImageButton) v.findViewById(R.id.playPauseButton);
        ImageButton fastForwardIB = (ImageButton) v.findViewById(R.id.fastForwardIB);
        ImageButton rewindIB = (ImageButton) v.findViewById(R.id.rewindIB);
        ProgressBar bufferPB = (ProgressBar) v.findViewById(R.id.bufferingPB);

        RelativeLayout playerControls = (RelativeLayout) v.findViewById(R.id.playerControls);

        titleTV.setBackgroundColor(vibrant);
        artistTV.setBackgroundColor(vibrant);
        upNextTV.setBackgroundColor(vibrant);

        playPauseButton.setBackgroundColor(darkVibrant);
        fastForwardIB.setBackgroundColor(darkVibrant);
        rewindIB.setBackgroundColor(darkVibrant);
        bufferPB.setBackgroundColor(darkVibrant);
        playerControls.setBackgroundColor(darkVibrant);



    }
}


class getStreamURLAsync extends AsyncTask<Song, Void, URL>
{
    //Get the actual stream URL for the song asynchronously.

    @Override
    protected URL doInBackground(Song... params) {

        try {
            URL streamURL = Mixen.grooveSharkSession.getStreamUrl(params[0]);
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
        super.onPostExecute(url);
        MixenPlayerService.doAction(Mixen.currentContext, MixenPlayerService.setup);
    }
}



