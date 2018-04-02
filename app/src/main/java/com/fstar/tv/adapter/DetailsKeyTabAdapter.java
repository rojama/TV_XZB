package com.fstar.tv.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;

import java.util.Collections;
import java.util.List;


public class DetailsKeyTabAdapter extends BaseAdapter {
	private Context context;
	private List<String> list;
	private int selectedTab;

	public DetailsKeyTabAdapter(Context context, List<String> list) {
		this.context = context;
		if (list == null) {
			list = Collections.emptyList();
		} else {
			this.list = list;
		}
	}

	public void setSelectedTab(int tab) {
		if (tab != selectedTab) {
			selectedTab = tab;
			notifyDataSetChanged();
		}
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Button btn = new Button(context);
		btn.setSingleLine(true);// 单行
//		btn.setEllipsize(TruncateAt.MARQUEE);// 跑马灯
//		btn.setMarqueeRepeatLimit(3);// 无限重复
		btn.setText(list.get(position));
		btn.setLayoutParams(new Gallery.LayoutParams(120, 60));
		btn.setGravity(Gravity.CENTER);
        btn.getBackground().setAlpha(50);
        btn.setTextSize(25);
        btn.setTextColor(Color.WHITE);
		return btn;
	}

}
