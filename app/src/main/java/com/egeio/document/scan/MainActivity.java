package com.egeio.document.scan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.egeio.opencv.OpenCVHelper;
import com.egeio.opencv.tools.CvUtils;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVHelper.init();
        imageView = findViewById(R.id.image);
    }

    public void onGrayOperate(View view) {
        Bitmap bitmapSrc = ((BitmapDrawable) getResources().getDrawable(
                R.drawable.test)).getBitmap();
        Bitmap bitmap = CvUtils.gray(CvUtils.modifyContrast(bitmapSrc, 1.5, 10));//CvUtils.gray();
//        Bitmap bitmap = CvUtils.modifyContrast(CvUtils.gray(bitmapSrc), 2.2, 50);
        imageView.setImageBitmap(bitmap);
    }

    public void onSetOriginPic(View view) {
        imageView.setImageResource(R.drawable.test);
    }

    public void onFaceDetection(View view) {
    }

    public void onDocumentScan(View view) {
        startActivity(new Intent(this, ScanActivity.class));
    }
}
