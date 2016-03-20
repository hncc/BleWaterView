package com.example.view;

import java.text.DecimalFormat;

import com.example.blewaterdemo.R;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Region.Op;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * ˮ�������ν���View
 *
 * @author cc
 * 
 */
@SuppressLint("DefaultLocale") public class WaterView extends FrameLayout {
	private static final int DEFAULT_TEXTCOLOT = 0xFFFFFFFF;

	private static final int DEFAULT_TEXTSIZE = 80;

	private double mPercent;
	private float mLinepercent;

	private Paint mPaint = new Paint();

	private Bitmap mBitmap;

	private Bitmap mScaledBitmap;

	private float mLeft;

	private int mSpeed = 10;

	private int mRepeatCount = 0;

	private Status mFlag = Status.NONE;

	private int mTextColor = DEFAULT_TEXTCOLOT;

	private int mTextSize = DEFAULT_TEXTSIZE;

	public WaterView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setTextColor(int color) {
		mTextColor = color;
	}

	public void setTextSize(int size) {
		mTextSize = size;
	}

	public void setPercent(double percent, float Linepercent) {
		mFlag = Status.RUNNING;
		mPercent = percent;
		mLinepercent = Linepercent;
		postInvalidate();

	}

	public void setStatus(Status status) {
		mFlag = status;
	}

	public void clear() {
		mFlag = Status.NONE;
		if (mScaledBitmap != null) {
			mScaledBitmap.recycle();
			mScaledBitmap = null;
		}

		if (mBitmap != null) {
			mBitmap.recycle();
			mBitmap = null;
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		int width = getWidth();
		int height = getHeight();

		// �ü���Բ����
		Path path = new Path();
		canvas.save();
		path.reset();
		canvas.clipPath(path);
		path.addCircle(width / 2, height / 2, width / 2, Direction.CCW);
		canvas.clipPath(path, Op.REPLACE);

		if (mFlag == Status.RUNNING) {
			if (mScaledBitmap == null) {
				mBitmap = BitmapFactory.decodeResource(getContext()
						.getResources(), R.drawable.wave2b);
				mScaledBitmap = Bitmap.createScaledBitmap(mBitmap,
						mBitmap.getWidth(), getHeight(), false);
				mBitmap.recycle();
				mBitmap = null;
				mRepeatCount = (int) Math.ceil(getWidth()
						/ mScaledBitmap.getWidth() + 0.5) + 1;
			}
			// 1-mpercentԲ��װ��Ϊ100%��1-mpercent*1.5Ϊ60%��㣬1 - mPercent *
			// 1.65����Ϊ60%
			for (int idx = 0; idx < mRepeatCount; idx++) {
				canvas.drawBitmap(mScaledBitmap, mLeft + (idx - 1)
						* mScaledBitmap.getWidth(),
						(float) ((1 - mPercent * 1.65) * getHeight()), null);
			}
			DecimalFormat    df   = new DecimalFormat("######0.0");  
			String x=df.format(mPercent * 100);
			 String str = x + "%";
			 
////			String str = String.format("%.1f", mPercent);
//			BigDecimal   b   =   new   BigDecimal(mPercent);  
//			double   s   =   b.setScale(3,   BigDecimal.ROUND_HALF_UP).doubleValue(); 
//			String str=s*100+"%";
			
			
			mPaint.setColor(mTextColor);
			mPaint.setTextSize(mTextSize);
			mPaint.setStyle(Style.FILL);
			canvas.drawText(str, (getWidth() - mPaint.measureText(str)) / 2,
					getHeight() / 2 + mTextSize / 2, mPaint);

			mLeft += mSpeed;
			if (mLeft >= mScaledBitmap.getWidth())
				mLeft = 0;
			// ������Բ��
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(15);
			mPaint.setAntiAlias(true);
			mPaint.setColor(Color.WHITE);
			canvas.drawCircle(width / 2, height / 2, width / 2 - 2, mPaint);
			// =================================


			postInvalidateDelayed(20);
		}
		canvas.restore();

	}

	public enum Status {
		RUNNING, NONE
	}

}
