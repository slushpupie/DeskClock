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
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;

public class DeskClockPreferenceActivity extends PreferenceActivity {

  public void onCreate(Bundle savedInstance) {
    super.onCreate(savedInstance);
    addPreferencesFromResource(R.xml.preferences);

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

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
