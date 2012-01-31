/*
 *  Copyright 2011 3Cats Software <rumburake@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  see NOTICE and LICENSE files in the top level project folder.
 *  
 *  Modified 2012 by Jay Kline
 */

package com.slushpupie.deskclock;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;

public class ColorPickerDialog extends Dialog {

	public interface OnColorSelectListener {
		public void onNewColor(int color);
	}

	private OnColorSelectListener onColorSelectListener;

	final View view;
	final SaturationValueView viewSatVal;
	final HueView viewHue;
	final TextView viewDebug;
	final ImageView viewHueCursor;
	final ImageView viewSatValCursor;
	final ImageView viewOldColor;
	final ImageView viewNewColor;

	float hueX = -1;
	float hueY = -1;
	float satValX = -1;
	float satValY = -1;

	float hsv[] = { 0, 0, 0 };
	int rgb = 0xFF000000;

	public boolean debug = false;

	void writeDebug() {
		String text = "";
		text += String.format("Hue: (%.0f, %.0f) : %.0f\n", hueX, hueY, hsv[0]);
		text += String.format("SatVal: (%.0f, %.0f)\n", satValX, satValY);
		viewDebug.setText(text);
	}

	void setHueCursor() {
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHueCursor
				.getLayoutParams();
		layoutParams.leftMargin = -2;
		layoutParams.topMargin = (int) (hueY - viewHueCursor.getHeight() / 2);
		viewHueCursor.setLayoutParams(layoutParams);
	}

	void setSatValCursor() {
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewSatValCursor
				.getLayoutParams();
		layoutParams.leftMargin = (int) (satValX - viewSatValCursor.getWidth() / 2);
		layoutParams.topMargin = (int) (satValY - viewSatValCursor.getHeight() / 2);
		viewSatValCursor.setLayoutParams(layoutParams);
	}

	/*
	 * The correct way to manage a dialog is using Activity's onCreateDialog()
	 * and onPrepareDialog() If you are doing so, then call
	 * setStartColor(yourDesiredStartColor) in onPrepareDialog()!
	 */
	public void setStartColor(int color, int defaultColor) {

		rgb = color;
		Color.colorToHSV(color, hsv);

		viewOldColor.setBackgroundColor(0XFF000000 | defaultColor);
		viewNewColor.setBackgroundColor(0XFF000000 | color);

		ViewTreeObserver vto = view.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			public void onGlobalLayout() {
				hueY = hsv[0] * viewHue.getMeasuredHeight() / 360.f;
				satValX = hsv[1] * viewSatVal.getMeasuredWidth();
				satValY = (1 - hsv[2]) * viewSatVal.getMeasuredHeight();

				viewSatVal.setHue(hsv[0]);
				setHueCursor();
				setSatValCursor();
				view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});

	}

	public ColorPickerDialog(Context context, int color, int defaultColor,
			OnColorSelectListener onColorSelectListener) {
		super(context);

		this.onColorSelectListener = onColorSelectListener;

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.color_picker);

		view = findViewById(R.id.color_picker_view);
		viewSatVal = (SaturationValueView) findViewById(R.id.SatVal);
		viewHue = (HueView) findViewById(R.id.Hue);
		viewDebug = (TextView) findViewById(R.id.Debug);
		viewHueCursor = (ImageView) findViewById(R.id.HueCursor);
		viewSatValCursor = (ImageView) findViewById(R.id.SatValCursor);
		viewOldColor = (ImageView) findViewById(R.id.OldColor);
		viewNewColor = (ImageView) findViewById(R.id.NewColor);

		setStartColor(color, defaultColor);

		viewHue.setOnTouchListener(new View.OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch (action) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_MOVE:
					float y = event.getY();
					if (y < 0) {
						y = 0;
					}
					if (y > viewHue.getMeasuredHeight()) {
						y = viewHue.getMeasuredHeight() - 0.1f;
					}
					hueY = y;
					hueX = event.getX();

					hsv[0] = y * 360.f / viewHue.getMeasuredHeight();
					viewSatVal.setHue(hsv[0]);
					setHueCursor();

					rgb = Color.HSVToColor(hsv);
					viewNewColor.setBackgroundColor(rgb);

					if (debug) {
						writeDebug();
					}
					return true;
				}
				return false;
			}
		});

		viewSatVal.setOnTouchListener(new View.OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch (action) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_MOVE:
					float y = event.getY();
					if (y < 0) {
						y = 0;
					}
					if (y > viewSatVal.getMeasuredHeight()) {
						y = viewSatVal.getMeasuredHeight() - 0.1f;
					}
					satValY = y;
					float x = event.getX();
					if (x < 0) {
						x = 0;
					}
					if (x > viewSatVal.getMeasuredWidth()) {
						x = viewSatVal.getMeasuredWidth() - 0.1f;
					}
					satValX = x;

					hsv[1] = x / viewSatVal.getMeasuredHeight();
					hsv[2] = 1 - y / viewSatVal.getMeasuredHeight();
					setSatValCursor();

					rgb = Color.HSVToColor(hsv);
					viewNewColor.setBackgroundColor(rgb);

					if (debug) {
						writeDebug();
					}
					return true;
				}
				return false;
			}
		});

		viewOldColor.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				viewOldColor.setBackgroundColor(0xFFFFFFFF);
				dismiss();
				return false;
			}
		});

		viewNewColor.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				viewNewColor.setBackgroundColor(0xFFFFFFFF);
				ColorPickerDialog.this.onColorSelectListener.onNewColor(rgb);
				dismiss();
				return false;
			}
		});

	}

}