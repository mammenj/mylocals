package com.indiabolbol.hookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.indiabolbol.hookup.service.client.GetHookData;
import com.indiabolbol.hookup.service.client.ServiceIRCService;

/**
 * 
 * Your key is: 0wa9rnpB9b0nEq3h8TuqzhRu9pYR30F10vLEb6w
 * 
 * This key is good for all apps signed with your certificate whose fingerprint
 * is: 35:EA:43:51:3E:DD:DC:A7:5C:6F:A5:9F:18:2F:5F:CD
 * 
 * Here is an example xml layout to get you started on your way to mapping
 * glory:
 * 
 * <com.google.android.maps.MapView android:layout_width="fill_parent"
 * android:layout_height="fill_parent"
 * android:apiKey="0wa9rnpB9b0nEq3h8TuqzhRu9pYR30F10vLEb6w" />
 * ------------------------------------------------ PRODUCTION RELEASE KEY
 * DC:65:F3:CF:06:C6:22:32:28:47:DB:B0:A1:87:2B:A4
 * 
 * 
 * 0wa9rnpB9b0kpKtNynqueN_TMEgmtvjF2Bg3Jng This key is good for all apps signed
 * with your certificate whose fingerprint is:
 * DC:65:F3:CF:06:C6:22:32:28:47:DB:B0:A1:87:2B:A4 Here is an example xml layout
 * to get you started on your way to mapping glory:
 * <com.google.android.maps.MapView android:layout_width="fill_parent"
 * android:layout_height="fill_parent"
 * android:apiKey="0wa9rnpB9b0kpKtNynqueN_TMEgmtvjF2Bg3Jng" />
 */
public class HookupMapView extends MapActivity {
	private double lat;
	private double lon;
	private MapView map;

	protected LocationManager myLocationManager = null;
	protected MapController mc;

	private static final String PROVIDER_NAME = LocationManager.GPS_PROVIDER;

	private static final String LOG_TAG = "HookupMapView";
	private HookupLocationListener mLocationListener;
	private Drawable marker = null;

	List<OverlayItem> mFriendList = Collections
			.synchronizedList(new ArrayList<OverlayItem>());
	SitesOverlay so = null;
	private GetHookData gethook = null;
	private ProgressDialog pd;
	String selected = null;
	Intent myConnectivtyIntent;
	private String curWindow;
	private String chatTarget;

	private ServiceIRCService mBoundService;

	private final String PREFS_NAME = "androidChatPrefs";
	SharedPreferences settings;
	private String nick;
	private final int PVT_CHAT = 5;
	private final int PVT_CHAT_OUT = 6;

	public Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case ServiceIRCService.MSG_CHANGEWINDOW:
				ServiceIRCService.lastwindow = curWindow;
				curWindow = (String) msg.obj;
				Log.i(LOG_TAG, "MSG UPDATE CHANGER " + curWindow);
				ServiceIRCService.curwindow = curWindow;
				break;
			case ServiceIRCService.MSG_UPDATECHAN:
				// updateView((String) msg.obj);
				Log.i(LOG_TAG, "MSG UPDATE CHANGER " + (String) msg.obj);
				break;

			case ServiceIRCService.MSG_RECVD:
				Log.i(LOG_TAG, "MSG UPDATE CHANGER " + msg.obj.toString());
				Toast.makeText(HookupMapView.this, msg.obj.toString(),
						Toast.LENGTH_SHORT).show();
				break;
			
			case ServiceIRCService.MSG_JOIN:
				Log.i(LOG_TAG, "MSG UPDATE CHANGER " + msg.obj.toString());
				Toast.makeText(HookupMapView.this, msg.obj.toString(),
						Toast.LENGTH_SHORT).show();
				break;

			case ServiceIRCService.MSG_NICKINUSE:
				Log.i(LOG_TAG, "MSG UPDATE CHANGER " + msg.obj.toString());
				showToast("CHAT NAME in USE, please change");
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setClass(getBaseContext(),
						com.indiabolbol.hookup.GFindsterPreferences.class);
				startActivity(intent);
				break;

			case ServiceIRCService.MSG_DISCONNECT:
				Log.i(LOG_TAG, "MSG_DISCONNECT " + msg.obj.toString());
				showToast(msg.obj.toString());
				Intent intentMain = new Intent(Intent.ACTION_MAIN);
				intentMain.setClass(getBaseContext(),
						com.indiabolbol.hookup.GFindster.class);
				startActivity(intentMain);
				break;

			case ServiceIRCService.MSG_PVTCHAT:
				Log.i(LOG_TAG, "MSG_PVTCHAT " + msg.obj.toString());
				// showToast(msg.obj.toString());

