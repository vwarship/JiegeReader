package com.zaoqibu.jiegereader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ToggleButton;

import com.zaoqibu.jiegereader.db.Reader;
import com.zaoqibu.jiegereader.db.ReaderProvider;
import com.zaoqibu.jiegereader.util.ViewHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vwarship on 2015/3/7.
 */
public class SubscriptionCenterAdapter extends CursorAdapter {
    private ReaderProvider readerProvider;
    //TODO: 暂时
    private Map<Integer, Boolean> cursorToIsFeedMap;

    public SubscriptionCenterAdapter(Context context, Cursor c, boolean autoRequery, ReaderProvider readerProvider) {
        super(context, c, autoRequery);
        this.readerProvider = readerProvider;
        cursorToIsFeedMap = new HashMap<>();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.subscription_list_item, parent, false);

        final ViewHolder viewHolder = ViewHolder.get(view);
        bindData(viewHolder, cursor);

        return view;
    }

    private void bindData(ViewHolder viewHolder, final Cursor cursor) {
        viewHolder.setText(R.id.tvSubscriptionName, cursor.getString(cursor.getColumnIndex(Reader.Rsses.COLUMN_NAME_TITLE)));

        final int id = cursor.getInt(cursor.getColumnIndex(Reader.Rsses._ID));
        final boolean isFeed = cursor.getInt(cursor.getColumnIndex(Reader.Rsses.COLUMN_NAME_IS_FEED)) > 0;

        if (!cursorToIsFeedMap.containsKey(id))
            cursorToIsFeedMap.put(id, isFeed);

        final ToggleButton toggleButton = (ToggleButton)viewHolder.getView(R.id.btnSubscription);
        toggleButton.setChecked(cursorToIsFeedMap.get(id));

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean curIsFeed = !cursorToIsFeedMap.get(id);
                cursorToIsFeedMap.put(id, curIsFeed);

                ContentValues values = new ContentValues();
                values.put(Reader.Rsses.COLUMN_NAME_IS_FEED, boolToInt(curIsFeed) );

                readerProvider.updateRsses(values,
                        String.format("%s=?", Reader.Newses._ID),
                        new String[]{String.valueOf(id)});
            }

            private int boolToInt(boolean b) {
                return b ? 1 : 0;
            }
        });
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        bindData(viewHolder, cursor);
    }
}
