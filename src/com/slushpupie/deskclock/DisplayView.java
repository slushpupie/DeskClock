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
	
	private String hr = "12";
	private String min = "00";
	private String sec = "";
	private String mer = "";
	
	private int wideDigit = 0;
	private int tallDigit = 0;
	private int wideLetter = 0;
	private int tallLetter = 0;
	private int wideColon = 0;
	private int tallColon = 0;
	
	private Paint paint;
	
	private boolean screenSaver = false;
	private boolean drawColon = true;
	
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
	
	public void setWideChar(String wideDigit, String wideLetter) {
		
		Rect boundingBox = new Rect(0, 0, 0, 0);
		paint.getTextBounds(wideDigit, 0, 1, boundingBox);
		this.wideDigit = boundingBox.width();
		this.tallDigit = boundingBox.height();
		paint.getTextBounds(wideLetter, 0, 1, boundingBox);
		this.wideLetter = boundingBox.width();
		this.tallLetter = boundingBox.height();
		paint.getTextBounds(":", 0, 1, boundingBox);
		this.wideColon = boundingBox.width();
		this.tallColon = boundingBox.height();
		
	}
	
	public void setTime(CharSequence time, CharSequence hr, CharSequence min, CharSequence sec, CharSequence mer) {
		if(!this.time.equals(time)) {
			this.time = time.toString();
			this.hr = hr.toString();
			this.min = min.toString();
			this.sec = sec.toString();
			this.mer = mer.toString();
			
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
	
	public void setDrawColon(boolean drawColon) {
		this.drawColon = drawColon;
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
    
    private void drawLetter(Canvas canvas, String letter, float width, float x, float y) {
    	
    	Rect boundingBox = new Rect(0,0,0,0);
    	paint.getTextBounds(letter, 0, letter.length(), boundingBox);

    	float dx = (width - boundingBox.width());
    	canvas.drawText(letter, x+dx, y, paint);
    }

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(time == null) 
			return;

		int maxWidth = wideDigit*4 + wideColon;
		if(sec.length() > 0) {
			maxWidth += wideDigit*2 + wideColon;
		}
		if(mer.length() > 0) {
			maxWidth += wideLetter*3;
		}
		
		float horz = ((float)(canvas.getWidth() - maxWidth)) * hF;
		float vert = ((float)(canvas.getHeight()) * vF  + ((float)tallDigit)/2f);
		
		float width = 0;
		float pad = (float)wideDigit * 0.1f;
		drawLetter(canvas, hr, wideDigit*2+pad, horz, vert); width += wideDigit*2+pad+pad;
		if(drawColon) 
			drawLetter(canvas, ":", wideColon+pad+pad, horz+width, vert);
		width += wideColon+pad+pad+pad;
		drawLetter(canvas, min, wideDigit*2+pad, horz+width, vert); width += wideDigit*2+pad+pad;
		if(sec.length() > 0) {
			if(drawColon)
				drawLetter(canvas, ":", wideColon+pad+pad, horz+width, vert);
			width += wideColon+pad+pad+pad;
			drawLetter(canvas, sec, wideDigit*2+pad, horz+width, vert); width += wideDigit*2+pad+pad;
		}
		if(mer.length() > 0) {
			drawLetter(canvas, " ", wideColon+pad, horz+width, vert); width += wideColon+pad+pad;
			drawLetter(canvas, mer, wideLetter*2+pad, horz+width, vert); width += wideLetter*2+pad+pad;
		}

		invalidate();
		return;
	}	
}
