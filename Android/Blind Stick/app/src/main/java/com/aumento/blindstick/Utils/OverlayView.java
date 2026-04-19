package com.aumento.blindstick.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class OverlayView extends View {

    private Paint boxPaint = new Paint();
    private float[][] boxes;
    private float[] scores;
    private int imgW, imgH;

    public OverlayView(Context c, AttributeSet a) {
        super(c, a);
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);
    }

    public void setResults(float[][] b, float[] s, float[] c,
                           int w, int h) {
        boxes = b;
        scores = s;
        imgW = w;
        imgH = h;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (boxes == null) return;

        float scaleX = getWidth();
        float scaleY = getHeight();

        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0.5f) {
                RectF rect = new RectF(
                        boxes[i][1] * scaleX,
                        boxes[i][0] * scaleY,
                        boxes[i][3] * scaleX,
                        boxes[i][2] * scaleY
                );
                canvas.drawRect(rect, boxPaint);
            }
        }
    }
}