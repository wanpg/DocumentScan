package com.egeio.opencv.view;

import android.animation.ObjectAnimator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.egeio.opencv.tools.AnimUtils;
import com.egeio.scan.R;

/**
 * Created by wangjinpeng on 2017/10/19.
 */

public class LoadingInfoHolder {

    private View areaLoading;

    private ImageView imageInfo;
    private TextView textInfo;

    public LoadingInfoHolder(View areaLoading) {
        this.areaLoading = areaLoading;
        imageInfo = areaLoading.findViewById(R.id.image_info);
        textInfo = areaLoading.findViewById(R.id.text_info);
    }

    public void showInfo(final int drawableRes, final String info) {
        areaLoading.setVisibility(View.VISIBLE);
        imageInfo.setImageResource(drawableRes);
        textInfo.setText(info);
    }

    public void hideInfo() {
        areaLoading.setVisibility(View.GONE);
    }

    public void shakeInfo() {
        final ObjectAnimator nope = AnimUtils.nope(areaLoading);
        nope.start();
    }

    public void showLoading(String loadingInfo) {
        areaLoading.setVisibility(View.VISIBLE);
        imageInfo.setVisibility(View.GONE);
        textInfo.setText(loadingInfo);
    }
}
