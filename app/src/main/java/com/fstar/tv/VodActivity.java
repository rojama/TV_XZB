package com.fstar.tv;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.fstar.tv.tools.Config;
import com.fstar.tv.tools.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by rojama on 2014-12-24.
 */
public class VodActivity  extends Activity {

    private VideoView mVideoView;
    private View viewTop;
    private TextView netSpeed, speedDanwei, shishi, srtTv;

    private Handler appHandler;

    private String media_id;
    private String media_name;
    private int series_no;

    private MediaController mMediaController;
    private ArrayList<HashMap<String,Object>> mediaInfo = new ArrayList<HashMap<String,Object>>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vod);

        Intent intent = getIntent();
        media_id = intent.getStringExtra("MEDIA_ID");
        media_name = intent.getStringExtra("MEDIA_NAME");
        series_no = intent.getIntExtra("SERIES_NO", 1);

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
                            String url_prefix = (String) mediaInfo.get(0).get("url_prefix");
                            String video_url = url_prefix + (String) mediaInfo.get(0).get("full");
                            System.out.println("prepar to play :" + video_url);
                            mVideoView.setVideoURI(Uri.parse(video_url));
                            mVideoView.requestFocus();
                            mVideoView.start();

                            appHandler.sendEmptyMessage(2);
                        }
                        break;
//                    case 2:
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