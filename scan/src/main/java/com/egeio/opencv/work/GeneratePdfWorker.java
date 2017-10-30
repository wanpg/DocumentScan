package com.egeio.opencv.work;

import android.content.Context;

import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.tools.Utils;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

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
                imread = Imgcodecs.imread(scanInfo.getPath());
                debug.end(String.format("第%d页读取图片", i));

                debug.start(String.format("第%d页透视转换", i));
                mat = CvUtils.formatFromScanInfo(imread, scanInfo, debug);
//                Utils.recycle(bitmapSrc);

                debug.end(String.format("第%d页透视转换", i));
                final Rectangle a4 = PageSize.A4;

                debug.start(String.format("第%d页mat2byte", i));

                MatOfByte matOfByte = new MatOfByte();
                Imgcodecs.imencode(".jpg", mat, matOfByte);
                byte[] bytes = matOfByte.toArray();
                debug.end(String.format("第%d页mat2byte", i));

                debug.start(String.format("第%d页生成Image", i));
                final Image image = Image.getInstance(bytes);
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
}
