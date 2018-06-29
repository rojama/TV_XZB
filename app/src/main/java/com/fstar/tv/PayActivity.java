package com.fstar.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.TextView;

import com.fstar.tv.tools.Config;
import com.fstar.tv.tools.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class PayActivity extends Activity {
    private Activity context;
    private MyApp myApp;
    private Handler appHandler;

    private TextView payDesc;
    private TextView payPrice;
    private ImageView barCode;

    String in_payDesc;
    double in_PayPrice;
    String in_PayId;
    String in_Attach;

    String code_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitvity_pay);
        context = this;
        myApp = (MyApp) getApplication();

        //get parmeart
        Intent intent = getIntent();
        in_payDesc = intent.getStringExtra("PayDesc");
        in_PayPrice = intent.getDoubleExtra("PayPrice" , 0);
        in_PayId = intent.getStringExtra("PayId");
        in_Attach = intent.getStringExtra("Attach");

        //get layout ui
        barCode = (ImageView) findViewById(R.id.BarCode);
        payDesc = (TextView) findViewById(R.id.PayDesc);
        payPrice = (TextView) findViewById(R.id.PayPrice);

        payDesc.setText("付款商品："+in_payDesc);
        payPrice.setText("付款金额："+in_PayPrice+"元");

        appHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        Bitmap bitmap = Utils.createQRImage(code_url, 600, 600);
                        barCode.setImageBitmap(bitmap);
                        break;
                    case 2:
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
        try {
            String processBO = "com.fstar.cms.WXBO";
            String processMETHOD = "get_code_url";
            String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                    + processBO;
            url += "&body=" + in_payDesc;
            url += "&total_fee=" + ((int)in_PayPrice*100); //折合成分来计算
            url += "&device_info=" + myApp.getDeviceId();
            url += "&product_id=" + in_PayId;
            url += "&attach=" + in_Attach; //付款月份数目
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                code_url = json.getString("code_url");

                Message message = new Message();
                message.what = 1;
                appHandler.sendMessage(message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
