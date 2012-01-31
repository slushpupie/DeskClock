package com.slushpupie.deskclock;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageViewText extends ImageView {

	private int mColor = 0;
	private String mText = null;

	public ImageViewText(Context context) {
		super(context);
	}

	public ImageViewText(Context context, AttributeSet attrs) {
		super(context, attrs);
		setAttrs(context, attrs, 0);
	}

	public ImageViewText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setAttrs(context, attrs, defStyle);

	}

	private void setAttrs(Context context, AttributeSet attrs, int defStyle) {
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.ImageViewText);
		context.obtainStyledAttributes(attrs, R.styleable.ImageViewText,
				defStyle, 0);

		CharSequence s = a.getString(R.styleable.ImageViewText_text);
		if (s != null)
			setText(s.toString());
	}

	@Override
	public void setBackgroundColor(int color) {
		super.setBackgroundColor(color);
		mColor = color;
	}

	public void setText(String text) {
		mText = text;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mText == null)
			return;

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setTextSize(12);
		paint.setTextAlign(Paint.Align.CENTER);

		if (Color.red(mColor) + Color.green(mColor) + Color.blue(mColor) < 384)
			paint.setColor(Color.WHITE);
		else
			paint.setColor(Color.BLACK);

		if (this.getHeight() > this.getWidth()) {
			canvas.rotate(90, this.getWidth() / 2, this.getHeight() / 2);
			canvas.drawText(mText, this.getWidth() / 2, this.getHeight() / 2,
					paint);
			canvas.restore();
		} else {
			canvas.drawText(mText, this.getWidth() / 2,
					this.getHeight() * 0.666f, paint);
		}

	}

}
