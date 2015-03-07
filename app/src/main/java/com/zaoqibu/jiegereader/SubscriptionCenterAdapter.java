package com.zaoqibu.jiegereader;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ToggleButton;

import com.zaoqibu.jiegereader.db.Reader;
import com.zaoqibu.jiegereader.util.ViewHolder;

/**
 * Created by vwarship on 2015/3/7.
 */
public class SubscriptionCenterAdapter extends CursorAdapter {
    public SubscriptionCenterAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.subscription_list_item, parent, false);

        final ViewHolder viewHolder = ViewHolder.get(view);
        bindData(viewHolder, cursor);

        return view;
    }

    private void bindData(ViewHolder viewHolder, Cursor cursor) {
        viewHolder.setText(R.id.tvSubscriptionName, cursor.getString(cursor.getColumnIndex(Reader.Rsses.COLUMN_NAME_TITLE)));
        ToggleButton toggleButton = (ToggleButton)viewHolder.getView(R.id.btnSubscription);
        toggleButton.setChecked(cursor.getInt(cursor.getColumnIndex(Reader.Rsses.COLUMN_NAME_IS_FEED)) > 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        bindData(viewHolder, cursor);
    }
}
