package com.egeio.opencv;

import android.support.annotation.MainThread;

import com.egeio.opencv.model.ScanInfo;

/**
 * Created by wangjinpeng on 2017/10/16.
 */

public interface ScanDataInterface {

    void toCamera();

    void toDotModify(ScanInfo scanInfo);

    void toEditPreview(ScanInfo scanInfo);

    @MainThread
    void onPdfGenerated(String savePath);

    ScanDataManager getScanDataManager();

    /**
     * 显示或者隐藏loading
     *
     * @param shown
     * @param msg   只在 shown == true 时有用
     */
    void showLoading(boolean shown, String msg);

    void onCameraException(Exception e);
}
