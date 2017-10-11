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

    // returns sequence of squares detected on the image.
    // the sequence is stored in the specified memory storage
    public void findSquares(Mat imageCome, List<List<Point>> squares, float scale) {
        squares.clear();

//        Mat pyr = new Mat();
//        Mat timg = imageCome.clone();
        Size imageComeSize = imageCome.size();
        Mat timg = new Mat();
        Imgproc.GaussianBlur(imageCome, timg, new Size(11, 11), 0);
        Mat gray0 = new Mat(imageComeSize, CvType.CV_8U);
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
                List<MatOfPoint> contours = new ArrayList<>();
                // find contours and store them all as a list
                Imgproc.findContours(gray.clone(), contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

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
                            squares.add(points);
                    }
                    approx.release();
                    if (matOfPoint1 != null) {
                        matOfPoint1.release();
                    }
                    if (matOfPoint2 != null) {
                        matOfPoint2.release();
                    }
                }
            }
        }

        gray.release();
        gray0.release();
        timg.release();
//        pyr.release();
    }
}
