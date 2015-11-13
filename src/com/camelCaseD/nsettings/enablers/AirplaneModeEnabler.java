package com.camelCaseD.nsettings.enablers;

import com.camelCaseD.nsettings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;

public class AirplaneModeEnabler implements Preference.OnPreferenceChangeListener {

    private final Context mContext;
    
    private final CheckBoxPreference mCheckBoxPref;

    private static final int EVENT_SERVICE_STATE_CHANGED = 3;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onAirplaneModeChanged();
                    break;
            }
        }
    };
    
    IntentFilter mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
        	  String action = intent.getAction();
        	  if(action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
        		  onAirplaneModeChanged();
        	  }
          }
    };

    public AirplaneModeEnabler(Context context, CheckBoxPreference airplaneModeCheckBoxPreference) {
        
        mContext = context;
        mCheckBoxPref = airplaneModeCheckBoxPreference;
        
        airplaneModeCheckBoxPreference.setPersistent(false);
    }

    public void resume() {
    	mContext.registerReceiver(mReceiver, mIntentFilter);
        
        // This is the widget enabled state, not the preference toggled state
        mCheckBoxPref.setEnabled(true);
        mCheckBoxPref.setChecked(isAirplaneModeOn(mContext));

        mCheckBoxPref.setOnPreferenceChangeListener(this);
    }
    
    public void pause() {
    	mContext.unregisterReceiver(mReceiver);
        mCheckBoxPref.setOnPreferenceChangeListener(null);
    }
    
    public static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void setAirplaneModeOn(boolean enabling) {
        
        mCheckBoxPref.setEnabled(false);
        mCheckBoxPref.setSummary(enabling ? "Airplane Mode Turning On..."
                : "Airplane Mode Turning Off...");
        
        // Change the system setting
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 
                                enabling ? 1 : 0);
        
        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcast(intent);
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     */
    private void onAirplaneModeChanged() {
        mCheckBoxPref.setChecked(isAirplaneModeOn(mContext));
        mCheckBoxPref.setSummary(isAirplaneModeOn(mContext) ? null : 
                mContext.getString(R.string.airplane_mode_summary));            
        mCheckBoxPref.setEnabled(true);
    }
    
    /**
     * Called when someone clicks on the checkbox preference.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {        
            setAirplaneModeOn((Boolean) newValue);
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // update checkbox state based on database value
            onAirplaneModeChanged();
        }
    }

}

