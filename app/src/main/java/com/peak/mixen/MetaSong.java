package com.peak.mixen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.arasthel.asyncjob.AsyncJob;
import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.io.InputStream;

import co.arcs.groove.thresher.Song;

@JsonObject
public class MetaSong {

    @JsonField
    public String name;
    @JsonField
    public String artist;
    @JsonField
    public String albumName;
    public String streamURL;
    @JsonField
    public String albumArtURL;
    public Bitmap albumArt;
    @JsonField
    public int duration;
    @JsonField
    public int playback_state;
    @JsonField
    public boolean isProposed;
    @JsonField
    public int positionInQueue;
    public Song matchingSong;

    public static final int NOW_PLAYING = 0;
    public static final int NOT_YET_PLAYED = 1;
    public static final int ALREADY_PLAYED = 2;

    public MetaSong(){}

    public MetaSong(Song song, int playback_state)
    {
        name = song.getName();
        artist = song.getArtistName();
        albumName = song.getAlbumName();
        albumArtURL = Mixen.COVER_ART_URL + song.getCoverArtFilename();
        duration = song.getDuration();
        this.playback_state = playback_state;
        this.matchingSong = song;

    }

    public MetaSong(Song song, int playback_state, boolean isForHost)
    {
        name = song.getName();
        artist = song.getArtistName();
        albumName = song.getAlbumName();
        albumArtURL = Mixen.COVER_ART_URL + song.getCoverArtFilename();
        duration = song.getDuration();
        this.playback_state = playback_state;
        this.matchingSong = song;

        downloadAlbumArtForService(isForHost);
        getStreamURLForService();

    }

    public void downloadAlbumArtForService(final boolean isForHost)
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
                            MixenBase.mixenPlayerFrag.albumArtIV.setImageBitmap(albumArt);
                            MixenBase.mixenPlayerFrag.generateAlbumArtPalette(MetaSong.this);
                            if(isForHost)
                            {
                                MixenPlayerService.instance.updateAlbumArtCache(); //TODO Make this method static? Should not depend on a current instace.
                            }
                        }
                    }
                });
            }
        });
    }

    public void getStreamURLForService()
    {
        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                String streamURL = null;
                try {
                    streamURL =  Mixen.grooveSharkSession.getStreamUrl(matchingSong).toString();
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

    public void setNowPlaying()
    {
        this.playback_state = NOW_PLAYING;
    }

    public void setAlreadyPlayed()
    {
        this.playback_state = ALREADY_PLAYED;
    }

}
