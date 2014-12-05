package com.indiabolbol.hookup.service.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.SearchManager;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;
import com.indiabolbol.hookup.GFindsterPreferences;
import com.indiabolbol.hookup.GFindsterUser;
import com.indiabolbol.hookup.HookupMapView;

public class GetHookData {

	/** ref to the calling activity */
	private static HookupMapView _activity;
	private Exception ex;
	private String dataFromServlet;
	List<OverlayItem> items = Collections
			.synchronizedList(new ArrayList<OverlayItem>());
	private static final String LOG_TAG = "GetHookData";
	private String selected;
	private static SharedPreferences preferences;
	private static boolean isNetworkThere = false;

	public void executeMapSearch(HookupMapView activity, String selected) {
		_activity = activity;
		Geocoder gc = new Geocoder(_activity);
	}

	public void execute(HookupMapView activity, String selected) {
		this.selected = selected;

		_activity = activity;

		// allows non-"edt" thread to be re-inserted into the "edt" queue
		final Handler uiThreadCallback = new Handler();

		// performs rendering in the "edt" thread, after background operation is
		// complete
		final Runnable runInUIThread = new Runnable() {
			public void run() {
				_showInUI();
			}
		};

		new Thread() {
			@Override
			public void run() {

				_doInBackgroundPost();
				uiThreadCallback.post(runInUIThread);
			}
		}.start();

		// Toast
		// .makeText(_activity, "Getting data from Server",
		// Toast.LENGTH_LONG).show();

	}

	public List<OverlayItem> getFriendList() {
		Log.d(LOG_TAG, "getFriendList ::" + items);
		return this.items;
	}

	/** this method is called in a non-"edt" thread */
	private void _doInBackgroundPost() {
		Log.i(LOG_TAG, "_doInBackgroundPost task - start");
		preferences = _activity.getSharedPreferences(
				GFindsterPreferences.PREFS_NAME, 0);

		Log.d(LOG_TAG, "_doInBackgroundPost running .............::");

		if (ServiceIRCService.ircConn != null
				&& ServiceIRCService.ircConn.isConnected()) {
			isNetworkThere = true;

			ServiceIRCService.listUsers();

			List<GFindsterUser> users = ServiceIRCService.getCurrentGFUsers();
			items.clear();
			for (GFindsterUser user : users) {
				if (user != null) {
					if (user.getLatitude() == 0 || user.getLongitude() == 0) {
						Log.d(LOG_TAG,
								"Latitud is not present so randomizing for	 :"
										+ user.getNick());

						Log.d(LOG_TAG, "Longitude From USER "
								+ user.getLongitude());
						Log.d(LOG_TAG, "Latitude From USER "
								+ user.getLatitude());
						double latitude = Math
								.round(ServiceIRCService.latitude)
								+ (Math.random());
						double longitude = Math
								.round(ServiceIRCService.longitude)
								+ (Math.random());
						user.setLatitude(latitude);
						user.setLongitude(longitude);
					}
					// Log.d(LOG_TAG,"Longitude From USER "
					// +user.getLongitude());
					// Log.d(LOG_TAG,"Latitude From USER " +user.getLatitude());
					items.add(new OverlayItem(getPoint(user.getLatitude(), user
							.getLongitude()), user.getNick(), user.getNick()));
				}

			}

			Log.d(LOG_TAG,
					"_doInBackgroundPost running .....adding items......items.size()..::"
							+ items.size());

			Log
					.d(LOG_TAG,
							"_doInBackgroundPost running .....finished adding items........::");

			// Log.d(getClass().getSimpleName(), responseBody);
			Log
					.d(LOG_TAG,
							"_doInBackgroundPost ENDDDDDDDED ..............id::");
		} else {
			isNetworkThere = false;

			Log.d(LOG_TAG, "ServiceIRC is NOT CONNECTED");

		}
	}

	private GeoPoint getPoint(double lat, double lon) {
		return (new GeoPoint((int) (lat * 1000000.0), (int) (lon * 1000000.0)));
	}

	private void _showInUI() {
		Log
				.d(LOG_TAG,
						"_showInUI started---------------------------------------------");
		if (items != null) {
			if (items.size() > 0) {
				Toast.makeText(_activity,
						"Found " + items.size() + " " + selected,
						Toast.LENGTH_LONG).show();

				_activity.updateMap();
				Log
						.d(LOG_TAG,
								"_showInUI updateMap finished---------------------------------------------");
			} else if (!isNetworkThere) {
				Toast.makeText(_activity, "Network Unavailable!",
						Toast.LENGTH_LONG).show();
				_activity.updateMap();
			}
		}
		if (ex != null)
			Toast.makeText(
					_activity,
					ex.getMessage() == null ? "Error" : "Error - "
							+ ex.getMessage(), Toast.LENGTH_SHORT).show();
		((HookupMapView) _activity)._updateItems(items);
	}
}
