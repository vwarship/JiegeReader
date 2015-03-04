package com.zaoqibu.jiegereader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.zaoqibu.jiegereader.rss.Item;
import com.zaoqibu.jiegereader.util.ViewHolder;

/**
 * Created by vwarship on 2015/3/3.
 */
public class NewsArrayAdapter extends ArrayAdapter<Item> {
    private int resource;
    private LayoutInflater inflater;

    public NewsArrayAdapter(Context context, int resource) {
        super(context, resource);
        this.resource = resource;

        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(resource, null);
        }

        Item news = getItem(position);

        final ViewHolder viewHolder = ViewHolder.get(convertView);
        viewHolder.setText(R.id.tvNewsTitle, news.getTitle());
        viewHolder.setText(R.id.tvNewsDate, news.getPubDate());

        return convertView;
    }

}
