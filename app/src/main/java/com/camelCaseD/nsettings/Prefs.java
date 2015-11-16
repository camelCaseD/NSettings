package com.camelCaseD.nsettings;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.content.Context;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;

public class Prefs extends PreferenceActivity {
	private static final String ENABLE_KEY = "enable";
	private static final boolean ENABLE_TRUE = true;
	private static final String ICON_KEY = "icon";
	private static final boolean ICON_TRUE = true;
	private static final String BOOTUP_KEY = "bootup";
	private static final boolean BOOTUP_TRUE = true;
	public static boolean BSTATE = false;
	public CheckBoxPreference bootup = (CheckBoxPreference) findPreference(BOOTUP_KEY);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
		
		Preference enable = findPreference(ENABLE_KEY);
        enable.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				if (getEnable(getBaseContext())) {
					notifyNS("NSettings Enabled", R.drawable.blank_32_x_32);
				} else {
					if (getIcon(getBaseContext())) {
						setCheckedNS("icon", false);
					}

					if (getBootup(getBaseContext())) {
						setCheckedNS("bootup", false);
					}

					cancelNS();
				}

				return true;
			}
		});
        
        Preference icon = findPreference(ICON_KEY);
        icon.setOnPreferenceClickListener(new OnPreferenceClickListener(){
        						public boolean onPreferenceClick(Preference preference){
			if (getIcon(getBaseContext())) {
				notifyNS("Icon Changed", R.drawable.n);
			} else {
				notifyNS("Icon Changed", R.drawable.blank_32_x_32);
			}

			return true;
				}

		});
        
        Preference bootup = findPreference(BOOTUP_KEY);
        
        bootup.setOnPreferenceClickListener(new OnPreferenceClickListener(){
							public boolean onPreferenceClick(Preference preference) {
				if(getBootup(getBaseContext())){
					BSTATE = true;
					Log.i("Prefs", "bState: " + BSTATE);
				}else{
					BSTATE = false;
					Log.i("Prefs", "bState: " + BSTATE);
				}
				return true;
			}
		});
	}
	
	public void setCheckedNS(String string, boolean b) {
		CheckBoxPreference method = (CheckBoxPreference) findPreference(string);
		
		method.setChecked(b);
	}
	
	
	private void cancelNS() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		
		mNotificationManager.cancel(101296);
		
		Context context2 = getApplicationContext();
		CharSequence text = "NSettings Disabled";
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context2, text, duration);
		toast.show();
	}

	public void notifyNS(String toast, int icona) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		Intent notificationIntent = new Intent(this, Toggles.class);
		PendingIntent contentIntent = PendingIntent.getActivity(
		    this, 0, notificationIntent, 0);

		Notification notification = new Notification(icona,
		    "NSettings Enabled", System.currentTimeMillis());
		
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		RemoteViews contentView = new RemoteViews(getPackageName(),
		    R.layout.notification);

		notification.contentView   = contentView;
		notification.contentIntent = contentIntent;		
		int HELLO_ID = 101296;

		mNotificationManager.notify(HELLO_ID, notification);
		
		toastNS(toast);
	}
	
	private void toastNS(String string) {
		Context context = getApplicationContext();
		CharSequence text = string;
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.prefs, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.homeM:
				startActivity(new Intent(this, NSettings.class));
				return true;
		}

		return false;
	}
	
	public static boolean getEnable(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(ENABLE_KEY, ENABLE_TRUE);
	}
	
	public static boolean getIcon(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(ICON_KEY, ICON_TRUE);
	}
	
	public static boolean getBootup(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(BOOTUP_KEY, BOOTUP_TRUE);
	}
        
	private void mainTogglesPref(Context context) {
		int checked = 0;

		if (checked == 0) {
			if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wifiMT", true)) {
				checked = 1;
			} else if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("airplaneMT", true)) {
				checked = 1;
			} else if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("bluetoothMT", true)) {
				checked = 1;
			} else if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("gpsMT", true)) {
				checked = 1;
			} else if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ringerModeMT", true)) {
				checked = 1;
			} else if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("vibrateMT", true)) {
				checked = 1;
			}
		}
	}
}
