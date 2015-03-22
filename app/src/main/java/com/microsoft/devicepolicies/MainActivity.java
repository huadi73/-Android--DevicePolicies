package com.microsoft.devicepolicies;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ToggleButton;


public class MainActivity extends ActionBarActivity {

    public int REQUEST_ENABLE = 1001;
    ToggleButton btnCamera;

    private CheckBoxPreference mDisableCameraCheckbox;
    DevicePolicyManager mDPM;
    ComponentName mDeviceAdminSample;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDeviceAdminSample = new ComponentName(this, MyDeviceAdminReceiver.class);

        if (!mDPM.isAdminActive(mDeviceAdminSample)) {
            // try to become active – must happen here in this activity, to get result
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdminSample);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "1234856");
            startActivityForResult(intent, REQUEST_ENABLE);
        } else {
            // Already is a device administrator, can do security operations now.
            mDPM.setCameraDisabled(mDeviceAdminSample, true);
        }

        btnCamera = (ToggleButton) findViewById(R.id.toggle_device_admin);
        btnCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mDPM.setCameraDisabled(mDeviceAdminSample, true);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQUEST_ENABLE == requestCode)
        {
            if (resultCode == Activity.RESULT_OK) {
                // Has become the device administrator.
                mDPM.setCameraDisabled(mDeviceAdminSample, true);
            } else {
                //Canceled or failed.
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
