package com.fstar.tv;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fstar.tv.tools.Config;
import com.fstar.tv.tools.ImageFileCache;
import com.fstar.tv.tools.Utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Rojama on 16/1/12.
 */
public class LoaddingActivity  extends Activity {
    private Activity context;
    private ImageView loddingimg;
    private Intent intent;
    private Handler appHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loadding);
        loddingimg = (ImageView) findViewById(R.id.imageView);
        intent = new Intent();
        context = this;
        appHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        loddingimg.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        Toast.makeText(context, "用户不被允许使用", Toast.LENGTH_LONG).show();
                }
                super.handleMessage(msg);
            }
        };

        //init data
        new Thread(){
            @Override
            public void run() {
                //初始化界面
                initData();
                super.run();
            }
        }.start();


    }

    private void initData() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId = getLocalMacAddress()+"-"+tm.getDeviceId()+"-"+android.os.Build.SERIAL;
            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "parseDeviceId";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO + "&DeviceId=" + deviceId;
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                boolean ok = json.getBoolean("ok");
                if (!ok){
                    Message message = new Message();
                    message.what = 2;
                    appHandler.sendMessage(message);
                    return;
                }
            }else{
                //检查网络连接
                checkNetworkInfo();
            }


            processMETHOD = "getSetting";
            url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            json = Utils.readHttpJSON(url);
            if (json != null) {
                //clear cache
                String clear = json.getString("ClearCacheOnStart");
                if ("1".equals(clear)){
                    MyApp app = (MyApp) getApplication();
                    app.getFileCache().removeAllCache();
                }
            }

            Thread.sleep(2000);
//            Message message = new Message();
//            message.what = 1;
//            appHandler.sendMessage(message);
//            Thread.sleep(500);

            intent.setClass(LoaddingActivity.this, MainActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getLocalMacAddress() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        return info.getMacAddress();
    }


    private void checkNetworkInfo()
    {
        if (!isNetworkConnected(context)){
            startActivity(new Intent(Settings.ACTION_SETTINGS));

            do{
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }while (!isNetworkConnected(context));
        }
    }

    public boolean isNetworkConnected(Context context) {
        return Utils.isIpReachable(Config.serverBaseIP);
//        if (context != null) {
//            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
//                    .getSystemService(Context.CONNECTIVITY_SERVICE);
//            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
//            if (mNetworkInfo != null) {
//                return mNetworkInfo.isAvailable();
//            }
//        }
//        return false;
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
