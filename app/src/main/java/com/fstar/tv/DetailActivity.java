package com.fstar.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Rojama on 2014/12/20.
 */
public class DetailActivity extends Activity {
    private Activity context;

    private TextView titlePath;
    private ListView listView;
    private GridView gridView;
    private Button searchButton;
    private ArrayList<String> list = new ArrayList<String>();  //当前列表名称
    private ArrayList<String> idList = new ArrayList<String>();  //当前列表ID
    private LinkedList<Map<String,Object>> pathList = new LinkedList<Map<String,Object>>(); //类别路径

    private ArrayList<HashMap<String, Object>> lstImageItem = new ArrayList<HashMap<String, Object>>();  //GRID列表

    private Button[] titles = new Button[6];
    private HashMap<String, HashMap<String, Object>> titleItem = new HashMap<String, HashMap<String, Object>>();  //标题列表

    private Handler appHandler;
    private String mode_id;
    private String mode_name;

    private Bitmap bitmapBG = null;

    private MyApp myApp;

//    private boolean isloading = false;
    private String now_UUID = "";
    private String now_Title = "";

    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        context = this;

        myApp = (MyApp) getApplication(); //获得自定义的应用程序MyApp

        titlePath = (TextView) findViewById(R.id.titlePath);
        listView = (ListView) findViewById(R.id.listView);
        gridView = (GridView) findViewById(R.id.gridView);
        searchButton = (Button) findViewById(R.id.search_button);
        titles[0] = (Button) findViewById(R.id.title_button1);
        titles[1] = (Button) findViewById(R.id.title_button2);
        titles[2] = (Button) findViewById(R.id.title_button3);
        titles[3] = (Button) findViewById(R.id.title_button4);
        titles[4] = (Button) findViewById(R.id.title_button5);
        titles[5] = (Button) findViewById(R.id.title_button6);

        Intent intent = getIntent();
        mode_id = intent.getStringExtra("MODE_ID");
        mode_name = intent.getStringExtra("MODE_NAME");

        searchButton.setFocusable(true);
        searchButton.setEnabled(true);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(DetailActivity.this, SearchActivity.class);
                intent.putExtra("MODE_ID", mode_id);
                intent.putExtra("MODE_NAME", mode_name);
                startActivity(intent);
            }
        });


        ArrayAdapter<String> listArrayAdapter = new ArrayAdapter<String>
                (this, R.layout.detail_list_item, list);
        listView.setAdapter(listArrayAdapter);
//        if (19 != Build.VERSION.SDK_INT) {
//            listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//                @Override
//                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                    updateGrid(position);
//                }
//
//                @Override
//                public void onNothingSelected(AdapterView<?> parent) {
//
//                }
//            });
//        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                if (isloading) return;  //防止多次加载
//                toast = Toast.makeText(context, "载入中...", Toast.LENGTH_LONG);
//                toast.show();
//                isloading = true;
                Map type = new HashMap();
                type.put("mode_id", idList.get(position));
                type.put("mode_name", list.get(position));
                new Thread() {
                    Map position;

                    public Thread setPosition(Map position) {
                        this.position = position;
                        return this;
                    }

                    @Override
                    public void run() {
                        loadSubList(position, false);
                        loadGrid(position);
//                        toast.cancel();
//                        isloading = false;
                        super.run();
                    }
                }.setPosition(type).start();
            }
        });
        listView.requestFocus();

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
                intent.setClass(DetailActivity.this, MediaInfoActivity.class);
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
                    case 1:
                        Bundle bundleData = msg.getData();
                        list.clear();
                        list.addAll((ArrayList<String>) bundleData.getSerializable("list"));
                        ((ArrayAdapter)listView.getAdapter()).notifyDataSetChanged();

                        // 恢复选择项
                        Map map = pathList.getFirst();
                        if (map.containsKey("index")){
                            listView.setSelection((Integer) map.get("index"));
                        }else{
//                            if (19 != Build.VERSION.SDK_INT) {
//                                if (listView.getSelectedItemPosition() <= 0) {
//                                    updateGrid(0);
//                                }
//                            }
                            listView.setSelectionAfterHeaderView();
                        }
                        listView.requestFocus();
                        break;
                    case 2: //更新视频列表
                        ((SimpleAdapter)gridView.getAdapter()).notifyDataSetChanged();
                        break;
                    case 3: //更新标题栏
                        for (int i=0; i<titles.length; i++){
                            HashMap<String, Object> titlemap = titleItem.get(String.valueOf(i+1));
                            if (titlemap != null){
                                titles[i].setText((String)titlemap.get("title_name"));
                                titles[i].setFocusable(true);
                                titles[i].setEnabled(true);
                                titles[i].setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Button button = (Button) view;
                                        Map type = new HashMap();
                                        HashMap<String, Object> titlemap = null;
                                        if (button.getId() == R.id.title_button1){
                                            titlemap = titleItem.get("1");
                                        }else if (button.getId() == R.id.title_button2){
                                            titlemap = titleItem.get("2");
                                        }else if (button.getId() == R.id.title_button3){
                                            titlemap = titleItem.get("3");
                                        }else if (button.getId() == R.id.title_button4){
                                            titlemap = titleItem.get("4");
                                        }else if (button.getId() == R.id.title_button5){
                                            titlemap = titleItem.get("5");
                                        }else if (button.getId() == R.id.title_button6){
                                            titlemap = titleItem.get("6");
                                        }
                                        type.put("mode_id",titlemap.get("type_id"));
                                        type.put("mode_name",titlemap.get("title_name"));

                                        new Thread() {
                                            Map position;

                                            public Thread setPosition(Map position) {
                                                this.position = position;
                                                return this;
                                            }

                                            @Override
                                            public void run() {
                                                while(pathList.size()>1){
                                                    pathList.pop();
                                                }
                                                loadSubList(position, false);
                                                loadGrid(position);
                                                super.run();
                                            }
                                        }.setPosition(type).start();
                                    }
                                });
                            }else{
                                titles[i].setFocusable(false);
                                titles[i].setEnabled(false);
                            }
                        }
                        break;
                    case 4: //更新背景
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            if (bitmapBG != null) gridView.setBackground(new BitmapDrawable(bitmapBG));
                        }
                        break;
                    case 5: //更新标题路径
                        //设置标题路径
