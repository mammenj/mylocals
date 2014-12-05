package com.indiabolbol.hookup.service.client;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

public class ThreadUpdateLocThread implements Runnable {
	private Context context;
	private LocationManager lm;
	private Criteria crit;

	public ThreadUpdateLocThread(Context c) {
		context = c;
		lm = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		crit = new Criteria();
		crit.setAccuracy(100); // 100 meters is more than enough
		crit.setPowerRequirement(Criteria.POWER_LOW); // lets try to not waste
		// power
	}

	public void run() {
		try {
			for (;;) {
				// if we're connected
				if (ServiceIRCService.state == 10) {
					Location l = lm.getLastKnownLocation("gps");
					ServiceIRCService.updateLocation(l.getLatitude(), l
							.getLongitude());
				}
				Thread.sleep(1000 * 60 * 5); // once every 5 minutes, when connected
			}
		} catch (InterruptedException e) {
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}
}