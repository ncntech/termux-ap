package com.termux.app.nastech;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * NasTech AI Terminal — 3D Wave Animation for Lock Screen
 */
public class NasTechWaveView extends View {

    private final Paint mPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private float mPhase = 0f;
    private final android.os.Handler mHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable mAnimator = () -> { mPhase += 0.04f; invalidate(); };

    public NasTechWaveView(Context context) { super(context); init(); }
    public NasTechWaveView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        mPaint1.setColor(0x2200C8FF); mPaint1.setStyle(Paint.Style.FILL);
        mPaint2.setColor(0x1500A8DD); mPaint2.setStyle(Paint.Style.FILL);
        mPaint3.setColor(0x0D007799); mPaint3.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        drawWave(canvas, mPaint1, w, h, 0.6f, mPhase, 80);
        drawWave(canvas, mPaint2, w, h, 0.5f, mPhase + 1f, 60);
        drawWave(canvas, mPaint3, w, h, 0.4f, mPhase + 2f, 40);
        mHandler.postDelayed(mAnimator, 16);
    }

    private void drawWave(Canvas c, Paint p, int w, int h, float heightRatio, float phase, int amp) {
        mPath.reset();
        float base = h * heightRatio;
        mPath.moveTo(0, base);
        for (int x = 0; x <= w; x += 4) {
            float y = base + (float)(amp * Math.sin((x * 0.012f) + phase));
            mPath.lineTo(x, y);
        }
        mPath.lineTo(w, h); mPath.lineTo(0, h); mPath.close();
        c.drawPath(mPath, p);
    }

    @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); mHandler.removeCallbacks(mAnimator); }
}