				String target = parseNickMsg(msg.obj.toString());
				if (target != null) {
					nick = target;
					showDialog(PVT_CHAT);

				}
				break;

			case ServiceIRCService.MSG_CHANCHAT:
				Log.i(LOG_TAG, "MSG_CHANCHAT " + msg.obj.toString());

				String[] msgs = parseNickMsgA(msg.obj.toString());
				if (msgs != null && msgs.length > 1) {
					Log.d(LOG_TAG, "CHAN: " + msgs[1] + " says: " + msgs[2]);
					showToast(msgs[1] + " says: " + msgs[2]);
				}

				break;
			case ServiceIRCService.MSG_CLEARPROGRESS:
				if (pd != null)
					pd.dismiss();
				break;

			}
		}
	};

	// private MyServiceThread mockServiceThread;

	// private static final String MAP_API_KEY =
	// "ABQIAAAAYoU7hw2TYkX2SrKcupn4ZBThCirRJ8YifFRsqYyBl-3RqlxwCxT9o3Q8AlXOgTAVHkGl-ii3rxZPOQ";

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(LOG_TAG, "onService connected......method");
			mBoundService = ((ServiceIRCService.IRCChatServiceBinder) service)
					.getService();

			// Toast.makeText(HookupMapView.this, "Connected to service....",
			// Toast.LENGTH_SHORT).show();

		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			Log.i(LOG_TAG, "onService connected......method");
			mBoundService = null;
			// Toast.makeText(HookupMapView.this, "Disconnected to service",
			// Toast.LENGTH_LONG).show();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "onCreate....");
		Log.i(LOG_TAG, "onCreate binding service");
		bindService(new Intent(HookupMapView.this, ServiceIRCService.class),
				mConnection, Context.BIND_AUTO_CREATE);

		Log.i(LOG_TAG, "Started ON CREATE........55555555555555555555....");
		curWindow = "HookupMapView";
		ServiceIRCService.setViewHandler(mHandler);
		setContentView(R.layout.mapview);
		// map.setOnKeyListener(mKeyListener);
		map = (MapView) findViewById(R.id.map);
		selected = getIntent().getStringExtra(
				"com.indiabolbol.hookup.showSelected");
		// pd = ProgressDialog.show(this, "Looking..", "Looking for " +
		// selected,
		// true, true);
		// gethook = new GetHookData();
		// gethook.execute(HookupMapView.this, selected);

		// myLocationManager = (LocationManager)
		// getSystemService(LOCATION_SERVICE);
		// initLocation();
		mc = map.getController();
		Log.i(LOG_TAG, "Started ON CREATE........111111111111111111111....");
		// http://code.google.com/android/toolbox/apis/lbs.html
		// use the telnet to fix the GPS location
		// telnet localhost 5554
		// longitude latitude
		// geo fix 78.418865 17.429843 4392
		// Location curLoc =
		// myLocationManager.getLastKnownLocation(PROVIDER_NAME);
		Log.i(LOG_TAG, "Started ON CREATE........22222222222222222222....");
		// lon = curLoc.getLatitude();
		// lat = curLoc.getLongitude();

		lat = ServiceIRCService.latitude;
		lon = ServiceIRCService.longitude;

		// lat = 17.429843;
		// lon = 78.418865;

		mc.setCenter(getPoint(lat, lon));
		// map.displayZoomControls(false);
		mc.setZoom(12);

		marker = getResources().getDrawable(R.drawable.girl);

		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker
				.getIntrinsicHeight());

	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(LOG_TAG, "onStart ...");
		ServiceIRCService.setViewHandler(mHandler);
		if (pd == null) {
			pd = ProgressDialog.show(this, "Looking..", "Looking for "
					+ selected, true, true);
		}
		// mFriendList.clear();
		gethook = new GetHookData();
		gethook.execute(HookupMapView.this, selected);

	}

	private void _showInUI() {
		Log
				.d(LOG_TAG,
						"_showInUI started---------------------------------------------");
		showToast("Users online : " + mBoundService.getCurrentUsers().size());
	}

	public void updateMap() {
		Log.i(LOG_TAG, "updateMap before invalidate");
		map.invalidate();
		so = new SitesOverlay(marker);
		so.populateFriends();
		// if (mFriendList.size() > 200) {
		// mc.setZoom(0);
		// }
		map.getOverlays().add(so);

		if (pd != null) {
			pd.dismiss();
		}
		Log.i(LOG_TAG, "updateMap after invalidate");

	}

	// private void initLocation() {
	// Log.i(LOG_TAG, "Initializing location");
	// mLocationListener = new HookupLocationListener();
	// myLocationManager.requestLocationUpdates(PROVIDER_NAME, 0, 0,
	// mLocationListener);
	// }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(LOG_TAG, "ON onDestroy............");

		// myLocationManager.removeUpdates(mLocationListener);
		unbindService(mConnection);
		// For testing I am not stopping it
		// mockServiceThread.stop();

	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private class HookupLocationListener implements LocationListener {
		public void onLocationChanged(Location loc) {
			if (loc == null) {
				Log.e(LOG_TAG, "location changed to null");
			} else {
				Log.d(LOG_TAG, "location changed : " + loc.toString());
			}
		}

		public void onStatusChanged(java.lang.String arg0, int arg1,
				Bundle extras) {
			Log.d(LOG_TAG, "Provider Status Changed : " + arg0);
		}

		public void onProviderEnabled(java.lang.String arg0) {
			Log.d(LOG_TAG, "Provider Enabled : " + arg0);
		}

		public void onProviderDisabled(java.lang.String arg0) {
			Log.d(LOG_TAG, "Provider Disabled : " + arg0);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(LOG_TAG, "On KEY DOWN keyCode " + keyCode);
		if (keyCode == KeyEvent.KEYCODE_S) {
			map.setSatellite(!map.isSatellite());
			return (true);
		} else if (keyCode == KeyEvent.KEYCODE_I) {
			Log.d(LOG_TAG, "On KEY DOWN keyCode KEYCODE_I "
					+ event.getKeyCode());
			// map.displayZoomControls(true);
			// this.map.displayZoomControls(true);
			this.mc.zoomIn();
//			this.mc.setCenter(getPoint(ServiceIRCService.latitude,
//					ServiceIRCService.longitude));
			return (true);
		} else if (keyCode == KeyEvent.KEYCODE_O) {
			Log.d(LOG_TAG, "On KEY DOWN keyCode KEYCODE_I "
					+ event.getKeyCode());
			// map.displayZoomControls(true);
			// this.map.displayZoomControls(true);
			this.mc.zoomOut();
//			this.mc.setCenter(getPoint(ServiceIRCService.latitude,
//					ServiceIRCService.longitude));
			return (true);
		}

		return (super.onKeyDown(keyCode, event));
	}

	protected class SitesOverlay extends ItemizedOverlay<OverlayItem> {
		private List<OverlayItem> items = new ArrayList<OverlayItem>();
		private Drawable marker = null;

		public SitesOverlay(Drawable marker) {
			super(marker);
			this.marker = marker;
		}

		@Override
		protected OverlayItem createItem(int i) {
			// return (items.get(i));
			return items.get(i);
		}

		public synchronized void populateFriends() {
			//
			Log.d(LOG_TAG, "synchronized populateFriends >>>>>>>>>>>>>>>>>>");
			// get it from server
			items = gethook.getFriendList();
			Log.d(LOG_TAG, "????????????????????????From Hookup items : "
					+ items.size());
			Log.d(LOG_TAG,
					"????????????????????????From Hookup items mFriendList: "
							+ mFriendList.size());
			mFriendList.clear();
			Log.d(LOG_TAG,
					"????????????????????????AAAAAAAAAAAAAAA From Hookup items mFriendList: "
							+ mFriendList.size());
			mFriendList = items;

			super.populate();
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);
			boundCenterBottom(marker);
		}

		@Override
		protected boolean onTap(int i) {
			Log.d(LOG_TAG, "OnTap tapped on" + items.get(i).getSnippet());
			chatTarget = items.get(i).getSnippet();
			// Toast.makeText(HookupMapView.this, items.get(i).getSnippet(),
			// Toast.LENGTH_SHORT).show();
			showDialog(PVT_CHAT_OUT);
			return (true);
		}

		@Override
		public int size() {
			// return (items.size());
			return items.size();
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub

		super.onPause();
		Log.i(LOG_TAG, "ON onPause............");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		// if (gethook == null) {
		// Log.i(LOG_TAG, "GetHook is null");
		// gethook = new GetHookData();
		// if (selected == null) {
		// Log.i(LOG_TAG, "SELECTED STRING IS NULL-------------------");
		// }
		// gethook.execute(HookupMapView.this, selected);
		// }

		super.onResume();
		Log.i(LOG_TAG, "ON onResume............");
		// if (pd == null) {
		// pd = ProgressDialog.show(this, "Looking..", "Looking for "
		// + selected, true, true);
		// }
		// gethook = new GetHookData();
		// gethook.execute(HookupMapView.this, selected);
		// pd = ProgressDialog.show(this, "Updating..", "Updating users", true,
		// true);
		// gethook.execute(HookupMapView.this, selected);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case PVT_CHAT:
			dialog.setTitle("PM with " + nick);
			break;
		case PVT_CHAT_OUT:
			dialog.setTitle("Chat with " + chatTarget);
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Log.i(LOG_TAG, "ON onCreateDialog............");
		if (id == PVT_CHAT) {
			String[] items = { "Accept", "Ignore", "No" };
			return new AlertDialog.Builder(HookupMapView.this).setTitle(
					"Private Chat with " + nick).setItems(items,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							/* User clicked so do some stuff */
							// String[] items = getResources().getStringArray(
							// R.array.private_chat_items);
							if (ServiceIRCService.ircConn != null
									&& ServiceIRCService.ircConn.isConnected()) {

								if (which == 0) {
									Intent intent = new Intent(
											Intent.ACTION_MAIN);
									intent
											.putExtra(
													"com.indiabolbol.hookup.showSelectedUser",
													nick);

									intent
											.setClass(
													getBaseContext(),
													com.indiabolbol.hookup.PrivateChatActivity.class);
									startActivity(intent);
								} else if (which == 1) {
									showToast("Ignoring user " + nick);
								} else if (which == 2) {
									showToast("No Private Chat with " + nick);
								}

							} else {
								showToast("Network Unvailabel!! Please connect again..");
							}
						}
					}).create();

		} else if (id == PVT_CHAT_OUT) {
			return new AlertDialog.Builder(HookupMapView.this).setTitle(
					"Chat with " + chatTarget).setItems(
					R.array.select_dialog_items,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							/* User clicked so do some stuff */
							String[] items = getResources().getStringArray(
									R.array.select_dialog_items);
							if (ServiceIRCService.ircConn != null
									&& ServiceIRCService.ircConn.isConnected()) {

								if (which == 0) {
									Intent intent = new Intent(
											Intent.ACTION_MAIN);
									// intent.addCategory(Intent.HOOKUP);
									// parent.getSelectedItem()
									intent
											.putExtra(
													"com.indiabolbol.hookup.showSelectedUser",
													chatTarget);

									intent
											.setClass(
													getBaseContext(),
													com.indiabolbol.hookup.PrivateChatActivity.class);
									startActivity(intent);
								} else if (which == 1) {
									new AlertDialog.Builder(HookupMapView.this)
											.setMessage(
													"Request for "
															+ items[which]
															+ " sent").show();
								} else if (which == 2) {
									Intent intent = new Intent(
											Intent.ACTION_MAIN);
									// intent.addCategory(Intent.HOOKUP);
									// parent.getSelectedItem()
									intent
											.putExtra(
													"com.indiabolbol.hookup.showSelectedUser",
													ServiceIRCService.chan);

									intent
											.setClass(
													getBaseContext(),
													com.indiabolbol.hookup.ChatActivity.class);
									startActivity(intent);
								}
							} else {
								showToast("Network Unvailabel!! Please connect again..");
							}
						}
					}).create();
		} else {
			return null;
		}
	}

	private GeoPoint getPoint(double lat, double lon) {
		return (new GeoPoint((int) (lat * 1000000.0), (int) (lon * 1000000.0)));
	}

	/**
	 * Thread to listen for location updates and presumable do something useful
	 */

	public void _updateItems(List<OverlayItem> items) {
		// TODO Auto-generated method stub

		// Toast.makeText(HookupMapView.this,
		// "GOT DATA FROM SERVER number::" + items.size(),
		// Toast.LENGTH_SHORT).show();
		this.mFriendList = items;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu, menu);
		inflater.inflate(R.menu.hookupmenu, menu);

		return true;
	}

	// Activity callback that lets your handle the selection in the class.
	// Return true to indicate that you've got it, false to indicate
	// that it should be handled by a declared handler object for that
	// item (handler objects are discouraged for reasons of efficiency).
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Toast.makeText(HookupMapView.this, "Menu Item Clicked Zoom",
		// Toast.LENGTH_SHORT);
		// return true;

		int itemid = item.getItemId();
		switch (itemid) {
		// case R.id.disconnectserver:
		// // showAlert("Disconnecting ....", "Network");
		// if (mBoundService != null) {
		// mBoundService.quitServer();
		// showToast("Disconnecting from server !!");
		// } else {
		// showToast("Already disconnected");
		// }
		// return true;
		// case R.id.connectserver:
		// // showAlert("Connect to server", "Network");
		// if (mBoundService != null) {
		// mBoundService.connect();
		// showToast("Connection in progress...please wait");
		// }
		// return true;
		case R.id.locationchat:

			if (ServiceIRCService.ircConn != null
					&& ServiceIRCService.ircConn.isConnected()) {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				// intent.addCategory(Intent.HOOKUP);
				// parent.getSelectedItem()
				intent.putExtra("com.indiabolbol.hookup.showSelectedUser",
						ServiceIRCService.chan);

				intent.setClass(getBaseContext(),
						com.indiabolbol.hookup.ChatActivity.class);
				startActivity(intent);
			} else {
				showToast("Network is disconnected, Please connect to server and chat");
			}

			return true;

		case R.id.preferences:
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.setClass(getBaseContext(),
					com.indiabolbol.hookup.GFindsterPreferences.class);
			startActivity(intent);

			return true;
		case R.id.zoomin:
			this.mc.zoomIn();
//			this.mc.setCenter(getPoint(ServiceIRCService.latitude,
//					ServiceIRCService.longitude));

			return true;
		case R.id.zoomout:

			this.mc.zoomOut();
//			this.mc.setCenter(getPoint(ServiceIRCService.latitude,
//					ServiceIRCService.longitude));
			return true;
		default:
			return true;
		}
	}

	private String parseNickMsg(String msgstr) {
		Log.d(LOG_TAG, "PARSE NICK msgstr " + msgstr);
		StringTokenizer st = new StringTokenizer(msgstr, "|");
		String[] msg = { "", "", "" };
		int count = 0;
		while (st.hasMoreTokens()) {
			msg[count] = st.nextToken();
			Log.d(LOG_TAG, "PARSE NICK " + msg[count]);
			count++;
		}
		if (!msg[0].startsWith("#")) {
			return msg[1];
		}
		return null;
	}

	private String[] parseNickMsgA(String msgstr) {
		Log.d(LOG_TAG, "PARSE NICK msgstr " + msgstr);
		StringTokenizer st = new StringTokenizer(msgstr, "|");
		String[] msg = { "", "", "" };
		int count = 0;
		while (st.hasMoreTokens() && count < 3) {
			msg[count] = st.nextToken();
			Log.d(LOG_TAG, "PARSE NICK " + msg[count]);
			count++;
		}

		return msg;
	}

	private void showAlert(String msg, String title) {
		Log.i(LOG_TAG, "ON showAlert...........");
		AlertDialog a = new AlertDialog.Builder(HookupMapView.this).setIcon(
				R.drawable.alert_dialog_icon).setTitle(title).setMessage(msg)
				.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

							}
						}).setNegativeButton(R.string.alert_dialog_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

								/* User clicked Cancel so do some stuff */
							}
						}).create();
		a.show();
	}

	protected void showToast(String msgtoShow) {
		// create the view
		View view = inflateView(R.layout.incoming_message_panel);

		// set the text in the view
		TextView tv = (TextView) view.findViewById(R.id.message);
		tv.setText(msgtoShow);

		// show the toast
		Toast toast = new Toast(this);
		toast.setView(view);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();
	}

	private View inflateView(int resource) {
		LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return vi.inflate(resource, null);
	}

	/**
	 * The notification is the icon and associated expanded entry in the status
	 * bar.
	 */
	protected void showNotification() {
		Log.i(LOG_TAG, "ON showNotification............");
		// look up the notification manager service
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// The details of our fake message
		CharSequence from = "Hello";
		CharSequence message = "indiabolbol. would you care to chat!!";

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, HookupMapView.class), 0);

		// The ticker text, this uses a formatted string so our message could be
		// localized
		String tickerText = "Ticker sample";

		// construct the Notification object.
		Notification notif = new Notification(R.drawable.stat_sample,
				tickerText, System.currentTimeMillis());

		// Set the info for the views that show in the notification panel.
		notif.setLatestEventInfo(this, from, message, contentIntent);

		// after a 100ms delay, vibrate for 250ms, pause for 100 ms and
		// then vibrate for 500ms.
		notif.vibrate = new long[] { 100, 250, 100, 500 };

		// Note that we use R.layout.incoming_message_panel as the ID for
		// the notification. It could be any integer you want, but we use
		// the convention of using a resource id for a string related to
		// the notification. It will always be a unique number within your
		// application.
		nm.notify(R.string.imcoming_message_ticker_text, notif);
		print("Completed notification");
	}

	private void print(String s) {
		Log.d(LOG_TAG, s);
		Toast.makeText(HookupMapView.this, s, Toast.LENGTH_SHORT).show();
	}

}
