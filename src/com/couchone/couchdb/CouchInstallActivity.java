package com.couchone.couchdb;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CouchInstallActivity extends Activity {

	public final static int ERROR = 0;
	public final static int PROGRESS = 1;
	public final static int COMPLETE = 2;

	private ProgressDialog installProgress;
	
	private CouchInstallActivity self = this;

	private Handler pHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			
			case CouchInstallActivity.ERROR:
				final Button installButton = (Button) findViewById(R.id.InstallButton);
				AlertDialog.Builder builder = new AlertDialog.Builder(self);
				builder.setMessage(self.getString(R.string.install_error))
						.setCancelable(false)
						.setPositiveButton("Ok",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										installButton.setEnabled(true);
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
				break;

			case CouchInstallActivity.PROGRESS:
				installProgress.setMessage("Unpacking files... " + msg.arg1);
				break;

			case CouchInstallActivity.COMPLETE:
				installProgress.dismiss();
				Log.v(CouchProcess.TAG, "Launching Couchdb activity");
				startActivity(new Intent(getApplicationContext(), CouchFutonActivity.class));
				break;
			}
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.install);
		final Context currentCtx = this;
		final Button installButton = (Button) findViewById(R.id.InstallButton);
		installButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				installButton.setEnabled(false);
				installProgress = ProgressDialog
						.show(currentCtx, "Installing CouchDB",
								"Unpacking files...", true, false);
				new Thread() {
					public void run() {
						startInstall();
					}
				}.start();

			}
		});
	}

	public void startInstall() { 
		try {
			CouchInstaller.doInstall(pHandler);
		} catch (Exception e) {
			e.printStackTrace();
			installProgress.dismiss();
			pHandler.sendMessage(pHandler.obtainMessage(CouchInstallActivity.ERROR));
		}		
	}	
}
