package com.egeio.opencv.work;

import android.content.Context;
import android.graphics.Bitmap;

import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.Utils;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
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

    public GeneratePdfWorker(Context context, List<ScanInfo> scanInfoList) {
        this.context = context;
        this.scanInfoList = scanInfoList;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssS", Locale.getDefault());
        savePath = Utils.getPdfFilePath(context, String.format(Locale.getDefault(), "新文档_%s.pdf", dateFormat.format(System.currentTimeMillis())));
    }

    @Override
    public void doWork() {
        drawByIText();
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
            for (ScanInfo scanInfo : scanInfoList) {
                imread = Imgcodecs.imread(scanInfo.getPath(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
                mat = CvUtils.formatFromScanInfo(imread, scanInfo);
                bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
                org.opencv.android.Utils.matToBitmap(mat, bitmap);
                final Rectangle a4 = PageSize.A4;

                final Image image = Image.getInstance(bitmapToByteArray(bitmap));
                image.setAlignment(Image.MIDDLE);
                image.setRotationDegrees(-scanInfo.getRotateAngle().getValue());
                image.scaleToFit(a4.getWidth(), a4.getHeight());
                image.setAbsolutePosition((a4.getWidth() - image.getScaledWidth()) / 2f, (a4.getHeight() - image.getScaledHeight()) / 2f);
                Utils.recycle(bitmap);
                document.newPage();
                document.add(image);
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

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
}
