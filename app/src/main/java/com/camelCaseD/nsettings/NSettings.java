package com.camelCaseD.nsettings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class NSettings extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        View prefsB = findViewById(R.id.prefsB);
        prefsB.setOnClickListener(this);
        View aboutB = findViewById(R.id.aboutB);
        aboutB.setOnClickListener(this);
    }

	public void onClick(View v) {
		switch(v.getId()){
		case R.id.aboutB:
			Intent i = new Intent(this, About.class);
			startActivity(i);
			break;
			
		case R.id.prefsB:
			Intent i1 = new Intent(this, Prefs.class);
			startActivity(i1);
			break;
		}
		
	}
}