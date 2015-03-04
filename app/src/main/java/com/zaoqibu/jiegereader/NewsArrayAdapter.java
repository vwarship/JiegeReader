package com.zaoqibu.jiegereader;

import android.content.Context;
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
    private LayoutInflater inflater;

    public NewsArrayAdapter(Context context, int resource) {
        super(context, resource);
        this.resource = resource;

        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

//        if (convertView == null) {
//            LayoutInflater inflater = LayoutInflater.from(getContext());
//            view = inflater.inflate(resource, parent, false);
//        }
//
//        Item news = getItem(position);
//
//        TextView newsTitle = (TextView)view.findViewById(R.id.tvNewsTitle);
//        newsTitle.setText(news.getTitle());
//
//        TextView newsDate = (TextView)view.findViewById(R.id.tvNewsDate);
//        newsDate.setText(news.getPubDate());


        //View Holder
        class ViewHolder {
            TextView title;
            TextView date;
        }
        final ViewHolder viewHolder;

        if (convertView == null) {
            view = inflater.inflate(resource, null);

            viewHolder = new ViewHolder();
            viewHolder.title = (TextView)view.findViewById(R.id.tvNewsTitle);
            viewHolder.date = (TextView)view.findViewById(R.id.tvNewsDate);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)view.getTag();
        }

        Item news = getItem(position);

        viewHolder.title.setText(news.getTitle());
        viewHolder.date.setText(news.getPubDate());

        return view;
    }
}
