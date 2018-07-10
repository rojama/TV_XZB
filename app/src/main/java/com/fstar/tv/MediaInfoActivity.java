package com.fstar.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fstar.tv.adapter.AllPagesAdapter;
import com.fstar.tv.adapter.DetailsKeyTabAdapter;
import com.fstar.tv.tools.Config;
import com.fstar.tv.tools.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by rojama on 2014-12-23.
 */
public class MediaInfoActivity  extends Activity {
    private Activity context;
    private Button replay, play, choose, colection;// 4个主操作按钮
    private TextView videoName, point, editors, actors, sharpness; // keyName;
    private TextView introduce, area, type, year;
    private ImageView poster, shadow;
    private LinearLayout keyLayoutT;
    private TextView vip;

    private String media_id;
    private String media_name;
    private HashMap last_paly;

    private Handler appHandler;

    private HashMap<String,Object> mediaInfo = new HashMap<String,Object>();
    private HashMap<Integer, String> mediaName = new HashMap<Integer, String>();
    private HashMap<Integer,Boolean> mediaVip = new HashMap<Integer,Boolean>();
    private MyApp myApp;

    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_media_info);

        myApp = (MyApp) getApplication(); //获得自定义的应用程序MyApp

        Intent intent = getIntent();
        media_id = intent.getStringExtra("MEDIA_ID");
        media_name = intent.getStringExtra("MEDIA_NAME");

        // 海报
        poster = (ImageView) findViewById(R.id.details_poster);
        // 清晰度
        sharpness = (TextView) findViewById(R.id.details_sharpness);
        // 影视名称
        videoName = (TextView) findViewById(R.id.details_name);
        // 评分
        point = (TextView) findViewById(R.id.details_rate);
        // 导演
        editors = (TextView) findViewById(R.id.details_director);
        // 所属地区
        area = (TextView) findViewById(R.id.details_playTimes);
        // 演员
        actors = (TextView) findViewById(R.id.details_actors);
        // 所属类别
        type = (TextView) findViewById(R.id.details_update);
        // 年份
        year = (TextView) findViewById(R.id.details_year);
        // 介绍
        introduce = (TextView) findViewById(R.id.details_video_introduce);
        // 重头播放
        replay = (Button) findViewById(R.id.details_replay);
        // 播放
        play = (Button) findViewById(R.id.details_play);
//        play.setVisibility(View.GONE);
        // 选集
        choose = (Button) findViewById(R.id.details_choose);
        choose.setTag("choose");
        choose.setVisibility(View.GONE);
        // 收藏
        colection = (Button) findViewById(R.id.details_colection);
//        colection.setVisibility(View.GONE);
//        // 电视剧选集布局
        keyLayoutT = (LinearLayout) findViewById(R.id.details_key_tv);
