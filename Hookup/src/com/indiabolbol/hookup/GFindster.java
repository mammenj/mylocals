package com.indiabolbol.hookup;

import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.indiabolbol.hookup.service.client.ServiceIRCService;

public class GFindster extends Activity {
	private String curWindow;
	private ImageButton peopleBtn;
	private ImageButton businessBtn;
	private ImageButton entertainBtn;
	private Intent myConnectivtyIntent;
	private Button btnConnect;
	private Button btnDisconnect;

	private ServiceIRCService mBoundService;
	private String nick;

	private static final String LOG_TAG = "GFindsterMain";
	public Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case ServiceIRCService.MSG_CHANGEWINDOW:
				ServiceIRCService.lastwindow = curWindow;
				curWindow = (String) msg.obj;
				Log.i(LOG_TAG, "MSG_CHANGEWINDOW " + curWindow);
				ServiceIRCService.curwindow = curWindow;
				break;

			case ServiceIRCService.MSG_NICKINUSE:
				Log.i(LOG_TAG, "MSG_NICKINUSE " + msg.obj.toString());
				showToast("Your CHAT NAME IS in USE, please change");
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setClass(getBaseContext(),
						com.indiabolbol.hookup.GFindsterPreferences.class);
				startActivity(intent);

				break;

			case ServiceIRCService.MSG_DISCONNECT:
				Log.i(LOG_TAG, "MSG_DISCONNECT " + msg.obj.toString());
				showToast(msg.obj.toString());
				btnConnect.setText("Connect");
				break;

			case ServiceIRCService.MSG_CONNECT:
				Log.i(LOG_TAG, "MSG_CONNECT " + msg.obj.toString());
				showToast("You are connected to server!!");
				btnConnect.setText("Disconnect");
				break;
				
			case ServiceIRCService.MSG_JOIN:
				Log.i(LOG_TAG, "MSG UPDATE CHANGER " + msg.obj.toString());
				Toast.makeText(GFindster.this, msg.obj.toString(),
						Toast.LENGTH_SHORT).show();
				break;

			case ServiceIRCService.MSG_PVTCHAT:
				Log.i(LOG_TAG, "11111111111MSG_PVTCHAT " + msg.obj.toString());

				String target = parseNickMsg(msg.obj.toString());
				if (target != null) {
					nick = target;
					Log.d(LOG_TAG, "2222222222222NICK IS " + nick);
					showDialog(1);
				} else {
					nick = "";
				}

				break;

