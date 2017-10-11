package com.egeio.opencv.tools;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.TypedValue;
import android.view.Surface;
import android.view.WindowManager;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/9/15.
 */

public class Utils {

    public static String getSaveFolder(Context context) {
        return context.getExternalCacheDir() + File.separator + "photo";
    }

    public static String getSavePath(Context context) {
        return getSaveFolder(context) + File.separator + "IMG_" + System.currentTimeMillis() + ".png";
    }

    public static void recycle(Bitmap bitmap) {
        if (bitmap != null) {
            try {
                bitmap.recycle();
            } catch (Exception ignored) {
            } finally {
                bitmap = null;
            }
        }
    }

    public static int dp2px(Context context, int dp) {
        Resources resources = context.getResources();
        return (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics()));
    }

    public static int getCameraOrientation(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = manager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public static Mat rotateMatByCamera(Context context, Mat mat) {
        Mat matResult;
        int cameraOrientation = Utils.getCameraOrientation(context);
        if (cameraOrientation == 90) {
            matResult = new Mat();
            Core.rotate(mat, matResult, Core.ROTATE_90_CLOCKWISE);
        } else if (cameraOrientation == 180) {
            matResult = new Mat();
            Core.rotate(mat, matResult, Core.ROTATE_180);
        } else if (cameraOrientation == 270) {
            matResult = new Mat();
            Core.rotate(mat, matResult, Core.ROTATE_90_COUNTERCLOCKWISE);
        } else {
            matResult = mat.clone();
        }
        return matResult;
    }

    public static int getCvRotateTypeByCamera(Context context) {
        int cameraOrientation = Utils.getCameraOrientation(context);
        if (cameraOrientation == 90) {
            return Core.ROTATE_90_CLOCKWISE;
        } else if (cameraOrientation == 180) {
            return Core.ROTATE_180;
        } else if (cameraOrientation == 270) {
            return Core.ROTATE_90_COUNTERCLOCKWISE;
        } else {
            return -1;
        }
    }

    public static MatOfPoint2f copy2MatPoint2f(MatOfPoint mat) {
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        matOfPoint2f.fromArray(mat.toArray());
        return matOfPoint2f;
    }

    public static Mat copy2Mat(Mat mat) {
        Mat matCopy = new Mat();
        mat.copyTo(matCopy);
        return matCopy;
    }

    public static MatOfPoint copy2MatPoint(MatOfPoint2f mat) {
        MatOfPoint matOfPoint = new MatOfPoint();
        matOfPoint.fromArray(mat.toArray());
        return matOfPoint;
    }

    public static List<Point> findLargestList(List<List<Point>> pointsList) {
        int maxIndex = -1;
        double maxArea = 0;
        for (int i = 0; i < pointsList.size(); i++) {
            List<Point> points = pointsList.get(i);
            double contourArea = Math.abs(Imgproc.contourArea(new MatOfPoint(points.toArray(new Point[]{}))));
            if (contourArea > maxArea) {
                maxIndex = i;
                maxArea = contourArea;
            }
        }
        return pointsList.get(maxIndex);
    }

    /**
     * 计算接近的size，主要用于相机裁剪，填充满屏幕
     *
     * @param src
     * @param dst
     * @param out
     * @return 输出尺寸和dst尺寸的缩放比率 out/dst
     */
    public static double calApproximateSize(Size src, Size dst, Size out) {
        double srcRatio = src.width / src.height;
        double dstRatio = dst.width / dst.height;

        // src的宽相对更宽
        if (dstRatio > srcRatio) {
            out.width = src.width;
            out.height = src.width / dstRatio;
        } else {
            out.width = src.height * dstRatio;
            out.height = src.height;
        }
        return out.height / dst.height;
    }

    public static void saveBufferToFile(byte[] bytes, String filePath) {
        FileOutputStream outputStream = null;
        try {
            File file = new File(filePath);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file);
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void saveBitmapToFile(Bitmap bitmap, String filePath) {
        FileOutputStream outputStream = null;
        try {
            File file = new File(filePath);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 清空文件夹，如果存在的话
     *
     * @param folderPath
     */
    public static void clearFolder(String folderPath) {
        clearFolder(new File(folderPath));
    }

    /**
     * 清空文件夹，如果存在的话
     *
     * @param folder
     */
    public static void clearFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    clearFolder(file);
                }
                file.delete();
            }
        }
    }

    /**
     * 计算两个点的距离
     *
     * @param p1
     * @param p2
     * @return
     */
    public static float distance(Point p1, Point p2) {
        return (float) Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    /**
     * 计算四个点的中心点
     *
     * @param p1
     * @param p2
     * @param p3
     * @param p4
     * @return
     */
    public static Point calCenter(Point p1, Point p2, Point p3, Point p4) {
        return calCenter(calCenter(p1, p3), calCenter(p2, p4));
    }

    /**
     * 计算两个点的中点
     *
     * @param p1
     * @param p2
     * @return
     */
    public static Point calCenter(Point p1, Point p2) {
        return new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
    }

    public static List<Point> sortPoints(List<Point> points) {
        List<Point> pointTemp = new ArrayList<>(points);
        Collections.sort(pointTemp, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                double v = o1.x - o2.x;
                if (v > 0) {
                    return 1;
                } else if (v < 0) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        List<Point> points1 = pointTemp.subList(0, 2);
        Collections.sort(points1, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                double v = o1.y - o2.y;
                if (v > 0) {
                    return 1;
                } else if (v < 0) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        Point topLeft = points1.get(0);
        int indexOfTopLeft = points.indexOf(topLeft);
        if (indexOfTopLeft > 0) {
            List<Point> points2 = points.subList(indexOfTopLeft, points.size());
            points2.addAll(points.subList(0, indexOfTopLeft));
            return points2;
        }
        return points;
    }

    public static Mat PerspectiveTransform(Mat src, List<Point> startCoords) {
        if (startCoords.size() != 4) return null;

        double width1, width2, height1, height2, avgw, avgh;

        startCoords = sortPoints(startCoords);

        List<Point> resultCoords = new ArrayList<>();

        width1 = distance(startCoords.get(0), startCoords.get(1));
        width2 = distance(startCoords.get(2), startCoords.get(3));
        height1 = distance(startCoords.get(0), startCoords.get(3));
        height2 = distance(startCoords.get(1), startCoords.get(2));
        avgw = (width1 + width2) / 2;
        avgh = (height1 + height2) / 2;

        resultCoords.add(new Point(0, 0));
        resultCoords.add(new Point(avgw - 1, 0));
        resultCoords.add(new Point(avgw - 1, avgh - 1));
        resultCoords.add(new Point(0, avgh - 1));

        Mat start = Converters.vector_Point2f_to_Mat(startCoords);
        Mat result = Converters.vector_Point2d_to_Mat(resultCoords);
        start.convertTo(start, CvType.CV_32FC2);
        result.convertTo(result, CvType.CV_32FC2);

        Size size = new Size(avgw, avgh);
        Mat mat = new Mat(size, CvType.CV_8UC1);
        Mat perspective = Imgproc.getPerspectiveTransform(start, result);
        Imgproc.warpPerspective(src, mat, perspective, size);
        return mat;
    }

    /**
     * 透视变形
     *
     * @param mat
     * @param points
     * @return
     */
    public static Mat warpPerspective(Mat mat, List<Point> points) {

        List<Point> points1 = new ArrayList<>(points);
        //此处查找左上角定焦的点

        // image center
        double u0 = (mat.cols()) / 2.0f;
        double v0 = (mat.rows()) / 2.0f;

        double width1 = distance(points1.get(0), points1.get(1));
        double width2 = distance(points1.get(2), points1.get(3));

        double height1 = distance(points1.get(0), points1.get(2));
        double height2 = distance(points1.get(1), points1.get(3));

        double width = Math.max(width1, width2);
        double height = Math.max(height1, height2);

        // visible aspect ratio
        double ar_vis = width / height;

        // make numpy arrays and append 1 for linear algebra
        double[] m1 = new double[]{points1.get(0).x, points1.get(0).y, 1};
        double[] m2 = new double[]{points1.get(1).x, points1.get(1).y, 1};
        double[] m3 = new double[]{points1.get(2).x, points1.get(2).y, 1};
        double[] m4 = new double[]{points1.get(3).x, points1.get(3).y, 1};

        // calculate the focal disrance
        double k2 = Number.dot(Number.cross(m1, m4), m3) / Number.dot(Number.cross(m2, m4), m3);
        double k3 = Number.dot(Number.cross(m1, m4), m2) / Number.dot(Number.cross(m3, m4), m2);

        double[] n2 = Number.minus(Number.multiply(m2, k2), m1);
        double[] n3 = Number.minus(Number.multiply(m3, k3), m1);

        double n21 = n2[0];
        double n22 = n2[1];
        double n23 = n2[2];

        double n31 = n3[0];
        double n32 = n3[1];
        double n33 = n3[2];

        double f = Math.sqrt(-(1.0 / (n23 * n33)) * ((n21 * n31 - (n21 * n33 + n23 * n31) * u0 + n23 * n33 * u0 * u0) + (n22 * n32 - (n22 * n33 + n23 * n32) * v0 + n23 * n33 * v0 * v0)));

        double[][] A = new double[][]{
                new double[]{f, 0, u0},
                new double[]{0, f, v0},
                new double[]{0, 0, 1},
        };

        // 矩阵对置
        double[][] At = Number.transpose(A);
        double[][] Ati = Number.inverseMatrix(new Array2DRowRealMatrix(At)).getData();
        double[][] Ai = Number.inverseMatrix(new Array2DRowRealMatrix(A)).getData();

        // calculate the real aspect ratio
        double ar_real = Math.sqrt(Number.dot(Number.dot(Number.dot(n2, Ati), Ai), n2) / Number.dot(Number.dot(Number.dot(n3, Ati), Ai), n3));

        int W, H;
        if (ar_real < ar_vis) {
            W = (int) width;
            H = (int) (W / ar_real);
        } else {
            H = (int) height;
            W = (int) (ar_real * H);
        }
        List<Point> points2 = new ArrayList<>();
        points2.add(new Point(0, 0));
        points2.add(new Point(W, 0));
        points2.add(new Point(0, H));
        points2.add(new Point(W, H));
        Mat pts1 = Converters.vector_Point_to_Mat(points1);
        Mat pts2 = Converters.vector_Point_to_Mat(points2);
        pts1.convertTo(pts1, CvType.CV_32FC2);
        pts2.convertTo(pts2, CvType.CV_32FC2);
        //project the image with the new w/h

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(pts1, pts2);
        Size size = new Size(W, H);
        Mat dst = new Mat(size, CvType.CV_8UC1);
        Imgproc.warpPerspective(mat, dst, perspectiveTransform, size);
        return dst;
    }
}
