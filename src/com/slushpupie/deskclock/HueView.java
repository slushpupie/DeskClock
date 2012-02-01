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
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;

public class HueView extends View {

  Paint paint;
  Shader hueShader;

  /**
   * @param context
   */
  public HueView(Context context) {
    super(context);
  }

  /**
   * @param context
   * @param attrs
   */
  public HueView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * @param context
   * @param attrs
   * @param defStyle
   */
  public HueView(Context context, AttributeSet attrs, int defStyle) {
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

    int colors[] = { 0xffff0000, 0xffffff00, 0xff00ff00, 0xff00ffff, 0xff0000ff, 0xffff00ff,
        0xffff0000 };

    if (null == paint) {
      paint = new Paint();
      hueShader = new LinearGradient(0, 0, 0, getMeasuredHeight(), colors, null, TileMode.CLAMP);
    }

    paint.setShader(hueShader);
    canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), paint);

  }

}