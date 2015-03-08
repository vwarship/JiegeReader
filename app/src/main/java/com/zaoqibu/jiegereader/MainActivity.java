package com.zaoqibu.jiegereader;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.zaoqibu.jiegereader.async.HtmlDownloader;
import com.zaoqibu.jiegereader.db.News;
import com.zaoqibu.jiegereader.db.Reader;
import com.zaoqibu.jiegereader.db.ReaderProvider;
import com.zaoqibu.jiegereader.db.RssFeed;
import com.zaoqibu.jiegereader.rss.Item;
import com.zaoqibu.jiegereader.rss.Rss;
import com.zaoqibu.jiegereader.rss.RssParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Fragment implements HtmlDownloader.HtmlDownloaderListener {
    private static String TAG = "MainActivity";
    private static final long ONE_SECOND = 1000;
    private static final long TWO_SECOND = 2000;
    private static final long ONE_HOUR = ONE_SECOND * 60 * 60;
    private static final long RSS_DOWNLOAD_DELAY = TWO_SECOND;
    private static final long RSS_DOWNLOAD_PERIOD = ONE_HOUR;

    private List<News> newsList;
    private NewsArrayAdapter newsArrayAdapter;
    private ReaderProvider readerProvider;
    private TimerTask rssDownloadTimerTask;

    List<RssFeed> rssFeeds = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView lvNewses = (ListView)getView().findViewById(R.id.lvNewses);

        newsList = new ArrayList<News>();
        newsArrayAdapter = new NewsArrayAdapter(this.getActivity(), R.layout.news_list_item, newsList);
        lvNewses.setAdapter(newsArrayAdapter);
        lvNewses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                News news = newsArrayAdapter.getItem(position);

                Intent intent = new Intent(MainActivity.this.getActivity(), ShowOriginalArticleActivity.class);
                intent.putExtra(ShowOriginalArticleActivity.EXTRA_TITLE, news.getTitle());
                intent.putExtra(ShowOriginalArticleActivity.EXTRA_URL, news.getLink());
                startActivity(intent);
            }
        });

        readerProvider = new ReaderProvider(this.getActivity());

        readNewsesFirst();
        readRssFeedsAsyncTask();
        rssDownloadTimerTask = createRssDownloadTimerTask();

        new Timer().schedule(rssDownloadTimerTask, RSS_DOWNLOAD_DELAY, RSS_DOWNLOAD_PERIOD);
    }

    private void readNewsesFirst() {
        final int limit = 0;
        final int offset = 20;
        readNewsesAsyncTask(limit, offset);
    }

    private int rssFeedId = 0;
    public void readNewses(int rssFeedId) {
        this.rssFeedId = rssFeedId;
        readNewsesFirst();
    }

    @Override
    public void onDownloaded(final int rssFeedId, String html) {
        new AsyncTask<String, ContentValues, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                final String html = params[0];

                RssParser rssParser = new RssParser();
                Rss rss = rssParser.parse(html);

                final String channelTitle = rss.getChannel().getTitle();
                for (Item item : rss.getChannel().getItems()) {
                    if (isNewsExist(item.getLink()))
                        continue;

                    ContentValues values = new ContentValues();
                    values.put(Reader.Newses.COLUMN_NAME_TITLE, item.getTitle());
                    values.put(Reader.Newses.COLUMN_NAME_LINK, item.getLink());
                    values.put(Reader.Newses.COLUMN_NAME_SOURCE, channelTitle);
                    values.put(Reader.Newses.COLUMN_NAME_DESCRIPTION, item.getDescription());
                    values.put(Reader.Newses.COLUMN_NAME_PUB_DATE, item.getPubDate());
                    values.put(Reader.Newses.COLUMN_NAME_RSS_ID, rssFeedId);

                    publishProgress(values);
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(ContentValues... values) {
                readerProvider.insert(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                readNewsesFirst();
            }
        }.execute(html);
    }

    private boolean isNewsExist(String link) {
        Cursor cursor = readerProvider.query(new String[]{Reader.Newses._ID},
                String.format("%s=?", Reader.Newses.COLUMN_NAME_LINK),
                new String[]{link}, null, null);

        boolean exist = cursor.getCount() > 0;
        cursor.close();

        return exist;
    }

    private void readNewsesAsyncTask(final int limit, final int offset) {
        new AsyncTask<Void, Void, List<News>>() {
            final String[] PROJECTION =
                    new String[] {
                            Reader.Newses._ID,
                            Reader.Newses.COLUMN_NAME_TITLE,
                            Reader.Newses.COLUMN_NAME_LINK,
                            Reader.Newses.COLUMN_NAME_SOURCE,
                            Reader.Newses.COLUMN_NAME_DESCRIPTION,
                            Reader.Newses.COLUMN_NAME_PUB_DATE
                    };

            @Override
            protected List<News> doInBackground(Void... params) {
                List<News> newses = new ArrayList<>();

                // TODO: 暂时显示所有文章
//                Cursor cursor = readerProvider.query(PROJECTION, null, null, "pub_date desc", String.format("%d, %d", limit, offset));
                String selection = null;
                String[] selectionArgs = null;
                if (rssFeedId > 0) {
                    selection = String.format("%s=?", Reader.Newses.COLUMN_NAME_RSS_ID);
                    selectionArgs = new String[]{String.valueOf(rssFeedId)};
                }
                Cursor cursor = readerProvider.query(PROJECTION, selection, selectionArgs, "pub_date desc", null);

                int idColumnIndex = cursor.getColumnIndex(Reader.Newses._ID);
                int titleColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_TITLE);
                int linkColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_LINK);
                int sourceColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_SOURCE);
                int descriptionColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_DESCRIPTION);
                int pubDateColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_PUB_DATE);

                while (cursor.moveToNext()) {
                    News news = new News();
                    news.setId(cursor.getInt(idColumnIndex));
                    news.setTitle(cursor.getString(titleColumnIndex));
                    news.setLink(cursor.getString(linkColumnIndex));
                    news.setSource(cursor.getString(sourceColumnIndex));
                    news.setDescription(cursor.getString(descriptionColumnIndex));
                    news.setPubDate(cursor.getLong(pubDateColumnIndex));

                    newses.add(news);
                }
                cursor.close();

                return newses;
            }

            @Override
            protected void onPostExecute(List<News> newses) {
                if (limit == 0)
                    MainActivity.this.newsList.clear();

                for (News news : newses)
                    MainActivity.this.newsList.add(news);

                newsArrayAdapter.notifyDataSetChanged();
            }
        }.execute();
    }

    private void readRssFeedsAsyncTask() {
        new AsyncTask<Void, Void, List<RssFeed>>() {
            private final String[] PROJECTION =
                    new String[] {
                            Reader.Rsses._ID,
                            Reader.Rsses.COLUMN_NAME_TITLE,
                            Reader.Rsses.COLUMN_NAME_LINK,
                            Reader.Rsses.COLUMN_NAME_IS_FEED,
                            Reader.Rsses.COLUMN_NAME_CREATE_DATE
                    };

            @Override
            protected List<RssFeed> doInBackground(Void... params) {
                List<RssFeed> rssFeeds = new ArrayList<>();

                Cursor cursor = readerProvider.queryRsses(PROJECTION,
                        String.format("%s=?", Reader.Rsses.COLUMN_NAME_IS_FEED),
                        new String[]{"1"}, null);

                int idColumnIndex = cursor.getColumnIndex(Reader.Rsses._ID);
                int titleColumnIndex = cursor.getColumnIndex(Reader.Rsses.COLUMN_NAME_TITLE);
                int linkColumnIndex = cursor.getColumnIndex(Reader.Rsses.COLUMN_NAME_LINK);
                int isFeedColumnIndex = cursor.getColumnIndex(Reader.Rsses.COLUMN_NAME_IS_FEED);
                int createDateColumnIndex = cursor.getColumnIndex(Reader.Rsses.COLUMN_NAME_CREATE_DATE);

                while (cursor.moveToNext()) {
                    RssFeed rssFeed = new RssFeed();
                    rssFeed.setId(cursor.getInt(idColumnIndex));
                    rssFeed.setTitle(cursor.getString(titleColumnIndex));
                    rssFeed.setLink(cursor.getString(linkColumnIndex));
                    rssFeed.setFeed(cursor.getInt(isFeedColumnIndex) > 0);
                    rssFeed.setCreateDate(cursor.getLong(createDateColumnIndex));

                    rssFeeds.add(rssFeed);
                }

                cursor.close();

                return rssFeeds;
            }

            @Override
            protected void onPostExecute(List<RssFeed> rssFeeds) {
                MainActivity.this.rssFeeds = rssFeeds;
            }
        }.execute();
    }

    private TimerTask createRssDownloadTimerTask() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                new HtmlDownloader(rssFeeds, MainActivity.this).execute();
            }
        };

        return timerTask;
    }

    @Override
    public void onDestroy() {
        rssDownloadTimerTask.cancel();
        rssDownloadTimerTask = null;
        super.onDestroy();
    }
}