			case ServiceIRCService.MSG_RECVD:
				Log.i(LOG_TAG, "MSG_RECVD " + msg.obj.toString());
				Toast.makeText(GFindster.this, msg.obj.toString(),
						Toast.LENGTH_SHORT).show();

			}
		}
	};

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
		setContentView(R.layout.gfindster_view);
		peopleBtn = (ImageButton) findViewById(R.id.peopleBtn);
		peopleBtn.setOnClickListener(mPeopleListener);

		businessBtn = (ImageButton) findViewById(R.id.businessBtn);
		businessBtn.setOnClickListener(mBusinessListener);

		entertainBtn = (ImageButton) findViewById(R.id.entertainBtn);
		entertainBtn.setOnClickListener(mEntertainListener);

		btnConnect = (Button) findViewById(R.id.btn_Connect);
		btnConnect.setOnClickListener(mConnectListener);

		// btnDisconnect = (Button) findViewById(R.id.btn_DisConnect);
		// btnDisconnect.setOnClickListener(mDisconnectListener);

		Log.i(LOG_TAG,
				"Started ON CREATE........3333333333333333AAAAAAAAAAAAA....");
		myConnectivtyIntent = new Intent(GFindster.this,
				ServiceIRCService.class);
		startService(myConnectivtyIntent);
		Log
				.i(LOG_TAG,
						"Started ON CREATE........3333333333333333BBBBBBBBBBBBBBBB....");

		ServiceIRCService.setViewHandler(mHandler);
		if (mBoundService != null) {
			if (mBoundService.ircConn != null
					&& mBoundService.ircConn.isConnected()) {
				btnConnect.setText("Disconnect");

			} else {
				btnConnect.setText("Connect");
			}

		}
	}

	protected void onDestroy() {
		super.onDestroy();
		Log.i(LOG_TAG, "ON onDestroy............");

		unbindService(mConnection);
		// For testing I am not stopping it
		// mockServiceThread.stop();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.servicemenu, menu);

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
//		case R.id.stopservice:
//			// showAlert("Disconnecting ....", "Network");
//			if (mBoundService != null) {
//				mBoundService.quitServer();
//				mBoundService.stopService();
//				showToast("Disconnecting & Stopping service from server !!");
//			} else {
//				showToast("Already disconnected");
//			}
//			return true;
//		case R.id.startservice:
//			// showAlert("Connect to server", "Network");
//			if (mBoundService != null) {
//				mBoundService.connect();
//				showToast("Connection in progress...please wait");
//			} else {
//				Log
//						.i(LOG_TAG,
//								"Started ON Connect........eeeeeeeeeeeeeeeeeeeeeeeeeee.");
//				myConnectivtyIntent = new Intent(GFindster.this,
//						ServiceIRCService.class);
//				startService(myConnectivtyIntent);
//				Log.i(LOG_TAG,
//						"Started ON Connect........ffffffffffffffffff....");
//
//				ServiceIRCService.setViewHandler(mHandler);
//				showToast("Starting service..please wait");
//				mBoundService.connect();
//			}
//			return true;
		case R.id.locationchat:
			if (mBoundService != null) {
				if (mBoundService.ircConn != null
						&& mBoundService.ircConn.isConnected()) {
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
			}
			return true;

		case R.id.preferences:
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.setClass(getBaseContext(),
					com.indiabolbol.hookup.GFindsterPreferences.class);
			startActivity(intent);

			return true;
		default:
			return true;
		}
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

	@Override
	protected void onStart() {

		super.onStart();
		Log.i(LOG_TAG, "ON onStart............");
		ServiceIRCService.setViewHandler(mHandler);
		bindService(new Intent(GFindster.this, ServiceIRCService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		if (mBoundService != null) {
			if (mBoundService.ircConn != null
					&& mBoundService.ircConn.isConnected() && ServiceIRCService.state==10) {
				btnConnect.setText("Disconnect");

			} else {
				btnConnect.setText("Connect");
			}

		}
	}

	private OnClickListener mConnectListener = new OnClickListener() {
		public void onClick(View v) {
			if (mBoundService != null
					&& btnConnect.getText().toString().equalsIgnoreCase(
							"Connect")) {
				mBoundService.connect();
				showToast("Connection in progress...please wait");
				btnConnect.setText("Connecting...");

			} else if (mBoundService != null
					&& btnConnect.getText().toString().equalsIgnoreCase(
							"Disconnect")) {
				mBoundService.quitServer();
				mBoundService.stopService();
				btnConnect.setText(R.string.text_connect);
			} else {
				showToast("Please wait...or try later");
			}
		}
	};

	// private OnClickListener mDisconnectListener = new OnClickListener() {
	// public void onClick(View v) {
	// if (mBoundService != null) {
	//
	// mBoundService.quitServer();
	// showToast("Connection in progress...please wait");
	// btnConnect.setText(R.string.text_disconnect);
	//
	// } else {
	// showToast("Please Restart by going back and restarting the application");
	// }
	// }
	// };
	private OnClickListener mPeopleListener = new OnClickListener() {
		public void onClick(View v) {
			// Cancel a previous call to startService(). Note that the
			// service will not actually stop at this point if there are
			// still bound clients.
			if (ServiceIRCService.ircConn != null
					&& ServiceIRCService.ircConn.isConnected()
					&& ServiceIRCService.state == 10) {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent
						.putExtra("com.indiabolbol.hookup.showSelected",
								"people");

				intent.setClass(getBaseContext(),
						com.indiabolbol.hookup.HookupMapView.class);

				startActivity(intent);
			} else {
				showToast("Network unavailable");
			}

		}
	};

	private OnClickListener mBusinessListener = new OnClickListener() {
		public void onClick(View v) {
			showToast("Under development..");
			// if (ServiceIRCService.ircConn != null
			// && ServiceIRCService.ircConn.isAlive()) {
			// Intent intent = new Intent(Intent.ACTION_MAIN);
			// intent.putExtra("com.indiabolbol.hookup.showSelected",
			// "business");
			//
			// intent.setClass(getBaseContext(),
			// com.indiabolbol.hookup.HookupMapView.class);
			//
			// startActivity(intent);
			// } else {
			// Toast.makeText(GFindster.this,
			// "Network unavailable:: Please try later or CONNECT!!",
			// Toast.LENGTH_LONG).show();
			// }

		}
	};

	private OnClickListener mEntertainListener = new OnClickListener() {
		public void onClick(View v) {
			showToast("Under development..");
			// if (ServiceIRCService.ircConn != null
			// && ServiceIRCService.ircConn.isAlive()) {
			// Intent intent = new Intent(Intent.ACTION_MAIN);
			// intent.putExtra("com.indiabolbol.hookup.showSelected",
			// "entertain");
			//
			// intent.setClass(getBaseContext(),
			// com.indiabolbol.hookup.HookupMapView.class);
			//
			// startActivity(intent);
			// } else {
			// Toast.makeText(GFindster.this,
			// "Network unavailable:: Please try later or CONNECT!!",
			// Toast.LENGTH_LONG).show();
			// }

		}
	};

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case 1:
			dialog.setTitle("PM with " + nick);
			break;
		case 2:
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		Log.i(LOG_TAG, "333333333333ON onCreateDialog............");
		String[] items = { "Accept", "Ignore", "No" };
		return new AlertDialog.Builder(GFindster.this).setTitle(
				"Private Chat Requested").setItems(items,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

						/* User clicked so do some stuff */
						// String[] items = getResources().getStringArray(
						// R.array.private_chat_items);
						if (ServiceIRCService.ircConn != null
								&& ServiceIRCService.ircConn.isConnected()) {

							if (which == 0) {
								Intent intent = new Intent(Intent.ACTION_MAIN);
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
								showToast("No private chat with " + nick);
							}
						} else {
							showToast("Network Unvailabel!! Please connect again..");
						}
					}
				}).create();
	}
}