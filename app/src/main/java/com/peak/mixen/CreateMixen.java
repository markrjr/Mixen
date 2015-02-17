package com.peak.mixen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;

import com.peak.salut.Salut;

public class CreateMixen extends Activity {

    EditText userNameET;
    TextView alertTV;
    Button createMixen;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_mixen);


        //Setup the UI buttons.
        userNameET = (EditText)findViewById(R.id.userNameET);
        alertTV = (TextView)findViewById(R.id.alertTV);
        createMixen = (Button)findViewById(R.id.startCreateService);
        getActionBar().hide();


        userNameET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {

                //This will handle tapping the "Done" or "Enter" button on the keyboard after entering text.

                boolean handled = false;
                if(actionId == EditorInfo.IME_ACTION_DONE);
                {
                    InputMethodManager inputManager = (InputMethodManager) CreateMixen.this.getSystemService(Context.INPUT_METHOD_SERVICE); // All this ridiculousness to hide the keyboard so that the user can see if an error has occurred in text validation.
                    inputManager.hideSoftInputFromWindow(CreateMixen.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);


                    onBtnClicked(findViewById(R.id.startCreateService)); //To act as though startCreateService was what actually called the method.
                    handled = true;

                }
                return handled;
            }
        });


    }


    public void onBtnClicked(View v)
    {
        switch(v.getId())
        {
            case R.id.startCreateService: {

                if (userNameET.getText().length() != 0 && userNameET.getText().toString().matches("^[a-zA-Z0-9]*$")) {

                    //Make sure the user's input is not null and does not contain spaces and symbols.

                    Log.i(Mixen.TAG, "Creating a Mixen service for: " + userNameET.getText().toString());
                    Mixen.username = userNameET.getText().toString();
                    Intent createNewMixen = new Intent(CreateMixen.this, SongQueue.class);
                    createNewMixen.putExtra("userName", userNameET.getText().toString());
                    this.finish();
                    startActivity(createNewMixen);
                    return;
                }
                else
                {
                    alertTV.setVisibility(View.VISIBLE);
                    return;
                }

            }


        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mixen_stage, menu);
        return true;
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        StartScreen.restoreControls();
    }
}
