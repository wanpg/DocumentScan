package com.egeio.opencv.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.Utils;

import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/13.
 */

public class DotModifyView extends PreviewImageView {

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

    private Paint pathPaint, dotPaint, maskPaint;
    private ScanInfo scanInfo;
    private boolean initialize = false;
    private int pathWidth, dotPathWidth, cornerRadius;

    private Path pointPath = new Path();
    private Path maskPath = new Path();
    private RectF maskRectF = new RectF();

    private void init() {
        if (initialize) {
            return;
        }
        initialize = true;
        pathWidth = Utils.dp2px(getContext(), 1.5f);
        dotPathWidth = Utils.dp2px(getContext(), 1f);
        cornerRadius = Utils.dp2px(getContext(), 20 / 2);

        pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setColor(Color.parseColor("#3296FF"));
        pathPaint.setStyle(Paint.Style.STROKE);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#FFFFFF"));
        dotPaint.setAlpha(127);//#7Fffffff

        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.BLACK);
        maskPaint.setAlpha((int) (255 * 0.3f));// 30%
    }

    private final List<PointD> rotatedPoints = new ArrayList<>();
    private float scale = 1f;

    /**
     * 图片绘制的起始偏移点
     */
    private PointF offsetPoint = new PointF();

    public void setScanInfo(ScanInfo scanInfo, Bitmap bitmap) {
        rotatedPoints.clear();
        this.scanInfo = scanInfo;
        if (scanInfo != null) {
            final Size originSize = scanInfo.getOriginSize();
            final int rotateAngleValue = scanInfo.getRotateAngle().getValue();
            final List<PointD> pointDList = CvUtils.rotatePoints(scanInfo.getCurrentPointInfo().getPoints(), originSize.width, originSize.height, rotateAngleValue);
            rotatedPoints.addAll(pointDList);
            setRotateAngle(rotateAngleValue);
        }
        setBitmap(bitmap);
        if (getWidth() > 0 && getHeight() > 0) {
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制蒙版和边界区域
        if (scanInfo != null && !rotatedPoints.isEmpty()) {
            final Size originSize = scanInfo.getOriginSize();
            final int rotateAngle = getRotateAngle();
            // point 旋转

            final int width = getWidth() - getPaddingLeft() - getPaddingRight();
            final int height = getHeight() - getPaddingTop() - getPaddingBottom();

            double rotatedOriginWidth = rotateAngle == 90 || rotateAngle == 270 ? originSize.height : originSize.width;
            double rotatedOriginHeight = rotateAngle == 90 || rotateAngle == 270 ? originSize.width : originSize.height;

            double widthScaleRatio = width / rotatedOriginWidth;
            double heightScaleRatio = height / rotatedOriginHeight;

            scale = (float) Math.min(widthScaleRatio, heightScaleRatio);

            offsetPoint.x = (float) ((width - rotatedOriginWidth * scale) / 2);
            offsetPoint.y = (float) ((height - rotatedOriginHeight * scale) / 2);

            // 选区路径
            pointPath.reset();
            pointPath.moveTo(offsetPoint.x + (float) rotatedPoints.get(0).x * scale, offsetPoint.y + (float) rotatedPoints.get(0).y * scale);
            pointPath.lineTo(offsetPoint.x + (float) rotatedPoints.get(1).x * scale, offsetPoint.y + (float) rotatedPoints.get(1).y * scale);
            pointPath.lineTo(offsetPoint.x + (float) rotatedPoints.get(2).x * scale, offsetPoint.y + (float) rotatedPoints.get(2).y * scale);
            pointPath.lineTo(offsetPoint.x + (float) rotatedPoints.get(3).x * scale, offsetPoint.y + (float) rotatedPoints.get(3).y * scale);
            pointPath.close();

            // FIXME: 2017/10/16 此处为了减少4.4以下机器的内存开销，决定不绘制蒙版
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && false) {
                // 绘制蒙版
                maskRectF.left = getPaddingLeft() + offsetPoint.x;
                maskRectF.top = getPaddingTop() + offsetPoint.y;
                maskRectF.right = (float) (maskRectF.left + rotatedOriginWidth * scale);
                maskRectF.bottom = (float) (maskRectF.top + rotatedOriginHeight * scale);
                maskPath.addRect(maskRectF, Path.Direction.CCW);
                maskPath.op(pointPath, Path.Op.XOR);
                canvas.drawRect(maskRectF, maskPaint);
            }

            // 绘制 选区路径
            pathPaint.setStrokeWidth(pathWidth);
            canvas.drawPath(pointPath, pathPaint);

            // 给每个点, 绘制圆环
            pathPaint.setStrokeWidth(dotPathWidth);
            for (PointD point : rotatedPoints) {
                canvas.drawCircle(offsetPoint.x + (float) point.x * scale, offsetPoint.y + (float) point.y * scale, cornerRadius, dotPaint);
                canvas.drawCircle(offsetPoint.x + (float) point.x * scale, offsetPoint.y + (float) point.y * scale, cornerRadius, pathPaint);
            }
        }
    }

    private PointD touchDownPoint = new PointD();
    private PointD curModifyPointD;
    private PointD curModifyPointDClone;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            recordTouchDown(event);
        } else if (action == MotionEvent.ACTION_MOVE) {
            operateTouchMove(event);
        } else if (action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_UP) {
            releaseTouch();
        }
        return true;
    }

    private void recordTouchDown(MotionEvent event) {
        // 记录起始点
        touchDownPoint.x = event.getX();
        touchDownPoint.y = event.getY();
        // 找出最近的点
        curModifyPointD = getClosestPointD(new PointD((touchDownPoint.x - offsetPoint.x) / scale, (touchDownPoint.y - offsetPoint.y) / scale));
        // 复制最近的点，作为后面位置计算的基准
        curModifyPointDClone = curModifyPointD.clone();
    }

    private void operateTouchMove(MotionEvent event) {
        // 改变指定的point
        final PointD tempPoint = curModifyPointD.clone();
        curModifyPointD.x = curModifyPointDClone.x + (event.getX() - touchDownPoint.x) / scale;
        curModifyPointD.y = curModifyPointDClone.y + (event.getY() - touchDownPoint.y) / scale;
        if (!checkListIsValid()) {
            curModifyPointD.x = tempPoint.x;
            curModifyPointD.y = tempPoint.y;
        }
        invalidate();
    }

    private void releaseTouch() {
        // 松开
        curModifyPointDClone = null;
        curModifyPointD = null;
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

    private boolean checkListIsValid() {
        final Size originSize = scanInfo.getOriginSize();
        final int rotateAngle = getRotateAngle();
        double rotatedOriginWidth = rotateAngle == 90 || rotateAngle == 270 ? originSize.height : originSize.width;
        double rotatedOriginHeight = rotateAngle == 90 || rotateAngle == 270 ? originSize.width : originSize.height;

        if (curModifyPointD.x < 0 || curModifyPointD.x > rotatedOriginWidth
                || curModifyPointD.y < 0 || curModifyPointD.y > rotatedOriginHeight) {
            return false;
        }

        boolean intersectant = isSegmentIntersectant(rotatedPoints.get(0), rotatedPoints.get(1), rotatedPoints.get(3), rotatedPoints.get(0))
                && isSegmentIntersectant(rotatedPoints.get(0), rotatedPoints.get(3), rotatedPoints.get(1), rotatedPoints.get(2));
        return !intersectant;
    }

    /**
     * @return
     */
    private boolean isSegmentIntersectant(PointD s1, PointD e1, PointD s2, PointD e2) {
        double tolerance = 1.0E-10D;
        Line line1 = new Line(new Vector2D(s1.x, s1.y), new Vector2D(e1.x, e1.y), tolerance);
        Line line2 = new Line(new Vector2D(s2.x, s2.y), new Vector2D(e2.x, e2.y), tolerance);
        final Vector2D intersection = line1.intersection(line2);
        if (intersection != null) {
            PointD p = new PointD(intersection.getX(), intersection.getY());
            return isPointInSegmentArea(s1, e1, p) && isPointInSegmentArea(s2, e2, p);
        }
        return false;
    }

    private boolean isPointInSegmentArea(PointD s, PointD e, PointD p) {
        double minX = Math.min(s.x, e.x);
        double maxX = Math.max(s.x, e.x);
        double minY = Math.min(s.y, e.y);
        double maxY = Math.max(s.y, e.y);

        return p.x >= minX && p.x <= maxX
                && p.y >= minY && p.y <= maxY;
    }
}
