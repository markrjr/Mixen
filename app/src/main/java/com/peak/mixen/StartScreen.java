package com.peak.mixen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.peak.salut.Salut;
import com.peak.salut.SalutCallback;

import java.util.HashMap;
import java.util.Map;


public class StartScreen extends Activity {
    public TextView progressBarInfoTV;
    public ProgressBar indeterminateProgress;
    public checkNetworkConnection check;

    private boolean pressedBefore = false;
    private TextView AppNameTV;
    private TextView DescriptTV;

    private static Button findMixen;
    private static Button createMixen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        //Map TextViews and other widgets.
        AppNameTV = (TextView)findViewById(R.id.appNameTV);
        DescriptTV = (TextView)findViewById(R.id.moreInfoTV);
        findMixen = (Button)findViewById(R.id.findMixen);
        createMixen = (Button)findViewById(R.id.createMixenButton);
        progressBarInfoTV = (TextView)findViewById(R.id.progressBarInfoTV);
        indeterminateProgress = (ProgressBar)findViewById(R.id.progressBar);

        Mixen.appColors = getResources().getIntArray(R.array.appcolors);

        getActionBar().hide();

        Mixen.currentContext = getApplicationContext();

    }


    public void skipNetworkCheck()
    {
        Log.i(Mixen.TAG, "Skipping network connection check...");

        Intent createNewMixen = new Intent(StartScreen.this, CreateMixen.class);

        Mixen.isHost = true; //User will host content for other users.

        startActivity(createNewMixen);
    }

    public void onBtnClicked(View v)
    {

        //Salut.checkIfIsWifiEnabled(getApplicationContext());

        switch(v.getId())
        {
            case R.id.createMixenButton:

                //In order to stream down songs, the user must obviously have a connection to the internet.

                findMixen.setVisibility(View.INVISIBLE);
                createMixen.setVisibility(View.INVISIBLE);
                indeterminateProgress.setVisibility(View.VISIBLE);
                progressBarInfoTV.setVisibility(View.VISIBLE);


                if (indeterminateProgress.getVisibility() == View.VISIBLE)
                {
                    indeterminateProgress.setVisibility(View.GONE);
                    progressBarInfoTV.setVisibility(View.GONE);
                    skipNetworkCheck();
                    return;
                }

                check = new checkNetworkConnection();
                check.execute(new SimpleCallback() {
                    @Override
                    public void call() {

                        validateNetwork();
                    }
                });


            return;

            case R.id.findMixen:

                findMixen.setVisibility(View.INVISIBLE);
                createMixen.setVisibility(View.INVISIBLE);
                indeterminateProgress.setVisibility(View.VISIBLE);
                progressBarInfoTV.setVisibility(View.VISIBLE);
                progressBarInfoTV.setText(R.string.mixen_network_search);

                Map appData = new HashMap();
                appData.put("username", null);
                appData.put("isHost", "FALSE");


                Mixen.network = new Salut(getApplicationContext(), "_mixen", appData);
                Mixen.network.startNetworkService();

                startActivity(new Intent(StartScreen.this, SongQueue.class));


            return;
        }
    }


    public void validateNetwork()
    {
        if (Mixen.networkisReachableAsync) {

            Log.i(Mixen.TAG, "An internet connection is available.");

            indeterminateProgress.setVisibility(View.GONE);
            progressBarInfoTV.setVisibility(View.GONE);


            Intent createNewMixen = new Intent(StartScreen.this, CreateMixen.class);

            Mixen.isHost = true; //User will host content for other users.

            startActivity(createNewMixen);

        } else {

            Log.e(Mixen.TAG, "There is no connection to the internet.");

            Intent provideMoreInfo = new Intent(StartScreen.this, MoreInfo.class);

            provideMoreInfo.putExtra("START_REASON", Mixen.NO_NETWORK);


            startActivity(provideMoreInfo);

            indeterminateProgress.setVisibility(View.GONE);
            progressBarInfoTV.setVisibility(View.GONE);

            findMixen.setVisibility(View.VISIBLE);
            createMixen.setVisibility(View.VISIBLE);

        }

    }


    public static void restoreControls()
    {
        //Called when MixenPlayer is closed for some reason.
        findMixen.setVisibility(View.VISIBLE);
        createMixen.setVisibility(View.VISIBLE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mixen_stage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId())
        {

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onBackPressed() {

        if (indeterminateProgress.getVisibility() == View.VISIBLE && progressBarInfoTV.getVisibility() == View.GONE)
        {
            indeterminateProgress.setVisibility(View.GONE);
            restoreControls();
            return;
        }

        if (pressedBefore)
        {
            //If the user has pressed the back button twice at this point kill the app.

            this.finish();
            Salut.disableWiFi(getApplicationContext());
            System.exit(0);
            
        }

        Toast.makeText(getApplicationContext(),
                "Press again to close the app.", Toast.LENGTH_SHORT)
                .show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                pressedBefore = false;
            }
        }, 5000);


        pressedBefore = true;
        return;

    }
}
