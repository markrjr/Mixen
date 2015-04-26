package com.peak.mixen;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.peak.salut.Callbacks.SalutServiceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutP2P;
import com.peak.salut.Callbacks.SalutCallback;

import java.util.ArrayList;


public class StartScreen extends Activity implements View.OnClickListener{
    private boolean pressedBefore = false;
    private Intent createNewMixen;
    private MaterialDialog enableWiFiDiag;
    private MaterialDialog wiFiFailureDiag;
    private MaterialDialog findingMixensProgress;
    private MaterialDialog cleanUpDialog;
    private MaterialDialog foundMixensDialog;

    private Button findMixen;
    private Button createMixen;
    private TextView appNameTV;
    private TextView textDivider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        //Map TextViews and other widgets.
        findMixen = (Button)findViewById(R.id.findMixen);
        createMixen = (Button)findViewById(R.id.createMixenButton);
        appNameTV = (TextView) findViewById(R.id.appNameTV);
        textDivider = (TextView) findViewById(R.id.textDivider);

        Mixen.appColors = getResources().getIntArray(R.array.appcolors);
        //TODO Set background to random color using this?

        Mixen.currentContext = getApplicationContext();

        Mixen.sharedPref = getSharedPreferences(Mixen.MIXEN_PREF_FILE, Context.MODE_PRIVATE);

        createDialogs();

        if(!isFirstRun())
        {
            createNewMixen = new Intent(StartScreen.this, MixenBase.class);
        }
        else
        {
            createNewMixen = new Intent(StartScreen.this, CreateMixen.class);
        }

        if(BuildConfig.DEBUG)
        {
            Mixen.debugFeaturesEnabled = true;
        }

        createMixen.setOnClickListener(this);
        findMixen.setOnClickListener(this);
        appNameTV.setOnClickListener(this);

