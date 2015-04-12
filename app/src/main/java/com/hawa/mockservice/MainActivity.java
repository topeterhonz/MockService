package com.hawa.mockservice;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends Activity {

    private boolean mIsServiceOn;

    @InjectView(R.id.service_switch_button)
    Button mServiceSwitchButton;

    @OnClick(R.id.service_switch_button)
    void onSwitchClicked() {
        mIsServiceOn = !mIsServiceOn;

        Intent intent = new Intent(this, MockService.class);
        if (mIsServiceOn) {
            intent.putExtra("Param", "Start");
            startService(intent);
            mServiceSwitchButton.setText("Service Started!");
        } else {
            stopService(intent);
            mServiceSwitchButton.setText("Service Stopped!");
        }
    }

    @OnClick(R.id.send_service_message_button)
    void onSendServiceClicked() {
        Intent intent = new Intent(this, MockService.class);
        intent.putExtra("Param", "Bypass");
        startService(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
    }

}
