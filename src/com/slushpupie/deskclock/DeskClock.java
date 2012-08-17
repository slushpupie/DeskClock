/*
 * Copyright (C) 2012 Jay Kline
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.slushpupie.deskclock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

public class DeskClock extends FragmentActivity implements
    SharedPreferences.OnSharedPreferenceChangeListener, OnTouchListener {

  private static final String LOG_TAG = "DeskClock";
  private static char digitcharset[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
  private static char lettercharset[] = { 'A', 'P' };
  private static int PREF_VERSION = 2;

  // for pinch-zoom
  private static int NONE = 0;
  private static int DRAG = 1;
  private static int ZOOM = 2;
  private int mode = NONE;
  private float oldDist = 1f;
  private Method getXMethod;
  private Method getYMethod;
  private boolean supportMultiTouch = false;

  private static final int DIALOG_CHANGELOG = 0;

  private Typeface[] fonts;

  private boolean isRunning = false;
  private Handler handler = new Handler();
  private final BroadcastReceiver intentReceiver;

  private final Runnable runUpdateTime = new Runnable() {
    public void run() {
      updateTime();
    }
  };
  private final Runnable runMoveDisplay = new Runnable() {
    public void run() {
      moveClock();
    }
  };

  // current state
  // private TextView display;
  private DisplayView display;
  private LinearLayout layout;
  private int displayWidth = -1;
  private int displayHeight = -1;;
  private boolean needsResizing = false;

  // backed by preferences
  private int prefsKeepSreenOn = 0;
  private int prefsScreenBrightness = 50;
  private int prefsButtonBrightness = 50;
  private boolean prefsLeadingZero = false;
  private boolean prefsMilitaryTime = false;
  private boolean prefsShowMeridiem = false;
  private int prefsFontColor = Color.WHITE;
  private int prefsBackgroundColor = Color.BLACK;
  private boolean prefsShowSeconds = false;
  private boolean prefsBlinkColon = false;
  private int prefsFont = 0;
  private int prefsScreenOrientation = -1;
  private boolean prefsScreenSaver = false;
  private long prefsScreenSaverSpeed = 500;
  private String lastChangelog = "";
  private int prefsScale = 100;
  private boolean prefsUndockExit = false;

  public void acknoledgeChangelog() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("pref_changelog", false);
    editor.putString("last_changelog", getString(R.string.app_version));
    editor.commit();
  }

  public DeskClock() {
    super();
    // determine if multitouch is really supported
    try {
      getXMethod = MotionEvent.class.getMethod("getX", new Class[] { int.class });
      getYMethod = MotionEvent.class.getMethod("getY", new Class[] { int.class });
      supportMultiTouch = true;
    } catch (NoSuchMethodException nsme) {
      supportMultiTouch = false;
    }

    intentReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_DOCK_EVENT.equals(action)) {
          int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0);
          switch (dockState) {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
              Log.d(LOG_TAG, "received EXTRA_DOCK_STATE_UNDOCKED");
              if (prefsUndockExit)
                finish();
              else
                Log.d(LOG_TAG, "Ignoring...");
              break;
            case Intent.EXTRA_DOCK_STATE_DESK:
              Log.d(LOG_TAG, "received EXTRA_DOCK_STATE_DESK");
              break;
          }
        }
        if (Intent.ACTION_TIME_TICK.equals(action)) {
          updateTime();
        }
      }
    };
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    setContentView(R.layout.main);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

    layout = (LinearLayout) findViewById(R.id.layout);
    display = (DisplayView) findViewById(R.id.display);
    registerForContextMenu(display);
    display.setOnTouchListener(this);

    fonts = new Typeface[17];
    fonts[0] = Typeface.DEFAULT_BOLD;
    fonts[1] = Typeface.SANS_SERIF;
    fonts[2] = Typeface.SERIF;
    fonts[3] = Typeface.MONOSPACE;
    fonts[4] = Typeface.createFromAsset(getAssets(), "fonts/Abduction2000.ttf");
    fonts[5] = Typeface.createFromAsset(getAssets(), "fonts/DSPoint.ttf");
    fonts[6] = Typeface.createFromAsset(getAssets(), "fonts/DSTerminal.ttf");
    fonts[7] = Typeface.createFromAsset(getAssets(), "fonts/DT104.ttf");
    fonts[8] = Typeface.createFromAsset(getAssets(), "fonts/Delusion.ttf");
    fonts[9] = Typeface.createFromAsset(getAssets(), "fonts/jd_scarabeo.ttf");
    fonts[10] = Typeface.createFromAsset(getAssets(), "fonts/stencilla.ttf");
    fonts[11] = Typeface.createFromAsset(getAssets(), "fonts/Digital2.ttf");
    fonts[12] = Typeface.createFromAsset(getAssets(), "fonts/DigitaldreamFat.ttf");
    fonts[13] = Typeface.createFromAsset(getAssets(), "fonts/DisplayDots.ttf");
    fonts[14] = Typeface.createFromAsset(getAssets(), "fonts/digi.otf");
    fonts[15] = Typeface.createFromAsset(getAssets(), "fonts/GentiumBinary.ttf");
    fonts[16] = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

    loadPrefs();

    if (lastChangelog == null || !lastChangelog.equals(getString(R.string.app_version))) {
      DialogFragment df = ChangelogDialog.newInstance();
      df.show(getSupportFragmentManager(), "dialog");
    }

    configureDisplay();
    resizeClock();

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    prefs.registerOnSharedPreferenceChangeListener(this);
  }

  /** Called when the activity becomes visible. */
  @Override
  public void onStart() {
    super.onStart();

    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_DOCK_EVENT);
    filter.addAction(Intent.ACTION_TIME_TICK);
    registerReceiver(intentReceiver, filter);

    isRunning = true;
    updateTime();
    handler.postDelayed(runMoveDisplay, prefsScreenSaverSpeed);
    Toast.makeText(getApplicationContext(), R.string.startup_toast, Toast.LENGTH_SHORT).show();
  }

  /** Called when the activity is no longer visible. */
  @Override
  public void onStop() {
    setScreenLock(0, 0, 0);
    unregisterReceiver(intentReceiver);
    isRunning = false;
    handler.removeCallbacks(runMoveDisplay);
    handler.removeCallbacks(runUpdateTime);
    super.onStop();
  }

  /** Called on configuration changes, such as screen rotate */
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    Log.d(LOG_TAG, "config change occurred");
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
      Intent intent = new Intent().setClass(this, DeskClockPreferenceActivity.class);
      startActivityForResult(intent, 0);
    }
    if (menuItem.getItemId() == R.id.menu_changelog) {
      DialogFragment df = ChangelogDialog.newInstance();
      df.show(getSupportFragmentManager(), "dialog");
    }
    return true;

  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem menuItem) {
    return onOptionsItemSelected(menuItem);
  }

  /** Called when a shared preference is changed, added, or removed */
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    loadPrefs();
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (!supportMultiTouch)
      return false;

    switch (event.getAction() & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN:
        mode = DRAG;
        break;
      case MotionEvent.ACTION_POINTER_DOWN:
        oldDist = spacing(event);
        if (oldDist > 10f) {
          mode = ZOOM;
        }
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP:
        mode = NONE;
        break;
      case MotionEvent.ACTION_MOVE:
        if (mode == DRAG) {
          // ..
        } else if (mode == ZOOM) {
          float newDist = spacing(event);
          if (newDist > 10f) {
            float scaleF = newDist / oldDist;
            int scale = (int) (scaleF * 100.0);
            if (scale < 0)
              scale = scale * -1;
            if (scale > 100)
              scale = 100;

            // change font size
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putInt("pref_scale", scale).commit();
          }
        }
        break;
    }
    if (mode == ZOOM) {
      return true;
    } else {
      return false;
    }
  }

  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_CHANGELOG:

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Standard AlertDialog does not support HTML-style links.
        // So rebuild the ScrollView->TextView with the appropriate
        // settings and set the view directly.
        TextView tv = new TextView(this);
        tv.setPadding(5, 5, 5, 5);
        tv.setLinksClickable(true);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(R.string.changeLog);
        tv.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        ScrollView sv = new ScrollView(this);
        sv.setPadding(14, 2, 10, 12);
        sv.addView(tv);
        builder.setView(sv).setCancelable(false).setTitle(R.string.changeLogTitle)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(DeskClock.this);
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

  /* Private methods */

  private void loadPrefs() {
    Log.d(LOG_TAG, "loading preferences");
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    boolean b = false;
    int i = 0;
    String s = null;

    i = prefs.getInt("pref_version", 1);
    if (i != PREF_VERSION) {
      upgradePrefs(prefs);
    }

    lastChangelog = prefs.getString("last_changelog", "");

    s = prefs.getString("pref_keep_screen_on", "no");
    if ("auto".equals(s))
      prefsKeepSreenOn = 1;
    else if ("manual".equals(s))
      prefsKeepSreenOn = 2;
    else
      prefsKeepSreenOn = 0;

    prefsScreenBrightness = prefs.getInt("pref_screen_brightness", 50);
    prefsButtonBrightness = prefs.getInt("pref_button_brightness", 50);

    setScreenLock(prefsKeepSreenOn, prefsScreenBrightness, prefsButtonBrightness);

    s = prefs.getString("pref_screen_orientation", "auto");
    if ("portrait".equals(s))
      prefsScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    else if ("landscape".equals(s))
      prefsScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    else
      prefsScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

    setRequestedOrientation(prefsScreenOrientation);

    b = prefs.getBoolean("pref_military_time", false);
    if (b != prefsMilitaryTime) {
      prefsMilitaryTime = b;
      needsResizing = true;
    }

    b = prefs.getBoolean("pref_leading_zero", false);
    if (b != prefsLeadingZero) {
      prefsLeadingZero = b;
      needsResizing = true;
    }

    b = prefs.getBoolean("pref_meridiem", false);
    if (b != prefsShowMeridiem) {
      prefsShowMeridiem = b;
      needsResizing = true;
    }

    try {
      prefsFontColor = prefs.getInt("pref_color", Color.WHITE);
    } catch (NumberFormatException e) {
      prefsFontColor = Color.WHITE;
    }
    display.setColor(prefsFontColor);

    try {
      prefsBackgroundColor = prefs.getInt("pref_background_color", Color.BLACK);
    } catch (NumberFormatException e) {
      prefsBackgroundColor = Color.BLACK;
    }
    layout.setBackgroundColor(prefsBackgroundColor);

    b = prefs.getBoolean("pref_show_seconds", false);
    if (b != prefsShowSeconds) {
      prefsShowSeconds = b;
      needsResizing = true;
    }

    prefsBlinkColon = prefs.getBoolean("pref_blink_seconds", false);

    try {
      i = Integer.valueOf(prefs.getString("pref_font", getString(R.string.pref_default_font)));
      if (i != prefsFont) {
        prefsFont = i;
        needsResizing = true;
      }
    } catch (NumberFormatException e) {
      if (prefsFont != Integer.valueOf(getString(R.string.pref_default_font))) {
        prefsFont = Integer.valueOf(getString(R.string.pref_default_font));
        needsResizing = true;
      }
    }

    prefsScreenSaverSpeed = prefs.getInt("pref_screensaver_speed", 500);
    if (prefsScreenSaverSpeed < 500)
      prefsScreenSaverSpeed = 500;
    if (prefsScreenSaverSpeed > 10000)
      prefsScreenSaverSpeed = 10000;
    b = prefs.getBoolean("pref_screensaver", false);
    if (b != prefsScreenSaver) {
      prefsScreenSaver = b;
      display.setScreenSaver(prefsScreenSaver);
      handler.removeCallbacks(runMoveDisplay);
      handler.postDelayed(runMoveDisplay, prefsScreenSaverSpeed);
      needsResizing = true;
    }

    i = prefs.getInt("pref_scale", 100);
    if (i != prefsScale) {
      prefsScale = i;
      needsResizing = true;
    }

    prefsUndockExit = prefs.getBoolean("pref_undock_exit", false);

  }

  private void upgradePrefs(SharedPreferences prefs) {
    Editor editor = prefs.edit();
    switch (prefs.getInt("pref_version", 1)) {
      case 1:
        Log.i(LOG_TAG, "Upgrading preferences from version 1 to version 2");
        String keepScreenOn = prefs.getString("pref_keep_screen_on", "");
        String newKeepScreenOn = "no";
        int screenBrightness = 50;
        int buttonBrightness = 50;
        if ("1".equals(keepScreenOn)) { // old 'dim' setting
          newKeepScreenOn = "manual";
          screenBrightness = 0;
          buttonBrightness = 0;
        } else if ("2".equals(keepScreenOn)) { // old 'bright' setting
          newKeepScreenOn = "manual";
          screenBrightness = 100;
          buttonBrightness = 100;
        }
        editor.putString("pref_keep_screen_on", newKeepScreenOn);
        editor.putInt("pref_screen_brightness", screenBrightness);
        editor.putInt("pref_button_brightness", buttonBrightness);
        editor.putInt("pref_version", 2);
        editor.commit();

      case 2:
        Log.i(LOG_TAG, "Upgrade complete.");
        return;
      default:
        Log.e(LOG_TAG, "Unknown preferences version");
    }
  }

  private void setScreenLock(int keepOn, int screenBrightness, int buttonBrightness) {
    Window window = getWindow();

    LayoutParams layoutParams = window.getAttributes();
    Field fButtonBrightness = null;
    try {
      fButtonBrightness = layoutParams.getClass().getField("buttonBrightness");
    } catch (NoSuchFieldException e) {

    }

    if (keepOn > 0) {

      if (keepOn == 1) {
        // Auto-brightness
        layoutParams.screenBrightness = -1.0f;
        try {
          if (fButtonBrightness != null)
            fButtonBrightness.set(layoutParams, -1.0f);
        } catch (IllegalAccessException e) {

        }
      } else if (keepOn == 2) {
        // Manual brightness

        // Setting to 0 turns the screen off, so dont allow that
        if (prefsScreenBrightness <= 100 && prefsScreenBrightness > 0)
          layoutParams.screenBrightness = (prefsScreenBrightness / 100.0f);
        if (prefsScreenBrightness < 1)
          layoutParams.screenBrightness = 0.01f;
        try {
          if (fButtonBrightness != null && prefsButtonBrightness <= 100
              && prefsButtonBrightness >= 0)
            fButtonBrightness.set(layoutParams, (prefsButtonBrightness / 100.0f));
        } catch (IllegalAccessException e) {

        }
      } else {
        Log.e(LOG_TAG, "Unknown keepOn value!");
        return;
      }
      window.setAttributes(layoutParams);
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    } else {
      // Disable KEEP_SCREEN_ON

      try {
        if (fButtonBrightness != null)
          fButtonBrightness.set(layoutParams, -1.0f);
      } catch (IllegalAccessException e) {

      }
      layoutParams.screenBrightness = -1.0f;
      window.setAttributes(layoutParams);
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      display.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    Log.d(LOG_TAG, "display configured");
    needsResizing = true;
  }

  private void resizeClock() {
    Log.d(LOG_TAG, "resizeClock() called");
    // determine largest digit
    char bdigit = digitcharset[0];
    Rect bb = getBoundingBox(String.valueOf(bdigit), fonts[prefsFont], 10);
    int w = bb.width();
    for (int i = 1; i < digitcharset.length; i++) {
      bb = getBoundingBox(String.valueOf(digitcharset[i]), fonts[prefsFont], 10);
      if (bb.width() > w) {
        bdigit = digitcharset[i];
        w = bb.width();
      }
    }
    // determine largest letter
    char bletter = lettercharset[0];
    bb = getBoundingBox(String.valueOf(bletter), fonts[prefsFont], 10);
    w = bb.width();
    for (int i = 1; i < lettercharset.length; i++) {
      bb = getBoundingBox(String.valueOf(lettercharset[i]), fonts[prefsFont], 10);
      if (bb.width() > w) {
        bletter = lettercharset[i];
        w = bb.width();
      }
    }

    String str = String.format("%c%c:%c%c", bdigit, bdigit, bdigit, bdigit);

    if (prefsShowSeconds)
      str = String.format("%s:%c%c", str, bdigit, bdigit);
    if (prefsShowMeridiem)
      str = String.format("%s %cM", str, bletter);

    Rect boundingBox = new Rect(0, 0, displayWidth - 5, displayHeight - 5);
    float fontSize = fitTextToRect(fonts[prefsFont], str, boundingBox);
    if (prefsScale != 100) {
      fontSize = fontSize * (0.01f * ((float) prefsScale));
    }
    if (prefsScreenSaver) {
      fontSize = fontSize * 0.8f;
    }

    int leftPadding = 0;
    Rect digitBounds = getBoundingBox("8", fonts[prefsFont], fontSize);
    int width = digitBounds.width();
    leftPadding = width * -4;

    display.setWideTime(str);
    display.setFont(fonts[prefsFont]);
    display.setPadding(leftPadding, 0, 0, 0);
    display.setSize(fontSize);
    needsResizing = false;
    display.setTime("");
  }

  private void moveClock() {
    display.move();
    handler.removeCallbacks(runMoveDisplay);
    handler.postDelayed(runMoveDisplay, prefsScreenSaverSpeed);
  }

  private void updateTime() {
    if (needsResizing) {
      resizeClock();
    }

    Calendar cal = Calendar.getInstance();
    StringBuffer format = new StringBuffer();

    if (prefsMilitaryTime)
      format.append("kk");
    else if (prefsLeadingZero)
      format.append("hh");
    else
      format.append("h");

    if (prefsBlinkColon && cal.get(Calendar.SECOND) % 2 == 0)
      format.append(" ");
    else
      format.append(":");

    format.append("mm");

    if (prefsShowSeconds) {
      if (prefsBlinkColon && cal.get(Calendar.SECOND) % 2 == 0)
        format.append(" ");
      else
        format.append(":");
      format.append("ss");
    }

    if (prefsShowMeridiem)
      format.append(" aa");

    display.setTime(DateFormat.format(format.toString(), cal));
    // layout.postInvalidate();

    if (isRunning && (prefsShowSeconds || prefsBlinkColon)) {
      handler.removeCallbacks(runUpdateTime);
      handler.postDelayed(runUpdateTime, 1000);
    }
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
        Log.d(LOG_TAG, "Discovered font size " + minGuess);
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

    Log.d(LOG_TAG, "Unable to discover font size");
    if (lastGuessTooSmall)
      return maxGuess;
    else
      return minGuess;

  }

  private Rect getBoundingBox(String text, Typeface font, float size) {
    Rect r = new Rect(0, 0, 0, 0);
    float widths[] = new float[text.length()];
    float width = 0;
    Paint paint = new Paint(0);
    paint.setTypeface(font);
    paint.setTextSize(size);
    paint.getTextBounds(text, 0, text.length(), r);
    paint.getTextWidths(text, widths);
    for (float w : widths)
      width += w;
    r.right = (int) width;
    return r;
  }

  private float spacing(MotionEvent event) {
    try {
      float x0 = ((Float) getXMethod.invoke(event, 0)).floatValue();
      float x1 = ((Float) getXMethod.invoke(event, 1)).floatValue();
      float x = x0 - x1;
      float y0 = ((Float) getYMethod.invoke(event, 0)).floatValue();
      float y1 = ((Float) getYMethod.invoke(event, 1)).floatValue();
      float y = y0 - y1;
      return FloatMath.sqrt(x * x + y * y);
    } catch (IllegalArgumentException iae) {
      return 0;
    } catch (IllegalAccessException e) {
      return 0;
    } catch (InvocationTargetException e) {
      return 0;
    }
  }
}
