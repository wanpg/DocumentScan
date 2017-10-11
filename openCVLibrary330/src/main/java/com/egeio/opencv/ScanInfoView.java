package com.egeio.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

import com.egeio.opencv.model.PointD;
import com.egeio.opencv.tools.Utils;

import java.util.ArrayList;
import java.util.List;

public class ScanInfoView extends View {
    public ScanInfoView(Context context) {
        super(context);
    }

    public ScanInfoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScanInfoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScanInfoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private final List<PointD> pointList = new ArrayList<>();
    private Bitmap thumbnail;
    float scale = 1f;
    Paint paint;

    public synchronized void setPoint(List<PointD> pointList, float scale) {
        this.scale = scale;
        this.pointList.clear();
        if (pointList != null) {
            this.pointList.addAll(pointList);
        }
        postInvalidate();
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
        postInvalidate();
    }

    public void clear() {
        if (pointList.isEmpty()) {
            return;
        }
        this.pointList.clear();
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!pointList.isEmpty()) {
            drawPoint(canvas, pointList);
        }
    }

    private void drawPoint(Canvas canvas, List<PointD> pointList) {
        if (canvas != null) {
            if (thumbnail != null && !thumbnail.isRecycled()) {
                canvas.drawBitmap(thumbnail, 0, 0, null);
            }

            if (pointList != null && pointList.size() == 4) {
                if (paint == null) {
                    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setColor(Color.YELLOW);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Utils.dp2px(getContext(), 2));
                }

                Path path = new Path();
                path.moveTo((float) pointList.get(0).x / scale, (float) pointList.get(0).y / scale);
                path.lineTo((float) pointList.get(1).x / scale, (float) pointList.get(1).y / scale);
                path.lineTo((float) pointList.get(2).x / scale, (float) pointList.get(2).y / scale);
                path.lineTo((float) pointList.get(3).x / scale, (float) pointList.get(3).y / scale);
                path.close();

                canvas.drawPath(path, paint);

                PointD center = Utils.calCenter(pointList.get(0), pointList.get(1), pointList.get(2), pointList.get(3));
                canvas.drawCircle((float) center.x / scale, (float) center.y / scale, 30, paint);
            }
        }
    }
}