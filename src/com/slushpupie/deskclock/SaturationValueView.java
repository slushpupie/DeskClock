/*
 *  Copyright 2011 3Cats Software <rumburake@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  see NOTICE and LICENSE files in the top level project folder.
 *  
 *  Modified 2012 by Jay Kline
 */

package com.slushpupie.deskclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;

public class SaturationValueView extends View {

	Paint paint;
	Shader valueShader;
	float color[] = { 0, 1, 1 };

	/**
	 * @param context
	 */
	public SaturationValueView(Context context) {
		super(context);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public SaturationValueView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public SaturationValueView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (null == paint) {
			paint = new Paint();
			valueShader = new LinearGradient(0, 0, 0, getMeasuredHeight(),
					0xffffffff, 0xff000000, TileMode.CLAMP);
		}

		int rgb = Color.HSVToColor(color);

		Shader saturationShader = new LinearGradient(0, 0, getMeasuredWidth(),
				0, 0xffffffff, rgb, TileMode.CLAMP);
		Shader composedShader = new ComposeShader(saturationShader,
				valueShader, PorterDuff.Mode.MULTIPLY);

		paint.setShader(composedShader);
		canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), paint);

	}

	void setHue(float hue) {
		color[0] = hue;
		invalidate();
	}

}