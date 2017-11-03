package com.egeio.opencv.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.view.PreviewImageView;
import com.egeio.opencv.work.ImageLoadWorker;
import com.egeio.opencv.work.Worker;
import com.egeio.scan.R;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class ImagePreviewFragment extends BaseScanFragment {

    public static Fragment createInstance(ScanInfo scanInfo) {
        ImagePreviewFragment fragment = new ImagePreviewFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("SCAN_INFO", scanInfo);
        fragment.setArguments(bundle);
        return fragment;
    }

    private View mContainer;
    private ScanInfo scanInfo;
    private PreviewImageView imageView;
    private boolean isFirst = true;
    Debug debug = new Debug(ImagePreviewFragment.class.getSimpleName());


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanInfo = getArguments().getParcelable("SCAN_INFO");
    }

    public ScanInfo getScanInfo() {
        return scanInfo;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mContainer == null) {
            mContainer = inflater.inflate(R.layout.fragment_image_preview, null);
            imageView = mContainer.findViewById(R.id.image);
        }
        return mContainer;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirst) {
            showImage();
        }
        isFirst = false;
    }

    public void updateScanInfo(ScanInfo scanInfo) {
        this.scanInfo = scanInfo;
        showImage();
    }

    public void rotate(ScanInfo scanInfo) {
        this.scanInfo = scanInfo;
        if (hasShownImage) {
            imageView.setRotateAngle(scanInfo.getRotateAngle().getValue());
            imageView.postInvalidate();
        } else {
            showImage();
        }
    }

    private Worker imageLoadWorker;
    private boolean hasShownImage = false;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (imageView != null) {
            imageView.setBitmap(null);
        }
    }

    private void showImage() {
        if (imageLoadWorker != null) {
            imageLoadWorker.stopWork();
        }
        new Thread(imageLoadWorker = new ImageLoadWorker(scanInfo) {

            @Override
            protected void doFirst() {
                debug.start("等待imageView绘制");
                if (imageView.getHeight() <= 0 || imageView.getWidth() <= 0) {
                    while (true) {
                        if (imageView.getWidth() > 0 && imageView.getHeight() > 0) {
                            break;
                        }
                        assertWorkStopped();
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                setSize(imageView.getWidth(), imageView.getHeight());
                debug.end("等待imageView绘制");
            }

            @Override
            public void onImageLoaded(Bitmap bitmap, ScanInfo scanInfo) {
                assertWorkStopped();
                ImagePreviewFragment.this.onImageLoaded(bitmap, scanInfo.getRotateAngle());
            }
        }).start();
    }

    private synchronized void onImageLoaded(final Bitmap bitmap, final ScanInfo.Angle angle) {
        hasShownImage = true;
        imageView.setBitmap(bitmap);
        imageView.setRotateAngle(scanInfo.getRotateAngle().getValue());
        imageView.postInvalidate();
    }
}
