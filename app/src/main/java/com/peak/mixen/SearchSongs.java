package com.peak.mixen;


import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ActionMenuView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.peak.mixen.Utils.HeaderListAdapter;
import com.peak.mixen.Utils.HeaderListCell;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.AlbumsPager;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TrackSimple;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class SearchSongs extends ActionBarActivity{

    private ProgressBar indeterminateProgress;
    private ListView songsLV;
    public boolean isFirstSong;
    public static SearchSongs instance;
    private ArrayList<HeaderListCell> fullCellList;
    private ArrayList<HeaderListCell> specificCellList;
    private List<AlbumSimple> foundAlbums;
    private List<Track> foundTracks;
    private HeaderListAdapter headerListAdapter;
    private boolean fullListIsVisible = true;
    private MenuItem searchField;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_songs);

        getSupportActionBar().setTitle(Mixen.username + "'s Mixen");

        // Boilerplate.
        songsLV = (ListView) findViewById(R.id.songsLV);
        indeterminateProgress = (ProgressBar)findViewById(R.id.progressBar);
        indeterminateProgress.setVisibility(View.GONE);

        indeterminateProgress.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.Snow_White),
                android.graphics.PorterDuff.Mode.SRC_IN);

        fullCellList = new ArrayList<>();
        specificCellList = new ArrayList<>();

        setupListAdapter(fullCellList);

        instance = this;
    }

    private void searchSpotify(String query)
    {
        fullCellList.removeAll(fullCellList);

        Mixen.spotify.searchTracks(query, new SpotifyCallback<TracksPager>() {

            @Override
            public void failure(SpotifyError spotifyError) {

                new MaterialDialog.Builder(SearchSongs.instance)
                        .title("Bummer :(")
                        .showListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialog) {
                                indeterminateProgress.setVisibility(View.GONE);
                            }
                        })
                        .content(R.string.generic_network_error)
                        .neutralText("Okay")
                        .show();
                return;
            }

            @Override
            public void success(final TracksPager tracksPager, Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        indeterminateProgress.setVisibility(View.GONE);
                        populateListView(tracksPager);
                        songsLV.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        Mixen.spotify.searchAlbums(query, new SpotifyCallback<AlbumsPager>() {

            @Override
            public void failure(SpotifyError spotifyError) {
                Log.d(Mixen.TAG, "Failed to get albums.");
            }

            @Override
            public void success(final AlbumsPager albumsPager, Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        indeterminateProgress.setVisibility(View.GONE);
                        populateListView(albumsPager);
                        songsLV.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    public static String humanReadableTimeString(long timeInMilliseconds)
    {
        return DurationFormatUtils.formatDuration(timeInMilliseconds, "m:ss");
    }

    private void handleIntent(Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            if(query.length() != 0 && query.matches("^[a-zA-Z0-9 ]*$"))
            {
                indeterminateProgress.setVisibility(View.VISIBLE);
                songsLV.setVisibility(View.INVISIBLE);

                searchSpotify(query);

            }
            else
            {
                SnackbarManager.show(Snackbar.with(this).text("Please use only letters and numbers in your query."));
            }
        }
    }



    private void populateListView(final Object searchResults)
    {
        if(searchResults instanceof TracksPager)
        {
            HeaderListCell sectionCell = new HeaderListCell("SONGS", null);
            sectionCell.setToSectionHeader();
            fullCellList.add(sectionCell);
            foundTracks = ((TracksPager) searchResults).tracks.items;
            int added = 0;
            for(Object track : foundTracks)
            {
                fullCellList.add(new HeaderListCell(((Track) track)));
                added++;

                if(added == 4)
                {
                    break;
                }
            }

            HeaderListCell moreItemsCell = new HeaderListCell(((TracksPager) searchResults).tracks.items.size() + " MORE...", null);
            moreItemsCell.hiddenCategory = "EXTRA_SONGS";
            fullCellList.add(moreItemsCell);
        }
        else if(searchResults instanceof AlbumsPager)
        {
            HeaderListCell sectionCell = new HeaderListCell("ALBUMS", null);
            sectionCell.setToSectionHeader();
            fullCellList.add(sectionCell);
            foundAlbums = ((AlbumsPager) searchResults).albums.items;
            int added = 0;
            for(Object album : foundAlbums)
            {
                fullCellList.add(new HeaderListCell(((AlbumSimple) album).name, null, "ALBUM"));
                added++;

                if(added == 4)
                {
                    break;
                }
            }

            HeaderListCell moreItemsCell = new HeaderListCell(((AlbumsPager) searchResults).albums.items.size() + " MORE...", null);
            moreItemsCell.hiddenCategory = "EXTRA_ALBUMS";
            fullCellList.add(moreItemsCell);

        headerListAdapter.notifyDataSetChanged();
        }
        fullListIsVisible = true;
    }

    private void populateSpecificListView(String typeOfResults)
    {
        setupListAdapter(specificCellList);
        specificCellList.removeAll(specificCellList);

        HeaderListCell sectionCell = new HeaderListCell("FOUND " + typeOfResults, null);
        sectionCell.setToSectionHeader();
        specificCellList.add(sectionCell);
        if(typeOfResults.equals("ALBUMS"))
        {
            for(Object album : foundAlbums)
            {
                specificCellList.add(new HeaderListCell(((AlbumSimple) album).name, null, "ALBUM"));
            }
        }
        else if(typeOfResults.equals("SONGS"))
        {
            for(Object track : foundTracks)
            {
                specificCellList.add(new HeaderListCell(((TrackSimple) track)));
            }
        }

        fullListIsVisible = false;
    }


    private void setupListAdapter(ArrayList<HeaderListCell> listOfCells)
    {
        headerListAdapter = new HeaderListAdapter(getApplicationContext(), listOfCells);

        // Assign adapter to ListView
        songsLV.setAdapter(headerListAdapter);

//         ListView Item Click Listener
        songsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {

                // ListView Clicked item value
                final HeaderListCell selected = (HeaderListCell) songsLV.getItemAtPosition(position);
                Intent viewAlbumInfo = new Intent(SearchSongs.this, AlbumView.class);


                    if(selected.hiddenCategory.equals("EXTRA_SONGS"))
                    {
                        populateSpecificListView("SONGS");
                    }
                    else if(selected.hiddenCategory.equals("EXTRA_ALBUMS"))
                    {
                        populateSpecificListView("ALBUMS");
                    }
                    else if(selected.hiddenCategory.equals("SONG"))
                    {
                        addTrackToQueue(SearchSongs.this, selected.trackSimple, true);

                    } else if(selected.hiddenCategory.equals("ALBUM"))
                    {
                        //Show a single album view.
                        for (AlbumSimple foundAlbum : foundAlbums) {
                            if (selected.getName().equals(foundAlbum.name)) {
                                viewAlbumInfo.putExtra("REQUESTED_ALBUM_ID", foundAlbum.id);
                                startActivity(viewAlbumInfo);
                            }
                    }
                }
            }
        });

    }
    public static void addTrackToQueue(final Activity activity, final TrackSimple track, boolean showSnackBar)
    {
        if(Mixen.isHost)
        {

            for(TrackSimple queuedTrack : MixenPlayerService.instance.spotifyQueue)
            {
                if(track.id.equals(queuedTrack.id))
                {
                    SnackbarManager.show(
                            Snackbar.with(activity)
                                    .text("This song has already been added to the queue. ")
                            , activity);
                    return;
                }
            }

        }

        if(showSnackBar)
        {
            SnackbarManager.show(
                    Snackbar.with(activity)
                            .text("Added " + track.name)
                    //.actionLabel("Undo")
                    //.actionColor(Color.YELLOW)
                    , activity);
        }

        Mixen.spotify.getTrack(track.id, new Callback<Track>() {
            @Override
            public void success(Track track, Response response) {

                if (MixenPlayerService.instance.spotifyQueue.isEmpty()) {
                    Log.i(Mixen.TAG, "First song added to queue.");

                    MixenPlayerService.instance.spotifyQueue.add(track);
                    MixenPlayerService.instance.queueSongPosition = 0;
                    MixenPlayerService.doAction(activity.getApplicationContext(), MixenPlayerService.preparePlayback);
                    instance.isFirstSong = true;
                } else {
                    instance.isFirstSong = false;
                    MixenPlayerService.instance.spotifyQueue.add(track);

                    if (!MixenPlayerService.instance.serviceIsBusy && !MixenPlayerService.instance.playerHasTrack) {
                        //If songs are in the queue, but have completed playback and a new one is suddenly added.
                        MixenPlayerService.instance.queueSongPosition++;
                        MixenPlayerService.doAction(activity.getApplicationContext(), MixenPlayerService.preparePlayback);
                    }

                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MixenBase.mixenPlayerFrag.updateUpNext();
                    }
                });

                //Mixen.network.sendDataToClients(metaSong);
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search_songs, menu);


        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        android.support.v7.widget.SearchView searchView =
                (android.support.v7.widget.SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        searchField = menu.findItem(R.id.search);
        searchField.expandActionView();

        return true;

    }

    @Override
    public void onBackPressed() {

        if(searchField.isActionViewExpanded())
        {
            searchField.collapseActionView();
        }

        if(!fullListIsVisible)
        {
            setupListAdapter(fullCellList);
            fullListIsVisible = true;
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
