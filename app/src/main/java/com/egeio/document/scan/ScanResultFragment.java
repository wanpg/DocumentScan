package com.egeio.document.scan;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import com.egeio.opencv.tools.Utils;

import java.io.File;

/**
 * Created by wangjinpeng on 2017/12/26.
 */

public class ScanResultFragment extends Fragment {

    public static Fragment instance(String savePath) {
        Bundle bundle = new Bundle();
        bundle.putString("savePath", savePath);
        Fragment fragment = new ScanResultFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private String savePath;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savePath = getArguments().getString("savePath");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, null);
        view.findViewById(R.id.view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(savePath));
                final File file = new File(savePath);
                Uri uri = FileProvider.getUriForFile(getContext(), "com.egeio.document.scan.fileprovider", file);    //第二个参数是manifest中定义的`authorities`
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_TITLE, file.getName());
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);    //这一步很重要。给目标应用一个临时的授权。
                startActivity(intent);    //或者其它最终处理方式
            }
        });
        return view;
    }
}