//                        titlePath.setText((String)((Map)pathList.getFirst()).get("mode_name"));
                        titlePath.setText("");
                        boolean first = true;
                        Iterator iterator = pathList.descendingIterator();
                        while (iterator.hasNext()){
                            if (!first) titlePath.append(" / ");
                            Map map2 = (Map) iterator.next();
                            titlePath.append((String) map2.get("mode_name"));
                            first = false;
                        }
                        if (!now_Title.isEmpty()){
                            titlePath.append(" / " + now_Title);
                        }
                        break;
                }
                super.handleMessage(msg);
            }
        };

        //init button data
        new Thread() {
            @Override
            public void run() {
                initData();
                super.run();
            }
        }.start();


    }


    private void initData() {
        Map map = new HashMap();
        map.put("mode_id",mode_id);
        map.put("mode_name", mode_name);
        loadSubList(map, false);
        loadTitle(mode_id);
        loadBg(mode_id);
    }

    private void updateGrid (int position){
        Map type = new HashMap();
        type.put("mode_id", idList.get(position));
        type.put("mode_name", list.get(position));
        new Thread() {
            Map position;

            public Thread setPosition(Map position) {
                this.position = position;
                return this;
            }

            @Override
            public void run() {
                loadGrid(position);
                super.run();
            }
        }.setPosition(type).start();
    }

    private synchronized void loadTitle(String block_id){
        try {
            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "getTitle";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            url += "&block_id=" + block_id;
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                JSONArray jsonArray = json.getJSONArray("title");
                if (jsonArray.length() == 0) {
                    return;
                }
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject sub = (JSONObject) jsonArray.get(i);
                    HashMap mm = (HashMap<String, Object>) Utils.parserToMap(sub.toString());
                    titleItem.put((String)mm.get("title_no"),mm);
                }
                //通知更新标题
                Message message = new Message();
                message.what = 3;
                appHandler.sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadBg(String block_id){
        try {
            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "getDetailBg";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            url += "&type_id=" + block_id;
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                JSONArray jsonArray = json.getJSONArray("subbg");
                if (jsonArray.length() == 0) {
                    return;
                }
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject sub = (JSONObject) jsonArray.get(i);
                    HashMap mm = (HashMap<String, Object>) Utils.parserToMap(sub.toString());
                    URL picUrl = new URL(Config.inmageUrlPrefix+(String) mm.get("image_bg"));
                    bitmapBG = myApp.getBitmap(picUrl.toString());
                }
                //通知更新
                Message message = new Message();
                message.what = 4;
                appHandler.sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadGrid(Map listMap) {
        try {
            String type_id = (String) listMap.get("mode_id");
            String super_name = (String) listMap.get("mode_name");

            String thread_UUID = UUID.randomUUID().toString();
            now_UUID = thread_UUID;

            Message message;

            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "parseMediaList";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            url += "&type_id=" + type_id;
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

    private synchronized void loadSubList(Map superMap, boolean isBack) {
        try {
            String super_id = (String) superMap.get("mode_id");
            String super_name = (String) superMap.get("mode_name");
            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "parseSubCate";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            url += "&super_id=" + super_id;
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                JSONArray jsonArray = json.getJSONArray("subtype");
                if (jsonArray.length() == 0) {
                    now_Title = super_name;
                    //通知更新标题
                    Message message = new Message();
                    message.what = 5;
                    appHandler.sendMessage(message);

                    return;
                }else{
                    now_Title = "";
                }

                ArrayList<String> list = new ArrayList<String>();
                idList.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject sub = (JSONObject) jsonArray.get(i);
                    HashMap mm = (HashMap<String, Object>) Utils.parserToMap(sub.toString());
                    list.add((String) mm.get("type_name"));
                    idList.add((String) mm.get("type_id"));
                }

                //返回不用加入
                if (!isBack) {
                    if (!pathList.isEmpty()) {
                        pathList.getFirst().put("index", listView.getSelectedItemPosition());
                    }
                    pathList.push(superMap);
                }


                //通知更新标题和列表
                Message message = new Message();
                Bundle bundleData = new Bundle();
                bundleData.putSerializable("list",list);
                message.setData(bundleData);
                message.what = 1;
                appHandler.sendMessage(message);
                message = new Message();
                message.what = 5;
                appHandler.sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //返回
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (pathList.size() > 1) {
                pathList.pop();
                new Thread() {
                    Map position;

                    public Thread setPosition(Map position) {
                        this.position = position;
                        return this;
                    }

                    @Override
                    public void run() {
                        loadSubList(position, true);
                        loadGrid(position);
                        super.run();
                    }
                }.setPosition(pathList.getFirst()).start();
                return false;
            }
        }
        return super.onKeyDown(keyCode,event);
    }
}


