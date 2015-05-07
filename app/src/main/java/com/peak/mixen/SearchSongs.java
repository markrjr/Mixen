package com.peak.mixen;


import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.peak.mixen.Utils.HeaderListAdapter;
import com.peak.mixen.Utils.HeaderListCell;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.ArrayList;
import java.util.List;

import co.arcs.groove.thresher.Song;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.AlbumsPager;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TrackSimple;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.client.Response;


public class SearchSongs extends ActionBarActivity{

    private ProgressBar indeterminateProgress;
    private ListView songsLV;
    public boolean isFirstSong;
    public static SearchSongs instance;
    private ArrayList<HeaderListCell> cellLists;
    private List<AlbumSimple> foundAlbums;
    private List<Track> foundTracks;
    private HeaderListAdapter headerListAdapter;
    private boolean fullListIsVisible = true;
    private ArrayList<HeaderListCell> fullCellList;



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

        setupListAdapter();

        instance = this;
    }

    private void searchSpotify(String query)
    {
        cellLists.removeAll(cellLists);
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
            cellLists.add(sectionCell);
            foundTracks = ((TracksPager) searchResults).tracks.items;
            int added = 0;
            for(Object track : foundTracks)
            {
                cellLists.add(new HeaderListCell(((Track) track).name, humanReadableTimeString(((Track) track).duration_ms)));
                added++;

                if(added == 4)
                {
                    break;
                }
            }

            HeaderListCell moreItemsCell = new HeaderListCell(((TracksPager) searchResults).tracks.items.size() + " MORE...", null);
            moreItemsCell.hiddenCategory = "EXTRA_SONGS";
            cellLists.add(moreItemsCell);
        }
        else if(searchResults instanceof AlbumsPager)
        {
            HeaderListCell sectionCell = new HeaderListCell("ALBUMS", null);
            sectionCell.setToSectionHeader();
            cellLists.add(sectionCell);
            foundAlbums = ((AlbumsPager) searchResults).albums.items;
            int added = 0;
            for(Object album : foundAlbums)
            {
                cellLists.add(new HeaderListCell(((AlbumSimple) album).name, null));
                added++;

                if(added == 4)
                {
                    break;
                }
            }

            HeaderListCell moreItemsCell = new HeaderListCell(((AlbumsPager) searchResults).albums.items.size() + " MORE...", null);
            moreItemsCell.hiddenCategory = "EXTRA_ALBUMS";
            cellLists.add(moreItemsCell);

        headerListAdapter.notifyDataSetChanged();

        }

        fullListIsVisible = true;
        fullCellList = cellLists;
    }

    private void populateSpecificListView(String typeOfResults)
    {
        cellLists.removeAll(cellLists);

        HeaderListCell sectionCell = new HeaderListCell("FOUND " + typeOfResults, null);
        sectionCell.setToSectionHeader();
        cellLists.add(sectionCell);
        if(typeOfResults.equals("ALBUM"))
        {
            for(Object album : foundAlbums)
            {
                cellLists.add(new HeaderListCell(((AlbumSimple) album).name, null));
            }
        }
        else if(typeOfResults.equals("SONGS"))
        {
            for(Object track : foundTracks)
            {
                cellLists.add(new HeaderListCell(((TrackSimple) track).name, null));
            }
        }

        fullListIsVisible = false;
        headerListAdapter.notifyDataSetChanged();
    }


    private void setupListAdapter()
    {
        if(cellLists == null)
        {
            cellLists = new ArrayList<>();
            fullCellList = new ArrayList<>();
        }

        headerListAdapter = new HeaderListAdapter(getApplicationContext(), cellLists);

        // Assign adapter to ListView
        songsLV.setAdapter(headerListAdapter);

//         ListView Item Click Listener
        songsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {

                // ListView Clicked item value
                HeaderListCell selected = (HeaderListCell) songsLV.getItemAtPosition(position);
                Intent viewAlbumInfo = new Intent(SearchSongs.this, AlbumView.class);

                if (selected.getCategory() == null) {
                    //Show the rest of the search results.
                    if (selected.getName().contains("MORE")) {
                        if(selected.hiddenCategory.equals("EXTRA_SONGS"))
                        {
                            populateSpecificListView("SONGS");
                        }
                        else if(selected.hiddenCategory.equals("EXTRA_ALBUMS"))
                        {
                            populateSpecificListView("ALBUMS");
                        }
                    } else {
                        //Show a single album view.
                        for (AlbumSimple foundAlbum : foundAlbums) {
                            if (selected.getName().equals(foundAlbum.name)) {
                                viewAlbumInfo.putExtra("REQUESTED_ALBUM_ID", foundAlbum.id);
                                startActivity(viewAlbumInfo);
                            }
                        }
                    }
                } else {
                    //This is a song.
                    signalPlaybackOrSendData();
                }
            }

        });

    }

    private void signalPlaybackOrSendData()
    {
        if(Mixen.isHost)
        {
//                    if(MixenPlayerService.instance.currentSong != null && MixenPlayerService.instance.currentSong == selected)
//                    {
//                        Toast.makeText(getApplicationContext(), "This song has already been added.", Toast.LENGTH_SHORT).show();
//                        return;
//                    }

            //addSongToQueue(selected);
            //TODO Fix undo action flow.

//                    SnackbarManager.show(
//                            Snackbar.with(getApplicationContext())
//                                    .text("Added " + selected.name)
//                            //.actionLabel("Undo")
//                            //.actionColor(Color.YELLOW)
//                            , SearchSongs.this);
        }
    }

    public void addSongToQueue(Song song)
    {
        MetaSong metaSong = new MetaSong(song, MetaSong.NOT_YET_PLAYED);

        if(Mixen.isHost)
        {
            metaSong.isProposed = false;
        }
        else
        {
            metaSong.isProposed = true;
        }

        if(MixenPlayerService.instance.queuedSongs.isEmpty())
        {
            Log.i(Mixen.TAG, "First song added to queue.");
            MixenPlayerService.instance.queuedSongs.add(song);
            MixenPlayerService.instance.proposedSongs.add(metaSong);
            MixenPlayerService.instance.queueSongPosition = 0;
            MixenPlayerService.doAction(getApplicationContext(), MixenPlayerService.getSongStreamURL);
            isFirstSong = true;
        }
        else
        {
            isFirstSong = false;
            MixenPlayerService.instance.queuedSongs.add(song);
            MixenPlayerService.instance.proposedSongs.add(metaSong);

            if(!MixenPlayerService.instance.serviceIsBusy && !MixenPlayerService.instance.playerHasTrack)
            {
                //If songs are in the queue, but have completed playback and a new one is suddenly added.
                MixenPlayerService.instance.queueSongPosition++;
                MixenPlayerService.doAction(getApplicationContext(), MixenPlayerService.getSongStreamURL);
            }

        }

        MixenBase.mixenPlayerFrag.updateUpNext();
        Mixen.network.sendDataToClients(metaSong);
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

        menu.findItem(R.id.search).expandActionView();

//        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View view, boolean hasFocus) {
//                if(!hasFocus)
//                {
//                    SearchSongs.this.finish();
//                }
//            }
//        });

        return true;

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if(!fullListIsVisible)
        {
            populateListView(fullCellList);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
