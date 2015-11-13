package com.camelCaseD.nsettings;

import java.util.Observable;
import java.util.Observer;

import com.camelCaseD.nsettings.enablers.AirplaneModeEnabler;
import com.camelCaseD.nsettings.enablers.BluetoothEnabler;
import com.camelCaseD.nsettings.enablers.WifiEnabler;

import customPrefs.SeekBarPreference;

import android.content.BroadcastReceiver;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.NameValueTable;
import android.provider.Settings.System;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Toggles extends PreferenceActivity implements Runnable {
	private BluetoothEnabler mBtEnabler;
	private WifiEnabler mWifiEnabler;
	private AirplaneModeEnabler mAirplaneModeEnabler;
	public CheckBoxPreference mGps;
	public CheckBoxPreference mRingerMode;
	public AudioManager mAudioManager;
	public CheckBoxPreference mVibrate;
	public CheckBoxPreference mAutoBright;
	private SeekBarPreference ringerVol;
	private SeekBarPreference mediaVol;
	private SeekBarPreference screenBright;
	private float backlightValue = 0.5f;
	private final Handler mRingerHandler = new Handler();
	private int mLastProgress;
	private Context mCxt;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                updateState(false);
            }
        }
    };
    
    private ContentQueryMap mContentQueryMap;
    private final class SettingsObserver implements Observer {
        public void update(Observable o, Object arg) {
            updateToggles();
        }
    }
    
    private final ContentObserver mRingerVolumeObserver = new ContentObserver(mRingerHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
//            if (ringerVol != null) {
                Log.i("co", "co: active");
                int volume = System.getInt(mCxt.getContentResolver(), System.VOLUME_SETTINGS[AudioManager.STREAM_RING], -1);
                // Works around an atomicity problem with volume updates
                // TODO: Fix the actual issue, probably in AudioService
                if (volume >= 0) {
                	Log.i("co", "if: active");
                	Log.i("co", ""+volume);
                    ringerVol.setProgress(volume);
                }
//            }
        }
    };

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.toggles);
        CheckBoxPreference bt = (CheckBoxPreference) findPreference("bluetooth");
        CheckBoxPreference wifi = (CheckBoxPreference) findPreference("wifi");
        CheckBoxPreference airplane = (CheckBoxPreference) findPreference("airplane");
        mRingerMode = (CheckBoxPreference) findPreference("ringerMode");
        mGps = (CheckBoxPreference) findPreference("gps");
        mVibrate = (CheckBoxPreference) findPreference("vibrate");
        mAutoBright = (CheckBoxPreference) findPreference("autoScreen");
        PreferenceScreen prefScreen = getPreferenceScreen();
        
        mBtEnabler = new BluetoothEnabler(this, bt, prefScreen);
        mWifiEnabler = new WifiEnabler(this, wifi);
        mAirplaneModeEnabler = new AirplaneModeEnabler(this, airplane);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				
        updateToggles();
        
     // listen for Location Manager settings changes
        Cursor settingsCursor = getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + NameValueTable.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, NameValueTable.NAME, true, null);
        mContentQueryMap.addObserver(new SettingsObserver());
        
        if(!canToggleGPS()) {
        	getPreferenceScreen().removePreference(mGps);
        }
        
        //Define seekBarPreferences
        ringerVol = new SeekBarPreference(this);
        mediaVol = new SeekBarPreference(this);
        screenBright = new SeekBarPreference(this);
        
        ringerVol.setTitle("Ringer Volume");
        mediaVol.setTitle("Media Volume");
        screenBright.setTitle("Screen Brightness");
        
        ringerVol.setSummary("Changes the ringer volume.");
        mediaVol.setSummary("Changes the media volume which is the channel used for videos and music.");
        screenBright.setSummary("Changes the screen brightness levels");
        
        ringerVol.setKey("ringerVol");
        mediaVol.setKey("mediaVol");
        screenBright.setKey("screenBright");
        
        Log.i("ASR", ""+mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        
        ringerVol.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        mediaVol.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        screenBright.setMax(100);
        
        ringerVol.setKeyIncrementProgress(1);
        mediaVol.setKeyIncrementProgress(1);
        screenBright.setKeyIncrementProgress(1);
        
        ringerVol.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_RING));
        mediaVol.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        
        //Define Preference Categories for reference
        PreferenceCategory audCat = (PreferenceCategory) findPreference("audCat");
        PreferenceCategory displayCat = (PreferenceCategory) findPreference("displayCat");
        
        //Add SeekBarPreferences to categories
        audCat.addPreference(ringerVol);
        audCat.addPreference(mediaVol);
        
        postSetVolume(ringerVol.getProgress());
        
        //SeekBarPreferences change listeners
        ringerVol.setOnProgressChangeListener(new OnSeekBarChangeListener() {
			  
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)  
            {  
                //change the volume, displaying a toast message containing the current volume and playing a feedback sound  
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, progress, AudioManager.FLAG_SHOW_UI + AudioManager.FLAG_PLAY_SOUND);  
            }

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        mediaVol.setOnProgressChangeListener(new OnSeekBarChangeListener() {
			  
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)  
            {  
            	mAudioManager.setStreamVolume(AudioManager.STREAM_RING, progress, AudioManager.FLAG_SHOW_UI + AudioManager.FLAG_PLAY_SOUND);
            }

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        screenBright.setOnProgressChangeListener(new OnSeekBarChangeListener() {
			  
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)  
            {  
                backlightValue = (float) progress/100;
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = backlightValue;
                getWindow().setAttributes(layoutParams);
                
                int sysBacklightValue = (int)(backlightValue * 255);
                android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, sysBacklightValue);
            }

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        
        displayCat.addPreference(screenBright);
        this.getContentResolver().registerContentObserver(System.getUriFor(System.VOLUME_SETTINGS[AudioManager.STREAM_RING]), false, mRingerVolumeObserver);
        mCxt = this;
}
	
	@Override
    protected void onResume() {
        super.onResume();
        
        mWifiEnabler.resume();
        mBtEnabler.resume();
        mAirplaneModeEnabler.resume();
        updateState(false);
        
        IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        
        //set seekBarPreference progress values
        ringerVol.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_RING));
        mediaVol.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        this.getContentResolver().registerContentObserver(System.getUriFor(System.VOLUME_SETTINGS[2]), false, mRingerVolumeObserver);
    }

	@Override
    protected void onPause() {
        super.onPause();

        mWifiEnabler.pause();
        mBtEnabler.pause();
        mAirplaneModeEnabler.pause();
        
        unregisterReceiver(mReceiver);
        this.getContentResolver().unregisterContentObserver(mRingerVolumeObserver);
    }
    
    private void turnGPSOn(){
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if(!provider.contains("gps")){ //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider"); 
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3")); 
            sendBroadcast(poke);
        }
    }

    private void turnGPSOff(){
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if(provider.contains("gps")){ //if gps is enabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3")); 
            sendBroadcast(poke);
        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mGps) {
            boolean enabled = mGps.isChecked();
            if(canToggleGPS()){
            	if(enabled) {
            		CheckBoxPreference airplane = (CheckBoxPreference) findPreference("airplane");
            		if(!airplane.isChecked()){
            			turnGPSOn();
            			mGps.setSummary("GPS On");
            		} else {
            			mGps.setChecked(false);
            		}
            	}else{
            		turnGPSOff();
            		mGps.setSummary("GPS Off");
            	}
            }
        }else if(preference == mRingerMode) {
        	boolean enabled = mRingerMode.isChecked();
        	if(enabled) {
        		mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            	mRingerMode.setSummary("Phone is Now Silent");
        	}else {
        		mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        		mRingerMode.setSummary("Phone is Now Audible");
        		
        		if(mVibrate.isChecked()) {
        			mVibrate.setChecked(false);
        			mVibrate.setSummary("Phone Will Not Vibrate");
        		}
        	}
        }else if(preference == mVibrate) {
        	boolean enabled = mVibrate.isChecked();
        	if(enabled) {
        		setPhoneVibrateSettingValue(enabled);
        		mVibrate.setSummary("Phone Will Now Vibrate");
        	}else {
        		setPhoneVibrateSettingValue(enabled);
        		mVibrate.setSummary("Phone Will Not Vibrate");
        	}
        }else if(preference == mAutoBright) {
        	boolean enabled = mAutoBright.isChecked();
        	if(enabled) {
        		 Settings.System.putInt(getBaseContext().getContentResolver(),
        	                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        		 mAutoBright.setSummary("Will now automatically detect screen brightness");
        	} else{
        		 Settings.System.putInt(getBaseContext().getContentResolver(),
        	                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        		 mAutoBright.setSummary("You can now manually change the screen brightness");
        	}
        }

        return false;
    }
    
    private boolean canToggleGPS() {
        PackageManager pacman = getPackageManager();
        PackageInfo pacInfo = null;

        try {
            pacInfo = pacman.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS);
        } catch (NameNotFoundException e) {
            return false; //package not found
        }

        if(pacInfo != null){
            for(ActivityInfo actInfo : pacInfo.receivers){
                //test if recevier is exported. if so, we can toggle GPS.
                if(actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported){
                    return true;
                }
            }
        }

        return false; //default
    }
    
    private void setPhoneVibrateSettingValue(boolean vibrate) {
        boolean vibeInSilent;
        int callsVibrateSetting;

        if (vibrate) {
        	callsVibrateSetting = AudioManager.VIBRATE_SETTING_ON;
            vibeInSilent = true;
        } else {
            callsVibrateSetting = AudioManager.VIBRATE_SETTING_OFF;
            vibeInSilent = false;
        }
        
        // might need to switch the ringer mode from one kind of "silent" to
        // another
        if (mRingerMode.isChecked()) {
            mAudioManager.setRingerMode(
                vibeInSilent ? AudioManager.RINGER_MODE_VIBRATE
                             : AudioManager.RINGER_MODE_SILENT);
        }

        mAudioManager.setVibrateSetting(
            AudioManager.VIBRATE_TYPE_RINGER,
            callsVibrateSetting);
    }
    
    private void updateState(boolean force) {
    	final int ringerMode = mAudioManager.getRingerMode();

        // NB: in the UI we now simply call this "silent mode". A separate
        // setting controls whether we're in RINGER_MODE_SILENT or
        // RINGER_MODE_VIBRATE.
        final boolean silentOrVibrateMode =
                ringerMode != AudioManager.RINGER_MODE_NORMAL;

        if (silentOrVibrateMode != mRingerMode.isChecked() || force) {
            mRingerMode.setChecked(silentOrVibrateMode);
            mVibrate.setChecked(silentOrVibrateMode);
            if(silentOrVibrateMode) {
            	mRingerMode.setSummary("Phone is Now Silent");
            	mVibrate.setSummary("Phone Will Now Vibrate");
            	ringerVol.setEnabled(false);
            	ringerVol.setProgress(0);
            }else {
            	mRingerMode.setSummary("Phone is Now Audible");
            	mVibrate.setSummary("Phone Will Not Vibrate");
            	ringerVol.setEnabled(true);
            	ringerVol.setProgress(1);
            }
        }
        
        boolean phoneVibrateSetting = getPhoneVibrateSettingValue();

        if (phoneVibrateSetting == mVibrate.isChecked() || force) {
            mVibrate.setChecked(phoneVibrateSetting);
        }
    }

	private boolean getPhoneVibrateSettingValue() {
		// Control phone vibe independent of silent mode
        int vibeSetting = 
            mAudioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
        
        boolean vibeState = false;
        
        if(vibeSetting == AudioManager.VIBRATE_SETTING_ON) {
        	vibeState = true;
        }else if(vibeSetting == AudioManager.VIBRATE_SETTING_OFF) {
        	vibeState = false;
        }
		return vibeState;
	}
    
    private void updateToggles() {
        ContentResolver res = getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        
        if(gpsEnabled){
        	mGps.setChecked(true);
        	mGps.setSummary("GPS On");
        }else {
        	mGps.setChecked(false);
        	mGps.setSummary("GPS Off");
        }
    }
    
    void postSetVolume(int progress) {
        // Do the volume changing separately to give responsive UI
        mLastProgress = progress;
        mRingerHandler.removeCallbacks(this);
        mRingerHandler.post(this);
    }
    
//    @Override  
//    public boolean onKeyDown(int keyCode, KeyEvent event)  
//    {  
//    	boolean keyDown = event.getAction() == KeyEvent.ACTION_DOWN;
////    	SeekBarPreference ringerVolS = (SeekBarPreference) findPreference("ringerVol");
////    	switch(keyCode){
////    	case KeyEvent.KEYCODE_VOLUME_DOWN:
////    		if(keyDown){
////    			ringerVolS.incrementProgressBy(-1);
////    			postSetVolume(ringerVol.getProgress());
////    		}
////    		return true;
////    	case KeyEvent.KEYCODE_VOLUME_UP:
////    		if(keyDown){
////    			ringerVolS.incrementProgressBy(1);
////    			postSetVolume(ringerVol.getProgress());
////    		}
////    		return true;
////    	default:
////    		return false;
////    	}
//    	//if one of the volume keys were pressed  
//        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)  
//        {  
//        	if(event.getAction() == KeyEvent.ACTION_DOWN) {
//            //change the seek bar progress indicator position 
//        	ringerVol.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_RING));
//        	Log.i("ASRC", ""+mAudioManager.getStreamVolume(AudioManager.STREAM_RING));
//        	Log.i("RVC", ""+ringerVol.getProgress());
////        	if(mAudioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
////        		ringerVol.setProgress(0);
////        		mRingerMode.setChecked(true);
////        		mRingerMode.setSummary("Phone is Now Silent");
////        		if(getPhoneVibrateSettingValue()) {
////        			mVibrate.setChecked(true);
////        			mVibrate.setSummary("Phone Will Now Vibrate");
////        		}else {
////        			mVibrate.setChecked(false);
////        			mVibrate.setSummary("Phone Will Not Vibrate");
////        		}
////        	}
//        	}
//        }  
//        //propagate the key event  
//        return super.onKeyDown(keyCode, event);  
//    }

	public void run() {
		mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mLastProgress, AudioManager.FLAG_SHOW_UI + AudioManager.FLAG_PLAY_SOUND);
	}
}