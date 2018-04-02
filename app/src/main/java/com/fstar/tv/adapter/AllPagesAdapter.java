package com.fstar.tv.adapter;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public class AllPagesAdapter extends PagerAdapter {
	private List<View> viewLists;

	public AllPagesAdapter(ArrayList<View> views) {
		super();
		this.viewLists = views;
	}

    @Override
    public int getCount() {                                                                 //获得size
        return viewLists.size();
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public void destroyItem(ViewGroup view, int position, Object object)                       //销毁Item
    {
        view.removeView(viewLists.get(position));
    }

    @Override
    public Object instantiateItem(ViewGroup view, int position)                                //实例化Item
    {
        view.addView(viewLists.get(position), 0);
        return viewLists.get(position);
    }

	public void dataChanged(ArrayList<View> views) {
		this.viewLists = views;
		notifyDataSetChanged();
	}

}
