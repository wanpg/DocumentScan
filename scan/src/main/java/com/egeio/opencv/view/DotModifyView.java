package com.egeio.opencv.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.tools.Utils;
import com.egeio.scan.R;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/13.
 */

public class DotModifyView extends View {

    public interface OnDotChangeListener {
        void onDotChange(PointD dotPointD, List<PointD> pointDList);
    }

    public DotModifyView(Context context) {
        super(context);
        init();
    }

    public DotModifyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DotModifyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DotModifyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private Debug debug = new Debug(DotModifyView.class.getSimpleName());

    private Paint pathPaint, dotPaint, maskPaint;
    private ScanInfo scanInfo;
    private boolean initialize = false;
    private int pathWidth, dotPathWidth, cornerRadius;

    private Path pointPath = new Path();
    private RectF maskRectF = new RectF();
    private Path maskPath = new Path();

    private Path verifyPath = new Path();
    final PorterDuffXfermode xfermodeClear = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

    private OnDotChangeListener onDotChangeListener;

    public void setOnDotChangeListener(OnDotChangeListener onDotChangeListener) {
        this.onDotChangeListener = onDotChangeListener;
    }

    private void init() {
        if (initialize) {
            return;
        }
        initialize = true;
        pathWidth = getResources().getDimensionPixelOffset(R.dimen.select_line_width_in_modify);
        dotPathWidth = Utils.dp2px(getContext(), 1f);
        cornerRadius = Utils.dp2px(getContext(), 20 / 2);

        pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.select_line));
        pathPaint.setStyle(Paint.Style.STROKE);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        dotPaint.setAlpha(127);//#7Fffffff

        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.BLACK);
        maskPaint.setAlpha((int) (255 * 0.3f));// 30%
    }

    private final List<PointD> rotatedPoints = new ArrayList<>();
    private float scale = 1f;

    public List<PointD> getModifiedPoints() {
        final Size originSize = scanInfo.getOriginSize();
        final int rotateAngle = scanInfo.getRotateAngle().getValue();

        double rotatedOriginWidth = rotateAngle == 90 || rotateAngle == 270 ? originSize.height : originSize.width;
        double rotatedOriginHeight = rotateAngle == 90 || rotateAngle == 270 ? originSize.width : originSize.height;
        return CvUtils.rotatePoints(rotatedPoints, rotatedOriginWidth, rotatedOriginHeight, (360 - rotateAngle) % 360);
    }

    /**
     * 图片绘制的起始偏移点
     */
    private PointF offsetPoint = new PointF();

    public void setScanInfo(ScanInfo scanInfo) {
        rotatedPoints.clear();
        this.scanInfo = scanInfo;
        calSeveralSize();
        if (scanInfo != null) {
            final Size originSize = scanInfo.getOriginSize();
            final int rotateAngleValue = scanInfo.getRotateAngle().getValue();
            final List<PointD> pointDList = CvUtils.rotatePoints(scanInfo.getCurrentPointInfo().getPoints(), originSize.width, originSize.height, rotateAngleValue);
            rotatedPoints.addAll(pointDList);
        }
        if (getWidth() > 0 && getHeight() > 0) {
            postInvalidate();
        }
    }

    int width, height;

    private void calSeveralSize() {
        final Size originSize = scanInfo.getOriginSize();
        final int rotateAngle = scanInfo.getRotateAngle().getValue();
        // point 旋转

        width = getWidth() - getPaddingLeft() - getPaddingRight();
        height = getHeight() - getPaddingTop() - getPaddingBottom();

        double rotatedOriginWidth = rotateAngle == 90 || rotateAngle == 270 ? originSize.height : originSize.width;
        double rotatedOriginHeight = rotateAngle == 90 || rotateAngle == 270 ? originSize.width : originSize.height;

        double widthScaleRatio = width / rotatedOriginWidth;
        double heightScaleRatio = height / rotatedOriginHeight;

        scale = (float) Math.min(widthScaleRatio, heightScaleRatio);

        offsetPoint.x = (float) ((width - rotatedOriginWidth * scale) / 2);
        offsetPoint.y = (float) ((height - rotatedOriginHeight * scale) / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制蒙版和边界区域
        if (scanInfo != null && !rotatedPoints.isEmpty()) {

            debug.start("绘制选中区域");

            // 选区路径
            pointPath.reset();
            pointPath.moveTo(offsetPoint.x + (float) rotatedPoints.get(0).x * scale, offsetPoint.y + (float) rotatedPoints.get(0).y * scale);
            pointPath.lineTo(offsetPoint.x + (float) rotatedPoints.get(1).x * scale, offsetPoint.y + (float) rotatedPoints.get(1).y * scale);
            pointPath.lineTo(offsetPoint.x + (float) rotatedPoints.get(2).x * scale, offsetPoint.y + (float) rotatedPoints.get(2).y * scale);
            pointPath.lineTo(offsetPoint.x + (float) rotatedPoints.get(3).x * scale, offsetPoint.y + (float) rotatedPoints.get(3).y * scale);
            pointPath.close();

            // 绘制蒙版
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                maskPath.reset();
                maskPath.addRect(getPaddingLeft(), getPaddingTop(), getPaddingLeft() + width, getPaddingTop() + height, Path.Direction.CCW);
                maskPath.op(pointPath, Path.Op.XOR);
                // FIXME: 2017/10/31 HUAWEI-P7手机上绘制一次15ms左右
                canvas.drawPath(maskPath, maskPaint);
            } else {
                maskRectF.left = getPaddingLeft();
                maskRectF.top = getPaddingTop();
                maskRectF.right = maskRectF.left + width;
                maskRectF.bottom = maskRectF.top + height;
                int canvasWidth = canvas.getWidth();
                int canvasHeight = canvas.getHeight();
                int layerId = canvas.saveLayer(0, 0, canvasWidth, canvasHeight, maskPaint, Canvas.ALL_SAVE_FLAG);
                canvas.drawRect(maskRectF, maskPaint);

                maskPaint.setXfermode(xfermodeClear);
                canvas.drawPath(pointPath, maskPaint);
                maskPaint.setXfermode(null);
                canvas.restoreToCount(layerId);
            }

            // 绘制 选区路径
            pathPaint.setStrokeWidth(pathWidth);
            canvas.drawPath(pointPath, pathPaint);

            // 给每个点, 绘制圆环
            pathPaint.setStrokeWidth(dotPathWidth);
            for (PointD point : rotatedPoints) {
                // 透明白色圆形
                canvas.drawCircle(offsetPoint.x + (float) point.x * scale, offsetPoint.y + (float) point.y * scale, cornerRadius, dotPaint);
                // 蓝色圆环
                canvas.drawCircle(offsetPoint.x + (float) point.x * scale, offsetPoint.y + (float) point.y * scale, cornerRadius, pathPaint);
            }
            debug.end("绘制选中区域");
        }
    }

    private PointD lastTouchPoint;
    private PointD curModifyPointD;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            recordTouchDown(event);
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (lastTouchPoint == null) {
                recordTouchDown(event);
            } else {
                operateTouchMove(event);
            }
        } else if (action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_UP) {
            releaseTouch();
        }
        return true;
    }

    private void recordTouchDown(MotionEvent event) {
        // 记录起始点
        lastTouchPoint = new PointD(event.getX(), event.getY());
        // 找出最近的点
        curModifyPointD = getClosestPointD(new PointD((lastTouchPoint.x - offsetPoint.x) / scale, (lastTouchPoint.y - offsetPoint.y) / scale));
        callZoomDot();
    }

    private void operateTouchMove(MotionEvent event) {
        // 改变指定的point
        final PointD tempPoint = curModifyPointD.clone();
        curModifyPointD.x = curModifyPointD.x + (event.getX() - lastTouchPoint.x) / scale;
        curModifyPointD.y = curModifyPointD.y + (event.getY() - lastTouchPoint.y) / scale;

        lastTouchPoint.x = event.getX();
        lastTouchPoint.y = event.getY();

        final Size originSize = scanInfo.getOriginSize();
        final int rotateAngle = scanInfo.getRotateAngle().getValue();
        double rotatedOriginWidth = rotateAngle == 90 || rotateAngle == 270 ? originSize.height : originSize.width;
        double rotatedOriginHeight = rotateAngle == 90 || rotateAngle == 270 ? originSize.width : originSize.height;
        if (!checkListIsValid(rotatedPoints, curModifyPointD, new Size(rotatedOriginWidth, rotatedOriginHeight), verifyPath)) {
            curModifyPointD.x = tempPoint.x;
            curModifyPointD.y = tempPoint.y;
        } else {
            postInvalidate();
            callZoomDot();
        }
    }

    private void releaseTouch() {
        // 松开
        curModifyPointD = null;
        lastTouchPoint = null;
        callZoomDot();
    }

    private void callZoomDot() {
        if (onDotChangeListener != null) {
            if (curModifyPointD == null) {
                onDotChangeListener.onDotChange(null, null);
            } else {
                List<PointD> pointDList = new ArrayList<>();
                PointD curPointD = null;
                for (PointD oldPoint : rotatedPoints) {
                    final PointD pointD = new PointD(oldPoint.x * scale + offsetPoint.x, oldPoint.y * scale + offsetPoint.y);
                    if (oldPoint == curModifyPointD) {
                        curPointD = pointD;
                        pointDList.add(curPointD);
                    } else {
                        pointDList.add(pointD);
                    }
                }
                onDotChangeListener.onDotChange(curPointD, pointDList);
            }
        }
    }

    /**
     * 计算最近的点
     *
     * @param startPoint
     * @return
     */
    private PointD getClosestPointD(PointD startPoint) {
        PointD pointDClosed = null;
        for (PointD pointD : rotatedPoints) {
            if (pointDClosed == null) {
                pointDClosed = pointD;
            } else {
                if (Utils.distance(startPoint, pointDClosed) > Utils.distance(startPoint, pointD)) {
                    pointDClosed = pointD;
                }
            }
        }
        return pointDClosed;
    }

    private static boolean checkListIsValid(List<PointD> pointDList, PointD curModifyPointD, Size size, Path path) {

        if (curModifyPointD.x < 0 || curModifyPointD.x > size.width
                || curModifyPointD.y < 0 || curModifyPointD.y > size.height) {
            return false;
        }

        boolean isConvex;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            path.reset();
            path.moveTo((float) pointDList.get(0).x, (float) pointDList.get(0).y);
            path.lineTo((float) pointDList.get(1).x, (float) pointDList.get(1).y);
            path.lineTo((float) pointDList.get(2).x, (float) pointDList.get(2).y);
            path.lineTo((float) pointDList.get(3).x, (float) pointDList.get(3).y);
            path.close();
            isConvex = path.isConvex();
        } else {
            final Mat mat = Converters.vector_Point_to_Mat(CvUtils.pointD2point(pointDList));
            isConvex = Imgproc.isContourConvex(new MatOfPoint(mat));
            mat.release();
        }
        return isConvex;
    }
}
