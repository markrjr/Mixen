package com.peak.mixen;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import com.arasthel.asyncjob.AsyncJob;


import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import co.arcs.groove.thresher.Song;



public class GrooveSharkRequests {

    public static AsyncJob getTopSongs()
    {
        AsyncJob getPopularSongs = new AsyncJob.AsyncJobBuilder<>()
                .doInBackground(new AsyncJob.AsyncAction<Object>() {
                    @Override
                    public List<Song> doAsync() {
                        try
                        {
                            return Mixen.grooveSharkSession.searchPopularSongs();
                        }
                        catch (Exception ex)
                        {
                            Log.d(Mixen.TAG, "There was an error while attempting to retrieve the data.");
                        }
                        return null;
                    }
                })
                .doWhenFinished(new AsyncJob.AsyncResultAction() {
                    @Override
                    public void onResult(Object topSongs) {
                        //
                    }
                })
                .create();

        return getPopularSongs;
    }

    public static AsyncJob searchForSong(final String songName)
    {
        AsyncJob querySongs = new AsyncJob.AsyncJobBuilder<>()
                .doInBackground(new AsyncJob.AsyncAction<Object>() {
                    @Override
                    public Object doAsync() {
                        try {
                            List songs = Mixen.grooveSharkSession.searchSongs(songName);
                            return songs;

                        } catch (Exception ex) {
                            Log.e(Mixen.TAG, "There was an error while attempting to retrieve the data.");
                            ex.printStackTrace();
                            return null;
                        }
                    }
                })
                .doWhenFinished(new AsyncJob.AsyncResultAction() {
                    @Override
                    public void onResult(Object foundSongs) {
                        SearchSongs.instance.postHandleSearchTask((ArrayList<Song>)foundSongs);
                    }
                })
                .create();

        return querySongs;
    }


}





