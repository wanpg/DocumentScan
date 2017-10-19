package com.egeio.opencv;

import com.egeio.opencv.tools.Utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class SquaresTracker {

    private static final String TAG = SquaresTracker.class.getSimpleName();

    final int thresh = 10, N = 2;

    double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    public List<List<Point>> findLargestSquares(Mat imageSrc, float scale) {
        // 处理image
        // 黑白
        Mat grayMat = new Mat();
        Imgproc.cvtColor(imageSrc, grayMat, Imgproc.COLOR_BGR2GRAY);
        // 模糊
        Mat bluredMat = new Mat();
        Imgproc.GaussianBlur(grayMat, bluredMat, new Size(5, 5), 0);
        // canny
        Mat cannyMat = new Mat();
        Imgproc.Canny(bluredMat, cannyMat, thresh, thresh * 2, 5, false);
        // threshold
        // CV_THRESH_OTSU = 8
        Mat thresholdMat = new Mat();
        Imgproc.threshold(cannyMat, thresholdMat, 0, 255, 8/*CV_THRESH_BINARY*/);

        return findContours(thresholdMat, scale);
    }

    // returns sequence of squares detected on the image.
    // the sequence is stored in the specified memory storage
    public void findSquares(Mat imageCome, List<List<Point>> squares, float scale) {

//        Mat pyr = new Mat();
//        Mat timg = imageCome.clone();
        Mat timg = new Mat();
        Imgproc.GaussianBlur(imageCome, timg, new Size(5, 5), 0);
        Mat gray0 = new Mat(imageCome.rows(), imageCome.cols(), CvType.CV_8U);
        Mat gray = new Mat();

        // down-scale and upscale the image to filter out the noise
//        Imgproc.pyrDown(imageCome, pyr, new Size(imageCome.cols() / 2, imageCome.rows() / 2));
//        Imgproc.pyrUp(pyr, timg, imageCome.size());

        // find squares in every color plane of the image
        for (int c = 0; c < 3; c++) {
            int ch[] = {c, 0};

            List<Mat> timgList = new ArrayList<>();
            timgList.add(timg);
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
                squares.addAll(findContours(gray, scale));
            }
        }

        gray.release();
        gray0.release();
        timg.release();
//        pyr.release();
    }


    private List<List<Point>> findContours(Mat src, float scale) {
        List<List<Point>> pointArrayList = new ArrayList<>();


        List<MatOfPoint> contours = new ArrayList<>();
        // find contours and store them all as a list
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // test each contour
        for (MatOfPoint mat : contours) {
            // approximate contour with accuracy proportional
            // to the contour perimeter
            MatOfPoint2f approx = new MatOfPoint2f();
            MatOfPoint2f matOfPoint2f = Utils.copy2MatPoint2f(mat);
            Imgproc.approxPolyDP(
                    matOfPoint2f,
                    approx,
                    Imgproc.arcLength(matOfPoint2f, true) * 0.02,
                    true);
            matOfPoint2f.release();

            // square contours should have 4 vertices after approximation
            // relatively large area (to filter out noisy contours)
            // and be convex.
            // Note: absolute value of an area is used because
            // area may be positive or negative - in accordance with the
            // contour orientation
            MatOfPoint matOfPoint1 = null;
            MatOfPoint matOfPoint2 = null;
            List<Point> points = approx.toList();

            if (points != null
                    &&
                    points.size() == 4
                    &&
                    Math.abs(Imgproc.contourArea(matOfPoint1 = Utils.copy2MatPoint(approx))) > 1024 * scale
                    &&
                    Imgproc.isContourConvex(matOfPoint2 = Utils.copy2MatPoint(approx))) {
                double maxCosine = 0;
                for (int j = 2; j < 5; j++) {
                    // find the maximum cosine of the angle between joint edges
                    double cosine = Math.abs(angle(points.get(j % 4), points.get(j - 2), points.get(j - 1)));
                    maxCosine = Math.max(maxCosine, cosine);
                }

                // if cosines of all angles are small
                // (all angles are ~90 degree) then write quandrange
                // vertices to resultant sequence
                if (maxCosine < 0.3)
                    pointArrayList.add(points);
            }
            approx.release();
            if (matOfPoint1 != null) {
                matOfPoint1.release();
            }
            if (matOfPoint2 != null) {
                matOfPoint2.release();
            }
        }
        return pointArrayList;
    }
}