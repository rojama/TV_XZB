package com.fstar.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

    String out_trade_no;
    String code_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.hideBottomUIMenu(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitvity_pay);
        context = this;
        myApp = (MyApp) getApplication();

        //get parmeart
        Intent intent = getIntent();
        in_payDesc = intent.getStringExtra("PayDesc");
        in_PayPrice = intent.getDoubleExtra("PayPrice" , 0);
        in_PayId = intent.getStringExtra("PayId");

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
                        Toast toast = Toast.makeText(context, "支付完成，请返回后继续播放", Toast.LENGTH_LONG);
                        toast.show();
                        context.onBackPressed();
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
            url += "&device_info=" + myApp.getDeviceId();
            url += "&product_id=" + in_PayId;
            JSONObject json = Utils.readHttpJSON(url);
            if (json != null) {
                code_url = json.getString("code_url");
                out_trade_no = json.getString("out_trade_no");
                appHandler.postDelayed(checkPay, 2000);
                Message message = new Message();
                message.what = 1;
                appHandler.sendMessage(message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean runCheckpay = true;
    private Runnable checkPay = new Runnable() {
        @Override
        public void run() {

            new Thread() {
                @Override
                public void run() {
                    try {
                        String processBO = "com.fstar.cms.WXBO";
                        String processMETHOD = "get_deal";
                        String url = Config.serverBaseUrl + "/cm?ProcessMETHOD=" + processMETHOD + "&ProcessBO="
                                + processBO;
                        url += "&out_trade_no=" + out_trade_no;
                        JSONObject json = Utils.readHttpJSON(url);
                        if (json != null) {
                            String transaction_id = json.getString("transaction_id");
                            if (transaction_id != null && !transaction_id.isEmpty()){
                                runCheckpay = false;
                                Message message = new Message();
                                message.what = 2;
                                appHandler.sendMessage(message);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (runCheckpay) {
                        appHandler.postDelayed(checkPay, 2000);
                    }
                    super.run();
                }
            }.start();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        runCheckpay = false;
        appHandler.removeCallbacks(checkPay);
    }
}
