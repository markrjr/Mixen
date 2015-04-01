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

import com.nispok.snackbar.SnackbarManager;
import com.peak.salut.Salut;
import com.peak.salut.SalutCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import co.arcs.groove.thresher.Client;
import it.neokree.materialtabs.MaterialTab;
import it.neokree.materialtabs.MaterialTabHost;
import it.neokree.materialtabs.MaterialTabListener;



public class MixenBase extends ActionBarActivity implements MaterialTabListener{

    public static MaterialTabHost mixenTabs;
    private ViewPager mPager;
    private ViewPagerAdapter pagerAdapter;
    private ArrayList<String> TabNames = new ArrayList<>();
    public static boolean userHasLeftApp = false;

    private boolean pressedBefore = false;
    public static SongQueueFrag songQueueFrag;
    public static MixenPlayerFrag mixenPlayerFrag;
    public static MixenUsersFrag mixenUsersFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixen_base);

        initMixen();

        getSupportActionBar().hide();
        mixenTabs = (MaterialTabHost) this.findViewById(R.id.mixenTabs);
        mPager = (ViewPager) this.findViewById(R.id.viewPager);

        TabNames.add("Up Next");
        TabNames.add("Now Playing");

        if (BuildConfig.DEBUG && Mixen.isHost) {
//            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                    .detectAll()    // detect everything potentially suspect
//                    .penaltyLog()   // penalty is to write to log
//                    .build());
//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                    .detectAll()
//                    .penaltyLog()
//                    .build());

            TabNames.add("Users");
            mixenUsersFrag = new MixenUsersFrag();
        }

        setupTabbedView();

        songQueueFrag = new SongQueueFrag();
        mixenPlayerFrag = new MixenPlayerFrag();

        Log.d(Mixen.TAG, "Mixen UI sucessfully initialized.");

    }

    public void initMixen()
    {

        Mixen.currentContext = getApplicationContext();

        Mixen.grooveSharkSession = new Client()
        {
            public static final int TIMEOUT = 5000;
        };
        //Mixen.grooveSharkSession.setDebugLoggingEnabled(true);

        if(BuildConfig.DEBUG)
        {
            if(Mixen.isHost)
            {
                setupMixenNetwork();
            }
            else
            {
                //Goto Users Tab
                //Mixen.network.discoverNetworkServices();
            }
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
                            .setText(TabNames.get(i));
            mixenTabs.addTab(tab);
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(BuildConfig.DEBUG)
        {
            Salut.disableWiFi(getApplicationContext());
            if(!Mixen.isHost)
            {
                Mixen.network.disposeServiceRequests();
            }
        }

    }

    public void setupMixenNetwork()
    {
        Map appData = new HashMap();
        appData.put("username", Mixen.username);
        appData.put("isHost", "TRUE");


        Mixen.network = new Salut(getApplicationContext(), "_mixen", appData);

        if(Mixen.isHost && !Mixen.network.serviceIsRunning)
        {
            //TODO Move to StartScreen?

            Mixen.network.startNetworkService(new SalutCallback() {
                @Override
                public void call() {
                    mixenUsersFrag.populateNetworkListView();
                }
            }, false);
            Mixen.network.serviceIsRunning = true;
        }

    }


    @Override
    protected void onPause() {
        super.onPause();

        if(BuildConfig.DEBUG)
        {

            unregisterReceiver(Mixen.network.receiver);
        }

        userHasLeftApp = true;
        if(MixenPlayerService.instance != null && MixenPlayerService.instance.playerIsPlaying())
        {
            MixenPlayerService.instance.startForeground(Mixen.MIXEN_NOTIFY_CODE, MixenPlayerService.instance.updateNotification());
            //Checking for pressedBefore fixes some illegal state exception caused by calling playerIsPlaying as the app is exiting.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(BuildConfig.DEBUG)
        {
            registerReceiver(Mixen.network.receiver, Mixen.network.intentFilter);
        }

        userHasLeftApp = false;
        if(MixenPlayerService.instance != null && MixenPlayerService.instance.playerHasTrack)
        {
            MixenPlayerService.instance.stopForeground(true);
        }
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

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(MaterialTab materialTab) {
        mPager.setCurrentItem(materialTab.getPosition());
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
            return TabNames.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TabNames.get(position) + position;
        }


    }

    @Override
    public void onBackPressed() {

        if (songQueueFrag.snackBarIsVisible)
        {
            SnackbarManager.dismiss();
            return;
        }

        if (pressedBefore)
        {
            //If the user has pressed the back button twice at this point kill the app.
            if(MixenPlayerService.instance != null && MixenPlayerService.instance.isRunning) //TODO Fix null with real guard.
            {
                stopService(new Intent(this, MixenPlayerService.class));
            }
            if(!Mixen.isHost)
            {
                Mixen.network.disposeServiceRequests();
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

