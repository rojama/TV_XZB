package com.fstar.tv;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.fstar.tv.adapter.TypeItemAdapter;
import com.fstar.tv.tools.Config;
import com.fstar.tv.tools.UpdateManager;
import com.fstar.tv.tools.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Rojama on 2014/12/28.
 */
public class MainActivity extends Activity{
    private Activity context;
    private static final int UPDATE = 1;
    private static final int INSTALL = 2;
    private List<Map<String,Object>> allButtons = new ArrayList<Map<String,Object>>();

    private Handler appHandler;
    private Gallery gallery;
    private ImageView logoImage;

    private UpdateManager mUpdateManager;
    private File instaillFile = null;

    private MyApp myApp;
    private ImageButton home_b_login;
    private ImageButton home_b1;
    private ImageButton home_b2;
    private ImageButton home_b3;
    private ImageButton home_b_setting;
    private ImageButton home_b_my;
    private ImageButton home_b_collect;
    private ImageButton home_b_search;

    private Toast devToast;

    @Override
    public void onBackPressed() {
        return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.hideBottomUIMenu(this);

        //瑙ｉ攣
//        wakeUpAndUnlock(this);

//        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
//        try {
//            Date dt = df.parse("2017-01-01");
//            if (dt.before(new Date())) {
//                System.out.println("expast");
//                return;
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }

        context = this;

