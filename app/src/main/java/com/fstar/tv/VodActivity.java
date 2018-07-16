package com.fstar.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.fstar.tv.tools.Config;
import com.fstar.tv.tools.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by rojama on 2014-12-24.
 */
public class VodActivity  extends Activity {
    private Activity context;
    private VideoView mVideoView;
    private View viewTop;
    private TextView netSpeed, speedDanwei, shishi, srtTv;
    private MyApp myApp;

    private Handler appHandler;

    private String media_id;
    private String media_name;
    private int series_no;

    private MediaController mMediaController;
    private HashMap<String,Object> play_info;
    private ArrayList<HashMap<String,Object>> mediaInfo = new ArrayList<HashMap<String,Object>>();
    private String[] products;
    private String[] items;
    private String[] bodys;
    private Double[] price;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.hideBottomUIMenu(this);
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vod);
        myApp = (MyApp) getApplication(); //获得自定义的应用程序MyApp

        Intent intent = getIntent();
//        media_id = intent.getStringExtra("MEDIA_ID");
//        media_name = intent.getStringExtra("MEDIA_NAME");
//        series_no = intent.getIntExtra("SERIES_NO", 1);
        play_info = (HashMap<String, Object>) intent.getSerializableExtra("PLAY_INFO");
        media_id = (String) play_info.get("media_id");
        media_name = (String) play_info.get("media_name");
        series_no = (int) play_info.get("series_no");

        viewTop = findViewById(R.id.vod_player_top_view);
        viewTop.setVisibility(View.VISIBLE);
        mVideoView = (VideoView) findViewById(R.id.vod_player_videoView);
//        netSpeed = (TextView) findViewById(R.id.vod_player_real_speed);
//        speedDanwei = (TextView) findViewById(R.id.vod_player_speed_danwei);
//        shishi = (TextView) findViewById(R.id.vod_player_speed_wenzi);
//        srtTv = (TextView) findViewById(R.id.vod_player_srt_tv);

        //Create media controller
        mMediaController = new MediaController(this);

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //播放下一集
                series_no ++;
                play_info.put("series_no",series_no);
                play_info.remove("last_time");

                new Thread() {
                    @Override
                    public void run() {
                        initData();
                        super.run();
                    }
                }.start();
            }
        });

        //设置MediaController
        mVideoView.setMediaController(mMediaController);

        appHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        if (mediaInfo.size()>0) {
                            myApp.addHistory(play_info);  //保存历史

                            String url_prefix = (String) mediaInfo.get(0).get("url_prefix");
                            String video_url = url_prefix + (String) mediaInfo.get(0).get("full");
                            String series_name = (String) mediaInfo.get(0).get("series_name");
                            if (series_name == null){
                                series_name = "";
                            }

                            boolean vip = false;
                            if (mediaInfo.get(0).get("vip") != null){
                                vip = Boolean.valueOf((String)  mediaInfo.get(0).get("vip"));
                            }

                            //判断vip
                            if (vip){
                                //检查vip是否有效
                                if (myApp.getVipDate()==null || myApp.getVipDate().before(new Date())){
                                    //重新访问数据库
                                    reflashVip();
                                    return;
                                }
                            }

                            System.out.println("prepar to play :" + video_url);
                            mVideoView.setVideoURI(Uri.parse(video_url));
                            mVideoView.requestFocus();
                            mVideoView.start();

                            if (!series_name.isEmpty()){
                                series_name = ":"+series_name;
                            }
                            String tip = "播放第"+series_no+"集"+series_name;

                            //跳转到上次播放位置
                            if (play_info.containsKey("last_time")){
                                int last_time = (int) play_info.get("last_time");
                                if (last_time != 0) {
                                    System.out.println("play from " + last_time);
                                    tip += "，从上次" + Utils.timeToString(last_time) + "继续";
                                    mVideoView.seekTo(last_time);
                                }
                            }

                            Toast toast = Toast.makeText(context, tip, Toast.LENGTH_LONG);
                            toast.show();

                            appHandler.sendEmptyMessage(2);
                        }
                        break;
                    case 2:  //记录播放进度
                        runPalyTime = true;
                        appHandler.postDelayed(playTime, 10000);
                        break;
                    case 3:
                        //还是没有vip权限
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setIcon(R.drawable.ic_launcher);
                        builder.setTitle("请选择购买方式");
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setClass(VodActivity.this, PayActivity.class);
                                intent.putExtra("PayId", products[which]);
                                intent.putExtra("PayDesc", bodys[which]);
                                intent.putExtra("PayPrice", price[which]);
                                startActivity(intent);
                            }
                        });
                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                context.onBackPressed();
                            }
                        });
                        builder.show();
                        break;
