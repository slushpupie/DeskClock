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

import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class DisplayView extends View {

  private String time = "88:88";
  private float tWidth = 0;

  private String wideTime = "88:88";
  private float wtWidth = 0;
  private float wtHeight = 0;

  private Paint paint;

  private boolean screenSaver = false;

  private Random r = new Random();

  private float vF = 0.5f;
  private float hF = 0.5f;

  private double moveVStep = (r.nextGaussian() * 0.01d);
  private double moveHStep = (r.nextGaussian() * 0.01d);

  private Rect boundingBox = new Rect(0, 0, 0, 0);

  public DisplayView(Context context) {
    super(context);
    initComponents();
  }

  public DisplayView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initComponents();
  }

  private void initComponents() {
    // Default Paint object does "DEV_KERNING" which kerns off the device
    // screen, ugly.
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    setPadding(1, 1, 1, 1);
  }

  public void setWideTime(String wideTime) {
    this.wideTime = wideTime;
    paint.getTextBounds(wideTime, 0, wideTime.length(), boundingBox);
    wtHeight = boundingBox.height();
    float wt[] = new float[wideTime.length()];
    wtWidth = 0;
    paint.getTextWidths(wideTime, wt);
    for (float w : wt)
      wtWidth += w;
  }

  public void setTime(CharSequence time) {
    if (!this.time.equals(time)) {
      this.time = time.toString();
      paint.getTextBounds((String) time, 0, time.length(), boundingBox);
      float[] wt = new float[time.length()];
      tWidth = 0;
      paint.getTextWidths((String) time, wt);
      for (float w : wt)
        tWidth += w;

      if (tWidth > wtWidth)
        wtWidth = tWidth;
      requestLayout();
      invalidate();
    }
  }

  public void setSize(float size) {
    paint.setTextSize(size);
    setWideTime(wideTime);
    requestLayout();
    invalidate();
  }

  public void setFont(Typeface font) {
    paint.setTypeface(font);
    requestLayout();
    invalidate();
  }

  public void setColor(int color) {
    paint.setColor(color);
    invalidate();
  }

  public void setScreenSaver(boolean screenSaver) {
    this.screenSaver = screenSaver;
    if (!screenSaver) {
      vF = 0.5f;
      hF = 0.5f;
    }

    requestLayout();
    invalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
  }

  /**
   * Determines the width of this view
   * 
   * @param measureSpec
   *          A measureSpec packed into an int
   * @return The width of the view, honoring constraints from measureSpec
   */
  private int measureWidth(int measureSpec) {
    int result = 0;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);

    if (specMode == MeasureSpec.EXACTLY) {
      // We were told how big to be
      result = specSize;
    } else {
      // Take as much space as we can
      if (specMode == MeasureSpec.AT_MOST) {
        result = specSize;
      } else {
        result = this.getWidth();
      }

    }

    return result;
  }

  /**
   * Determines the height of this view
   * 
   * @param measureSpec
   *          A measureSpec packed into an int
   * @return The height of the view, honoring constraints from measureSpec
   */
  private int measureHeight(int measureSpec) {
    int result = 0;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);

    if (specMode == MeasureSpec.EXACTLY) {
      // We were told how big to be
      result = specSize;
    } else {

      if (specMode == MeasureSpec.AT_MOST) {
        result = specSize;
      } else {
        result = this.getHeight();
      }
    }
    return result;
  }

  protected void move() {
    if (screenSaver) {

      vF += moveVStep;
      if (vF > 1) {
        vF = 1;
        moveVStep = -1.0d * (r.nextGaussian() * 0.01d);
      }
      if (vF < 0) {
        vF = 0;
        moveVStep = (r.nextGaussian() * 0.01d);
      }

      hF += moveHStep;
      if (hF > 1) {
        hF = 1;
        moveHStep = -1.0d * (r.nextGaussian() * 0.01d);
      }
      if (hF < 0) {
        hF = 0;
        moveHStep = (r.nextGaussian() * 0.01d);
      }
      invalidate();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (time == null)
      return;

    float horz = ((float) (canvas.getWidth() - wtWidth));
    float vert = ((float) (canvas.getHeight() - wtHeight));

    float horzDelta = (wtWidth - tWidth) / 2;

    canvas.drawText(time, (horz * hF) + horzDelta, (vert * vF) + wtHeight, paint);
    return;
  }

}
