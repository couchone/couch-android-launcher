package org.couchdb.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CouchDB extends Activity {
	public final static String TAG = "CouchDB";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        boolean installed = CouchInstaller.checkInstalled();
        if(!installed)
        	startActivity(new Intent(this, CouchInstallActivity.class));
        final Button startButton = (Button) findViewById(R.id.StartButton);
        final Button stopButton = (Button) findViewById(R.id.StopButton);
        startButton.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				startService(new Intent(getApplicationContext(), CouchService.class));
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
			}
        	
        });
        
        stopButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				stopService(new Intent(getApplicationContext(), CouchService.class));
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
			}   	
        });
	}
}