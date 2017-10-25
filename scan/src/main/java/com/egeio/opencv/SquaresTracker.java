package com.egeio.opencv;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SquaresTracker {

    private static final String TAG = SquaresTracker.class.getSimpleName();

    private void saveMat(String name, Mat mat) {
        if (true) {
            return;
        }
        final String folder = DocumentScan.CACHE_FOLDER_PATH + File.separator + System.currentTimeMillis() + File.separator;
        final File file = new File(folder);
        if (!file.exists()) {
            file.mkdirs();
        }
        Imgcodecs.imwrite(folder + name, mat);
    }

    final int thresh = 10, N = 3;

    double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    public List<List<Point>> findLargestSquares(Mat imageSrc) {
        double minArea = Math.pow(Math.min(imageSrc.width(), imageSrc.height()) / 3, 2);
        // 处理image
        // 模糊
        saveMat("1_src.png", imageSrc);

        Mat bluredMat = new Mat();
        Imgproc.GaussianBlur(imageSrc, bluredMat, new Size(5, 5), 0);
        saveMat("2_bluredMat.png", bluredMat);

        // 黑白
        Mat grayMat = new Mat();
        Imgproc.cvtColor(bluredMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        saveMat("3_grayMat.png", grayMat);

        // canny
        Mat cannyMat = new Mat();
        Imgproc.Canny(grayMat, cannyMat, thresh, thresh * 2, 7, false);
        saveMat("4_cannyMat1.png", cannyMat);
        // threshold
        // CV_THRESH_OTSU = 8
//        Mat thresholdMat = new Mat();
//        Imgproc.threshold(cannyMat, thresholdMat, 0, 255, 8/*CV_THRESH_BINARY*/);
//        saveMat("5_thresholdMat.png", thresholdMat);

        return findContours(cannyMat, minArea);
    }

    public List<List<Point>> findLargestSquares1(Mat imageSrc) {
        double minArea = Math.pow(Math.min(imageSrc.width(), imageSrc.height()) / 3, 2);
        saveMat("1_src.png", imageSrc);
        // 处理image
        // 模糊
        Mat bluredMat = new Mat();
        Imgproc.GaussianBlur(imageSrc, bluredMat, new Size(5, 5), 0);
        saveMat("2_bluredMat.png", bluredMat);

        // 黑白
        Mat grayMat = new Mat();
        Imgproc.cvtColor(bluredMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        saveMat("3_grayMat.png", grayMat);

        // Canny
        Mat cannyMat = new Mat();
        List<List<Point>> squares = new ArrayList<>();
        // try several threshold levels
        for (int l = 0; l < 1; l++) {
            // hack: use Canny instead of zero threshold level.
            // Canny helps to catch squares with gradient shading
            // apply Canny. Take the upper threshold from slider
            // and set the lower to 0 (which forces edges merging)
            Imgproc.Canny(grayMat, cannyMat, thresh, thresh * 4, 3, false);
            // dilate canny output to remove potential
            // holes between edge segments
            Imgproc.dilate(cannyMat, cannyMat, new Mat(), new Point(-1, -1), 1);

            saveMat((4 + l) + "_cannyMat.png", cannyMat);
            squares.addAll(findContours(cannyMat, minArea));
        }
        return squares;
    }

    // returns sequence of squares detected on the image.
    // the sequence is stored in the specified memory storage
    public List<List<Point>> findLargestSquares2(Mat imageCome, float scale) {
        double minArea = Math.pow(Math.min(imageCome.width(), imageCome.height()) / 3, 2);
        List<List<Point>> squares = new ArrayList<>();
        Mat bluredMat = new Mat();
        Imgproc.GaussianBlur(imageCome, bluredMat, new Size(5, 5), 0);
        Mat gray0 = new Mat(imageCome.rows(), imageCome.cols(), CvType.CV_8U);
        Mat gray = new Mat();

        // down-scale and upscale the image to filter out the noise
//        Imgproc.pyrDown(imageCome, pyr, new Size(imageCome.cols() / 2, imageCome.rows() / 2));
//        Imgproc.pyrUp(pyr, timg, imageCome.size());

        // find squares in every color plane of the image
        for (int c = 0; c < 3; c++) {
            int ch[] = {c, 0};

            List<Mat> timgList = new ArrayList<>();
            timgList.add(bluredMat);
            List<Mat> gray0List = new ArrayList<>();
            gray0List.add(gray0);
            MatOfInt matOfInt = new MatOfInt(ch);
            Core.mixChannels(timgList, gray0List, matOfInt);// 将给定图片的某个通道改为另一个通道颜色

            // try several threshold levels
            for (int l = 0; l < N; l++) {
                // hack: use Canny instead of zero threshold level.
                // Canny helps to catch squares with gradient shading
                if (l == 0) {
                    // apply Canny. Take the upper threshold from slider
                    // and set the lower to 0 (which forces edges merging)
                    Imgproc.Canny(gray0, gray, thresh, thresh * 2, 3, false);
                    // dilate canny output to remove potential
                    // holes between edge segments
                    Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1);
                } else {
                    // apply threshold if l!=0:
                    //     tgray(x,y) = gray(x,y) < (l+1)*255/N ? 255 : 0
                    Imgproc.threshold(gray0, gray, (l + 1) * 255 / N, 255, 0/*CV_THRESH_BINARY*/);
                }
                squares.addAll(findContours(gray, minArea));
            }
        }

        gray.release();
        gray0.release();
        bluredMat.release();
        return squares;
    }


    private List<List<Point>> findContours(Mat src, double minArea) {
        List<List<Point>> pointArrayList = new ArrayList<>();


        List<MatOfPoint> contours = new ArrayList<>();
        // 寻找边缘
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // 检测点
        for (MatOfPoint mat : contours) {
            // 转换为双精度点
            mat.convertTo(mat, CvType.CV_32FC2);
            // approximate contour with accuracy proportional
            // to the contour perimeter
            // 多变形逼近
            MatOfPoint2f approx = new MatOfPoint2f();

            Imgproc.approxPolyDP(
                    mat,
                    approx,
                    Imgproc.arcLength(mat, true) * 0.02,
                    true);

            // square contours should have 4 vertices after approximation
            // relatively large area (to filter out noisy contours)
            // and be convex.
            // Note: absolute value of an area is used because
            // area may be positive or negative - in accordance with the
            // contour orientation
            List<Point> points = new ArrayList<>(approx.toList());
            if (points.size() >= 4 &&
                    Math.abs(Imgproc.contourArea(approx)) > minArea) {
                final List<Point> pointList = selectPoints(points, 5);
                if (pointList.size() == 4
                        && Imgproc.isContourConvex(Converters.vector_Point_to_Mat(pointList))) {
                    double maxCosine = 0;
                    for (int j = 2; j < 5; j++) {
                        // find the maximum cosine of the angle between joint edges
                        double cosine = Math.abs(angle(points.get(j % 4), points.get(j - 2), points.get(j - 1)));
                        maxCosine = Math.max(maxCosine, cosine);
                    }

                    if (maxCosine < 0.3)
                        pointArrayList.add(points);

                }
            }
            approx.release();
        }
        return pointArrayList;
    }

    private List<Point> selectPoints(List<Point> pointList, int selectTimes) {
        if (pointList.size() > 4) {
            double arc = Imgproc.arcLength(Converters.vector_Point2f_to_Mat(pointList), true);
            int index = 0;
            while (index != pointList.size() - 1) {
                if (pointList.size() == 4) {
                    break;
                }
                final Point p = pointList.get(index);
                if (index != 0) {
                    Point lastP = pointList.get(index - 1);
                    double pointLength = Math.sqrt(Math.pow(p.x - lastP.x, 2) + Math.pow((p.y - lastP.y), 2));
                    if (pointLength < arc * 0.01 * selectTimes && pointList.size() > 4) {
                        pointList.remove(index);
                        continue;
                    }
                }
                index++;
            }
            if (pointList.size() > 4) {
                return selectPoints(pointList, selectTimes + 1);
            }
        }
        return pointList;
    }
}
