package com.peak.mixen;

import android.app.Activity;
import android.content.Context;
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
        createMixen = (Button)findViewById(R.id.backButton);
        progressBarInfoTV = (TextView)findViewById(R.id.progressBarInfoTV);
        indeterminateProgress = (ProgressBar)findViewById(R.id.progressBar);

        getActionBar().hide();
        Mixen.currentContext = getApplicationContext();


    }



    public void onBtnClicked(View v)
    {
        switch(v.getId())
        {
            case R.id.backButton:

                //In order to stream down songs, the user must obviously have a connection to the internet.

                check = new checkNetworkConnection();
                check.execute();

                findMixen.setVisibility(View.INVISIBLE);
                createMixen.setVisibility(View.INVISIBLE);
                indeterminateProgress.setVisibility(View.VISIBLE);
                progressBarInfoTV.setVisibility(View.VISIBLE);


                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    public void run() {
                        if (Mixen.networkisReachableAsync) {

                            Log.i(Mixen.TAG, "An internet connection is available.");

                            Intent createNewMixen = new Intent(StartScreen.this, CreateMixen.class);

                            startActivity(createNewMixen);

                            indeterminateProgress.setVisibility(View.GONE);
                            progressBarInfoTV.setVisibility(View.GONE);


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
                }, 2000);

                return;

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

        if (pressedBefore)
        {
            //If the user has pressed the back button twice at this point kill the app.
            this.finish();
            System.exit(0);
            
        }

        Toast.makeText(getApplicationContext(),
                "Press again to close the app.", Toast.LENGTH_SHORT)
                .show();

        pressedBefore = true;
        return;

    }
}
