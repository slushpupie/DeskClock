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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;

public class DeskClockPreferenceActivity extends PreferenceActivity implements
    OnSharedPreferenceChangeListener {

  ListPreference mKeepScreenOn;
  SeekBarPreference mScreenBrightness;
  SeekBarPreference mTempScreenBrightness;
  SeekBarPreference mButtonBrightness;
  CheckBoxPreference mLeadingZero;
  ListPreference mScreenOrientation;
  ListPreference mFont;
  SeekBarPreference mScale;

  boolean showButtonBrightness = true;

  public void onCreate(Bundle savedInstance) {
    super.onCreate(savedInstance);
    addPreferencesFromResource(R.xml.preferences);

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    mKeepScreenOn = (ListPreference) findPreference("pref_keep_screen_on");
    mScreenBrightness = (SeekBarPreference) findPreference("pref_screen_brightness");
    mTempScreenBrightness = (SeekBarPreference) findPreference("pref_screen_tmp_brightness");
    mButtonBrightness = (SeekBarPreference) findPreference("pref_button_brightness");
    mLeadingZero = (CheckBoxPreference) findPreference("pref_leading_zero");
    mScreenOrientation = (ListPreference) findPreference("pref_screen_orientation");
    mFont = (ListPreference) findPreference("pref_font");
    mScale = (SeekBarPreference) findPreference("pref_scale");

    try {
      // Can we even set the buttonBrightness?
      getWindow().getAttributes().getClass().getField("buttonBrightness");
    } catch (NoSuchFieldException e) {
      showButtonBrightness = false;
      mButtonBrightness.setSummary(R.string.pref_unavailable_button_brightness);
    }

    Preference colorPref = findPreference("pref_color");
    final ColorPickerDialog clPicker = new ColorPickerDialog(this, prefs.getInt("pref_color",
        Color.WHITE), Color.WHITE, new FontColorChangeListener());
    colorPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
        clPicker.show();
        return true;
      }
    });

    Preference bgColorPref = findPreference("pref_background_color");
    final ColorPickerDialog bgPicker = new ColorPickerDialog(this, prefs.getInt(
        "pref_background_color", Color.BLACK), Color.BLACK, new BackgroundColorChangeListener());
    bgColorPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
        bgPicker.show();
        return true;
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    mKeepScreenOn.setSummary(mKeepScreenOn.getEntry());
    String val = prefs.getString("pref_keep_screen_on",
        getString(R.string.pref_default_keep_screen_on));

    if ("manual".equals(val)) {
      mScreenBrightness.setEnabled(true);
      mScreenBrightness.setSummary(getString(R.string.pref_summary_en_screen_brightness) + " "
          + mScreenBrightness.getProgress() + "%");
      mTempScreenBrightness.setEnabled(true);
      mTempScreenBrightness.setSummary(getString(R.string.pref_summary_en_screen_tmp_brightness)
          + " " + mTempScreenBrightness.getProgress() + "%");
      if (showButtonBrightness) {
        mButtonBrightness.setEnabled(true);
        mButtonBrightness.setSummary(getString(R.string.pref_summary_en_button_brightness) + " "
            + mButtonBrightness.getProgress() + "%");
      } else {
        mButtonBrightness.setEnabled(false);
      }
    } else {
      mScreenBrightness.setEnabled(false);
      mTempScreenBrightness.setEnabled(false);
      mButtonBrightness.setEnabled(false);
    }

    mScreenOrientation.setSummary(mScreenOrientation.getEntry());

    mFont.setSummary(getString(R.string.pref_summary_font) + ": " + mFont.getEntry());

    mScale.setSummary(getString(R.string.pref_summary_scale) + " " + mScale.getProgress() + "%");

    prefs.registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(
        this);
  }

  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals("pref_keep_screen_on")) {
      String val = sharedPreferences.getString("pref_keep_screen_on",
          getString(R.string.pref_default_keep_screen_on));
      if ("manual".equals(val)) {
        mKeepScreenOn
            .setSummary(getResources().getStringArray(R.array.pref_options_keep_screen_on)[2]);
        mScreenBrightness.setEnabled(true);
        mScreenBrightness.setSummary(getString(R.string.pref_summary_en_screen_brightness) + " "
            + mScreenBrightness.getProgress() + "%");
        if (showButtonBrightness) {
          mButtonBrightness.setEnabled(true);
          mButtonBrightness.setSummary(getString(R.string.pref_summary_en_button_brightness) + " "
              + mButtonBrightness.getProgress() + "%");
        } else {
          mButtonBrightness.setEnabled(false);
        }
      } else {
        mScreenBrightness.setEnabled(false);
        mButtonBrightness.setEnabled(false);
        mScreenBrightness.setSummary(R.string.pref_summary_screen_brightness);
        if (showButtonBrightness)
          mButtonBrightness.setSummary(R.string.pref_summary_button_brightness);

        if ("auto".equals(val)) {
          mKeepScreenOn.setSummary(getResources().getStringArray(
              R.array.pref_options_keep_screen_on)[1]);
        } else {
          mKeepScreenOn.setSummary(getResources().getStringArray(
              R.array.pref_options_keep_screen_on)[0]);
        }
      }
    }

    if (key.equals("pref_military_time")) {
      boolean val = sharedPreferences.getBoolean("pref_military_time",
          Boolean.valueOf(getString(R.string.pref_default_military_time)));
      if (val) {
        mLeadingZero.setEnabled(false);
      } else {
        mLeadingZero.setEnabled(true);
      }
    }

    if (key.equals("pref_screen_orientation")) {
      mScreenOrientation.setSummary(mScreenOrientation.getEntry());
    }

    if (key.equals("pref_font")) {
      mFont.setSummary(getString(R.string.pref_summary_font) + ": " + mFont.getEntry());
    }

    if (key.equals("pref_scale")) {
      mScale.setSummary(getString(R.string.pref_summary_scale) + " " + mScale.getProgress() + "%");
    }
  }

  private class FontColorChangeListener implements ColorPickerDialog.OnColorSelectListener {

    @Override
    public void onNewColor(int color) {
      SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(
          DeskClockPreferenceActivity.this).edit();
      prefs.putInt("pref_color", color);
      prefs.commit();
    }
  }

  private class BackgroundColorChangeListener implements ColorPickerDialog.OnColorSelectListener {

    @Override
    public void onNewColor(int color) {
      SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(
          DeskClockPreferenceActivity.this).edit();
      prefs.putInt("pref_background_color", color);
      prefs.commit();
    }
  }
}
