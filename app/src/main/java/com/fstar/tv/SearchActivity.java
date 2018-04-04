package com.fstar.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.fstar.tv.tools.Config;
import com.fstar.tv.tools.Utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class SearchActivity extends Activity {
    private Activity context;
    private MyApp myApp;
    private TableLayout inputGrid;
    private GridView gridView;
    private TextView inputText;
    private ArrayList<HashMap<String, Object>> lstImageItem = new ArrayList<HashMap<String, Object>>();  //GRID列表
    private Handler appHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        context = this;
        myApp = (MyApp) getApplication();

        //get layout ui
        inputGrid = (TableLayout) findViewById(R.id.inputGrid);
        inputText = (TextView) findViewById(R.id.inputText);
        gridView = (GridView) findViewById(R.id.gridView);

        //add input puttons
        //设置Button的布局参数
        int buttonSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());

        char[] puttonStrs = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        int culno = 5;
        TableRow tableRow = null;
        for (char puttonStr : puttonStrs){
            if (culno == 5){
                tableRow = new TableRow(context);
                inputGrid.addView(tableRow);
                culno = 0;
            }
            culno ++;

            Button button = new Button(context);
            button.setText(puttonStr+"");
            button.setTextSize(30);
            button.setWidth(buttonSize);
            button.setHeight(buttonSize);
            tableRow.addView(button);
            //add even
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    inputText.append(((Button)v).getText());
                }
            });
        }

        //del
        TableRow.LayoutParams tlp = new TableRow.LayoutParams();
        tlp.span = 2;
        Button button = new Button(context);
        button.setText("删除");
        button.setTextSize(30);
        button.setTextColor(0xFFEE4000);
        button.setWidth(buttonSize);
        button.setHeight(buttonSize);
        button.setLayoutParams(tlp);
        tableRow.addView(button);
        //add even
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence text = inputText.getText();
                if (text.length() > 0) {
                    inputText.setText(text.subSequence(0, text.length() - 1));
                }
            }
        });

        //confom
        button = new Button(context);
        button.setText("搜索");
        button.setTextSize(30);
        button.setTextColor(0xFF7B68EE);
        button.setWidth(buttonSize);
        button.setHeight(buttonSize);
        button.setLayoutParams(tlp);
        tableRow.addView(button);
        //add even
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence text = inputText.getText();
                if (text.length() > 0) {
                    //init button data
                    new Thread() {
                        @Override
                        public void run() {
                            loadGrid();
                            super.run();
                        }
                    }.start();
                }
            }
        });

        //grid init
        SimpleAdapter saImageItems = new SimpleAdapter(this,
                lstImageItem,//数据来源
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
        gridView.setAdapter(saImageItems);
        //添加消息处理
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.setClass(SearchActivity.this, MediaInfoActivity.class);
                HashMap<String, Object> itemMap = lstImageItem.get(position);
                intent.putExtra("MEDIA_ID", (String) itemMap.get("ItemId"));
                intent.putExtra("MEDIA_NAME", (String) itemMap.get("ItemText"));
                startActivity(intent);
            }
        });


        appHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 2: //更新视频列表
                        ((SimpleAdapter)gridView.getAdapter()).notifyDataSetChanged();
                        break;
                }
                super.handleMessage(msg);
            }
        };


    }

    private String now_UUID = "";
    //create buttons
    private void loadGrid() {
        CharSequence text = inputText.getText();
        try {

            String thread_UUID = UUID.randomUUID().toString();
            now_UUID = thread_UUID;

            Message message;

            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "searchMedia";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            url += "&key_word=" + text;
            JSONObject json = Utils.readHttpJSON(url);

            lstImageItem.clear();

            if (json != null) {
                JSONArray jsonArray = json.getJSONArray("mediaList");

                long time = 0;

                for (int i = 0; i < jsonArray.length(); i++) {
                    if (!now_UUID.equals(thread_UUID)) return;  //判断重复线程
                    JSONObject sub = (JSONObject) jsonArray.get(i);
                    HashMap mm = (HashMap<String, Object>) Utils.parserToMap(sub.toString());
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
                    if (!now_UUID.equals(thread_UUID)) return;  //判断重复线程
                    lstImageItem.add(map);

                    long nowtime = new Date().getTime();
                    if (time == 0 || nowtime > time + 500) {
                        time = nowtime;

                        //通知更新标题和列表
                        message = new Message();
                        message.what = 2;
                        appHandler.sendMessage(message);
                    }
                }
            }

            message = new Message();
            message.what = 2;
            appHandler.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
