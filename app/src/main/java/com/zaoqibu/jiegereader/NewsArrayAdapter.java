package com.zaoqibu.jiegereader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.zaoqibu.jiegereader.db.News;
import com.zaoqibu.jiegereader.util.DateUtil;
import com.zaoqibu.jiegereader.util.ViewHolder;

import java.util.List;

/**
 * Created by vwarship on 2015/3/3.
 */
public class NewsArrayAdapter extends ArrayAdapter<News> {
    private int resource;
    private DateUtil dateUtil;

    public NewsArrayAdapter(Context context, int resource, List<News> newsList) {
        super(context, resource, newsList);
        this.resource = resource;

        dateUtil = new DateUtil();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(resource, parent, false);
        }

        News news = getItem(position);

        final ViewHolder viewHolder = ViewHolder.get(convertView);
        viewHolder.setText(R.id.tvNewsTitle, news.getTitle());
        viewHolder.setText(R.id.tvSource, news.getSource());
        viewHolder.setText(R.id.tvNewsDate, dateUtil.timeToReadable(news.getPubDate()) );

        return convertView;
    }

}
