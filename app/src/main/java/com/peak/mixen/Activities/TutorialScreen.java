package com.peak.mixen.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.github.paolorotolo.appintro.AppIntro;
import com.peak.mixen.Fragments.FinalTutorialSlide;
import com.peak.mixen.Fragments.FirstTutorialSlide;
import com.peak.mixen.Fragments.SecondTutorialSlide;
import com.peak.mixen.Fragments.SongQueueFrag;
import com.peak.mixen.Fragments.ThirdTutorialSlide;
import com.peak.mixen.R;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;

public class TutorialScreen extends AppIntro {

    // Please DO NOT override onCreate. Use init
    @Override
    public void init(Bundle savedInstanceState) {

        // Add your slide's fragments here
        // AppIntro will automatically generate the dots indicator and buttons.
        addSlide(new FirstTutorialSlide(), getApplicationContext());
        addSlide(new SecondTutorialSlide(), getApplicationContext());
        addSlide(new ThirdTutorialSlide(), getApplicationContext());
        addSlide(new FinalTutorialSlide(), getApplicationContext());

        // OPTIONAL METHODS
        // Override bar/separator color

        // Hide Skip button
        showSkipButton(false);
    }

    @Override
    public void onBackPressed() {
        // Do nothing, you can't leave.
    }

    @Override
    public void onSkipPressed() {
        // Do something when users tap on Skip button.
    }

    @Override
    public void onDonePressed() {
        Event e = new Event("Viewed Tutorial", true);
        Tapstream.getInstance().fireEvent(e);
        setResult(RESULT_OK);
        this.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tutorial_screen, menu);
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
}