//        // 综艺选集软键布局
//        keyLayoutA = (LinearLayout) findViewById(R.id.details_key_arts);

        vip = (TextView) findViewById(R.id.textViewVIP);

        //查询上次观看记录
        last_paly = myApp.getHistroy(media_id);
        if (!last_paly.isEmpty()){
            int last_series = (int) last_paly.get("last_series");
            play.setText("继续第"+last_series+"集");
        }

        appHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        poster.setImageBitmap((Bitmap) mediaInfo.get("image_bm"));
                        videoName.setText((String) mediaInfo.get("media_name"));
                        point.setText((String) mediaInfo.get("score"));
                        sharpness.setText("高清");
                        editors.append((String) mediaInfo.get("director"));
                        actors.append((String) mediaInfo.get("actor"));
                        introduce.setText((String) mediaInfo.get("media_describe"));
                        play.requestFocus();
                        int total_series = Integer.parseInt("0"+(String) mediaInfo.get("total_series"));
                        if (total_series > 1){
                            choose.setVisibility(View.VISIBLE); //显示选集按钮
                        }
                        break;
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

        //init listenner
        /* 播放按键的 监听 跳转 */
        play.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (media_id != null && !media_id.isEmpty()) {
                //myApp.addHistory(mediaInfo);
                Intent intent = new Intent();
                intent.setClass(MediaInfoActivity.this, VodActivity.class);
                //intent.putExtra("MEDIA_ID", (String) mediaInfo.get("media_id"));
                //intent.putExtra("MEDIA_NAME", (String) mediaInfo.get("media_name"));
                mediaInfo.put("series_no", 1);
                last_paly = myApp.getHistroy(media_id);
                if (last_paly != null && !last_paly.isEmpty()){
                    int last_series = (int) last_paly.get("last_series");
                    int last_time = (int) last_paly.get("last_time");
                    mediaInfo.put("series_no", last_series);
                    mediaInfo.put("last_time", last_time);
                }
                intent.putExtra("PLAY_INFO", mediaInfo);
                startActivity(intent);
            }
            }
        });

        choose.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (keyLayoutT.getVisibility() == View.VISIBLE) {
                keyLayoutT.setVisibility(View.GONE);
            } else {
                int total_series = Integer.parseInt("0"+(String) mediaInfo.get("total_series"));
                CreateTvLayout(total_series);
                keyLayoutT.setVisibility(View.VISIBLE);
                keyLayoutT.requestFocus();
            }
            }
        });

        if (myApp.isColection(media_id)){
            colection.setText("取消收藏");
        }else{
            colection.setText("收藏");
        }

        colection.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (media_id != null && !media_id.isEmpty()) {
                    if (myApp.isColection(media_id)){
                        myApp.delColection(media_id);
                        colection.setText("收藏");
                        Toast.makeText(context, "已经取消收藏", Toast.LENGTH_SHORT).show();
                    }else{
                        myApp.addColection(mediaInfo);
                        colection.setText("取消收藏");
                        Toast.makeText(context, "已经收藏完成", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        //获取焦点后将选集面板隐藏
//        View.OnFocusChangeListener chooseHide = new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//            if (hasFocus){
//                if (keyLayoutT.getVisibility() == View.VISIBLE) {
//                    keyLayoutT.setVisibility(View.GONE);
//                }
//            }
//            }
//        };
//
//        play.setOnFocusChangeListener(chooseHide);
//        choose.setOnFocusChangeListener(chooseHide);
//        colection.setOnFocusChangeListener(chooseHide);
    }

    @Override
    protected void onResume() {
        super.onResume();

        last_paly = myApp.getHistroy(media_id);
        if (!last_paly.isEmpty()){
            int last_series = (int) last_paly.get("last_series");
            play.setText("继续第"+last_series+"集");
        }

        //更新VIP状态
        if (myApp.getVipDate()==null || myApp.getVipDate().before(new Date())){
            //vip.setText("请购买VIP");
        }else{
            SimpleDateFormat formatdateD=new SimpleDateFormat("yyyy年M月d日");
            vip.setText("VIP有效期到\n"+ formatdateD.format(myApp.getVipDate()));
        }
    }

    private void initData() {
        try {
            String processBO = "com.fstar.cms.TVServerBO";
            String processMETHOD = "parseMediaInfo";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            url += "&media_id=" + media_id;
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                JSONArray jsonArray = json.getJSONArray("mediaInfo");
                if (jsonArray.length() == 0) {
                    return;
                }
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject sub = (JSONObject) jsonArray.get(i);
                    mediaInfo = (HashMap<String, Object>) Utils.parserToMap(sub.toString());
                    URL picUrl = new URL(Config.inmageUrlPrefix+(String) mediaInfo.get("image"));
                    //Bitmap bitmap = BitmapFactory.decodeStream(picUrl.openStream());
                    Bitmap bitmap = myApp.getBitmap(picUrl.toString());
                    mediaInfo.put("image_bm",bitmap);
                }
                //url for name
                jsonArray = json.getJSONArray("mediaUrl");
                HashMap<String, Object> subdata;
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject sub = (JSONObject) jsonArray.get(i);
                    subdata = (HashMap<String, Object>) Utils.parserToMap(sub.toString());
                    int series_no = Integer.valueOf((String)subdata.get("series_no"));
                    mediaName.put(series_no, (String)subdata.get("series_name"));
                    if (subdata.get("vip") != null) {
                        mediaVip.put(series_no, Boolean.valueOf((String) subdata.get("vip")));
                    }else{
                        mediaVip.put(series_no,false);
                    }
                }


                //通知更新标题和列表
                Message message = new Message();
                message.what = 1;
                appHandler.sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void CreateTvLayout(int total_series) {
        // 每10集一个标签
        final Gallery keyGallery = (Gallery) findViewById(R.id.details_key_gallery);
        // 集数编号
        final ViewPager keyPager = (ViewPager) findViewById(R.id.details_key_pager);

        keyGallery.setAdapter(new DetailsKeyTabAdapter(this,countStrArr(total_series, false)));

        keyPager.setAdapter(new AllPagesAdapter(addViewToPager(total_series,false)));

        keyGallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                keyPager.setCurrentItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        keyPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int arg0) {
                keyGallery.setSelection(arg0);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    //根据集数产生(整十级)标签字符串数组
    private List<String> countStrArr(int num, boolean containUpdate) {
        List<String> name = new ArrayList<String>();
        // 不包含更新，顺序排列
        if (!containUpdate) {
            int index = 1;
            StringBuilder sb = new StringBuilder();
            while (index <= num) {
                if (index % 10 == 1) {
                    sb.append(index);
                    sb.append('-');
                } else if (index % 10 == 0) {
                    sb.append(index);
                    name.add(sb.toString());
                    sb = new StringBuilder();
                }
                if (index == num && index % 10 != 0) {
                    sb.append(index);
                    name.add(sb.toString());
                }
                index++;
            }
        } else {
            int index = num;
            int count = 1;
            StringBuilder sb = new StringBuilder();
            while (index >= 1) {
                if (count % 10 == 1) {
                    sb.append(index);
                    sb.append('-');
                } else if (count % 10 == 0) {
                    sb.append(index);
                    name.add(sb.toString());
                    sb = new StringBuilder();
                }
                if (index == 1) {
                    sb.append(index);
                    name.add(sb.toString());
                }
                index--;
                count++;
            }
        }
        return name;
    }

    private ArrayList<View> addViewToPager(int num,  boolean containUodate) {
        System.out.println("集数" + num);
        LinearLayout line = new LinearLayout(this);
        ArrayList<View> pages = new ArrayList<View>();
        // 不包含更新，顺序排列
        if (!containUodate) {
            int index = 1;
            while (index <= num) {
                line.addView(createSetBTN(index));
                if (index % 10 == 0) {
                    pages.add(line);
                    line = new LinearLayout(this);
                } else {
                    if (index == num) {
                        pages.add(line);
                    }
                }
                index++;
            }
        } else {
            int index = num;
            int count = 1;
            while (index >= 1) {
                line.addView(createSetBTN(index));
                if (count % 10 == 0) {
                    pages.add(line);
                    line = new LinearLayout(this);
                } else {
                    if (index == 1) {
                        pages.add(line);
                    }
                }
                index--;
                count++;
            }
        }
        return pages;
    }


    private Button createSetBTN(int index) {
        final Button btn = new Button(this);
        btn.setWidth(110);
        btn.setHeight(60);
        btn.setSingleLine(true);
        btn.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        btn.setMarqueeRepeatLimit(-1);
        btn.setText("第" + index + "集");
        btn.setTextSize(25);
        btn.setTag(index);
        btn.getBackground().setAlpha(100);
        btn.setTextColor(Color.WHITE);

        if (!mediaVip.containsKey(index)){
            btn.setTextColor(Color.GRAY);
            return btn;   //没有维护地址的按钮是没有绑定事件的
        }
        //本季收费
        if(mediaVip.get(index)){
            btn.setTextColor(Color.YELLOW);
        }

        // 跳转到播放器
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (media_id != null && !media_id.isEmpty()) {
                    //myApp.addHistory(mediaInfo);
                    Intent intent = new Intent();
                    intent.setClass(MediaInfoActivity.this, VodActivity.class);
                    //intent.putExtra("MEDIA_ID", (String) mediaInfo.get("media_id"));
                    //intent.putExtra("MEDIA_NAME", (String) mediaInfo.get("media_name"));
                    //intent.putExtra("SERIES_NO", (Integer) v.getTag());
                    mediaInfo.put("series_no", (Integer) v.getTag());
                    mediaInfo.remove("last_time");
                    intent.putExtra("PLAY_INFO", mediaInfo);
                    startActivity(intent);
                }
            }
        });
        btn.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View arg0, boolean hasFocus) {
                int tag = (int) arg0.getTag();
                String name = mediaName.get(tag);
                if(hasFocus && !name.isEmpty()){
                    String text = "第" + tag + "集:" + name;
                    if (toast != null) {
                        toast.setText(text);
                        toast.setDuration(Toast.LENGTH_SHORT);
                        toast.show();
                    } else
                    {
                        toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    btn.setText(text);
                }else{
                    btn.setText("第" + tag + "集");
                }
            }
        });

        return btn;
    }

}
