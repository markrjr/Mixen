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

import kaaes.spotify.webapi.android.models.Track;

@JsonObject
public class MetaTrack {

    @JsonField
    public String name;
    @JsonField
    public String artist;
    @JsonField
    public String albumName;
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

    public static final int NOW_PLAYING = 0;
    public static final int NOT_YET_PLAYED = 1;
    public static final int ALREADY_PLAYED = 2;
    public static final int NOW_PLAYING_PAUSED = 3;


    public MetaTrack(){}

    public MetaTrack(Track track)
    {
        this.name = track.name;
        this.artist = track.artists.get(0).name;
        this.albumName = track.album.name;
        this.albumArtURL = track.album.images.get(0).url;
        this.duration = (int)track.duration_ms;
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
