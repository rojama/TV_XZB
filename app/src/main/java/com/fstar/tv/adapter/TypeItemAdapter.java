package com.fstar.tv.adapter;

import android.app.Activity;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.fstar.tv.R;

import java.util.List;
import java.util.Map;


/**
 * Created by Rojama on 2014/12/28.
 */
public class TypeItemAdapter extends BaseAdapter {

    public static int MAX_VALUE = 1000;
    private Activity context;
    private int selectItem;
    private List<Map<String, Object>> list;

    public TypeItemAdapter(Activity context, List<Map<String, Object>> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        if (list.isEmpty()){
            return 0;
        }
        return MAX_VALUE;
    }

    @Override
    public Object getItem(int position) {
        return list.get(position % list.size());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (list.isEmpty()){
            return null;
        }
        LayoutInflater inflater = context.getLayoutInflater();
        View itemView = inflater.inflate(R.layout.main_type_item, null);
        itemView.setFocusable(true);
        Map info = list.get(position % list.size());

        int screenWidth = parent.getWidth();
        int screenHeight = parent.getHeight();

        //System.out.println(screenWidth + "/" + screenHeight);

        ImageView typeItemImage_bg = (ImageView) itemView.findViewById(R.id.typeItemImage_bg);
        ImageView typeItemImage_fg = (ImageView) itemView.findViewById(R.id.typeItemImage_fg);

        float bigWidth = 1.25f;  //放大倍数
        float bigHeight = 1.25f;  //放大倍数

        float spacing = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,120,context.getResources().getDisplayMetrics());

        int nomorlWidth = (int)((screenWidth-spacing)/(6+bigWidth));  //60=6*spacing
        int nomorHeight = (int)(screenHeight/bigHeight);

        //放大
        if (position == selectItem){
            itemView.requestFocus();
            typeItemImage_bg.setImageResource(R.drawable.button_bg_h);
        	typeItemImage_fg.setImageBitmap((android.graphics.Bitmap) info.get("IMAGE_SEL"));
            typeItemImage_bg.getLayoutParams().width = (int)(nomorlWidth*bigWidth);
            typeItemImage_bg.getLayoutParams().height = (int)(nomorHeight*bigHeight);
            typeItemImage_fg.getLayoutParams().width = (int)(nomorlWidth*bigWidth);
            typeItemImage_fg.getLayoutParams().height = (int)(nomorHeight*bigHeight);
        }else{
        	typeItemImage_bg.setImageResource(R.drawable.button_bg);
        	typeItemImage_fg.setImageBitmap((android.graphics.Bitmap) info.get("IMAGE"));
            typeItemImage_bg.getLayoutParams().width = nomorlWidth;
            typeItemImage_bg.getLayoutParams().height = nomorHeight;
            typeItemImage_fg.getLayoutParams().width = nomorlWidth;
            typeItemImage_fg.getLayoutParams().height = nomorHeight;
        }
        return itemView;
    }


    public void setSelectItem(int selectItem) {
        if (this.selectItem != selectItem) {
            this.selectItem = selectItem;
            notifyDataSetChanged();
        }
    }

    public Object getSelectItem() {
        return this.getItem(selectItem);
    }

}
