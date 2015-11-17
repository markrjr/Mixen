package com.peak.mixen.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.peak.mixen.Mixen;
import com.peak.mixen.R;
import com.peak.mixen.Service.MixenPlayerService;
import com.peak.mixen.Service.PlaybackSnapshot;


public class SettingsScreen extends ActionBarActivity implements View.OnClickListener{

    private static boolean hasShownWarning;
    private CheckBox enableAmoled;
    private CheckBox enableCleanOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_screen);
        getSupportActionBar().hide();

        Button resetUserBtn = (Button)findViewById(R.id.reset_username);
        Button resetAppBtn = (Button)findViewById(R.id.reset_app);
        Button logoutUser = (Button)findViewById(R.id.logout);
        enableAmoled = (CheckBox)findViewById(R.id.enable_amoled);
        enableCleanOnly = (CheckBox)findViewById(R.id.enable_clean_only);


        if(Mixen.amoledMode)
        {
            RelativeLayout baseLayout = (RelativeLayout)findViewById(R.id.settingsBase);
            baseLayout.setBackgroundColor(Color.BLACK);
            enableAmoled.setChecked(true);
        }

        setupClickChangeListeners();
        resetUserBtn.setOnClickListener(this);
        resetAppBtn.setOnClickListener(this);
        logoutUser.setOnClickListener(this);
        enableCleanOnly.setClickable(false);

        if(Mixen.network != null && Mixen.network.isRunningAsHost)
        {
            enableCleanOnly.setClickable(true);
            enableCleanOnly.setAlpha(1);
        }

        if(!hasShownWarning)
            showWarningDiag();
    }

    private void setupClickChangeListeners()
    {
        enableCleanOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Only clean songs are now allowed.", Toast.LENGTH_SHORT).show();
                    PlaybackSnapshot.explictAllowed = false;
                    MixenPlayerService.instance.playerServiceSnapshot.updateNetworkPlayer();
                } else {
                    Toast.makeText(getApplicationContext(), "All types of songs are now allowed.", Toast.LENGTH_SHORT).show();
                    PlaybackSnapshot.explictAllowed = true;
                    MixenPlayerService.instance.playerServiceSnapshot.updateNetworkPlayerSettings();
                }
            }
        });

        enableAmoled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    Mixen.amoledMode = true;
                    Mixen.sharedPref.edit().putBoolean("amoledMode", Mixen.amoledMode).apply();
                    showRestartDialog();
                }
                else if(Mixen.amoledMode && !isChecked)
                {
                    Mixen.amoledMode = false;
                    Mixen.sharedPref.edit().putBoolean("amoledMode", Mixen.amoledMode).apply();
                    showRestartDialog();
                }
            }
        });
    }

    private void showRestartDialog()
    {
        new MaterialDialog.Builder(this)
                .title("Warning")
                .content("You must restart the app to apply the requested settings.")
                .neutralText("Okay")
                .show();
    }

    private void showWarningDiag()
    {

        new MaterialDialog.Builder(SettingsScreen.this)
                .title("Warning")
                .content("Some of the settings here are experimental and may require a restart of the app.")
                .neutralText("Okay")
                .show();

        hasShownWarning = true;
    }

    public void clearAppSettings()
    {
        Mixen.sharedPref.edit().clear().apply();
        showRestartDialog();

    }

    private void setUsername()
    {
        new MaterialDialog.Builder(SettingsScreen.this)
                .title("Who are you?")
                .content(R.string.username_overwrite)
                .negativeText("Cancel")
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.username_hint, R.string.blank_string, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                        if (charSequence.length() != 0 && charSequence.toString().matches("^[a-zA-Z0-9]*$")) {

                            Mixen.username = charSequence.toString();

                            SharedPreferences.Editor prefs = Mixen.sharedPref.edit();
                            prefs.putString("username", Mixen.username).apply();
                            prefs.commit();

                            materialDialog.dismiss();
                            showRestartDialog();
                        }
                        else
                        {
                            materialDialog.getContentView().setText(getResources().getString(R.string.username_protocol));
                            materialDialog.getContentView().setTextColor(getResources().getColor(R.color.Radical_Red));
                        }
                    }
                })
                .autoDismiss(false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings_screen, menu);
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
    public void onClick(View view) {

        if(view.getId() == R.id.reset_username)
        {
            setUsername();
        }
        else if(view.getId() == R.id.reset_app)
        {
            clearAppSettings();
        }
        else if(view.getId() == R.id.logout)
        {
            Intent logoutUser = new Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.spotify.com"));
            startActivity(logoutUser);
            //TODO On user logout close app.
        }
    }
}
