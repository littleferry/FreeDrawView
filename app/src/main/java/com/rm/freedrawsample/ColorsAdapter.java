package com.rm.freedrawsample;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;

import com.rm.freedrawview.FreeDrawHelper;

/**
 * Created by Administrator on 2017/4/24.
 */

public class ColorsAdapter extends BaseAdapter {
    private TypedArray colors;
    private int selIndex = 0;

    public ColorsAdapter(Context context) {
        colors = ColorHelper.getColors(context);
    }

    public int getSelIndex() {
        return selIndex;
    }

    public void setSelIndex(int selIndex) {
        this.selIndex = selIndex;
        notifyDataSetChanged();
    }

    /**
     * PagerAdapter管理数据大小
     */
    @Override
    public int getCount() { // 取得要显示内容的数量
        return colors.length();
    }

    public Object getItem(int position) { // 每个资源的位置
        return colors.getIndex(position);
    }

    public long getItemId(int position) { // 取得每个项的ID
        return position;
    }

    // 将资源设置到一个组件之中，很明显这个组件是ImageView
    public View getView(int position, View convertView, ViewGroup parent) {
        RelativeLayout view = new RelativeLayout(parent.getContext());
        View v = new View(parent.getContext());
        int width = (int) FreeDrawHelper.convertDpToPixels(50);
        view.addView(v, width, -1);
        v.setBackgroundColor(colors.getColor(position, Color.BLACK));
        int pad = (int) FreeDrawHelper.convertDpToPixels(5);
        view.setPadding(pad, pad, pad, pad);
        view.setBackgroundColor(position==selIndex?Color.RED:Color.TRANSPARENT);
        return view; // 返回该view对象，作为key
    }
}
