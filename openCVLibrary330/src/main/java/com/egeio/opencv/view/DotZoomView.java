package com.egeio.opencv.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.Utils;

import org.opencv.R;

import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/17.
 */

public class DotZoomView extends View {

    public DotZoomView(Context context) {
        super(context);
    }

    public DotZoomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DotZoomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DotZoomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ScanInfo scanInfo;
    private Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    private Matrix matrix = new Matrix();

    public void setScanInfo(ScanInfo scanInfo) {
        this.scanInfo = scanInfo;
    }

    private PointD dotPointD;
    private List<PointD> pointDList;
    private Bitmap bitmap;
    private Bitmap roundBitmap;
    Path path = new Path();

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    /**
     * @param dotPointD 显示图片对应坐标系的当前的点
     */
    public void drawDot(PointD dotPointD, List<PointD> pointDList) {
        this.dotPointD = dotPointD;
        this.pointDList = pointDList;
        if (dotPointD != null) {
            Utils.recycle(roundBitmap);
            roundBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(roundBitmap);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            c.drawCircle((float) dotPointD.x, (float) dotPointD.y + getPaddingTop() - Utils.dp2px(getContext(), 80), Utils.dp2px(getContext(), 63), p);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap == null || dotPointD == null) {
            return;
        }

        final int rotateAngle = scanInfo.getRotateAngle().getValue();
        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();

        int rotatedBitmapWidth = rotateAngle == 90 || rotateAngle == 270 ? bitmap.getHeight() : bitmap.getWidth();
        int rotatedBitmapHeight = rotateAngle == 90 || rotateAngle == 270 ? bitmap.getWidth() : bitmap.getHeight();

        float widthScaleRatio = width * 1f / rotatedBitmapWidth;
        float heightScaleRatio = height * 1f / rotatedBitmapHeight;

        // 缩放至屏幕合适的尺寸
        final float scale = Math.min(widthScaleRatio, heightScaleRatio);
        // 相对于屏幕显示尺寸缩放比例
        final int zoomScale = 3;
        // 相对于原始尺寸的绘制比例
        float drawScaleRatio = scale * zoomScale;

        final float offsetX = getPaddingLeft();
        final float offsetY = getPaddingTop() - Utils.dp2px(getContext(), 80);

        paint.reset();
        paint.setAntiAlias(true);

        // 创建新的layer
        final int canvasWidth = canvas.getWidth();
        final int canvasHeight = canvas.getHeight();
        int layerId = canvas.saveLayer(0, 0, canvasWidth, canvasHeight, paint, Canvas.ALL_SAVE_FLAG);

        // 绘制图片
        matrix.reset();
        matrix.preTranslate(-bitmap.getWidth() / 2f, -bitmap.getHeight() / 2f);
        matrix.postRotate(rotateAngle);
        matrix.postScale(drawScaleRatio, drawScaleRatio);
        final double rx = (dotPointD.x) * (zoomScale - 1) - width * (zoomScale - 1) / 2f;
        final double ry = (dotPointD.y) * (zoomScale - 1) - height * (zoomScale - 1) / 2f;
        matrix.postTranslate(width / 2f + offsetX - (float) rx, height / 2f + offsetY - (float) ry);
        canvas.drawBitmap(bitmap, matrix, paint);

        // 绘制线
        if (pointDList != null) {
            final float offsetDotX = (float) (dotPointD.x * (zoomScale - 1));
            final float offsetDotY = (float) (dotPointD.y * (zoomScale - 1));

            path.reset();
            boolean isFirst = true;
            for (PointD pointD : pointDList) {
                double x = pointD.x * zoomScale - offsetDotX;
                double y = pointD.y * zoomScale - offsetDotY + offsetY;

                if (isFirst) {
                    path.moveTo((float) x, (float) y);
                } else {
                    path.lineTo((float) x, (float) y);
                }
                isFirst = false;
            }
            path.close();

            paint.setColor(ContextCompat.getColor(getContext(), R.color.select_line));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Utils.dp2px(getContext(), 2.5f));
            canvas.drawPath(path, paint);
        }

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(roundBitmap, 0, 0, paint);
        paint.setXfermode(null);

        canvas.restoreToCount(layerId);


