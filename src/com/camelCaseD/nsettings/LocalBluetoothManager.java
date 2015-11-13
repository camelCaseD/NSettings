package com.camelCaseD.nsettings;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Config;
import android.util.Log;

public class LocalBluetoothManager {
	private static final String TAG = "LocalBluetoothManager";
	static final boolean V = Config.LOGV;
	
	private BluetoothAdapter mAdapter;
	
	private int mState = BluetoothAdapter.ERROR;
	
	private static LocalBluetoothManager INSTANCE;
	private boolean mInitialized;
	
	public int getBluetoothState() {

		if (mState == BluetoothAdapter.ERROR) {
			syncBluetoothState();
	    }

	    return mState;
	}
	
	public static LocalBluetoothManager getInstance(Context context) {
        synchronized (LocalBluetoothManager.class) {
            if (INSTANCE == null) {
                INSTANCE = new LocalBluetoothManager();
            }

            if (!INSTANCE.init(context)) {
                return null;
            }

            return INSTANCE;
        }
    }
	
	private boolean init(Context context) {
        if (mInitialized) return true;
        mInitialized = true;

        context.getApplicationContext();

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            return false;
        }

        return true;
    }

    private void syncBluetoothState() {
        int bluetoothState;

        if (mAdapter != null) {
            bluetoothState = mAdapter.isEnabled()
                    ? BluetoothAdapter.STATE_ON
                    : BluetoothAdapter.STATE_OFF;
        } else {
            bluetoothState = BluetoothAdapter.ERROR;
        }

        
    }

    public void setBluetoothEnabled(boolean enabled) {
        boolean wasSetStateSuccessful = enabled
                ? mAdapter.enable()
                : mAdapter.disable();

        if (wasSetStateSuccessful) {
            
        } else {
            if (V) {
                Log.v(TAG,
                        "setBluetoothEnabled call, manager didn't return success for enabled: "
                                + enabled);
            }

            syncBluetoothState();
        }
    }
}
