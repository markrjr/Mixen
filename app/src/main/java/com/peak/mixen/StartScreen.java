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
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.peak.salut.Salut;
import com.peak.salut.Callbacks.SalutCallback;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;


public class StartScreen extends Activity implements View.OnClickListener{
    private boolean pressedBefore = false;
    public static Intent createNewMixen;
    public static StartScreen instance;
    public MaterialDialog indeterminateProgressDiag;
    private MaterialDialog wiFiFailureDiag;
    private MaterialDialog findingMixensProgress;
    private MaterialDialog cleanUpDialog;
    private MaterialDialog foundMixensDialog;
    private boolean isActuallyFirstRun;
    public static boolean hasSpotifyToken;

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

        startService(new Intent(this, MixenPlayerService.class).setAction(MixenPlayerService.init));

        createMixen.setOnClickListener(this);
        findMixen.setOnClickListener(this);
        appNameTV.setOnClickListener(this);
        instance = this;
    }

    private boolean isFirstRun()
    {
        isActuallyFirstRun = true;

        Mixen.username = Mixen.sharedPref.getString("username", "Anonymous");

        if(!Mixen.username.equals("Anonymous"))
            isActuallyFirstRun = false;

        return isActuallyFirstRun;
    }

    public static boolean hasSpotifyToken()
    {
        Mixen.spotifyToken = Mixen.sharedPref.getString("SESSION_TOKEN", "NotYetAuthenticated");
        Mixen.spotifyTokenExpiry = Mixen.sharedPref.getLong("SESSION_TOKEN_EXPIRE", 0);

        if(!Mixen.spotifyToken.equals("NotYetAuthenticated") && Mixen.spotifyTokenExpiry != 0)
        {
            if(System.currentTimeMillis() > Mixen.spotifyTokenExpiry + 3600000) //Check if an hour has elapsed.
            {
                Log.d(Mixen.TAG, "Session token has expired.");
                hasSpotifyToken = false;
            }
            else
            {
                hasSpotifyToken = true;
            }
        }



        return hasSpotifyToken;

    }

    public void startMixen()
    {
        Config playerConfig = new Config(this, Mixen.spotifyToken, Mixen.CLIENT_ID);
        MixenPlayerService.instance.spotifyPlayer = Spotify.getPlayer(playerConfig, MixenPlayerService.instance, new Player.InitializationObserver() {
            @Override
            public void onInitialized(Player player) {
                player.addConnectionStateCallback(MixenPlayerService.instance);
                player.addPlayerNotificationCallback(MixenPlayerService.instance);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(Mixen.TAG, "An error occurred while attempting to start some part of the Spotify framework. " + throwable.getMessage());
                showSpotifyErrorDiag();
            }
        });

    }

    private void authenticateSpotifyUser()
    {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(Mixen.CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                Mixen.REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, Mixen.MIXEN_SERVICE_PORT, request);
    }

    private void createDialogs()
    {
        findingMixensProgress = new MaterialDialog.Builder(this)
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

        indeterminateProgressDiag = new MaterialDialog.Builder(this)
                .title("Just a sec...")
                .content("We're setting up a few things.")
                .progress(true, 0)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        restoreControls();
                    }
                })
                .cancelable(false)
                .build();

        wiFiFailureDiag = new MaterialDialog.Builder(this)
                .title("Bummer :(")
                .content("We had trouble checking if WiFi was on, please double check your settings.")
                .neutralText("Okay")
                .build();
    }
    public void clearAppSettings()
    {
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
                startService(new Intent(this, MixenPlayerService.class).setAction(MixenPlayerService.init));

                hideControls();

                if(!hasSpotifyToken())
                    authenticateSpotifyUser();
                else
                {
                    Log.d(Mixen.TAG, "Token already established.");
                    startMixen();
                }

                return;

            case R.id.findMixen: {

                Mixen.isHost = false;

                    findingMixensProgress.show();
                    findingMixensProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            restoreControls();
                        }
                    });

                    if(isActuallyFirstRun) {
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

                            if (Mixen.network.foundHostServices.size() == 1) {

                                Mixen.network.connectToHostService(Mixen.network.foundHostServices.get(0), new SalutCallback() {
                                    @Override
                                    public void call() {
                                        StartScreen.this.startActivity(createNewMixen);
                                        Toast.makeText(getApplicationContext(), "You're now connected to " + Mixen.network.foundHostServices.get(0).readableName + "'s Mixen.", Toast.LENGTH_LONG).show();
                                    }
                                }, new SalutCallback() {
                                    @Override
                                    public void call() {
                                        cleanUpDialog.setContent("We had a problem connection to " + Mixen.network.foundHostServices.get(0).readableName + "'s Mixen. Please try again momentarily.");
                                        cleanUpDialog.show();
                                    }
                                });
                            } else {

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
                return;
            }
        }
    }

    public void showSpotifyErrorDiag()
    {
        new MaterialDialog.Builder(this)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        restoreControls();
                    }
                })
                .neutralText("Okay")
                .title("Bummer :(")
                .content(R.string.generic_network_error)
                .build()
                .show();
    }


    public void checkWiFiConfig(final View v) {

        if(!Salut.isWiFiEnabled(this))
        {
            //SalutP2P.enableWiFi(getApplicationContext());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    handleButtonClicks(v);
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
        indeterminateProgressDiag.dismiss();

    }

    public void hideControls()
    {
        findMixen.setVisibility(View.INVISIBLE);
        textDivider.setVisibility(View.INVISIBLE);
        createMixen.setVisibility(View.INVISIBLE);
        indeterminateProgressDiag.show();
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
            super.onBackPressed();
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

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.appNameTV)
        {
            Mixen.showAbout(this);
        }
        else
        {
            hideControls();
            checkWiFiConfig(v);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Mixen.MIXEN_SERVICE_PORT) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);

            switch (response.getType()) {
                // Response was successful and contains auth token.
                case TOKEN:
                    Mixen.spotifyToken = response.getAccessToken();
                    Mixen.spotifyTokenExpiry = System.currentTimeMillis();
                    Mixen.sharedPref.edit().putString("SESSION_TOKEN", Mixen.spotifyToken).apply();
                    Mixen.sharedPref.edit().putLong("SESSION_TOKEN_EXPIRE", Mixen.spotifyTokenExpiry).apply();
                    startMixen();
                    break;

                default:
                    showSpotifyErrorDiag();
                    break;
            }
        }
    }
}
