package com.egeio.opencv.edit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.egeio.opencv.ScanEditInterface;
import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.PointInfo;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.view.DotModifyView;
import com.egeio.opencv.work.Worker;

import org.opencv.R;
import org.opencv.core.Size;

import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/16.
 */

public class DotModifyFragment extends Fragment {

    public static Fragment createInstance(int currentIndex) {
        DotModifyFragment fragment = new DotModifyFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("INDEX", currentIndex);
        fragment.setArguments(bundle);
        return fragment;
    }

    private View mContainer;
    private DotModifyView dotModifyView;
    private ScanInfo scanInfo;
    private ScanEditInterface scanEditInterface;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int index = getArguments().getInt("INDEX");
        scanEditInterface = (ScanEditInterface) getActivity();
        scanInfo = scanEditInterface.getScanInfo(index);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mContainer == null) {
            mContainer = inflater.inflate(R.layout.fragment_dot_modify, null);
            dotModifyView = mContainer.findViewById(R.id.dot_modify);
            mContainer.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanEditInterface.toEditPreview(scanInfo);
                }
            });
            mContainer.findViewById(R.id.complete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final List<PointD> modifiedPoints = dotModifyView.getModifiedPoints();
                    scanInfo.setCurrentPointInfo(new PointInfo(modifiedPoints));
                    scanEditInterface.toEditPreview(scanInfo);
                }
            });
        }
        return mContainer;
    }

    private Worker imageLoadWorker;
    private boolean isFirst = true;
    private Bitmap cachedBitmap;

    @Override
    public void onResume() {
        super.onResume();
        if (isFirst) {
            new Thread(imageLoadWorker = new Worker() {
                @Override
                public void doWork() {
                    try {
                        while (true) {
                            if (dotModifyView.getWidth() > 0 && dotModifyView.getHeight() > 0) {
                                break;
                            }
                            assertWorkStopped();
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        assertWorkStopped();
                        int imageViewMaxSide = Math.max(dotModifyView.getWidth(), dotModifyView.getHeight());
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.RGB_565;
                        final Size originSize = scanInfo.getOriginSize();
                        options.inSampleSize = Math.max(2, CvUtils.calculateInSampleSize((int) originSize.width, (int) originSize.height, imageViewMaxSide, imageViewMaxSide));
                        cachedBitmap = BitmapFactory.decodeFile(scanInfo.getPath(), options);
                        dotModifyView.setScanInfo(scanInfo, cachedBitmap);
                    } catch (Exception e) {
                        if (e instanceof Worker.WorkStoppedException) {
                            // 此处回收无用的临时资源
                        }
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        isFirst = false;
    }
}
