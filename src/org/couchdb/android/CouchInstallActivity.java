package org.couchdb.android;

import java.io.IOException;
import java.net.UnknownHostException;

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

	private Handler pHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CouchInstallActivity.ERROR:
				final Button installButton = (Button) findViewById(R.id.InstallButton);
				InstallError err = (InstallError) msg.obj;
				AlertDialog.Builder builder = new AlertDialog.Builder(
						CouchInstallActivity.this);
				builder.setMessage(err.description)
						.setCancelable(false)
						.setPositiveButton("Ok",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
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
				Log.v(CouchDB.TAG, "Launching Couchdb activity");
				startActivity(new Intent(getApplicationContext(), CouchDB.class));
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
						try {
							CouchInstaller.doInstall(pHandler);
						} catch (UnknownHostException e) {
							e.printStackTrace();
							installProgress.dismiss();
							Message progress = new Message();
							progress.what = CouchInstallActivity.ERROR;
							progress.obj = new InstallError(
									"There was an error fetching the binaries, are you online? Please try again.");
							pHandler.sendMessage(progress);
						} catch (IOException e) {
							e.printStackTrace();
							installProgress.dismiss();
							Message progress = new Message();
							progress.what = CouchInstallActivity.ERROR;
							progress.obj = new InstallError(
									"There was an error fetching the binaries. Please try again.");
							pHandler.sendMessage(progress);
						}
					}
				}.start();

			}
		});
	}

	class InstallError {
		public String description;

		public InstallError(String description) {
			this.description = description;
		}
	}
}
