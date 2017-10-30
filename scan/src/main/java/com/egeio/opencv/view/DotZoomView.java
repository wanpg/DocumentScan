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
import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.tools.Utils;
import com.egeio.scan.R;

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

    private Debug debug = new Debug(DotZoomView.class.getSimpleName());
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ScanInfo scanInfo;
    private Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
    private Xfermode xfermodeDSTOUT = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    private Matrix matrix = new Matrix();

    public void setScanInfoAndBitmap(ScanInfo scanInfo, Bitmap bitmap) {
        this.scanInfo = scanInfo;
        this.bitmap = bitmap;
        calSeveralSize();
    }

    private PointD dotPointD;
    private List<PointD> pointDList;
    private Bitmap bitmap;
    private Bitmap roundBitmap;
    Path path = new Path();

    /**
     * @param dotPointD  显示图片对应坐标系的当前的点
     * @param pointDList
     */
    public void drawDot(PointD dotPointD, List<PointD> pointDList) {
        this.dotPointD = dotPointD;
        this.pointDList = pointDList;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        } else {
            createRoundBitmap();
        }
        postInvalidate();
    }

    private int width, height;
    private int rotateAngle;
    // 相对于原始尺寸的绘制比例
    float drawScaleRatio;
    // 相对于屏幕显示尺寸缩放比例
    final float zoomScale = 2.5f;
    float offsetX, offsetY;
    int canvasWidth, canvasHeight;

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap == null || dotPointD == null) {
            return;
        }

        debug.start("绘制放大镜");

        // 创建新的layer
        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();

        paint.reset();
        paint.setAntiAlias(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            drawLargeKITKAT(canvas);
        } else {
            drawNormal(canvas);
        }
        debug.end("绘制放大镜");
    }

    private void calSeveralSize() {
        rotateAngle = scanInfo.getRotateAngle().getValue();
        width = getWidth() - getPaddingLeft() - getPaddingRight();
        height = getHeight() - getPaddingTop() - getPaddingBottom();

        int rotatedBitmapWidth = rotateAngle == 90 || rotateAngle == 270 ? bitmap.getHeight() : bitmap.getWidth();
        int rotatedBitmapHeight = rotateAngle == 90 || rotateAngle == 270 ? bitmap.getWidth() : bitmap.getHeight();

        float widthScaleRatio = width * 1f / rotatedBitmapWidth;
        float heightScaleRatio = height * 1f / rotatedBitmapHeight;

        // 缩放至屏幕合适的尺寸
        final float scale = Math.min(widthScaleRatio, heightScaleRatio);

        // 相对于原始尺寸的绘制比例
        drawScaleRatio = scale * zoomScale;

        offsetX = getPaddingLeft();
        offsetY = getPaddingTop() - Utils.dp2px(getContext(), 80);
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void drawLargeKITKAT(Canvas canvas) {
        int layerId = canvas.saveLayer(0, 0, canvasWidth, canvasHeight, paint, Canvas.ALL_SAVE_FLAG);

        //绘制缩放的图片
        drawScaleImage(canvas);

        // 绘制线
        // HUAWEI-P7 绘制3ms
        drawScaleLines(canvas);

        // 绘制圆
        drawCircle(canvas, layerId);
    }

    private void drawNormal(Canvas canvas) {
        int layerId = canvas.saveLayer(0, 0, canvasWidth, canvasHeight, paint, Canvas.ALL_SAVE_FLAG);
        // 绘制缩放的图片
        drawScaleImage(canvas);

        // 绘制线
        drawScaleLines(canvas);

        drawMagnifyCircle(canvas, layerId);
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void drawCircle(Canvas canvas, int restoreLayerId) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        Path path = new Path();
        path.addRect(0, 0, canvas.getWidth(), canvas.getHeight(), Path.Direction.CCW);

        Path circlePath = new Path();
        circlePath.addCircle((float) dotPointD.x, (float) dotPointD.y + offsetY, Utils.dp2px(getContext(), 61.5f), Path.Direction.CCW);
        path.op(circlePath, Path.Op.XOR);

        // FIXME: 2017/10/31 HUAWEI-P7绘制 35ms
        paint.setXfermode(xfermodeDSTOUT);
        canvas.drawPath(path, paint);
        paint.setXfermode(null);
        canvas.restoreToCount(restoreLayerId);

        // 绘制背景白色
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Utils.dp2px(getContext(), 3));
        canvas.drawCircle((float) dotPointD.x, (float) dotPointD.y + offsetY, Utils.dp2px(getContext(), 61.5f), paint);
    }

    /**
     * 绘制放大的图片
     *
     * @param canvas
     */
    private void drawScaleImage(Canvas canvas) {
        matrix.reset();
        matrix.preTranslate(-bitmap.getWidth() / 2f, -bitmap.getHeight() / 2f);
        matrix.postRotate(rotateAngle);
        matrix.postScale(drawScaleRatio, drawScaleRatio);
        final double rx = (dotPointD.x) * (zoomScale - 1) - width * (zoomScale - 1) / 2f;
        final double ry = (dotPointD.y) * (zoomScale - 1) - height * (zoomScale - 1) / 2f;
        matrix.postTranslate(width / 2f + offsetX - (float) rx, height / 2f + offsetY - (float) ry);

        // FIXME: 2017/10/31 HUAWEI-P7绘制 50ms
        canvas.drawBitmap(bitmap, matrix, paint);
    }

    /**
     * 绘制放大的边框
     *
     * @param canvas
     */
    private void drawScaleLines(Canvas canvas) {
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
    }

    /**
     * 绘制放大镜，用bitmap作为相交计算的蒙版
     *
     * @param canvas
     * @param restoreLayerId
     */
    private void drawMagnifyCircle(Canvas canvas, int restoreLayerId) {
        paint.setXfermode(xfermode);
        canvas.drawBitmap(roundBitmap, 0, 0, paint);
        paint.setXfermode(null);
        canvas.restoreToCount(restoreLayerId);
        // 绘制背景白色
        paint.reset();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Utils.dp2px(getContext(), 3));
        canvas.drawCircle((float) dotPointD.x, (float) dotPointD.y + offsetY, Utils.dp2px(getContext(), 61.5f), paint);
    }

    private void createRoundBitmap() {
        if (dotPointD != null) {
            Utils.recycle(roundBitmap);
            roundBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(roundBitmap);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            c.drawCircle((float) dotPointD.x, (float) dotPointD.y + getPaddingTop() - Utils.dp2px(getContext(), 80), Utils.dp2px(getContext(), 63), p);
        }
    }
}
