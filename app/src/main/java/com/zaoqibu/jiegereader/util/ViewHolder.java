package com.zaoqibu.jiegereader.util;

import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

/**
 * Created by vwarship on 2015/3/4.
 */
public class ViewHolder {
    private SparseArray<View> views;
    private View convertView;

    private ViewHolder(View convertView) {
        this.convertView = convertView;
        views = new SparseArray<View>();
    }
    public static ViewHolder get(View convertView) {
        ViewHolder viewHolder = (ViewHolder)convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        return viewHolder;
    }

    public <T extends View> T getView(int resId) {
        View view = views.get(resId);
        if (view == null) {
            view = convertView.findViewById(resId);
            views.put(resId, view);
        }

        return (T) view;
    }

    public void setText(int resId, String text) {
        TextView view = getView(resId);
        view.setText(text);
    }

}
