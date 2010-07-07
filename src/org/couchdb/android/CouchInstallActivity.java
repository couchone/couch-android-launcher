package org.couchdb.android;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CouchInstallActivity extends Activity {
	private ProgressDialog installProgress;
	
	private Handler pHandler = new Handler() {
		@Override
        public void handleMessage(Message msg) {
			installProgress.setMessage("Unpacking files... " + msg.arg1);
			if(msg.arg2 == 1)
			{
				installProgress.dismiss();
				startActivity(new Intent(getApplicationContext(), CouchDB.class));
			}
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.install);
		final Button installButton = (Button) findViewById(R.id.InstallButton);
		final Context currentCtx = this;
		installButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				installButton.setEnabled(false);
				installProgress = ProgressDialog.show(currentCtx, "Installing CouchDB", "Unpacking files...", true, false);
				new Thread() {
					public void run()
					{
						try {
							CouchInstaller.doInstall(pHandler);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}.start();

			}
		});
	}
}