        devToast = Toast.makeText(context,"功能开发中",Toast.LENGTH_SHORT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myApp = (MyApp) getApplication(); //获得自定义的应用程序MyApp

        logoImage = (ImageView) findViewById(R.id.logoImage);
        gallery = (Gallery) findViewById(R.id.typeGallery);

        TypeItemAdapter tia = new TypeItemAdapter(context,allButtons);
        gallery.setAdapter(tia);

        gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    Intent intent = new Intent();
                    Map<String, Object> buttonMap = (Map<String, Object>) gallery.getAdapter().getItem(position);

                    String app = ((String) buttonMap.get("APP")).trim();
                    if ("".equals(app)) {
                        intent.setClass(MainActivity.this, DetailActivity.class);
                    } else {  //启动其它程序
                        String[] apps = app.split("@");
                        if (isAppInstalled(apps[0])){
                            ComponentName componetName = new ComponentName(apps[0], apps[1]);
                            intent.setAction(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setComponent(componetName);
                        }else{
                            DownloadThread download = new DownloadThread(apps[2]);
                            download.start();
                            Toast.makeText(getApplicationContext(), "开始下载需要的第三方应用，请稍等！", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    intent.putExtra("MODE_ID", (String) buttonMap.get("type_id"));
                    intent.putExtra("MODE_NAME", (String) buttonMap.get("type_name"));
                    startActivity(intent);
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "找不到相应的程序，无法启动。！", Toast.LENGTH_LONG).show();
                }
            }
        });

//        鏀惧ぇ鍔ㄧ敾
        gallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TypeItemAdapter)parent.getAdapter()).setSelectItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        appHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPDATE:
                        ((TypeItemAdapter)gallery.getAdapter()).notifyDataSetChanged();
                        gallery.setSelection(TypeItemAdapter.MAX_VALUE/2);
                        break;
                    case INSTALL:
                        if (instaillFile == null){
                            Toast.makeText(getApplicationContext(), "无法下载需要的第三方应用！", Toast.LENGTH_LONG).show();
                        }else{
                            Intent intent = new Intent();
                            Toast.makeText(getApplicationContext(), "下载完成，请手动安装！", Toast.LENGTH_LONG).show();
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setAction(android.content.Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(instaillFile),
                                    "application/vnd.android.package-archive");
                            startActivity(intent);
                        }
                        break;

                }
                super.handleMessage(msg);
            }
        };

        //设置首页按钮高亮
        View.OnFocusChangeListener imageButtonOnFocusChangeListener = new View.OnFocusChangeListener() {
            public void onFocusChange(View view, boolean isFocused) {
                if (isFocused == true) {
                    view.setBackgroundColor(0x88FF0000);
                }
                else {
                    view.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        };

        home_b1 = (ImageButton) findViewById(R.id.home_b1);
        home_b2 = (ImageButton) findViewById(R.id.home_b2);
        home_b3 = (ImageButton) findViewById(R.id.home_b3);
        home_b_setting = (ImageButton) findViewById(R.id.home_b_setting);
        home_b_my = (ImageButton) findViewById(R.id.home_b_my);
        home_b_login = (ImageButton) findViewById(R.id.home_b_login);
        home_b_collect = (ImageButton) findViewById(R.id.home_b_collect);
        home_b_search = (ImageButton) findViewById(R.id.home_b_search);

        home_b1.setOnFocusChangeListener(imageButtonOnFocusChangeListener);
        home_b2.setOnFocusChangeListener(imageButtonOnFocusChangeListener);
        home_b3.setOnFocusChangeListener(imageButtonOnFocusChangeListener);
        home_b_setting.setOnFocusChangeListener(imageButtonOnFocusChangeListener);
        home_b_my.setOnFocusChangeListener(imageButtonOnFocusChangeListener);
        home_b_login.setOnFocusChangeListener(imageButtonOnFocusChangeListener);
        home_b_collect.setOnFocusChangeListener(imageButtonOnFocusChangeListener);
        home_b_search.setOnFocusChangeListener(imageButtonOnFocusChangeListener);

        home_b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devToast.show();
            }
        });
        home_b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devToast.show();
            }
        });
        home_b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devToast.show();
            }
        });
        home_b_my.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devToast.show();
            }
        });
        home_b_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devToast.show();
            }
        });

        home_b_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); //直接进入手机中的wifi网络设置界面
            }
        });
        home_b_collect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CollectActivity.class);
                startActivity(intent);
            }
        });
        home_b_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });

        //init data
        new Thread(){
            @Override
            public void run() {
                //初始化界面
                initData();
                super.run();
            }
        }.start();


        //这里来检测版本是否需要更新
        mUpdateManager = new UpdateManager(context);
        mUpdateManager.checkUpdate();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void initData() {
        try {
            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "parseTopCate";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                JSONArray jsonArray = json.getJSONArray("type");
                for (int i=0; i<jsonArray.length(); i++){
                    JSONObject sub = (JSONObject)jsonArray.get(i);
                    HashMap<String, Object> buttonMap = (HashMap<String, Object>) Utils.parserToMap(sub.toString());
                    URL picUrl = new URL(Config.inmageUrlPrefix+(String) buttonMap.get("image"));
                    //Bitmap pngBM = BitmapFactory.decodeStream(picUrl.openStream());
                    Bitmap pngBM = myApp.getBitmap(picUrl.toString());
                    buttonMap.put("IMAGE", pngBM);
                    URL picUrl_sel = new URL(Config.inmageUrlPrefix+(String) buttonMap.get("image_sel"));
                    //Bitmap pngBM_sel = BitmapFactory.decodeStream(picUrl_sel.openStream());
                    Bitmap pngBM_sel = myApp.getBitmap(picUrl_sel.toString());
                    buttonMap.put("IMAGE_SEL", pngBM_sel);
                    buttonMap.put("TEXT", buttonMap.get("type_name"));
                    buttonMap.put("APP", buttonMap.get("component"));
                    allButtons.add(buttonMap);
                }
                //閫氱煡鐣岄潰鏇存柊
                Message message = new Message();
                message.what = UPDATE;
                appHandler.sendMessage(message);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    //4.判断一个apk文件是否已经安装成功（根据报名判断该应用是否已经成功安装）
    private boolean isAppInstalled(String packageName){
        PackageManager pm = context.getPackageManager();
        boolean installed =false;
        try{
            pm.getPackageInfo(packageName,PackageManager.GET_ACTIVITIES);
            installed =true;
        }catch(PackageManager.NameNotFoundException e){
            installed =false;
        }
        return installed;
    }

    public class DownloadThread extends Thread{

        public String url;

        public DownloadThread(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {
                instaillFile = Utils.downLoadFile(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Message message = new Message();
            message.what = INSTALL;
            appHandler.sendMessage(message);
        }
    }


}
