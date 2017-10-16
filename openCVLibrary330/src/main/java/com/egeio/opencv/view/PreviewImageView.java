package com.egeio.opencv.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by wangjinpeng on 2017/10/13.
 */

public class PreviewImageView extends View {


    private Matrix matrix = new Matrix();

    public PreviewImageView(Context context) {
        super(context);
    }

    public PreviewImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PreviewImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PreviewImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private Bitmap bitmap;
    private int rotateAngle;

    public synchronized void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public synchronized void setRotateAngle(int rotateAngle) {
        this.rotateAngle = rotateAngle;
    }

    public int getRotateAngle() {
        return rotateAngle;
    }

    private float drawScaleRatio = 1f;

    public float getDrawScaleRatio() {
        return drawScaleRatio;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        synchronized (PreviewImageView.this) {
            if (bitmap == null) {
                return;
            }
            final int width = getWidth() - getPaddingLeft() - getPaddingRight();
            final int height = getHeight() - getPaddingTop() - getPaddingBottom();

            int rotatedBitmapWidth = rotateAngle == 90 || rotateAngle == 270 ? bitmap.getHeight() : bitmap.getWidth();
            int rotatedBitmapHeight = rotateAngle == 90 || rotateAngle == 270 ? bitmap.getWidth() : bitmap.getHeight();

            float widthScaleRatio = width * 1f / rotatedBitmapWidth;
            float heightScaleRatio = height * 1f / rotatedBitmapHeight;

            drawScaleRatio = Math.min(widthScaleRatio, heightScaleRatio);

            matrix.reset();
            matrix.preTranslate(-bitmap.getWidth() / 2f, -bitmap.getHeight() / 2f);
            matrix.postRotate(rotateAngle);
            matrix.postScale(drawScaleRatio, drawScaleRatio);
            matrix.postTranslate(width / 2f + getPaddingLeft(), height / 2f + getPaddingTop());
            canvas.drawBitmap(bitmap, matrix, null);
        }
    }
}
