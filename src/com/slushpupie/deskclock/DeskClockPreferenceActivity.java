package com.slushpupie.deskclock;

import com.slushpupie.deskclock.ColorPickerDialog.OnColorChangedListener;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;

public class DeskClockPreferenceActivity extends PreferenceActivity implements  ColorPickerDialog.OnColorChangedListener {

	private ColorPickerDialog.OnColorChangedListener backgroundColorChangeListener;
	private Preference.OnPreferenceClickListener backgroundColorClickListener;
	private ColorPickerDialog.OnColorChangedListener colorChangeListener;
	private Preference.OnPreferenceClickListener colorClickListener;

	public void onCreate(Bundle savedInstance)
	  {
	    super.onCreate(savedInstance);
	    addPreferencesFromResource(R.xml.preferences);
	    
	    
	    
	    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    
	     

	    Preference colorPref = findPreference("pref_color");
	    final ColorPickerDialog clPicker = new ColorPickerDialog(
	    			this, 
				this,
				"pref_color",
				prefs.getInt("pref_color", Color.WHITE),
				Color.WHITE);
	    colorPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				clPicker.show();
				return true;
			}
	    	
	    });
	    
	    
	    
	    Preference bgColorPref = findPreference("pref_background_color");
	    final ColorPickerDialog bgPicker = new ColorPickerDialog(
				this, 
				this, 
				"pref_background_color",
				prefs.getInt( "pref_background_color", Color.BLACK),  
				Color.BLACK);
	    bgColorPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    	public boolean onPreferenceClick(Preference preference) {
				bgPicker.show();
				return true;
			}
	    });
	    
	    
	  }

	public void colorChanged(String key, int color) {
		if("pref_color".equals(key)) {
			SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
			prefs.putInt("pref_color", color);
		    prefs.commit();
		} else if("pref_background_color".equals(key)) {
			SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
			prefs.putInt("pref_background_color", color);
		    prefs.commit();
		}
	}
	

}