//                    case 3:
//                        rxByte = TrafficStats.getTotalRxBytes();
//                        currentTime = System.currentTimeMillis();
//                        appHandler.postDelayed(speed, 1000);
                }
                super.handleMessage(msg);
            }
        };

        //init data
        new Thread() {
            @Override
            public void run() {
                initData();
                super.run();
            }
        }.start();
    }

    private void initData() {
        try {
            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "getProduct";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                JSONArray jsonArray = json.getJSONArray("Products");
                if (jsonArray.length() == 0) {
                    this.finish();
                    return;
                }

                bodys = new String[jsonArray.length()];
                products = new String[jsonArray.length()];
                items = new String[jsonArray.length()];
                price = new Double[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject sub = (JSONObject) jsonArray.get(i);
                    double cnyprice = sub.getInt("total_fee")/100.00;
                    cnyprice = new BigDecimal(cnyprice).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
                    products[i] = sub.getString("product_id");
                    price[i] = cnyprice;
                    items[i] = sub.getString("body")+"("+cnyprice+"元)";
                    bodys[i] = sub.getString("body");
                }
            }else{
                this.finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "parseMediaUrl";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            url += "&media_id=" + media_id;
            url += "&series_no=" + series_no;
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                JSONArray jsonArray = json.getJSONArray("mediaUrl");
                if (jsonArray.length() == 0) {
                    this.finish();
                    return;
                }
                mediaInfo.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject sub = (JSONObject) jsonArray.get(i);
                    mediaInfo.add((HashMap<String, Object>) Utils.parserToMap(sub.toString()));
                }

                //通知更新标题和列表
                Message message = new Message();
                message.what = 1;
                appHandler.sendMessage(message);
            }else{
                this.finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reflashVip(){
        //init data
        new Thread() {
            @Override
            public void run() {
                myApp.reflashVipDate();
                Message message = new Message();
                if (myApp.getVipDate()==null || myApp.getVipDate().before(new Date())){
                    message.what = 3;
                }else{
                    message.what = 1;
                }
                appHandler.sendMessage(message);
                super.run();
            }
        }.start();

    }

    //记录播放进度
    private boolean runPalyTime = true;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        runPalyTime = false;
        appHandler.removeCallbacks(playTime);
    }

    private Runnable playTime = new Runnable() {
        @Override
        public void run() {
            int last_time = mVideoView.getCurrentPosition();
            myApp.updateLastTime(media_id, last_time);
            if (runPalyTime) {
                appHandler.postDelayed(playTime, 10000);
            }
        }
    };

    //返回时重新判断vip
    @Override
    protected void onRestart() {
        super.onRestart();
        new Thread() {
            @Override
            public void run() {
                initData();
                super.run();
            }
        }.start();
    }

//    private long rxByte, currentTime;
//    private Runnable speed = new Runnable() {
//        @Override
//        public void run() {
//            if (rxByte != 0 && currentTime != 0) {
//                long tempTime = System.currentTimeMillis();
//                long tempByte = TrafficStats.getTotalRxBytes();
//                String speed = null;
//                String danwei = null;
//                if ((tempByte - rxByte) != 0 && (tempTime - currentTime) != 0) {
//                    long DownloadByte = ((tempByte - rxByte)
//                            / (tempTime - currentTime) * 1000 / 1024);
//                    if (DownloadByte < 1000) {
//                        speed = DownloadByte + "";
//                        danwei = " KB/S";
//
//                    } else {
//                        double DownloadByte2 = (double) DownloadByte / 1024D;
//                        speed = new DecimalFormat("#.##").format(DownloadByte2);
//                        danwei = " MB/S";
//                    }
//                    shishi.setVisibility(View.VISIBLE);
//                    netSpeed.setText(speed);
//                    speedDanwei.setText(danwei);
//                }
//                rxByte = tempByte;
//                currentTime = tempTime;
//            }
//            appHandler.postDelayed(speed, 1000);
//        }
//    };
}
