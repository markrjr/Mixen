package com.peak.mixen;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.peak.salut.Salut;
import com.peak.salut.SalutCallback;

import java.util.HashMap;
import java.util.Map;


public class StartScreen extends Activity {
    public TextView progressBarInfoTV;
    public ProgressBar indeterminateProgress;
    public checkNetworkConnection check;


    private boolean pressedBefore = false;
    private Intent createNewMixen;
    private TextView AppNameTV;
    private TextView DescriptTV;

    private Button findMixen;
    private Button createMixen;

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


        Mixen.currentContext = getApplicationContext();

        Mixen.sharedPref = getSharedPreferences(Mixen.MIXEN_PREF_FILE, Context.MODE_PRIVATE);


        if(!isFirstRun())
        {
            createNewMixen = new Intent(StartScreen.this, MixenBase.class);
            progressBarInfoTV.setText("Restoring " + Mixen.username + "'s mixen...");
        }
        else
        {
            createNewMixen = new Intent(StartScreen.this, CreateMixen.class);
        }

        indeterminateProgress.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.Snow_White),
                android.graphics.PorterDuff.Mode.SRC_IN);
    }

    public boolean isFirstRun()
    {
        boolean isFirstRun;


            isFirstRun = Mixen.sharedPref.getBoolean("FIRST_RUN", true);
            Mixen.username = Mixen.sharedPref.getString("username", "Anonymous");

        return isFirstRun;

    }


    public void skipNetworkCheck()
    {
        Log.i(Mixen.TAG, "Skipping network connection check...");

        Mixen.isHost = true; //User will host content for other users.

        if(!isFirstRun())
        {
            startActivity(createNewMixen);
            hideProgress();
            restoreControls();
        }
        else
        {
            startActivity(createNewMixen);
            hideProgress();
            restoreControls();
        }

    }

    public void clearAppSettings()
    {
        //If the user has pressed the back button twice at this point kill the app.

        Mixen.sharedPref.edit().clear().apply();
        Toast.makeText(getApplicationContext(),
                "Removed all settings. Please restart the app.", Toast.LENGTH_SHORT)
                .show();

        if(BuildConfig.DEBUG)
        {
            Salut.disableWiFi(getApplicationContext());
        }

        this.finish();
    }

    public void onBtnClicked(View v) {

        if(BuildConfig.DEBUG)
        {
            Salut.checkIfIsWifiEnabled(getApplicationContext());
        }

        switch (v.getId()) {
            case R.id.createMixenButton:

                //In order to stream down songs, the user must obviously have a connection to the internet.
                showProgress();
                hideControls();

                startService(new Intent(this, MixenPlayerService.class).setAction(MixenPlayerService.init));
                skipNetworkCheck();

//                check = new checkNetworkConnection();
//                check.execute(new SimpleCallback() {
//                    @Override
//                    public void call() {
//
//                        validateNetwork();
//                    }
//                });

                return;

            case R.id.findMixen: {
                if (BuildConfig.DEBUG) {
                    final MaterialDialog findingMixensProgress = new MaterialDialog.Builder(this)
                            .title("Searching for nearby Mixens...")
                            .content("Please wait...")
                            .progress(true, 0)
                            .build();

                    final MaterialDialog cleanUpDialog = new MaterialDialog.Builder(this)
                            .title("Bummer :(")
                            .content(R.string.discover_p2p_error)
                            .neutralText("Okay")
                            .build();

                    findingMixensProgress.show();

                    Map appData = new HashMap();
                    appData.put("username", null);
                    appData.put("isHost", "FALSE");

                    Mixen.network = new Salut(getApplicationContext(), "_mixen", appData);

                    Mixen.network.discoverNetworkServices(new SalutCallback() {
                              @Override
                              public void call() {
                                  findingMixensProgress.dismiss();
                                  startActivity(new Intent(StartScreen.this, MixenBase.class));
                              }
                          }, false,
                            new SalutCallback() {
                                @Override
                                public void call() {
                                    findingMixensProgress.dismiss();
                                    cleanUpDialog.show();
                                }
                            }, 5000);

                } else {
                    hideControls();

                    new MaterialDialog.Builder(this)
                            .title("Bummer :(")
                            .content("This feature isn't quite ready yet, come back later.")
                            .neutralText("Okay")
                            .dismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    restoreControls();
                                }
                            })
                            .show();

                    return;
                }
            }
        }
    }


    public void validateNetwork()
    {
        if (Mixen.networkisReachableAsync) {

            Log.i(Mixen.TAG, "An internet connection is available.");

            hideProgress();

            Intent createNewMixen = new Intent(StartScreen.this, CreateMixen.class);

            Mixen.isHost = true; //User will host content for other users.

            startActivity(createNewMixen);

        } else {

            Log.e(Mixen.TAG, "There is no connection to the internet.");

            Intent provideMoreInfo = new Intent(StartScreen.this, MoreInfo.class);

            provideMoreInfo.putExtra("START_REASON", Mixen.NO_NETWORK);


            startActivity(provideMoreInfo);

            hideProgress();
            restoreControls();

        }

    }


    public void restoreControls()
    {
        findMixen.setVisibility(View.VISIBLE);
        createMixen.setVisibility(View.VISIBLE);

    }

    public void hideControls()
    {
        findMixen.setVisibility(View.INVISIBLE);
        createMixen.setVisibility(View.INVISIBLE);
    }

    public void showProgress()
    {
        indeterminateProgress.setVisibility(View.VISIBLE);
        progressBarInfoTV.setVisibility(View.VISIBLE);
    }

    public void hideProgress()
    {
        indeterminateProgress.setVisibility(View.GONE);
        progressBarInfoTV.setVisibility(View.GONE);
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

            if(BuildConfig.DEBUG)
            {
                Salut.disableWiFi(getApplicationContext());
            }
            System.exit(0);
            this.finish();

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
