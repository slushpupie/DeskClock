package com.slushpupie.deskclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.Calendar;



public class DeskClock extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener
 {

	private static final String LOG_TAG = "DeskClock";
	private static char digitcharset[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
	private static char lettercharset[] = { 'A', 'P' };
	
	private static final int DIALOG_CHANGELOG = 0;
	
	private Typeface[] fonts;

	private PowerManager.WakeLock wl = null;
	private boolean isRunning = false;
	private RefreshHandler handler = new RefreshHandler();
	
	// current state
	//private TextView display;
	private DisplayView display;
	private LinearLayout layout;
	private int displayWidth = -1;
	private int displayHeight = -1;;
	private boolean needsResizing = false;

	// backed by preferences
	private int prefsKeepSreenOn = 0;
	private boolean prefsMilitaryTime = false;
	private boolean prefsShowMeridiem = false;
	private int prefsFontColor = Color.WHITE;
	private int prefsBackgroundColor = Color.BLACK;
	private boolean prefsShowSeconds = false;
	private boolean prefsBlinkColon = false;
	private int prefsFont = 0;
	private int prefsScreenOrientation = -1;
	private boolean prefsScreenSaver = false;
	private String lastChangelog = "";
	private int prefsScale = 100;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		layout = (LinearLayout) findViewById(R.id.layout);
		display = (DisplayView) findViewById(R.id.display);
		
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
		
		if(lastChangelog == null || !lastChangelog.equals(getString(R.string.app_version))) {
			showDialog(DIALOG_CHANGELOG);
		}
		
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
		if (menuItem.getItemId() == R.id.menu_changelog) {
			showDialog(DIALOG_CHANGELOG);
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
		
		lastChangelog = prefs.getString("last_changelog", "");
		
		
		try {
			prefsKeepSreenOn = Integer.valueOf(prefs 
				.getString(
						"pref_keep_screen_on",
						"0"));
		} catch (NumberFormatException e) {
			prefsKeepSreenOn = 0;
		}
		setScreenLock(prefsKeepSreenOn);
		
		String pso = prefs.getString("pref_screen_orientation", "auto");
		if("portrait".equals(pso)) {
			prefsScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		} else if("landscape".equals(pso)) {
			prefsScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		} else {
			prefsScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		}
		setRequestedOrientation(prefsScreenOrientation);
	

		prefsMilitaryTime = prefs
				.getBoolean(
						"pref_military_time",
						false);

		boolean showMeridiem = prefs
				.getBoolean(
						"pref_meridiem",
						false);
		if(showMeridiem != prefsShowMeridiem) {
			prefsShowMeridiem = showMeridiem;
			needsResizing = true;
		}
		
		try {
			prefsFontColor = prefs.getInt("pref_color",
				Color.WHITE);
		} catch (NumberFormatException e) {
			prefsFontColor = Color.WHITE;
		}
		display.setColor(prefsFontColor);

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

		boolean ss = prefs.getBoolean("pref_screensaver", false);
		if(ss != prefsScreenSaver) {
			prefsScreenSaver = ss;
			display.setScreenSaver(prefsScreenSaver);
			needsResizing = true;
		}
		
		int sc = prefs.getInt("pref_scale", 100);
		if(sc != prefsScale) {
			prefsScale = prefs.getInt("pref_scale", 100);
			needsResizing = true;
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
					
					Window window = getWindow();
					LayoutParams layoutParams = window.getAttributes();
					try {
						Field buttonBrightness = layoutParams.getClass().getField("buttonBrightness");
						buttonBrightness.set(layoutParams, 0);
					} catch (NoSuchFieldException e) {
						
					} catch (IllegalAccessException e) {
						
					}
					window.setAttributes(layoutParams);
					
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

		layout.setBackgroundColor(Color.WHITE);
		display.setBackgroundColor(prefsBackgroundColor);
		display.setColor(prefsFontColor);
		display.setScreenSaver(prefsScreenSaver);

		Log.d(LOG_TAG,"display configured");
		needsResizing = true;
	}

	private void resizeClock() {

		//determine largest digit
		char bdigit = digitcharset[0];
		Rect bb = getBoundingBox(String.valueOf(bdigit), fonts[prefsFont], 10);
		int w = bb.width();
		for(int i = 1; i < digitcharset.length; i++) {
			bb = getBoundingBox(String.valueOf(digitcharset[i]), fonts[prefsFont], 10);
			if(bb.width() > w) {
				bdigit = digitcharset[i];
				w = bb.width();
			}
		}
		//determine largest letter
		char bletter = lettercharset[0];
		bb = getBoundingBox(String.valueOf(bletter), fonts[prefsFont], 10);
		w = bb.width();
		for(int i = 1; i < lettercharset.length; i++) {
			bb = getBoundingBox(String.valueOf(lettercharset[i]), fonts[prefsFont], 10);
			if(bb.width() > w) {
				bletter = lettercharset[i];
				w = bb.width();
			}
		}

		String str = String.format("%c%c:%c%c", bdigit,bdigit,bdigit,bdigit);
		
		if (prefsShowSeconds)
			str = String.format("%s:%c%c", str, bdigit,bdigit);
		if (prefsShowMeridiem)
			str = String.format("%s %cM", str, bletter);

		Rect boundingBox = new Rect(0, 0, displayWidth, displayHeight);
		float fontSize = fitTextToRect(fonts[prefsFont], str, boundingBox);
		if(prefsScale != 100) {
			fontSize = fontSize * (0.01f * ((float)prefsScale));
		}
		if(prefsScreenSaver) {
			fontSize = fontSize * 0.8f;
		}

		int leftPadding = 0;
		Rect digitBounds = getBoundingBox("8", fonts[prefsFont], fontSize ); 
		int width = digitBounds.width(); 
		leftPadding = width * -4;
		
		display.setWideTime(str);
		display.setFont(fonts[prefsFont]);
		display.setPadding(leftPadding, 0, 0, 0);
		display.setSize(fontSize );

		needsResizing = false;
		updateTime();
	}

	private void updateTime() {
		if (needsResizing) {
			resizeClock();
			return;
		}

		Calendar cal = Calendar.getInstance();

		char colon = ':';
		if(prefsBlinkColon && cal.get(Calendar.SECOND) % 2 == 0) {
			colon = ' ';
		}
		
		
		String format = String.format("h%cmm",colon);

		if (prefsMilitaryTime)
			format = String.format("k%cmm",colon);

		if (prefsShowSeconds)
			format = format + String.format("%css",colon);
		
		if (prefsShowMeridiem)
			format = format + " aa";

		//Log.d(LOG_TAG,"Setting time to "+localTime.format(format));
		
		
		display.setTime(DateFormat.format(format, cal));
		//layout.postInvalidate();
		if(isRunning)
			handler.tick();

	}

	private float fitTextToRect(Typeface font, String text, Rect fitRect) {

		int width = fitRect.width(); 
		int height = fitRect.height();

		int minGuess = 0; 
		int maxGuess = 640;
		int guess = 320;

		Rect r;
		boolean lastGuessTooSmall = true; 

		for (int i = 0; i < 32; i++) {

			if (minGuess + 1 == maxGuess) {
				Log.d(LOG_TAG,"Discovered font size "+minGuess);
				r = getBoundingBox(text, font, guess);
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
		Rect r = new Rect(0, 0, 0, 0); 
		Paint paint = new Paint(); 
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
            sendMessageDelayed(obtainMessage(0), 250);
		}
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
			case DIALOG_CHANGELOG:
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.changeLog)
					.setCancelable(false)
					.setTitle(R.string.changeLogTitle)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DeskClock.this);
							SharedPreferences.Editor editor = prefs.edit();
							editor.putBoolean("pref_changelog", false);
							editor.putString("last_changelog", getString(R.string.app_version));
							editor.commit();
						}
					});
				return builder.create();
			default:
				return null;
		}
	}
}
