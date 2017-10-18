package com.egeio.opencv;

import com.egeio.opencv.model.ScanInfo;

/**
 * Created by wangjinpeng on 2017/10/16.
 */

public interface ScanEditInterface {

    void toCamera();

    void toDotModify(ScanInfo scanInfo);

    void toEditPreview(ScanInfo scanInfo);

    ScanInfo getScanInfo(int index);

    int indexOfScanInfo(ScanInfo info);

    int getScanInfoSize();

    void remove(int index);

    void add(ScanInfo scanInfo);
}
