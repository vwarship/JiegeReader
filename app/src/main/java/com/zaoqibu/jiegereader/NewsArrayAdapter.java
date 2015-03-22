package com.zaoqibu.jiegereader;

import android.content.ContentValues;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.zaoqibu.jiegereader.db.News;
import com.zaoqibu.jiegereader.db.Reader;
import com.zaoqibu.jiegereader.db.ReaderProvider;
import com.zaoqibu.jiegereader.util.DateUtil;
import com.zaoqibu.jiegereader.util.ViewHolder;

import java.util.List;

/**
 * Created by vwarship on 2015/3/3.
 */
public class NewsArrayAdapter extends ArrayAdapter<News> {
    private int resource;
    private DateUtil dateUtil;
    private ReaderProvider readerProvider;

    public NewsArrayAdapter(Context context, int resource, List<News> newsList) {
        super(context, resource, newsList);
        this.resource = resource;

        dateUtil = new DateUtil();
        readerProvider = new ReaderProvider(context);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(resource, parent, false);
        }

        final News news = getItem(position);

        final ViewHolder viewHolder = ViewHolder.get(convertView);
        viewHolder.setText(R.id.tvNewsTitle, news.getTitle());
        viewHolder.setText(R.id.tvSource, news.getSource());
        viewHolder.setText(R.id.tvNewsDate, dateUtil.timeToReadable(news.getPubDate()) );

        TextView tvNewsTitle = viewHolder.getView(R.id.tvNewsTitle);
        if (news.getState() == Reader.Newses.StateValue.Readed.getValue()) {
            tvNewsTitle.setTextColor(getContext().getResources().getColor(android.R.color.darker_gray));
        } else {
            tvNewsTitle.setTextColor(getContext().getResources().getColor(android.R.color.black));
        }

        ImageView ivDeleteNews = viewHolder.getView(R.id.ivDeleteNews);
        ivDeleteNews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(Reader.Newses.COLUMN_NAME_STATE, Reader.Newses.StateValue.Deleted.getValue());

                int count = readerProvider.updateNews(contentValues,
                        String.format("%s=?", Reader.Newses._ID),
                        new String[]{String.valueOf(news.getId())});

                if (count > 0) {
                    NewsArrayAdapter.this.remove(NewsArrayAdapter.this.getItem(position));
                    NewsArrayAdapter.this.notifyDataSetChanged();
                }
            }
        });

        return convertView;
    }

}
