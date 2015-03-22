package com.zaoqibu.jiegereader;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.zaoqibu.jiegereader.async.HtmlDownloader;
import com.zaoqibu.jiegereader.db.News;
import com.zaoqibu.jiegereader.db.Reader;
import com.zaoqibu.jiegereader.db.ReaderProvider;
import com.zaoqibu.jiegereader.db.RssFeed;
import com.zaoqibu.jiegereader.rss.Item;
import com.zaoqibu.jiegereader.rss.Rss;
import com.zaoqibu.jiegereader.rss.RssParser;
import com.zaoqibu.jiegereader.util.VibratorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Fragment implements HtmlDownloader.HtmlDownloaderListener,
        SwipeRefreshLayout.OnRefreshListener
{
    private static String TAG = "MainActivity";
    private static final long ONE_SECOND = 1000;
    private static final long TWO_SECOND = 2000;
    private static final long ONE_HOUR = ONE_SECOND * 60 * 60;
    private static final long RSS_DOWNLOAD_DELAY = ONE_HOUR;
    private static final long RSS_DOWNLOAD_PERIOD = ONE_HOUR;

    private static final int SHOW_NEWS_COUNT = 20;

    private List<News> newsList;
    private NewsArrayAdapter newsArrayAdapter;
    private ReaderProvider readerProvider;
    private Timer rssDownloadTimer;

    private List<RssFeed> rssFeeds = new ArrayList<>();

    private HtmlDownloader htmlDownloader;

    private SwipeRefreshLayout swipeRefreshLayout;

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
        swipeRefreshLayout = (SwipeRefreshLayout)getView().findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright);

        final ListView lvNewses = (ListView)getView().findViewById(R.id.lvNewses);
        final ImageButton ibTop = (ImageButton)getView().findViewById(R.id.ibTop);
        ibTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VibratorUtil.vibrate(getActivity());
                lvNewses.setSelectionFromTop(0, 0);
            }
        });

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

                updateNewsStateWithReaded(news);
            }
        });

        lvNewses.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    if (view.getLastVisiblePosition() >= view.getCount() - 1) {
                        readNewsesAsyncTask(newsList.size()-1, SHOW_NEWS_COUNT);
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // 避免出现闪烁
                if (firstVisibleItem == visibleItemCount)
                    return;

                if (firstVisibleItem > visibleItemCount)
                    ibTop.setVisibility(View.VISIBLE);
                else
                    ibTop.setVisibility(View.INVISIBLE);
            }
        });

        readerProvider = new ReaderProvider(this.getActivity());

        htmlDownloader = new HtmlDownloader(MainActivity.this);

        readNewsesFirst();
        readRssFeedsAsyncTask();

        executeRssDownloadTaskWithOneHour();
        executeRssDownloadTaskWithNow();
    }

    private void executeRssDownloadTaskWithOneHour() {
        rssDownloadTimer = new Timer();
        rssDownloadTimer.schedule(createRssDownloadTimerTask(), RSS_DOWNLOAD_DELAY, RSS_DOWNLOAD_PERIOD);
    }

    private void executeRssDownloadTaskWithNow() {
        // 立即执行一次任务，这个时间最少是1000。
        new Timer().schedule(createRssDownloadTimerTask(), ONE_SECOND);
    }

    private void readNewsesFirst() {
        final int limit = 0;
        final int offset = SHOW_NEWS_COUNT;
        readNewsesAsyncTask(limit, offset);
    }

    private int rssFeedId = 0;
    public void readNewses(int rssFeedId) {
        this.rssFeedId = rssFeedId;
        readNewsesFirst();
    }

    private void updateNewsStateWithReaded(News news) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Reader.Newses.COLUMN_NAME_STATE, Reader.Newses.StateValue.Readed.getValue());

        int count = readerProvider.updateNews(contentValues,
                String.format("%s=?", Reader.Newses._ID),
                new String[]{String.valueOf(news.getId())});

        if (count > 0) {
            news.setState(Reader.Newses.StateValue.Readed.getValue());
            newsArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDownloaded(final int rssFeedId, String html) {
        new AsyncTask<String, ContentValues, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                final String html = params[0];
                if (html == null || html.isEmpty())
                    return null;

                RssParser rssParser = new RssParser();
                Rss rss = rssParser.parse(html);

                final String channelTitle = rss.getChannel().getTitle();
                Log.i(TAG, String.format("Channel Title: %s", channelTitle==null ? "null" : channelTitle));
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
                            Reader.Newses.COLUMN_NAME_STATE,
                            Reader.Newses.COLUMN_NAME_PUB_DATE
                    };

            @Override
            protected List<News> doInBackground(Void... params) {
                List<News> newses = new ArrayList<>();

                String selection = null;
                String[] selectionArgs = null;
                if (rssFeedId > 0) {
                    selection = String.format("%s=? AND %s NOT IN (?)",
                            Reader.Newses.COLUMN_NAME_RSS_ID, Reader.Newses.COLUMN_NAME_STATE);
                    selectionArgs = new String[]{String.valueOf(rssFeedId), String.valueOf(Reader.Newses.StateValue.Deleted.getValue())};
                } else {
                    selection = String.format("%s NOT IN (?)",
                            Reader.Newses.COLUMN_NAME_STATE);
                    selectionArgs = new String[]{String.valueOf(Reader.Newses.StateValue.Deleted.getValue())};
                }
                Cursor cursor = readerProvider.query(PROJECTION,
                        selection, selectionArgs,
                        "pub_date desc",
                        String.format("%d, %d", limit, offset));

                int idColumnIndex = cursor.getColumnIndex(Reader.Newses._ID);
                int titleColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_TITLE);
                int linkColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_LINK);
                int sourceColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_SOURCE);
                int stateColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_STATE);
                int pubDateColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_PUB_DATE);

                while (cursor.moveToNext()) {
                    News news = new News();
                    news.setId(cursor.getInt(idColumnIndex));
                    news.setTitle(cursor.getString(titleColumnIndex));
                    news.setLink(cursor.getString(linkColumnIndex));
                    news.setSource(cursor.getString(sourceColumnIndex));
                    news.setState(cursor.getInt(stateColumnIndex));
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
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                ConnectivityManager con=(ConnectivityManager)getActivity().getSystemService(Activity.CONNECTIVITY_SERVICE);
                boolean wifi=con.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
                boolean internet=con.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();

                if (wifi || internet) {
                    if (htmlDownloader.getStatus() != AsyncTask.Status.RUNNING) {
                        htmlDownloader = new HtmlDownloader(MainActivity.this);
                        htmlDownloader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, rssFeeds);
                    }
                }
            }
        };

        return timerTask;
    }

    @Override
    public void onDestroy() {
        rssDownloadTimer.cancel();
        rssDownloadTimer = null;
        super.onDestroy();
    }

    @Override
    public void onRefresh() {
        if (htmlDownloader.getStatus() == AsyncTask.Status.RUNNING) {
            swipeRefreshLayout.setRefreshing(false);
        } else {
            executeRssDownloadTaskWithNow();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 5000);
        }
    }
}
