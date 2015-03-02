package com.zaoqibu.jiegereader;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.zaoqibu.jiegereader.rss.Item;

/**
 * Created by vwarship on 2015/3/3.
 */
public class NewsArrayAdapter extends ArrayAdapter<Item> {
    private int resource;

    public NewsArrayAdapter(Context context, int resource) {
        super(context, resource);
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(resource, parent, false);
        }

        Item news = getItem(position);

        TextView newsTitle = (TextView)view.findViewById(R.id.tvNewsTitle);
        newsTitle.setText(news.getTitle());

        TextView tvDescription = (TextView)view.findViewById(R.id.tvDescription);
        tvDescription.setText(news.getDescription());

        TextView newsDate = (TextView)view.findViewById(R.id.tvNewsDate);
        newsDate.setText(news.getPubDate());

        return view;
    }
}
