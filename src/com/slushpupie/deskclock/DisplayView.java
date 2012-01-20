package com.slushpupie.deskclock;

import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class DisplayView extends View {
	
	private String time = "88:88";
	private String wideTime = "88:88";
	
	private Paint paint;
	
	private boolean screenSaver = false;
	
	private Random r = new Random();

	private float vF = 0.5f;
	private float hF = 0.5f;

	public DisplayView(Context context) {
		super(context);
		initComponents();
	}
	
	public DisplayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initComponents();
	}
	
	private void initComponents() {
		paint = new Paint();
		setPadding(1,1,1,1);
	}
	
	public void setWideTime(String wideTime) {
		this.wideTime = wideTime;
	}
	
	public void setTime(CharSequence time) {
		if(!this.time.equals(time)) {
			this.time = time.toString();
			if(screenSaver) {
				vF = r.nextFloat();
				hF = r.nextFloat();
			}
			requestLayout();
			invalidate();
		}
	}
	
	public void setSize(float size) {
		paint.setTextSize(size);
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
		if(!screenSaver) {
			vF = 0.5f;
			hF = 0.5f;
		}

		requestLayout();
		invalidate();
	}
	
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }
    
    
    /**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
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
     * @param measureSpec A measureSpec packed into an int
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

	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(time == null) 
			return;
		
		Rect boundingBox1 = new Rect(0, 0, 0, 0);
		paint.getTextBounds(wideTime, 0, wideTime.length(), boundingBox1);
		Rect boundingBox2 = new Rect(0, 0, 0, 0);
		paint.getTextBounds(time, 0, time.length(), boundingBox2);
		
		float horz = ((float)(canvas.getWidth() - boundingBox1.width()));
		float vert = ((float)(canvas.getHeight() - boundingBox1.height()));
		
		
		float horzDelta = (boundingBox1.width() - boundingBox2.width());

		canvas.drawText(time, (horz*hF)+horzDelta, vert*vF+boundingBox2.height(), paint);
		invalidate();
		return;
	}
	
	
	
}
