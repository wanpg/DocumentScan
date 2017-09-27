package com.egeio.document.scan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/9/25.
 */

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

    private final List<Point> pointList = new ArrayList<>();
    float scale = 1f;

    public void setPoint(List<Point> pointList, float scale) {
        this.scale = scale;
        this.pointList.clear();
        if (pointList != null) {
            this.pointList.addAll(pointList);
        }
        postInvalidate();
    }

    Paint paint;

    private void drawPoint(Canvas canvas, List<Point> pointList) {
        if (canvas != null && pointList != null && pointList.size() == 4) {
            if (paint == null) {
                paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(Color.YELLOW);
                paint.setAlpha(128);
                paint.setStrokeWidth(15);
            }

            // 最顶部的且是相对最左侧的point

            Path path = new Path();
            path.moveTo((float) pointList.get(0).x / scale, (float) pointList.get(0).y / scale);
            path.lineTo((float) pointList.get(1).x / scale, (float) pointList.get(1).y / scale);
            path.lineTo((float) pointList.get(2).x / scale, (float) pointList.get(2).y / scale);
            path.lineTo((float) pointList.get(3).x / scale, (float) pointList.get(3).y / scale);
            path.lineTo((float) pointList.get(0).x / scale, (float) pointList.get(0).y / scale);

            canvas.drawPath(path, paint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!pointList.isEmpty()) {
            drawPoint(canvas, pointList);
        }
    }

    public void clear() {
        this.pointList.clear();
        postInvalidate();
    }
}
