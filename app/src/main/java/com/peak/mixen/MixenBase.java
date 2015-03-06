package com.peak.mixen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;


import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.peak.salut.Salut;
import com.peak.salut.SalutCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;
import it.neokree.materialtabs.MaterialTab;
import it.neokree.materialtabs.MaterialTabHost;
import it.neokree.materialtabs.MaterialTabListener;


public class MixenBase extends ActionBarActivity implements MaterialTabListener{

    private MaterialTabHost mixenTabs;
    private ViewPager mPager;
    private ViewPagerAdapter pagerAdapter;
    private String[] TABNAMES = {"Up Next", "Now Playing", "Users"};

    boolean pressedBefore = false;
    public SongQueueFrag songQueueFrag;
    public MixenPlayerFrag mixenPlayerFrag;
    public MixenUsersFrag mixenUsersFrag;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixen_base);

        getSupportActionBar().hide();
        //getSupportActionBar().setTitle(" " + Mixen.username + "'s Mixen"); //Will have been set by create Mixen or from network discovery.

        mixenTabs = (MaterialTabHost) this.findViewById(R.id.mixenTabs);
        mPager = (ViewPager) this.findViewById(R.id.viewPager);

        mixenTabs.setBackgroundColor(getResources().getColor(R.color.Dark_Primary));
        mPager.setBackgroundColor(getResources().getColor(R.color.Dark_Primary));
        //getSupportActionBar().hide();

        setupTabbedView();

        songQueueFrag = new SongQueueFrag();
        mixenPlayerFrag = new MixenPlayerFrag();
        mixenUsersFrag = new MixenUsersFrag();

        initMixen();
    }

    public void initMixen()
    {

        Mixen.currentContext = getApplicationContext();

        Mixen.grooveSharkSession = new Client();
        MixenPlayerService.queuedSongs = new ArrayList<Song>();
        MixenPlayerService.proposedSongs = new ArrayList<Song>();

        if(Mixen.isHost)
        {
            setupMixenNetwork();
        }
        else
        {
            //Goto Users Tab

        }

    }



    public void setupTabbedView()
    {

        pagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // when user do a swipe the selected tab change
                mixenTabs.setSelectedNavigationItem(position);
            }
        });


        for (int i = 0; i < pagerAdapter.getCount(); i++) {
            MaterialTab tab = mixenTabs.newTab()
                            .setTabListener(this)
                            .setText(TABNAMES[i]);
            mixenTabs.addTab(tab);
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Salut.disableWiFi(getApplicationContext());
    }

    public void setupMixenNetwork()
    {
        Map appData = new HashMap();
        appData.put("username", Mixen.username);
        appData.put("isHost", "TRUE");


        Mixen.network = new Salut(getApplicationContext(), "_mixen", appData);

        if(Mixen.isHost && !Mixen.network.serviceIsRunning)
        {


            Mixen.network.startNetworkService(new SalutCallback() {
                @Override
                public void call() {
                    //mixenUsersFrag.populateNetworkListView();
                }
            });
            Mixen.network.serviceIsRunning = true;
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(Mixen.network.receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(Mixen.network.receiver, Mixen.network.intentFilter);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mixen_base, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(MaterialTab materialTab) {

    }

    @Override
    public void onTabReselected(MaterialTab materialTab) {

    }

    @Override
    public void onTabUnselected(MaterialTab materialTab) {

    }

    private class ViewPagerAdapter extends FragmentStatePagerAdapter {

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);

        }

        public Fragment getItem(int num) {

            Fragment fragment = null;
            switch(num)
            {
                case 0:
                    fragment = songQueueFrag;
                    break;
                case 1:
                    fragment = mixenPlayerFrag;
                    break;
                case 2:
                    fragment = mixenUsersFrag;
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return TABNAMES.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TABNAMES[position] + position;
        }


    }

    @Override
    public void onBackPressed() {

        if (pressedBefore)
        {
            //If the user has pressed the back button twice at this point kill the app.
            if(MixenPlayerService.isRunning && MixenPlayerService.playerIsPlaying())
            {
                stopService(new Intent(this, MixenPlayerService.class));
            }

            this.finish();
            return;

        }

        Toast.makeText(getApplicationContext(),
                "Press again to close the player.", Toast.LENGTH_SHORT)
                .show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                pressedBefore = false;
            }
        }, 5000);


        pressedBefore = true;
    }
}

