package com.peak.mixen.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.peak.mixen.MetaTrack;
import com.peak.mixen.Mixen;
import com.peak.mixen.R;
import com.peak.mixen.Utils.HeaderListAdapter;
import com.peak.mixen.Utils.HeaderListCell;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.TrackSimple;
import retrofit.client.Response;


public class AlbumView extends ActionBarActivity implements View.OnClickListener {

    private ListView songsLV;
    private String albumID;
    private Album foundAlbum;
    private ProgressBar progressBar;
    private ImageView albumArtHeader;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_view);

        getSupportActionBar().setTitle("");

        songsLV = (ListView) findViewById(R.id.songsLV);
        albumArtHeader = (ImageView) findViewById(R.id.albumArtHeader);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        fab = (FloatingActionButton) findViewById(R.id.playAlbumBtn);

        if(Mixen.amoledMode)
        {
            songsLV.setDivider(new ColorDrawable(getResources().getColor(R.color.EXP_4)));
            songsLV.setDividerHeight(10);
            songsLV.setBackgroundColor(Color.BLACK);
            RelativeLayout baseLayout = (RelativeLayout)findViewById(R.id.albumViewLayout);
            baseLayout.setBackgroundColor(Color.BLACK);
        }

        fab.setOnClickListener(this);

        progressBar.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.Snow_White),
                android.graphics.PorterDuff.Mode.SRC_IN);

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    public void handleIntent(Intent intent)
    {
        if(intent.getExtras() == null)
        {
            this.finish();
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

                foundAlbum = album;


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        populateUI(album);
                        if(Mixen.isHost)
                        {
                            fab.setVisibility(View.VISIBLE);
                        }
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
                    .into(albumArtHeader, new Callback() {
                        @Override
                        public void onSuccess() {
                            albumArtHeader.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError() {

                        }
                    });

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

        HeaderListCell sectionCell = new HeaderListCell(album.tracks.total + " TRACKS" , "HEADER");
        sectionCell.setToSectionHeader();
        cellLists.add(sectionCell);
        for(TrackSimple track : albumTracks)
        {
            HeaderListCell headerListCell = new HeaderListCell(track);
            headerListCell.category = SearchSongs.humanReadableTimeString(track.duration_ms);
            cellLists.add(headerListCell);
        }

        HeaderListAdapter headerListAdapter = new HeaderListAdapter(getApplicationContext(), cellLists);

        songsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // ListView Clicked item value
                HeaderListCell selected = (HeaderListCell) songsLV.getItemAtPosition(position);


                if (selected.hiddenCategory.equals("SONG")) {
                    SearchSongs.addTrackToQueue(AlbumView.this, new MetaTrack(selected.trackSimple), true);
                }

            }
        });

        // Assign adapter to ListView

        songsLV.setAdapter(headerListAdapter);

        progressBar.setVisibility(View.INVISIBLE);
        songsLV.setVisibility(View.VISIBLE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_album_view, menu);
        return true;
    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.playAlbumBtn)
        {
            if(foundAlbum != null)
            {
                for(TrackSimple track : foundAlbum.tracks.items)
                {
                    SearchSongs.addTrackToQueue(AlbumView.this, new MetaTrack(track), false);
                }

                SnackbarManager.show(
                        Snackbar.with(this)
                                .text("Added " + foundAlbum.name)
                        //.actionLabel("Undo")
                        //.actionColor(Color.YELLOW)
                        , this);
            }
        }
    }
}
