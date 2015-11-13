package com.camelCaseD.nsettings.enablers;

import com.camelCaseD.nsettings.LocalBluetoothManager;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

public class BluetoothEnabler implements Preference.OnPreferenceChangeListener {
	private final Context mContext;
    private final CheckBoxPreference mCheckBox;
    private final CharSequence mOriginalSummary;
    
    private final LocalBluetoothManager mLocalManager;

    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
		public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            handleStateChanged(state);
        }
    };
    
    public BluetoothEnabler(Context context, CheckBoxPreference checkBox, PreferenceScreen prefScreen) {
        mContext = context;
        mCheckBox = checkBox;
        mOriginalSummary = checkBox.getSummary();
        checkBox.setPersistent(false);

        mLocalManager = LocalBluetoothManager.getInstance(context);
        if (mLocalManager == null) {
            // Bluetooth is not supported
        	prefScreen.removePreference(checkBox);
        }
        
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }
    
    public void resume() {
    	if (mLocalManager == null) {
            return;
        }

        // Bluetooth state is not sticky, so set it manually
        handleStateChanged(mLocalManager.getBluetoothState());
    	
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mCheckBox.setOnPreferenceChangeListener(this);
    }
    
    public void pause() {
    	if (mLocalManager == null) {
            return;
        }
    	
        mContext.unregisterReceiver(mReceiver);
        mCheckBox.setOnPreferenceChangeListener(null);
    }
    
    public boolean onPreferenceChange(Preference preference, Object value) {
    	boolean enable = (Boolean) value;
    	
    	mLocalManager.setBluetoothEnabled(enable);
        mCheckBox.setEnabled(false);

        // Don't update UI to opposite state until we're sure
        return false;
    }
    
    private void handleStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                mCheckBox.setSummary("Turning On");
                mCheckBox.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_ON:
                mCheckBox.setChecked(true);
                mCheckBox.setSummary("Bluetooth On");
                mCheckBox.setEnabled(true);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mCheckBox.setSummary("Turning Off");
                mCheckBox.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_OFF:
                mCheckBox.setChecked(false);
                mCheckBox.setSummary(mOriginalSummary);
                mCheckBox.setEnabled(true);
                break;
            default:
                mCheckBox.setChecked(false);
                mCheckBox.setSummary(mOriginalSummary);
                mCheckBox.setEnabled(true);
        }
    }
}
