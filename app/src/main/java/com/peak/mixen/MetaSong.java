package com.peak.mixen;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.arasthel.asyncjob.AsyncJob;

import java.io.InputStream;
import java.net.URL;

import co.arcs.groove.thresher.Song;

/**
 * Created by markrjr on 4/14/15.
 */
public class MetaSong {

    public String name;
    public String artist;
    public String albumName;
    public String streamURL;
    public String albumArtURL;
    public Bitmap albumArt;
    public int duration;

    public MetaSong(Song song)
    {
        name = song.getName();
        artist = song.getArtistName();
        albumName = song.getAlbumName();
        albumArtURL = Mixen.COVER_ART_URL + song.getCoverArtFilename();
        duration = song.getDuration();

    }

    public MetaSong(Song song, ImageView imageView)
    {
        name = song.getName();
        artist = song.getArtistName();
        albumName = song.getAlbumName();
        albumArtURL = Mixen.COVER_ART_URL + song.getCoverArtFilename();
        duration = song.getDuration();

        downloadAlbumArtForService(imageView);
        getStreamURLForService(song);

    }

    public void downloadAlbumArtForService(final ImageView imageView)
    {
        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                Bitmap downloadedArt = null;
                try {
                    InputStream in = new java.net.URL(albumArtURL).openStream();
                    downloadedArt = BitmapFactory.decodeStream(in);
                } catch (Exception e) {
                    Log.e(Mixen.TAG, "Failed to get download art. It will not be available for this song.");
                    e.printStackTrace();
                }

                albumArt = downloadedArt;
                AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                    @Override
                    public void doInUIThread() {

                        if(albumArt != null)
                        {
                            imageView.setImageBitmap(albumArt);
                            //MixenPlayerService.instance.currentAlbumArt = (Bitmap)downloadedArt;
                            MixenBase.mixenPlayerFrag.generateAlbumArtPalette();
                            MixenPlayerService.instance.updateAlbumArtCache();
                        }
                    }
                });
            }
        });
    }

    public void getStreamURLForService(final Song song)
    {
        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                String streamURL = null;
                try {
                    streamURL =  Mixen.grooveSharkSession.getStreamUrl(song).toString();
                } catch (Exception ex)
                {
                    Log.e(Mixen.TAG, "Failed to get a stream URL for this song.");
                }

                MetaSong.this.streamURL = streamURL;
                AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                    @Override
                    public void doInUIThread() {
                        if(MetaSong.this.streamURL != null)
                            MixenPlayerService.doAction(Mixen.currentContext, MixenPlayerService.preparePlayback);
                    }
                });
            }
        });
    }


    public void downloadAlbumArt()
    {
        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                Bitmap downloadedArt = null;
                try {
                    InputStream in = new java.net.URL(albumArtURL).openStream();
                    downloadedArt = BitmapFactory.decodeStream(in);
                } catch (Exception e) {
                    Log.e(Mixen.TAG, "Failed to get download art. It will not be available for this song.");
                    e.printStackTrace();
                }
                albumArt = downloadedArt;
            }
        });
    }

    public void getStreamURL(final Song song)
    {
        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                String streamURL = null;
                try {
                    streamURL =  Mixen.grooveSharkSession.getStreamUrl(song).toString();
                } catch (Exception ex)
                {
                    Log.e(Mixen.TAG, "Failed to get a stream URL for this song.");
                }

                MetaSong.this.streamURL = streamURL;
            }
        });
    }

}
