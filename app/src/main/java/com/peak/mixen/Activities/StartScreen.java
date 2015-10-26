package com.peak.mixen.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.parse.Parse;
import com.parse.ParseObject;
import com.peak.mixen.Mixen;
import com.peak.mixen.Service.MixenPlayerService;
import com.peak.mixen.R;
import com.peak.mixen.Utils.ActivityAnimator;
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
    private MaterialDialog explanationDiag;
    public boolean isActuallyFirstRun;
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

        createNewMixen = new Intent(StartScreen.this, MixenBase.class);

        createDialogs();

        checkifFirstRun();

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            new MaterialDialog.Builder(getApplicationContext())
                    .title("Larger Device Detected")
                    .content("It looks like you're using Mixen on a screen or device with a large display or large resolution. The app's interface may look A LOT different.")
                    .neutralText("Okay");
        }

        if(Mixen.amoledMode)
        {
            RelativeLayout baseLayout = (RelativeLayout)findViewById(R.id.startScreenBase);
            baseLayout.setBackgroundColor(Color.BLACK);
        }

        createMixen.setOnClickListener(this);
        findMixen.setOnClickListener(this);
        appNameTV.setOnClickListener(this);

        Parse.initialize(this, Mixen.getParseSecret(), Mixen.getParseKey());

        instance = this;
    }

    private void checkifFirstRun()
    {
        isActuallyFirstRun = true;

        Mixen.username = Mixen.sharedPref.getString("username", "Anonymous");
        Mixen.hasSeenTutorial = Mixen.sharedPref.getBoolean("hasSeenTutorial", false);
        Mixen.amoledMode = Mixen.sharedPref.getBoolean("amoledMode", false);

        if(!Mixen.username.equals("Anonymous"))
            isActuallyFirstRun = false;
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
        Config playerConfig = new Config(this, Mixen.spotifyToken, Mixen.getClientId());
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
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(Mixen.getClientId(),
                AuthenticationResponse.Type.TOKEN,
                Mixen.getRedirectUri());
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        builder.setShowDialog(true);
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginInBrowser(this, request);
    }

    private void createDialogs()
    {

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

        explanationDiag = new MaterialDialog.Builder(this)
                .title("Spotify Premium")
                .content("Creating a Mixen requires a Spotify premium account, joining one does not. Do you have Spotify Premium?")
                .positiveText("Yes")
                .negativeText("No")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        createMixen();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        restoreControls();
                    }
                })
                .build();
    }

    private void createMixen()
    {
        indeterminateProgressDiag.show();
        //This needs to be post delayed because the MixenPlayerService hasn't been initialized on the initial called of this function.
        //TODO Fix.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                hideControls();

                if (!hasSpotifyToken())
                    authenticateSpotifyUser();
                else {
                    Log.d(Mixen.TAG, "Token already established.");
                    startMixen();
                }

            }
        }, 2000);

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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        if (uri != null)
        {
            AuthenticationResponse response = AuthenticationResponse.fromUri(uri);

            switch (response.getType()) {
                // Response was successful and contains auth token.
                case TOKEN:
                    Mixen.spotifyToken = response.getAccessToken();
                    Mixen.spotifyTokenExpiry = System.currentTimeMillis();
                    Mixen.sharedPref.edit().putString("SESSION_TOKEN", Mixen.spotifyToken).apply();
                    Mixen.sharedPref.edit().putLong("SESSION_TOKEN_EXPIRE", Mixen.spotifyTokenExpiry).apply();
                    startMixen();
                    return;

                case ERROR:
                    showSpotifyErrorDiag();
                    return;

                default:
                    restoreControls();
                    return;
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
    protected void onResume() {
        super.onResume();
        if(getIntent() != null && getIntent().getData() == null && indeterminateProgressDiag.isShowing())
        {
            indeterminateProgressDiag.dismiss();
            //User canceled auth flow. Am I doing this wrong if I manually have to detect this?
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        if(MixenPlayerService.instance != null)
//        {
//            stopService(new Intent(this, MixenPlayerService.class));
//        }
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

        startService(new Intent(StartScreen.instance, MixenPlayerService.class).setAction(MixenPlayerService.init));

        if(v.getId() == R.id.appNameTV)
        {
            Mixen.showAbout(this);
        }
        else if(v.getId() == R.id.findMixen)
        {
            Mixen.isHost = false;
            startActivity(createNewMixen);
            new ActivityAnimator().fadeAnimation(this);
        }
        else if(v.getId() == R.id.createMixenButton)
        {
            Mixen.isHost = true;
            if(isActuallyFirstRun)
            {
                explanationDiag.show();
            }
            else{
                createMixen();
            }
        }
    }
}
