package com.peak.mixen;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.peak.mixen.Utils.HeaderListAdapter;
import com.peak.mixen.Utils.HeaderListCell;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

import app.majestylabs.helpers.ImageBlurrer;
import de.hdodenhof.circleimageview.CircleImageView;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.TrackSimple;
import retrofit.client.Response;


public class AlbumView extends ActionBarActivity {

    private ImageView artistArtBackgroundIV;
    private CircleImageView albumArtIV;
    private ListView songsLV;
    private String albumID;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_view);

        getSupportActionBar().setTitle("");

        artistArtBackgroundIV = (ImageView) findViewById(R.id.artistArtBG);
        albumArtIV = (CircleImageView) findViewById(R.id.albumArt);
        songsLV = (ListView) findViewById(R.id.songsLV);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        progressBar.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.Snow_White),
                android.graphics.PorterDuff.Mode.SRC_IN);

        progressBar.setVisibility(View.VISIBLE);

        if(getIntent().getExtras() == null)
        {
            return;
        }
        else
        {
            albumID = getIntent().getStringExtra("REQUESTED_ALBUM_ID");
        }

        getAlbum();

    }

    public void getAlbum()
    {
        Mixen.spotify.getAlbum(albumID, new SpotifyCallback<Album>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                new MaterialDialog.Builder(getApplicationContext())
                        .title("Bummer :(")
                        .content("We had problem getting that album from Spotify, please try again later.")
                        .neutralText("Okay")
                        .dismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                AlbumView.this.finish();
                            }
                        });
            }

            @Override
            public void success(final Album album, Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        populateUI(album);
                    }
                });

                getArtistArt(album.artists.get(0).id);
            }
        });
    }

    public void getArtistArt(String artistID)
    {
        Mixen.spotify.getArtist(artistID, new SpotifyCallback<Artist>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                Log.e(Mixen.TAG, "Failed to get artist.");
            }

            @Override
            public void success(final Artist artist, Response response) {

                if(artist.images.size() == 0)
                {
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Picasso.with(getApplicationContext())
                                .load(artist.images.get(0).url)
                                .into(new Target() {
                                    @Override
                                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                        Bitmap blurredArtistArt = ImageBlurrer.blurImage(getApplicationContext(), bitmap, 12.5f, 1.25f);
                                        artistArtBackgroundIV.setImageBitmap(blurredArtistArt);
                                        artistArtBackgroundIV.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    }

                                    @Override
                                    public void onBitmapFailed(Drawable errorDrawable) {

                                    }

                                    @Override
                                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                                    }
                                });
                    }
                });
            }
        });
    }

    public void populateUI(final Album album)
    {
        ArrayList<HeaderListCell> cellLists = new ArrayList<>();

        if(album.images.size() > 0)
        {
            String albumArtURL = album.images.get(0).url;
            Picasso.with(getApplicationContext())
                    .load(albumArtURL)
                    .placeholder(R.drawable.mixen_icon)
                    .fit()
                    .into(albumArtIV);
            albumArtIV.setVisibility(View.VISIBLE);
        }
        else
        {
            albumArtIV.setVisibility(View.INVISIBLE);
        }

        if(album.artists.size() == 1)
        {
            getSupportActionBar().setTitle(album.name + " by " + album.artists.get(0).name);
        }
        else
        {
            String artistTitleString = "";

            for(ArtistSimple artist: album.artists)
            {
                artistTitleString += artist.name + " ,";
            }
            String withoutTrailingComma = artistTitleString.substring(0, artistTitleString.length() - 1); //Remove the trailing comma?
            getSupportActionBar().setTitle(album.name + " by " + withoutTrailingComma);
        }



        ArrayList<TrackSimple> albumTracks = new ArrayList<>();
        albumTracks.addAll(album.tracks.items);

        HeaderListCell sectionCell = new HeaderListCell(album.tracks.total + " TRACKS" , null);
        sectionCell.setToSectionHeader();
        cellLists.add(sectionCell);
        for(TrackSimple track : albumTracks)
        {
            cellLists.add(new HeaderListCell(track.name, SearchSongs.humanReadableTimeString(track.duration_ms)));
        }

        HeaderListAdapter headerListAdapter = new HeaderListAdapter(getApplicationContext(), cellLists);

        // Assign adapter to ListView
        songsLV.setAdapter(headerListAdapter);

        progressBar.setVisibility(View.INVISIBLE);
        songsLV.setVisibility(View.VISIBLE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mixen_base, menu);
        return true;
    }
}