        // 绘制背景白色
        paint.reset();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Utils.dp2px(getContext(), 3));
        canvas.drawCircle((float) dotPointD.x, (float) dotPointD.y + offsetY, Utils.dp2px(getContext(), 61.5f), paint);
    }

    /*@Override
    protected void onDraw(Canvas canvas) {
        if (bitmap == null || dotPointD == null) {
            return;
        }

        final int rotateAngle = scanInfo.getRotateAngle().getValue();
        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();

        int rotatedBitmapWidth = rotateAngle == 90 || rotateAngle == 270 ? bitmap.getHeight() : bitmap.getWidth();
        int rotatedBitmapHeight = rotateAngle == 90 || rotateAngle == 270 ? bitmap.getWidth() : bitmap.getHeight();

        float widthScaleRatio = width * 1f / rotatedBitmapWidth;
        float heightScaleRatio = height * 1f / rotatedBitmapHeight;

        // 缩放至屏幕合适的尺寸
        final float scale = Math.min(widthScaleRatio, heightScaleRatio);
        // 相对于屏幕显示尺寸缩放比例
        final int zoomScale = 3;
        // 相对于原始尺寸的绘制比例
        float drawScaleRatio = scale * zoomScale;

        final float offsetX = (width - rotatedBitmapWidth * scale) / 2;
        final float offsetY = (height - rotatedBitmapHeight * scale) / 2;

        paint.reset();
        paint.setAntiAlias(true);

        // 绘制背景白色
        paint.setColor(Color.WHITE);
        canvas.drawCircle((float) dotPointD.x, (float) dotPointD.y + getPaddingTop(), Utils.dp2px(getContext(), 63), paint);

        // 创建新的layer
        final int canvasWidth = canvas.getWidth();
        final int canvasHeight = canvas.getHeight();
        int layerId = canvas.saveLayer(0, 0, canvasWidth, canvasHeight, paint, Canvas.ALL_SAVE_FLAG);

        // 绘制蒙版白色
        canvas.drawCircle((float) dotPointD.x, (float) dotPointD.y + getPaddingTop(), Utils.dp2px(getContext(), 60), paint);

        // 设置图层混合模式
        paint.setXfermode(xfermode);

        // 绘制图片
        matrix.reset();
        matrix.preTranslate(-bitmap.getWidth() / 2f, -bitmap.getHeight() / 2f);
        matrix.postRotate(rotateAngle);
        matrix.postScale(drawScaleRatio, drawScaleRatio);
        final double rx = (dotPointD.x) * (zoomScale - 1) - width * (zoomScale - 1) / 2f;
        final double ry = (dotPointD.y) * (zoomScale - 1) - height * (zoomScale - 1) / 2f;
        matrix.postTranslate(width / 2f + getPaddingLeft() - (float) rx, height / 2f + getPaddingTop() - (float) ry);
        canvas.drawBitmap(bitmap, matrix, paint);

        // 合并图层到制定layer
        paint.setXfermode(null);
        canvas.restoreToCount(layerId);

        canvas.restore();
        // 绘制线
        if (pointDList != null) {
            layerId = canvas.saveLayer(0, 0, canvasWidth, canvasHeight, paint, Canvas.ALL_SAVE_FLAG);
            paint.setColor(Color.WHITE);
            canvas.drawCircle((float) dotPointD.x, (float) dotPointD.y + getPaddingTop(), Utils.dp2px(getContext(), 60), paint);
            final float offsetDotX = (float) (dotPointD.x * (zoomScale - 1));
            final float offsetDotY = (float) (dotPointD.y * (zoomScale - 1));

            path.reset();
            Double minX = null, minY = null, maxX = null, maxY = null;
            boolean isFirst = true;
            for (PointD pointD : pointDList) {
                double x = pointD.x * zoomScale - offsetDotX;
                double y = pointD.y * zoomScale - offsetDotY + getPaddingTop();

                minX = minX == null ? x : Math.min(minX, x);
                minY = minY == null ? y : Math.min(minY, y);
                maxX = maxX == null ? x : Math.max(minX, x);
                maxY = maxY == null ? y : Math.max(maxY, y);
                if (isFirst) {
                    path.moveTo((float) x, (float) y);
                } else {
                    path.lineTo((float) x, (float) y);
                }
                isFirst = false;
            }
            path.close();

            paint.setXfermode(xfermode);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.select_line));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Utils.dp2px(getContext(), 2.5f));
            canvas.drawPath(path, paint);
            paint.setXfermode(null);

            canvas.restoreToCount(layerId);
        }
    }*/

    PointD calPoint(PointD dotPoint, PointD point, PointD center, int r) {

        double a2 = center.x;
        double b2 = center.y;

        if (dotPoint.y == point.y) {
            // 纵坐标相等，水平直线
            double y = dotPoint.y;
            double d = Math.sqrt(r * r - Math.pow(y - b2, 2));
            double x1 = d + a2;
            double x2 = -d + a2;
            return new PointD(x1, dotPoint.y);
        } else if (dotPoint.x == point.x) {
            double x = dotPoint.x;
            double d = Math.sqrt(r * r - Math.pow(x - a2, 2));
            double y1 = d + b2;
            double y2 = -d + b2;
            return new PointD(dotPoint.x, y1);
        } else {
            double a1 = (dotPoint.y - point.y) / (dotPoint.x - point.x);
            double b1 = dotPoint.y - a1 * dotPoint.x;

            double a = 1 + a1 * a1;
            double b = -2 * a2 + 2 * a1 * b1 - 2 * a1 * b2;
            double c = a2 * a2 + b1 * b1 + b2 + b2 - 2 * b1 * b2 - r * r;

            double x, y;
            if (a == 0) {
                x = -c / b;
                y = x * a1 + b1;
                return new PointD(x, y);
            } else {
                double delt = 2 * b - 4 * a * c;
                if (delt > 0) {
                    double x1 = (-b + Math.sqrt(delt)) / (2 * a);
                    double y1 = x1 * a1 + b1;
                    double x2 = (-b - Math.sqrt(delt)) / (2 * a);
                    double y2 = x2 * a1 + b1;
                    return new PointD(x1, y1);
                } else if (delt == 0) {
                    x = -b / (2 * a);
                    y = x * a1 + b1;
                    return new PointD(x, y);
                } else {
                    return null;
                }
            }
        }
    }
}
