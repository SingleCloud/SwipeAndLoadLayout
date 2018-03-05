package com.singlecloud.swipeandloadlayout;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.singlecloud.swipeandloadlib.IViewScrollListener;
import com.singlecloud.swipeandloadlib.SwipeAndLoadLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String[] TEST_SOURCE_NAME = {"姓名0", "姓名1", "姓名2", "姓名3", "姓名4", "姓名5", "姓名6", "姓名7", "姓名8", "姓名9", "姓名10", "姓名11", "姓名12", "姓名13", "姓名14", "姓名15", "姓名16", "姓名17", "姓名18", "姓名19", "姓名20"};
    private static final String[] TEST_SOURCE_CONTENT = {"内容0", "内容1", "内容2", "内容3", "内容4", "内容5", "内容6", "内容7", "内容8", "内容9", "内容10", "内容11", "内容12", "内容13", "内容14", "内容15", "内容16", "内容17", "内容18", "内容19", "内容20"};

    private SwipeAndLoadLayout stlRoot;
    private ListView lvMain;
    private SimpleAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stlRoot = findViewById(R.id.stl_main);
        lvMain = findViewById(R.id.lv_main);
        List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            Map<String, String> obj = new HashMap<>();
            obj.put("name", TEST_SOURCE_NAME[i]);
            obj.put("content", TEST_SOURCE_CONTENT[i]);
            list.add(obj);
        }
        mAdapter = new SimpleAdapter(this, list,
                R.layout.item_main,
                new String[]{"name", "content"},
                new int[]{R.id.tv_item_name, R.id.tv_item_content});
        lvMain.setAdapter(mAdapter);
        final View view = View.inflate(this, R.layout.item_main, null);
        final View view2 = View.inflate(this, R.layout.item_main, null);
        stlRoot.setHeaderView(view, new IViewScrollListener() {

            @Override
            public float getThreshold() {
                return 198;
            }

            @Override
            public void onStart() {
                ((TextView) view.findViewById(R.id.tv_item_content)).setText("内容");
            }

            @Override
            public void onExecuting() {
                ((TextView) view.findViewById(R.id.tv_item_content)).setText("正在刷新");
            }

            @Override
            public void onThreshold(boolean willBeExecuted) {
                ((TextView) view.findViewById(R.id.tv_item_content)).setText(willBeExecuted ? "松开刷新" : "内容");
            }
        });
        stlRoot.setFooterView(view2, new IViewScrollListener() {

            @Override
            public float getThreshold() {
                return 198;
            }

            @Override
            public void onStart() {

            }

            @Override
            public void onExecuting() {

            }

            @Override
            public void onThreshold(boolean willBeExecuted) {

            }
        });
        stlRoot.setOnRefreshListener(() -> new Handler().postDelayed(()-> stlRoot.onRefreshCompleted(),2000));
    }
}
