package com.fstar.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.fstar.tv.tools.Config;
import com.fstar.tv.tools.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class CollectActivity extends Activity {
    private Activity context;
    private MyApp myApp;
    private GridView history;
    private GridView colection;
    private Button clearHistory;
    private ArrayList<HashMap<String, Object>> lstImageItem_history = new ArrayList<HashMap<String, Object>>();  //GRID列表
    private ArrayList<HashMap<String, Object>> lstImageItem_colection = new ArrayList<HashMap<String, Object>>();  //GRID列表
    private Handler appHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect);
        context = this;
        myApp = (MyApp) getApplication();

        //get layout ui
        history = (GridView) findViewById(R.id.history);
        colection = (GridView) findViewById(R.id.colection);
        clearHistory = (Button) findViewById(R.id.clearHistory);

        //history grid init
        SimpleAdapter saImageItems = new SimpleAdapter(this,
                lstImageItem_history,//数据来源
                R.layout.detail_grid_item,//night_item的XML实现
                //动态数组与ImageItem对应的子项
                new String[] {"ItemImage","ItemText"},
                //ImageItem的XML文件里面的一个ImageView,两个TextView ID
                new int[] {R.id.ItemImage,R.id.ItemText});

        SimpleAdapter.ViewBinder binder = new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view instanceof ImageView) {
                    ((ImageView) view).setImageBitmap((Bitmap) data);
                    return true;
                }
                return false;
            }
        };
        saImageItems.setViewBinder(binder);
        //添加并且显示
        history.setAdapter(saImageItems);
        //添加消息处理
        history.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.setClass(CollectActivity.this, MediaInfoActivity.class);
                HashMap<String, Object> itemMap = lstImageItem_history.get(position);
                intent.putExtra("MEDIA_ID", (String) itemMap.get("ItemId"));
                intent.putExtra("MEDIA_NAME", (String) itemMap.get("ItemText"));
                startActivity(intent);
            }
        });

        //colection grid init
        saImageItems = new SimpleAdapter(this,
                lstImageItem_colection,//数据来源
                R.layout.detail_grid_item,//night_item的XML实现
                //动态数组与ImageItem对应的子项
                new String[] {"ItemImage","ItemText"},
                //ImageItem的XML文件里面的一个ImageView,两个TextView ID
                new int[] {R.id.ItemImage,R.id.ItemText});

        binder = new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view instanceof ImageView) {
                    ((ImageView) view).setImageBitmap((Bitmap) data);
                    return true;
                }
                return false;
            }
        };
        saImageItems.setViewBinder(binder);
        //添加并且显示
        colection.setAdapter(saImageItems);
        //添加消息处理
        colection.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.setClass(CollectActivity.this, MediaInfoActivity.class);
                HashMap<String, Object> itemMap = lstImageItem_colection.get(position);
                intent.putExtra("MEDIA_ID", (String) itemMap.get("ItemId"));
                intent.putExtra("MEDIA_NAME", (String) itemMap.get("ItemText"));
                startActivity(intent);
            }
        });

        clearHistory.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myApp.clearHistory();
                initData(lstImageItem_history, 1);
            }
        });

        appHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1: //更新视频列表
                        ((SimpleAdapter)history.getAdapter()).notifyDataSetChanged();
                        history.requestFocus();
                        break;
                    case 2: //更新视频列表
                        ((SimpleAdapter)colection.getAdapter()).notifyDataSetChanged();
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }


    @Override
    protected void onResume() {
        super.onResume();
        Utils.hideBottomUIMenu(this);

        //init data
        new Thread() {
            @Override
            public void run() {
                initData(lstImageItem_history, 1);
                super.run();
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                initData(lstImageItem_colection, 2);
                super.run();
            }
        }.start();

    }


    //create buttons
    private void initData(ArrayList<HashMap<String, Object>> items, int what) {
        try {
            Message message;
            items.clear();
            //通知更新
            message = new Message();
            message.what = what;
            appHandler.sendMessage(message);

            long time = 0;

            ArrayList<HashMap> arrayList = new ArrayList();

            if (what==1){
                arrayList = myApp.getHistory();
            }else if (what==2){
                arrayList = myApp.getColection();
            }

            for (HashMap mm : arrayList){
                HashMap<String, Object> map = new HashMap<String, Object>();
                URL picUrl = new URL(Config.inmageUrlPrefix+(String) mm.get("image"));
                //Bitmap bitmap = BitmapFactory.decodeStream(picUrl.openStream());
                Bitmap bitmap = myApp.getBitmap(picUrl.toString());

                Matrix matrix = new Matrix();
                matrix.postScale(100f / bitmap.getWidth(), 150f / bitmap.getHeight()); //长和宽放大缩小的比例
                Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);

                map.put("ItemImage", resizeBmp);
                map.put("ItemText", (String) mm.get("media_name"));
                map.put("ItemId", (String) mm.get("media_id"));
                items.add(map);

                long nowtime = new Date().getTime();
                if (time == 0 || nowtime > time + 500) {
                    time = nowtime;

                    //通知更新
                    message = new Message();
                    message.what = what;
                    appHandler.sendMessage(message);
                }
            }

            message = new Message();
            message.what = what;
            appHandler.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
