package com.peak.mixen;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import co.arcs.groove.thresher.GroovesharkException;
import co.arcs.groove.thresher.Song;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.AlbumsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by markrjr on 1/28/15.
 */


public class GrooveSharkRequests {



    public static void findSong(String songName, SimpleCallback functionToCallOnCompletion)
    {
        new querySongs(songName, functionToCallOnCompletion).execute();
    }

    public static void findAlbumUsingSpotify(SpotifyService spotifyInstance, final String albumName, final SimpleCallback onRequestHasFinished) {

        spotifyInstance.searchAlbums(albumName, new Callback<AlbumsPager>() {

            @Override
            public void success(AlbumsPager albumsPager, Response response) {

                for (Album album : albumsPager.albums.items) {

                    if (album.name.equals(albumName)) {
                        Log.d(Mixen.TAG, "Found album " + album.name + " using Spotify");
                        return;
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(Mixen.TAG, "There was an issue retrieving the data from Spotify.");
                //Log.e(Mixen.TAG, error.toString());
            }

        });

        return;

    }


}

class querySongs extends AsyncTask<String, Void, Void> {
    String songName;
    SimpleCallback functionToCallOnCompletion;

    public querySongs(String songName, SimpleCallback function) {
        this.songName = songName;
        this.functionToCallOnCompletion = function;
    }

    public List<Song> removeDups(List<Song> songs) {

        List<Song> listOfSongs = songs;

        for (int i = 1; i < listOfSongs.size(); i++) {
            if (listOfSongs.get(i).getName().equals(listOfSongs.get(i - 1))) {
                listOfSongs.remove(i);
            }
        }

        return listOfSongs;
    }

    //Try to search for songs asynchronously.
    @Override
    protected Void doInBackground(String... params) {

        try {
            List songs = MixenPlayer.grooveSharkSession.searchSongs(songName);
            SearchSongs.foundSongs = new ArrayList<Song>(10);
            SearchSongs.foundSongs.addAll(removeDups(songs));

            functionToCallOnCompletion.call();
            return null;

        } catch (IOException IOError) {
            Log.e(Mixen.TAG, "IOError");
        } catch (GroovesharkException GSExcep) {
            Log.e(Mixen.TAG, "Grooveshark Exception");
        } catch (NullPointerException nullReturn) {
            Log.e(Mixen.TAG, "There was a network error while attempting to retrieving the data.");
        }

        functionToCallOnCompletion.call();
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
            Log.e(Mixen.TAG, "There was an error retrieving artwork, it will not be available for this session.");
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



