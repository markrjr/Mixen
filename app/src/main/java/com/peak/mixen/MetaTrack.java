package com.peak.mixen;

import android.graphics.Bitmap;

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
    @JsonField
    public String spotifyID;
    @JsonField
    public int duration;
    @JsonField
    public String addedBy;
    public Bitmap albumArt;

    public MetaTrack(){}

    public MetaTrack(Track track)
    {
        this.name = track.name;
        this.artist = track.artists.get(0).name;
        this.albumName = track.album.name;
        this.albumArtURL = track.album.images.get(0).url;
        this.spotifyID = track.id;
        this.duration = (int)track.duration_ms;
        this.addedBy = Mixen.username;
    }

}