        appNameTV.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(v.getId() == R.id.appNameTV)
                {
                    Mixen.debugFeaturesEnabled = true;
                    Toast.makeText(getApplicationContext(), "Debug features are now enabled, only God can help you now.", Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });

    }

    private boolean isFirstRun()
    {
        boolean isFirstRun = true;

        Mixen.username = Mixen.sharedPref.getString("username", "Anonymous");

        if(!Mixen.username.equals("Anonymous"))
            isFirstRun = false;

        return isFirstRun;

    }

    private void createDialogs()
    {
        findingMixensProgress = new MaterialDialog.Builder(this)
                .autoDismiss(false)
                .title("Searching for nearby Mixens...")
                .content("Please wait...")
                .theme(Theme.DARK)
                .progress(true, 0)
                .build();

        cleanUpDialog = new MaterialDialog.Builder(this)
                .title("Bummer :(")
                .content(R.string.discover_p2p_error)
                .neutralText("Okay")
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        restoreControls();
                    }
                })
                .build();

        enableWiFiDiag = new MaterialDialog.Builder(this)
                .autoDismiss(false)
                .title("Just a sec...")
                .content("We're turning on WiFi...")
                .progress(true, 0)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        restoreControls();
                    }
                })
                .build();

        wiFiFailureDiag = new MaterialDialog.Builder(this)
                .title("Bummer :(")
                .content("We had trouble checking if WiFi was on, please double check your settings.")
                .neutralText("Okay")
                .build();
    }
    public void clearAppSettings()
    {
        //If the user has pressed the back button twice at this point kill the app.

        Mixen.sharedPref.edit().clear().apply();
        Toast.makeText(getApplicationContext(),
                "Removed all settings. Please restart the app.", Toast.LENGTH_SHORT)
                .show();

        this.finish();
    }


    private void handleButtonClicks(View v)
    {

        switch (v.getId()) {

            case R.id.createMixenButton:

                Mixen.isHost = true;

                //In order to stream down songs, the user must obviously have a connection to the internet.
                startService(new Intent(this, MixenPlayerService.class).setAction(MixenPlayerService.init));

                Log.i(Mixen.TAG, "Skipping network connection check...");

                startActivity(createNewMixen);

                restoreControls();
                return;

            case R.id.findMixen: {

                Mixen.isHost = false;
                //TODO Double check of if debugFeaturesAreEnabled.
                if (Mixen.debugFeaturesEnabled) {

                    findingMixensProgress.show();
                    findingMixensProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            restoreControls();
                        }
                    });

                    if(isFirstRun()) {
                        //TODO Make it so that the create screen comes up before discovering the service.
                        Mixen.network = new Salut(this, "_mixen", "Anonymous", Mixen.MIXEN_SERVICE_PORT);
                    }
                    else
                    {
                        Mixen.network = new Salut(this, "_mixen", Mixen.username, Mixen.MIXEN_SERVICE_PORT);
                    }

                    Mixen.network.discoverNetworkServicesWithTimeOut(new SalutCallback() {
                        @Override
                        public void call() {
                            findingMixensProgress.dismiss();

                            if(Mixen.network.foundHostServices.size() == 1)
                            {

                                Mixen.network.connectToHostService(Mixen.network.foundHostServices.get(0), new SalutCallback() {
                                    @Override
                                    public void call() {
                                        StartScreen.this.startActivity(createNewMixen);
                                        Toast.makeText(getApplicationContext(), "You're now connected to " + Mixen.network.foundHostServices.get(0).readableName + "'s Mixen.", Toast.LENGTH_LONG).show();
                                    }
                                }, new SalutCallback() {
                                    @Override
                                    public void call() {
                                        cleanUpDialog.setContent("We had a problem connection to " + Mixen.network.foundHostServices.get(0).readableName + " 's Mixen. Please try again momentarily.");
                                        cleanUpDialog.show();
                                    }
                                });
                            }
                            else
                            {

                                String[] foundNames = Mixen.network.getReadableFoundNames().toArray(new String[Mixen.network.foundHostServices.size()]);

                                foundMixensDialog = new MaterialDialog.Builder(StartScreen.this)
                                        .title("We found a few people nearby.")
                                        .theme(Theme.DARK)
                                        .items(foundNames)
                                        .dismissListener(new DialogInterface.OnDismissListener() {
                                            @Override
                                            public void onDismiss(DialogInterface dialog) {
                                                restoreControls();
                                            }
                                        })
                                        .itemsCallback(new MaterialDialog.ListCallback() {
                                            @Override
                                            public void onSelection(MaterialDialog materialDialog, View view, final int i, CharSequence charSequence) {

                                                Mixen.network.connectToHostService(Mixen.network.foundHostServices.get(i), new SalutCallback() {
                                                    @Override
                                                    public void call() {
                                                        StartScreen.this.startActivity(createNewMixen);
                                                        Toast.makeText(getApplicationContext(), "You're now connected to " + Mixen.network.foundHostServices.get(i).readableName + "'s Mixen.", Toast.LENGTH_LONG).show();
                                                    }
                                                }, new SalutCallback() {
                                                    @Override
                                                    public void call() {
                                                        cleanUpDialog.setContent("We had a problem connection to " + Mixen.network.foundHostServices.get(i).readableName + " 's Mixen. Please try again momentarily.");
                                                        cleanUpDialog.show();
                                                    }
                                                });
                                            }
                                        })
                                        .build();
                                foundMixensDialog.show();
                            }

                        }
                    }, new SalutCallback() {
                        @Override
                        public void call() {
                            findingMixensProgress.dismiss();
                            cleanUpDialog.show();
                        }
                    }, 3000);

                } else {

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
                }

                return;
            }
        }
    }


    public void checkWiFiConfig(final View v) {

        if(!SalutP2P.isWiFiEnabled(this))
        {
            enableWiFiDiag.show();
            SalutP2P.enableWiFi(getApplicationContext());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    handleButtonClicks(v);
                    enableWiFiDiag.dismiss();
                }
            }, 2500);
        }
        else
        {
            handleButtonClicks(v);
        }

    }


    public void restoreControls()
    {
        findMixen.setVisibility(View.VISIBLE);
        textDivider.setVisibility(View.VISIBLE);
        createMixen.setVisibility(View.VISIBLE);

    }

    public void hideControls()
    {
        findMixen.setVisibility(View.INVISIBLE);
        textDivider.setVisibility(View.INVISIBLE);
        createMixen.setVisibility(View.INVISIBLE);
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

        if (pressedBefore)
        {
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

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.appNameTV)
        {
            Mixen.showAbout(this);
        }
        else
        {
            if(Mixen.debugFeaturesEnabled)
            {
                hideControls();
                checkWiFiConfig(v);
            }
            else
            {
                handleButtonClicks(v);
            }

        }
    }
}
