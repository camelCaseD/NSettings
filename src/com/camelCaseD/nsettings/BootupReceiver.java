package com.camelCaseD.nsettings;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class BootupReceiver extends BroadcastReceiver {

	private static final boolean BOOTUP_TRUE = true;
	private static final String BOOTUP_KEY = "bootup";
	private static final boolean ICON_TRUE = true;
	private static final String ICON_KEY = "icon";

	@Override
	public void onReceive(Context context, Intent intent) {
		
		if(getBootup(context)) {
			if(getIcon(context)){
			NotificationManager NotifyM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			Notification Notify = new Notification(R.drawable.n,
				    "NSettings Enabled", System.currentTimeMillis());
			
			Notify.flags |= Notification.FLAG_NO_CLEAR;
			Notify.flags |= Notification.FLAG_ONGOING_EVENT;
			
			RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);
			Notify.contentView = contentView;
			
			Intent notificationIntent = new Intent(context, Toggles.class);
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
			Notify.contentIntent = contentIntent;
			
			NotifyM.notify(101296, Notify);
			}else{
				NotificationManager NotifyM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification Notify = new Notification(R.drawable.blank_32_x_32,
					    "NSettings Enabled", System.currentTimeMillis());
				
				Notify.flags |= Notification.FLAG_NO_CLEAR;
				Notify.flags |= Notification.FLAG_ONGOING_EVENT;
				
				RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);
				Notify.contentView = contentView;
				
				Intent notificationIntent = new Intent(context, Toggles.class);
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
				Notify.contentIntent = contentIntent;
				
				NotifyM.notify(101296, Notify);
			}
		}else{
			
		}
		
		Intent serviceIntent = new Intent();
		serviceIntent.setAction("com.brandon.labs.nsettings.NotifyService");
		context.startService(serviceIntent);
	}

	public static boolean getBootup(Context context){
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(BOOTUP_KEY, BOOTUP_TRUE);
	}
	
	public static boolean getIcon(Context context){
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(ICON_KEY, ICON_TRUE);
	}
	
}
