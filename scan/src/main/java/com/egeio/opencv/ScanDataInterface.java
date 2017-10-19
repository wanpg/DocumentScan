package com.egeio.opencv;

import com.egeio.opencv.model.ScanInfo;

import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/16.
 */

public interface ScanDataInterface {

    void toCamera();

    void toDotModify(ScanInfo scanInfo);

    void toEditPreview(ScanInfo scanInfo);

    void onPdfGenerated(String savePath);

    ScanDataManager getScanDataManager();

}
