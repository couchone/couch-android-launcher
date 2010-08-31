package org.couchdb.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class CouchInstaller {
	final static String baseUrl = "http://couchdb-android.s3.amazonaws.com/";
	final static String dataPath = "/data/data/org.couchdb.android";
	// SD card isn't -ideal-, but right now we are really hefty.
	final static String TAG = "CouchDB";

	private static void installPackage(String pkg, Handler handler)
			throws IOException {
		HttpClient pkgHttpClient = new DefaultHttpClient();
		HttpGet tgzrequest = new HttpGet(baseUrl + pkg + ".tgz");
		HttpResponse response = pkgHttpClient.execute(tgzrequest);
		ArrayList<String> installedfiles = new ArrayList<String>();
		StatusLine status = response.getStatusLine();
		Log.d(TAG, "Request returned status " + status);
		if (status.getStatusCode() == 200) {
			HttpEntity entity = response.getEntity();
			InputStream instream = entity.getContent();
			// int fileSize = (int) entity.getContentLength();
			TarArchiveInputStream tarstream = new TarArchiveInputStream(
					new GZIPInputStream(instream));
			TarArchiveEntry e = null;
			int files = 0;
			while ((e = tarstream.getNextTarEntry()) != null) {
				if (e.isDirectory()) {
					new File(e.getName()).mkdir();
					Log.v(TAG, "MKDIR: " + e.getName());
				} else if (!"".equals(e.getLinkName())) {
					Log.v(TAG,
							"LINK: " + e.getName() + " -> " + e.getLinkName());
					Runtime.getRuntime().exec(
							new String[] { "ln", "-s", e.getName(),
									e.getLinkName() });
					installedfiles.add(e.getName());
				} else {
					File target = new File(e.getName());
					if(target.getParent() != null) new File(target.getParent()).mkdirs();
					Log.v(TAG, "Extracting " + e.getName());
					IOUtils.copy(tarstream, new FileOutputStream(target));
					installedfiles.add(e.getName());
				}
				Runtime.getRuntime().exec("chmod 755 " + e.getName()); //TODO: Set to actual tar perms.
				files++;
				Message progress = new Message();
				progress.arg1 = files++;
				progress.arg2 = 0;
				handler.sendMessage(progress);
			}

			tarstream.close();
			instream.close();
			FileWriter iLOWriter = new FileWriter(dataPath + "/" + pkg + ".installedfiles");
			for (String file : installedfiles)
				iLOWriter.write(file+"\n");
			iLOWriter.close();
			for (String file : installedfiles)
			{
				if(file.endsWith(".postinst.sh"))
				{
					Runtime.getRuntime().exec("sh " + file);
				}
			}
		} else {
			throw new IOException();
		}
	}

	public static boolean checkInstalled() {
		for (String pkg : packageSet()) {
			File f = new File(dataPath + "/" + pkg + ".installedfiles");
			if (!f.exists())
				return false;
		}
		return true;
	}

	public static List<String> packageSet() {
		ArrayList<String> packages = new ArrayList<String>();

		// TODO: Different CPU arch support.
		// TODO: Some kind of sane remote manifest for this (remote updater)
		packages.add("couch-erl-1.0"); // CouchDB, Erlang, CouchJS
		packages.add("fixup-1.0"); //Cleanup old mochi, retrigger DNS fix install.
		packages.add("dns-fix"); //Add inet config to fallback on erlang resolver
		if (android.os.Build.VERSION.SDK_INT == 7)
			packages.add("couch-icu-driver-eclair");
		else if (android.os.Build.VERSION.SDK_INT == 8)
			packages.add("couch-icu-driver-froyo");
		else {
			// TODO: Probably should throw some exception here.
			Log.e(TAG, "Unsupported OS/arch combo");
			return null;
		}
		return packages;
	}

	public static void doInstall(Handler handler) throws IOException {
		for(String pkg : packageSet()) {
			if(!(new File(dataPath + "/" + pkg + ".installedfiles")).exists())
				installPackage(pkg, handler);
		}

		Message done = Message.obtain();
		done.arg2 = 1;
		handler.sendMessage(done);
	}

}
