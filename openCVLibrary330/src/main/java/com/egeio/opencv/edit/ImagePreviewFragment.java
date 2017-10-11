package com.egeio.opencv.edit;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.MatBitmapTransformation;

import org.opencv.R;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class ImagePreviewFragment extends Fragment {

    public static Fragment createInstance(ScanInfo scanInfo) {
        ImagePreviewFragment fragment = new ImagePreviewFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("SCAN_INFO", scanInfo);
        fragment.setArguments(bundle);
        return fragment;
    }


    private ScanInfo scanInfo;
    private ImageView imageView;
    private boolean isFirst = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanInfo = getArguments().getParcelable("SCAN_INFO");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_image_preview, null);
        imageView = inflate.findViewById(R.id.image);
        return inflate;
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

    private void showImage() {
        Glide.with(this)
                .load(scanInfo.getPath())
                .asBitmap()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .atMost()
                .transform(new MatBitmapTransformation(getContext(), scanInfo))
                .into(imageView);
    }
}
