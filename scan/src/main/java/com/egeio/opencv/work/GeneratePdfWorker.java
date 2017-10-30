package com.egeio.opencv.work;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;

import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.tools.Utils;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.utils.Converters;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Created by wangjinpeng on 2017/10/19.
 */

public abstract class GeneratePdfWorker extends Worker {

    public abstract void onPdfGenerated(String savePath);

    private Context context;

    private final List<ScanInfo> scanInfoList;

    private final String savePath;

    private Debug debug = new Debug(GeneratePdfWorker.class.getSimpleName());

    public GeneratePdfWorker(Context context, List<ScanInfo> scanInfoList) {
        this.context = context;
        this.scanInfoList = scanInfoList;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssS", Locale.getDefault());
        savePath = Utils.getPdfFilePath(context, String.format(Locale.getDefault(), "新文档_%s.pdf", dateFormat.format(System.currentTimeMillis())));
    }

    @Override
    public void doWork() {
        drawByIText1();
    }

    private void drawByIText1() {
        Document document = null;
        Bitmap bitmapSrc = null;
        Mat imread = null;
        Mat mat = null;
        PdfWriter pdfWriter = null;
        try {
            document = new Document();
            Utils.createFile(savePath);
            pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(savePath));
            document.open();
            for (int i = 0; i < scanInfoList.size(); i++) {
                ScanInfo scanInfo = scanInfoList.get(i);
                debug.start(String.format("第%d页读取图片", i));
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmapSrc = BitmapFactory.decodeFile(scanInfo.getPath(), options);
                imread = new Mat();
                org.opencv.android.Utils.bitmapToMat(bitmapSrc, imread);
                debug.end(String.format("第%d页读取图片", i));
                debug.start(String.format("第%d页透视转换", i));
                mat = CvUtils.formatFromScanInfo(imread, scanInfo);
                Utils.recycle(bitmapSrc);
                debug.end(String.format("第%d页透视转换", i));
                final Rectangle a4 = PageSize.A4;

                debug.start(String.format("第%d页mat2byte", i));

                byte[] bytes = matToByteArray(mat);
                debug.end(String.format("第%d页mat2byte", i));

                debug.start(String.format("第%d页生成Image", i));
                final Image image = Image.getInstance(mat.width(), mat.height(), 3, 2, bytes);
                mat.release();
                debug.end(String.format("第%d页生成Image", i));
                debug.start(String.format("第%d页写入pdf", i));
                image.setAlignment(Image.MIDDLE);
                image.setRotationDegrees(-scanInfo.getRotateAngle().getValue());
                image.scaleToFit(a4.getWidth(), a4.getHeight());
                image.setAbsolutePosition((a4.getWidth() - image.getScaledWidth()) / 2f, (a4.getHeight() - image.getScaledHeight()) / 2f);
                document.newPage();
                document.add(image);
                debug.end(String.format("第%d页写入pdf", i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mat != null) {
                mat.release();
            }
            if (imread != null) {
                imread.release();
            }
            if (document != null) {
                document.close();
            }
            if (pdfWriter != null) {
                pdfWriter.close();
            }
        }
        onPdfGenerated(savePath);
    }

    private void drawByIText() {
        Document document = null;
        Mat imread = null;
        Mat mat = null;
        Bitmap bitmap = null;
        PdfWriter pdfWriter = null;
        try {
            document = new Document();
            Utils.createFile(savePath);
            pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(savePath));
            document.open();
            for (int i = 0; i < scanInfoList.size(); i++) {
                ScanInfo scanInfo = scanInfoList.get(i);
                debug.start(String.format("第%d页读取图片", i));
                imread = Imgcodecs.imread(scanInfo.getPath(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
                debug.end(String.format("第%d页读取图片", i));
                debug.start(String.format("第%d页透视转换", i));
                mat = CvUtils.formatFromScanInfo(imread, scanInfo);
                debug.end(String.format("第%d页透视转换", i));
                debug.start(String.format("第%d页创建bitmap", i));
                bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
                org.opencv.android.Utils.matToBitmap(mat, bitmap);
                debug.end(String.format("第%d页创建bitmap", i));
                final Rectangle a4 = PageSize.A4;

                debug.start(String.format("第%d页写入pdf", i));
                final Image image = Image.getInstance(bitmapToByteArray(bitmap));
                image.setAlignment(Image.MIDDLE);
                image.setRotationDegrees(-scanInfo.getRotateAngle().getValue());
                image.scaleToFit(a4.getWidth(), a4.getHeight());
                image.setAbsolutePosition((a4.getWidth() - image.getScaledWidth()) / 2f, (a4.getHeight() - image.getScaledHeight()) / 2f);
                Utils.recycle(bitmap);
                document.newPage();
                document.add(image);
                debug.end(String.format("第%d页写入pdf", i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.recycle(bitmap);
            if (mat != null) {
                mat.release();
            }
            if (imread != null) {
                imread.release();
            }
            if (document != null) {
                document.close();
            }
            if (pdfWriter != null) {
                pdfWriter.close();
            }
        }
        onPdfGenerated(savePath);
    }

    private byte[] matToByteArray(Mat mat) {
        int length = (int) (mat.total() * mat.elemSize());
        byte buffer[] = new byte[length];
        mat.get(0, 0, buffer);
        return buffer;
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        debug.start(String.format("bitmap to byte array"));
        ByteArrayOutputStream stream = null;
        try {
            stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            debug.end(String.format("bitmap to byte array"));
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

//    private void drawByPdfDocument() {
//        // create a new document
//        PdfDocument document = new PdfDocument();
//
//        // crate a page description
//        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(new Rect(0, 0, 100, 100), 1).create();
//
//        // start a page
//        Page page = document.startPage(pageInfo);
//
//        // draw something on the page
//        View content = getContentView();
//        content.draw(page.getCanvas());
//
//        // finish the page
//        document.finishPage(page);
//
//        // write the document content
//        document.writeTo(getOutputStream());
//
//        //close the document
//        document.close();
//    }
}
