package com.slushpupie.deskclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DeskClock extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener
 {

	private static final String LOG_TAG = "DeskClock";
	
	private Typeface[] fonts;

	private PowerManager.WakeLock wl = null;
	private boolean isRunning = false;
	private RefreshHandler handler = new RefreshHandler();

	// current state
	private TextView display;
	private LinearLayout layout;
	private int displayWidth = -1;
	private int displayHeight = -1;;
	private boolean needsResizing = false;

	// backed by preferences
	private int prefsKeepSreenOn = 0;
	private boolean prefsMilitaryTime = false;
	private int prefsFontColor = Color.WHITE;
	private int prefsBackgroundColor = Color.BLACK;
	private boolean prefsShowSeconds = false;
	private boolean prefsBlinkColon = false;
	private int prefsFont = 0;

	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		layout = (LinearLayout) findViewById(R.id.layout);
		display = (TextView) findViewById(R.id.display);

		fonts = new Typeface[15];
		fonts[0] = Typeface.DEFAULT_BOLD;
		fonts[1] = Typeface.SANS_SERIF;
		fonts[2] = Typeface.SERIF;
		fonts[3] = Typeface.MONOSPACE;
		fonts[4] = Typeface.createFromAsset(getAssets(),
				"fonts/Abduction2000.ttf");
		fonts[5] = Typeface.createFromAsset(getAssets(), "fonts/DSPoint.ttf");
		fonts[6] = Typeface
				.createFromAsset(getAssets(), "fonts/DSTerminal.ttf");
		fonts[7] = Typeface.createFromAsset(getAssets(), "fonts/DT104.ttf");
		fonts[8] = Typeface.createFromAsset(getAssets(), "fonts/Delusion.ttf");
		// fonts[ 9] = Typeface.createFromAsset(getAssets(),
		// "fonts/DigitalReadout.ttf");
		// fonts[10] = Typeface.createFromAsset(getAssets(),
		// "fonts/DigitalReadoutItialics.ttf");
		fonts[11] = Typeface.createFromAsset(getAssets(), "fonts/Digital2.ttf");
		fonts[12] = Typeface.createFromAsset(getAssets(),
				"fonts/DigitaldreamFat.ttf");
		fonts[13] = Typeface.createFromAsset(getAssets(),
				"fonts/DisplayDots.ttf");
		fonts[14] = Typeface.createFromAsset(getAssets(), "fonts/digi.otf");


		loadPrefs();
		configureDisplay();
		resizeClock();
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	/** Called when the activity becomes visible. */
	@Override
	public void onStart() {
		super.onStart();

		/*
		Thread localThread = new Thread(new Runnable() {
			public void run() {
				try {
					while (isRunning) {
						Thread.sleep(500);
						Message message = Message.obtain(handler);
						handler.dispatchMessage(message);
					}
				} catch (Throwable t) {
					Log.e(LOG_TAG, "Something bad happened", t);
				}

			}

		});
		isRunning = true;
		localThread.start();
		*/
		
		isRunning = true;
		updateTime();
	}

	/** Called when the activity is no longer visible. */
	@Override
	public void onStop() {
		setScreenLock(0); //release any wakelocks
		isRunning = false;
		super.onStop();
	}

	/** Called before the activity is destroyed. */
	@Override
	public void onDestroy() {
		if (wl != null) {
			wl.release();
			wl = null;
		}
		super.onDestroy();
	}

	/** Called on configuration changes, such as screen rotate */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(LOG_TAG,"config change occurred");
		configureDisplay();
				
	}

	/** Called when first creating menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	/** Called when a menu item is selected */
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		if (menuItem.getItemId() == R.id.menu_prefs) {
			Intent intent = new Intent().setClass(this,
					DeskClockPreferenceActivity.class);
			startActivityForResult(intent, 0);
		}
		return true;

	}
	
	/** Called when a shared preference is changed, added, or removed */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		loadPrefs();
	}


	/* Private methods */

	private void loadPrefs() {
		Log.d(LOG_TAG,"loading preferences");
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		try {
			prefsKeepSreenOn = Integer.valueOf(prefs 
				.getString(
						"pref_keep_screen_on",
						"0"));
		} catch (NumberFormatException e) {
			prefsKeepSreenOn = 0;
		}
		setScreenLock(prefsKeepSreenOn);

		prefsMilitaryTime = prefs
				.getBoolean(
						"pref_military_time",
						false);

		try {
			prefsFontColor = prefs.getInt("pref_color",
				Color.WHITE);
		} catch (NumberFormatException e) {
			prefsFontColor = Color.WHITE;
		}
		display.setTextColor(prefsFontColor);

		try {
			prefsBackgroundColor = prefs.getInt(
				"pref_background_color", Color.BLACK);
		} catch (NumberFormatException e) {
			prefsBackgroundColor = Color.BLACK;
		}
		layout.setBackgroundColor(prefsBackgroundColor);

		boolean showSeconds = prefs.getBoolean( "pref_show_seconds", false);
		if (prefsShowSeconds != showSeconds) {
			prefsShowSeconds = showSeconds;
			needsResizing = true;
		}
		
		prefsBlinkColon = prefs.getBoolean("pref_blink_seconds", false);

		try {
			int n = Integer.valueOf(prefs.getString("pref_font", 
					getString(R.string.pref_default_font)));
			if (n != prefsFont) {
				prefsFont = n;
				needsResizing = true;
			}
		} catch (NumberFormatException e) {
			if(prefsFont != Integer.valueOf(getString(R.string.pref_default_font))) {
				prefsFont = Integer.valueOf(getString(R.string.pref_default_font));
				needsResizing = true;
			}
		}

	}

	private void setScreenLock(int keepOn) {
		if (keepOn > 0) {
			if (wl == null) {
				PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
				/*
				 * Could use SCREEN_DIM_WAKE_LOCK SCREEN_BRIGHT_WAKE_LOCK
				 */
				 if(keepOn == 1) {
					wl = pm.newWakeLock(
						PowerManager.SCREEN_DIM_WAKE_LOCK, "DeskClock");
					Log.d(LOG_TAG,"Using DIM wakelock");
				} else if(keepOn == 2) {
					wl = pm.newWakeLock(
						PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "DeskCLock");
					Log.d(LOG_TAG,"Using BRIGHT wakelock");
				} else {
					Log.e(LOG_TAG,"Unknown wakelock value!");
					return;
				}
				wl.acquire();
				Log.d(LOG_TAG,"WakeLock acquired");
			}
		} else {
			if (wl != null) {
				Log.d(LOG_TAG,"WakeLock released");
				wl.release();
				wl = null;
			}
		}

	}

	private void configureDisplay() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		displayWidth = metrics.widthPixels;
		displayHeight = metrics.heightPixels;

		layout.setBackgroundColor(prefsBackgroundColor);
		display.setTextColor(prefsFontColor);
		display.setGravity(Gravity.CENTER);
		display.setEllipsize(TextUtils.TruncateAt.MARQUEE);
		display.setSingleLine();

		Log.d(LOG_TAG,"display configured");
		needsResizing = true;
	}

	private void resizeClock() {
		int leftPadding = 0;
		/*
		 * // Is this really needed? if (prefsFont == 0) { Rect digitBounds =
		 * getBoundingBox("8", fonts[prefsFont]); int width =
		 * digitBounds.width(); leftPadding = width * -4; }
		 */

		display.setTypeface(fonts[prefsFont]);
		display.setPadding(leftPadding, 0, 0, 0);

		Rect boundingBox = new Rect(0, 0, displayWidth, displayHeight);

		String str = "88:88:";
		if (prefsShowSeconds)
			str = "88:88:88:";

		display.setTextSize(1, fitTextToRect(fonts[prefsFont], str, boundingBox));

		needsResizing = false;
		updateTime();
	}

	private void updateTime() {
		if (needsResizing) {
			resizeClock();
			return;
		}


		Time localTime = new Time();
		long time = System.currentTimeMillis();
		localTime.set(time);

		char colon = ':';
		if(prefsBlinkColon && localTime.second % 2 == 0) {
			colon = ' ';
		}
		
		String format = String.format("%%l%c%%M",colon);

		if (prefsMilitaryTime)
			format = String.format("%%H%c%%M",colon);

		if (prefsShowSeconds)
			format = format + String.format("%c%%S",colon);

		//Log.d(LOG_TAG,"Setting time to "+localTime.format(format));
		
		display.setText(localTime.format(format));
		//layout.postInvalidate();
		if(isRunning)
			handler.tick();

	}

	private float fitTextToRect(Typeface font, String text, Rect fitRect) {

		int width = fitRect.width(); // v16
		int height = fitRect.height(); // v6

		int minGuess = 0; // v11
		int maxGuess = 640; // v9
		int guess = 320; // v5

		Rect r;
		boolean lastGuessTooSmall = true; // v7

		for (int i = 0; i < 32; i++) {

			if (minGuess + 1 == maxGuess) {
				Log.d(LOG_TAG,"Discovered font size "+minGuess);
				return minGuess;
			}

			r = getBoundingBox(text, font, guess);
			if (r.width() > width || r.height() > height) {
				maxGuess = guess;
				lastGuessTooSmall = false;

			} else {
				minGuess = guess;
				lastGuessTooSmall = true;
			}
			guess = (minGuess + maxGuess) / 2;
		}

		Log.d(LOG_TAG,"Unable to discover font size");
		if (lastGuessTooSmall)
			return maxGuess;
		else
			return minGuess;

	}

	private Rect getBoundingBox(String text, Typeface font, float size) {
		Rect r = new Rect(0, 0, 0, 0); // v13
		Paint paint = new Paint(); // v12
		paint.setTypeface(font);
		paint.setTextSize(size);
		paint.getTextBounds(text, 0, text.length(), r);
		return r;
	}
	
	private class RefreshHandler extends Handler {
		
		public void handleMessage(Message message) {
			updateTime();
			
		}
		public void tick() {
			this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), 500);
		}
	}

}
