package com.peak.mixen;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.peak.salut.Salut;
import com.peak.salut.SalutBeam;
import com.peak.salut.SalutBeamCallback;
import com.peak.salut.SalutCallback;
import com.peak.salut.SalutDeviceCallback;

import java.util.HashMap;
import java.util.Map;


public class StartScreen extends Activity implements View.OnClickListener{
    public TextView progressBarInfoTV;
    public ProgressBar indeterminateProgress;


    private boolean pressedBefore = false;
    private Intent createNewMixen;
    private MaterialDialog enableWiFiDiag;
    private MaterialDialog wiFiFailureDiag;
    private MaterialDialog findingMixensProgress;
    private MaterialDialog cleanUpDialog;
    private SalutBeam beamHelper;

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
        progressBarInfoTV = (TextView)findViewById(R.id.progressBarInfoTV);
        appNameTV = (TextView) findViewById(R.id.appNameTV);
        textDivider = (TextView) findViewById(R.id.textDivider);
        indeterminateProgress = (ProgressBar)findViewById(R.id.progressBar);

        Mixen.appColors = getResources().getIntArray(R.array.appcolors);
        //TODO Set background to random color using this?

        Mixen.currentContext = getApplicationContext();

        Mixen.sharedPref = getSharedPreferences(Mixen.MIXEN_PREF_FILE, Context.MODE_PRIVATE);

        createDialogs();

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
        boolean isFirstRun;


            isFirstRun = Mixen.sharedPref.getBoolean("FIRST_RUN", true);
            Mixen.username = Mixen.sharedPref.getString("username", "Anonymous");

        return isFirstRun;

    }

    private void createDialogs()
    {
        findingMixensProgress = new MaterialDialog.Builder(this)
                .title("Searching for nearby Mixens...")
                .content("Please wait...")
                .progress(true, 0)
                .build();

        cleanUpDialog = new MaterialDialog.Builder(this)
                .title("Bummer :(")
                .content(R.string.discover_p2p_error)
                .neutralText("Okay")
                .build();

        enableWiFiDiag = new MaterialDialog.Builder(this)
                .title("This is important.")
                .content("Please turn on WiFi first.")
                .build();

        wiFiFailureDiag = new MaterialDialog.Builder(this)
                .title("Bummer :(")
                .content("We had trouble turning on WiFi, please double check your settings.")
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

                if(!isFirstRun())
                {
                    startActivity(createNewMixen);
                }
                else
                {
                    startActivity(createNewMixen);
                }

                restoreControls();
                return;

            case R.id.findMixen: {

                Mixen.isHost = false;

                if (Mixen.debugFeaturesEnabled) {

                    findingMixensProgress.show();

                    Map appData = new HashMap();
                    appData.put("username", null);
                    appData.put("isHost", "FALSE");

                    Mixen.network = new Salut(getApplicationContext(), "Client",  "_mixen", appData);

                    Mixen.network.discoverNetworkServicesWithTimeout(new SalutDeviceCallback() {
                        @Override
                        public void call(Map<String, String> serviceData, WifiP2pDevice foundDevice) {
                            restoreControls();
                            findingMixensProgress.dismiss();
                            startActivity(new Intent(StartScreen.this, MixenBase.class));
                            Toast.makeText(getApplicationContext(), "You're now connected to " + serviceData.get("username") + " 's Mixen.", Toast.LENGTH_SHORT).show();
                        }
                    }, false, new SalutCallback() {
                        @Override
                        public void call() {
                            restoreControls();
                            findingMixensProgress.dismiss();
                            cleanUpDialog.show();
                        }
                    }, 5000);



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

                    return;
                }
            }
        }
    }


    public void checkWiFiConfig(final View v) {

        if(!Salut.hotspotIsEnabled(this))
        {
            if(!Salut.isWiFiEnabled(this))
            {
                enableWiFiDiag.show();
                enableWiFiDiag.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        restoreControls();
                    }
                });
            }
            else
            {
                handleButtonClicks(v);
            }
        }
        else
        {
            wiFiFailureDiag.show();
            restoreControls();
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
    protected void onResume() {
        super.onResume();

        if(Mixen.debugFeaturesEnabled)
        {
            try
            {
                beamHelper = new SalutBeam(this);
                beamHelper.beamPayload("https://plus.google.com/communities/105179969153892633137");
                beamHelper.onBeamRecieved(getIntent(), new SalutBeamCallback() {
                    @Override
                    public void call(String beamData) {
                        Log.d(Mixen.TAG, beamData);
                    }
                });
            }
            catch(Exception ex)
            {
                Log.d(Mixen.TAG, "NFC sharing will not be available.");
            }
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
