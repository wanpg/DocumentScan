package com.egeio.document.scan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.egeio.opencv.OpenCVHelper;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initDebug();
        imageView = findViewById(R.id.image);
    }

    public void onGrayOperate(View view) {
        Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(
                R.drawable.img_7706_thumb)).getBitmap();
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);
        int[] resultPixes = OpenCVHelper.gray(pix, w, h);
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        result.setPixels(resultPixes, 0, w, 0, 0, w, h);
        imageView.setImageBitmap(result);
    }

    public void onSetOriginPic(View view) {
        imageView.setImageResource(R.drawable.img_7706_thumb);
    }

    public void onFaceDetection(View view) {
    }

    public void onDocumentScan(View view) {
        startActivity(new Intent(this, ScanActivity.class));
    }
}
