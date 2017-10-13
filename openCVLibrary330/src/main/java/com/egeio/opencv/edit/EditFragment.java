package com.egeio.opencv.edit;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.view.FragmentPagerAdapter;

import org.opencv.R;

import java.util.ArrayList;

/**
 * Created by wangjinpeng on 2017/9/30.
 */

public class EditFragment extends Fragment {

    private View mContainer;
    private ViewPager viewPager;
    private View areaCrop, areaOptimize, areaRotate, areaDelete;

    private ArrayList<ScanInfo> scanInfoArrayList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanInfoArrayList = getArguments().getParcelableArrayList("SCAN_INFO_ARRAY");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mContainer == null) {
            mContainer = inflater.inflate(R.layout.fragment_edit, null);
            initView();
        }
        return mContainer;
    }

    FragmentPagerAdapter pagerAdapter;

    private void initView() {
        viewPager = mContainer.findViewById(R.id.view_pager);
        areaCrop = mContainer.findViewById(R.id.area_crop);
        areaOptimize = mContainer.findViewById(R.id.area_optimize);
        areaRotate = mContainer.findViewById(R.id.area_rotate);
        areaDelete = mContainer.findViewById(R.id.area_delete);
        areaCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        areaOptimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = viewPager.getCurrentItem();
                ScanInfo scanInfo = scanInfoArrayList.get(currentItem);
                if (scanInfo != null) {
                    scanInfo.setOptimized(!scanInfo.isOptimized());
                    String tag = FragmentPagerAdapter.makeFragmentName(viewPager.getId(), pagerAdapter.getItemId(currentItem));
                    Fragment fragmentByTag = getChildFragmentManager().findFragmentByTag(tag);
                    if (fragmentByTag != null && fragmentByTag instanceof ImagePreviewFragment) {
                        ((ImagePreviewFragment) fragmentByTag).updateScanInfo(scanInfo);
                    }
                }

            }
        });
        areaRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = viewPager.getCurrentItem();
                ScanInfo scanInfo = scanInfoArrayList.get(currentItem);
                if (scanInfo != null) {
                    int value = scanInfo.getRotateAngle().getValue();
                    value += 270;// 逆时针旋转
                    if (value >= 360) {
                        value = value % 360;
                    }
                    scanInfo.setRotateAngle(value);
                    String tag = FragmentPagerAdapter.makeFragmentName(viewPager.getId(), pagerAdapter.getItemId(currentItem));
                    Fragment fragmentByTag = getChildFragmentManager().findFragmentByTag(tag);
                    if (fragmentByTag != null && fragmentByTag instanceof ImagePreviewFragment) {
                        ((ImagePreviewFragment) fragmentByTag).rotate(scanInfo);
                    }
                }
            }
        });
        areaDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        pagerAdapter = new FragmentPagerAdapter(getChildFragmentManager()) {

            @Override
            public int getCount() {
                return scanInfoArrayList.size();
            }

            @Override
            public Fragment getItem(int position) {
                return ImagePreviewFragment.createInstance(scanInfoArrayList.get(position));
            }

            @Override
            public long getItemId(int position) {
                return super.getItemId(position);
            }

            @Override
            public float getPageWidth(int position) {
                return super.getPageWidth(position);
            }
        };
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
}
