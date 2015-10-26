package com.peak.mixen.Activities;


import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.peak.mixen.MetaTrack;
import com.peak.mixen.Mixen;
import com.peak.mixen.Service.MixenPlayerService;
import com.peak.mixen.R;
import com.peak.mixen.Service.PlaybackSnapshot;
import com.peak.mixen.Utils.ActivityAnimator;
import com.peak.mixen.Utils.HeaderListAdapter;
import com.peak.mixen.Utils.HeaderListCell;
import com.peak.mixen.RecentSearchesProvider;


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


public class SearchSongs extends ActionBarActivity {

    private ProgressBar indeterminateProgress;
    private ListView songsLV;
    public static boolean isFirstSong;
    private ArrayList<HeaderListCell> fullCellList;
    private ArrayList<HeaderListCell> specificCellList;
    private List<AlbumSimple> foundAlbums;
    private List<Track> foundTracks;
    private HeaderListAdapter headerListAdapter;
    private boolean fullListIsVisible = true;
    private MenuItem searchField;
    private MaterialDialog spotifyErrorDiag;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_songs);

        if(Mixen.isHost)
        {
            getSupportActionBar().setTitle(Mixen.username+ "'s Mixen");
        }
        else
        {
//            getSupportActionBar().setTitle(Mixen.network.registeredHost.readableName + "'s Mixen");
        }

        // Boilerplate.
        songsLV = (ListView) findViewById(R.id.songsLV);
        indeterminateProgress = (ProgressBar) findViewById(R.id.progressBar);
        indeterminateProgress.setVisibility(View.GONE);

        if(Mixen.amoledMode)
        {
            songsLV.setDivider(new ColorDrawable(getResources().getColor(R.color.EXP_4)));
            songsLV.setDividerHeight(10);
            RelativeLayout baseLayout = (RelativeLayout)findViewById(R.id.searchBase);
            baseLayout.setBackgroundColor(Color.BLACK);
        }

        indeterminateProgress.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.Snow_White),
                android.graphics.PorterDuff.Mode.SRC_IN);

        fullCellList = new ArrayList<>();
        specificCellList = new ArrayList<>();

        setupDiags();
        setupListAdapter(fullCellList);
    }


    private void setupDiags()
    {
        spotifyErrorDiag = new MaterialDialog.Builder(this)
                .title("Bummer :(")
                .showListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        indeterminateProgress.setVisibility(View.GONE);
                    }
                })
                .content(R.string.generic_network_error)
                .neutralText("Okay")
                .build();

    }

    private void searchSpotify(String query) {

        fullCellList.removeAll(fullCellList);

        Mixen.spotify.searchTracks(query, new SpotifyCallback<TracksPager>() {

            @Override
            public void failure(SpotifyError spotifyError) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SearchSongs.this.spotifyErrorDiag.show();
                    }
                });
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

    public static String humanReadableTimeString(long timeInMilliseconds) {
        return DurationFormatUtils.formatDuration(timeInMilliseconds, "m:ss");
    }

    private void handleIntent(Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            if (query.length() != 0 && query.matches("^[a-zA-Z0-9 ]*$")) {
                indeterminateProgress.setVisibility(View.VISIBLE);
                songsLV.setVisibility(View.INVISIBLE);

                searchSpotify(query);
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                        RecentSearchesProvider.AUTHORITY, RecentSearchesProvider.MODE);
                suggestions.saveRecentQuery(query, null);

            } else {
                SnackbarManager.show(Snackbar.with(this).text("Please use only letters and numbers in your query."));
            }
        }
    }


    private void populateListView(final Object searchResults) {
        if (searchResults instanceof TracksPager) {
            HeaderListCell sectionCell = new HeaderListCell("SONGS", "HEADER");
            sectionCell.setToSectionHeader();
            fullCellList.add(sectionCell);
            foundTracks = ((TracksPager) searchResults).tracks.items;
            int added = 0;
            for (Object track : foundTracks) {
                fullCellList.add(new HeaderListCell(((Track) track)));
                added++;

                if (added == 4) {
                    break;
                }
            }

            HeaderListCell moreItemsCell = new HeaderListCell(((TracksPager) searchResults).tracks.items.size() + " MORE...", "EXTRA_SONGS");
            fullCellList.add(moreItemsCell);
        } else if (searchResults instanceof AlbumsPager) {
            HeaderListCell sectionCell = new HeaderListCell("ALBUMS", "HEADER");
            sectionCell.setToSectionHeader();
            fullCellList.add(sectionCell);
            foundAlbums = ((AlbumsPager) searchResults).albums.items;
            int added = 0;
            for (Object album : foundAlbums) {
                fullCellList.add(new HeaderListCell(((AlbumSimple) album)));
                added++;

                if (added == 4) {
                    break;
                }
            }

            HeaderListCell moreItemsCell = new HeaderListCell(((AlbumsPager) searchResults).albums.items.size() + " MORE...", "EXTRA_ALBUMS");
            fullCellList.add(moreItemsCell);

            headerListAdapter.notifyDataSetChanged();
        }
        fullListIsVisible = true;
    }

    private void populateSpecificListView(String typeOfResults) {
        setupListAdapter(specificCellList);
        specificCellList.removeAll(specificCellList);

        HeaderListCell sectionCell = new HeaderListCell("FOUND " + typeOfResults, "HEADER");
        sectionCell.setToSectionHeader();
        specificCellList.add(sectionCell);
        if (typeOfResults.equals("ALBUMS")) {
            for (Object album : foundAlbums) {
                specificCellList.add(new HeaderListCell(((AlbumSimple) album)));
            }
        } else if (typeOfResults.equals("SONGS")) {
            for (Object track : foundTracks) {
                specificCellList.add(new HeaderListCell(((TrackSimple) track)));
            }
        }

        fullListIsVisible = false;
    }


    private void setupListAdapter(ArrayList<HeaderListCell> listOfCells) {
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


                if (selected.hiddenCategory.equals("EXTRA_SONGS")) {
                    populateSpecificListView("SONGS");
                } else if (selected.hiddenCategory.equals("EXTRA_ALBUMS")) {
                    populateSpecificListView("ALBUMS");
                } else if (selected.hiddenCategory.equals("SONG")) {
                    addTrackToQueue(SearchSongs.this, new MetaTrack(selected.trackSimple), true);

                } else if (selected.hiddenCategory.equals("ALBUM")) {
                    viewAlbumInfo.putExtra("REQUESTED_ALBUM_ID", selected.albumSimple.id);
                    startActivity(viewAlbumInfo);
                    new ActivityAnimator().fadeAnimation(SearchSongs.this);
                }
            }
        });

    }

    private static void actuallyAddToQueue(Activity activity, MetaTrack track)
    {
        if (MixenPlayerService.instance.metaQueue.isEmpty()) {
            Log.i(Mixen.TAG, "First song added to queue.");

            MixenPlayerService.instance.metaQueue.add(track);
            MixenPlayerService.instance.queueSongPosition = 0;
            MixenPlayerService.doAction(activity.getApplicationContext(), MixenPlayerService.preparePlayback);
            isFirstSong = true;
        } else {
            isFirstSong = false;
            MixenPlayerService.instance.metaQueue.add(track);
            if (!MixenPlayerService.instance.serviceIsBusy && !MixenPlayerService.instance.playerHasTrack) {
                //If songs are in the queue, but have completed playback and a new one is suddenly added.
                MixenPlayerService.instance.queueSongPosition++;
                MixenPlayerService.doAction(activity.getApplicationContext(), MixenPlayerService.preparePlayback);
            }

        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MixenBase.mixenPlayerFrag.bufferPB.setVisibility(View.INVISIBLE);
                MixenBase.mixenPlayerFrag.updateUpNext();
                MixenBase.songQueueFrag.updateQueueUI();
            }
        });
    }

    public static void addTrackToQueue(final Activity activity, final MetaTrack metaTrack, boolean showSnackBar)
    {

//        if(metaTrack.explicit && Mixen.network != null && !PlaybackSnapshot.explictAllowed)
//        {
//            if(Mixen.network.isRunningAsHost || Mixen.network.registeredHost != null)
//            {
//                SnackbarManager.show(
//                        Snackbar.with(activity)
//                                .text("Explicit tracks have been disabled by the host.")
//                        , activity);
//                return;
//            }
//        }

        for(MetaTrack queuedTrack : MixenPlayerService.instance.metaQueue)
        {
            if(metaTrack.spotifyID.equals(queuedTrack.spotifyID))
            {
                SnackbarManager.show(
                        Snackbar.with(activity)
                                .text("This song has already been added to the queue. ")
                        , activity);
                return;
            }
        }

        if(showSnackBar)
        {
            SnackbarManager.show(
                    Snackbar.with(activity)
                            .text("Added " + metaTrack.name)
                    //.actionLabel("Undo")
                    //.actionColor(Color.YELLOW)
                    , activity);
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MixenBase.mixenPlayerFrag.bufferPB.setVisibility(View.VISIBLE);
            }
        });

        Mixen.spotify.getTrack(metaTrack.spotifyID, new Callback<Track>() {
            @Override
            public void success(Track track, Response response) {
                MetaTrack trackToAdd = new MetaTrack(track);
                trackToAdd.addedBy = metaTrack.addedBy;
                actuallyAddToQueue(activity, trackToAdd);
                MixenPlayerService.instance.playerServiceSnapshot.updateNetworkQueue();
            }

            @Override
            public void failure(RetrofitError error) {
                MixenBase.mixenPlayerFrag.bufferPB.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.clear_search_history) {
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    RecentSearchesProvider.AUTHORITY, RecentSearchesProvider.MODE);
            suggestions.clearHistory();
            return true;
        }

        return super.onOptionsItemSelected(item);
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

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchField.collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

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
            setResult(RESULT_OK);
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
