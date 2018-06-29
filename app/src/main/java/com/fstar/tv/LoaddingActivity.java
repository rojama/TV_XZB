package com.fstar.tv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.fstar.tv.tools.Config;
import com.fstar.tv.tools.Utils;

import org.json.JSONObject;

import java.text.SimpleDateFormat;

/**
 * Created by Rojama on 16/1/12.
 */
public class LoaddingActivity  extends Activity {
    private Activity context;
    private ImageView loddingimg;
    private Intent intent;
    private Handler appHandler;
    private MyApp myApp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loadding);
        loddingimg = (ImageView) findViewById(R.id.imageView);
        intent = new Intent();
        context = this;
        myApp = (MyApp) getApplication(); //获得自定义的应用程序MyApp

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
            String deviceId = Utils.getLocalMac(context)+"-"+Utils.getIMIEStatus(context)+"-"+Utils.getAndroidId(context);
            myApp.setDeviceId(deviceId);
            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "parseDeviceId";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO + "&DeviceId=" + deviceId + "&history=Y";;
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                //记录设备的vip时间
                if(json.has("validity")) {
                    String validity = json.getString("validity");
                    if (validity != null && !validity.isEmpty()) {
                        SimpleDateFormat formatdateD = new SimpleDateFormat("yyyy-MM-dd");
                        myApp.setVipDate(formatdateD.parse(validity));
                    }
                    myApp.setDeviceId(json.getString("device_info"));
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
