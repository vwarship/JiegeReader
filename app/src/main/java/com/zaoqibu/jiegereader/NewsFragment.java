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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.umeng.analytics.MobclickAgent;
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


public class NewsFragment extends Fragment implements HtmlDownloader.HtmlDownloaderListener,
        SwipeRefreshLayout.OnRefreshListener
{
    private static final String TAG = "NewsFragment";
    private static final long ONE_SECOND = 1000;
    private static final long TWO_SECOND = 2000;
    private static final long ONE_MINUTE = ONE_SECOND * 60;
    private static final long ONE_HOUR = ONE_MINUTE * 60;
    private static final long ONE_DAY = ONE_HOUR * 24;
    private static final long THREE_DAY = ONE_DAY * 3;
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
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initSwipeRefreshLayout();

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

                Intent intent = new Intent(NewsFragment.this.getActivity(), ShowOriginalArticleActivity.class);
                intent.putExtra(ShowOriginalArticleActivity.EXTRA_TITLE, news.getTitle());
                intent.putExtra(ShowOriginalArticleActivity.EXTRA_URL, news.getLink());
                startActivity(intent);

                updateNewsStateWithReaded(news);

                MobclickAgent.onEvent(NewsFragment.this.getActivity(), "read_news");
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

        htmlDownloader = new HtmlDownloader(NewsFragment.this);

        readNewsesFirst();
        readRssFeedsAsyncTask();

        executeRssDownloadTaskWithOneHourSchedule();
        autoExecuteRssDownloadTask();

        autoDeleteNewsWithOld();
    }

    private void executeRssDownloadTaskWithOneHourSchedule() {
        rssDownloadTimer = new Timer();
        rssDownloadTimer.schedule(createRssDownloadTimerTask(), RSS_DOWNLOAD_DELAY, RSS_DOWNLOAD_PERIOD);
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
    public void onDownloaded(final RssFeed rssFeed, final String html) {
        new AsyncTask<String, ContentValues, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                final String html = params[0];
                if (html == null || html.isEmpty())
                    return null;

                RssParser rssParser = new RssParser();
                Rss rss = rssParser.parse(html);

                for (Item item : rss.getChannel().getItems()) {
                    if (isNewsExist(item.getLink()))
                        continue;

                    ContentValues values = new ContentValues();
                    values.put(Reader.Newses.COLUMN_NAME_TITLE, item.getTitle());
                    values.put(Reader.Newses.COLUMN_NAME_LINK, item.getLink());
                    values.put(Reader.Newses.COLUMN_NAME_SOURCE, rssFeed.getTitle());
                    values.put(Reader.Newses.COLUMN_NAME_DESCRIPTION, item.getDescription());
                    values.put(Reader.Newses.COLUMN_NAME_PUB_DATE, item.getPubDate());
                    values.put(Reader.Newses.COLUMN_NAME_RSS_ID, rssFeed.getId());

                    publishProgress(values);
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(ContentValues... values) {
                readerProvider.insertNews(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                readNewsesFirst();
            }
        }.execute(html);
    }

    private boolean isNewsExist(String link) {
        Cursor cursor = readerProvider.queryNews(new String[]{Reader.Newses._ID},
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
                    selection = String.format(" %s IN (?, ?) AND %s=? ",
                            Reader.Newses.COLUMN_NAME_STATE, Reader.Newses.COLUMN_NAME_RSS_ID);
                    selectionArgs = new String[]{
                            String.valueOf(Reader.Newses.StateValue.Unread.getValue()),
                            String.valueOf(Reader.Newses.StateValue.Readed.getValue()),
                            String.valueOf(rssFeedId)};
                } else {
                    selection = String.format("%s IN (?, ?)",
                            Reader.Newses.COLUMN_NAME_STATE);
                    selectionArgs = new String[]{
                            String.valueOf(Reader.Newses.StateValue.Unread.getValue()),
                            String.valueOf(Reader.Newses.StateValue.Readed.getValue())};
                }
                Cursor cursor = readerProvider.queryNews(PROJECTION,
                        selection, selectionArgs,
                        Reader.Newses.COLUMN_NAME_PUB_DATE + " desc",
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
                    NewsFragment.this.newsList.clear();

                for (News news : newses)
                    NewsFragment.this.newsList.add(news);

                newsArrayAdapter.notifyDataSetChanged();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void updateRssFeeds() {
        readRssFeedsAsyncTask();
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
                NewsFragment.this.rssFeeds = rssFeeds;
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
                        htmlDownloader = new HtmlDownloader(NewsFragment.this);
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

    private void initSwipeRefreshLayout() {
        swipeRefreshLayout = (SwipeRefreshLayout)getView().findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    @Override
    public void onRefresh() {
        if (htmlDownloader.getStatus() == AsyncTask.Status.RUNNING) {
            swipeRefreshLayout.setRefreshing(false);
        } else {
            nowExecuteRssDownloadTask();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 5000);
        }
    }

    private void nowExecuteRssDownloadTask() {
        // 立即执行一次任务，这个时间最少是1000。
        new Timer().schedule(createRssDownloadTimerTask(), TWO_SECOND);
    }

    private void autoExecuteRssDownloadTask() {
        swipeRefreshLayout.setProgressViewOffset(false, 0, getResources().getDisplayMetrics().heightPixels/3);
        swipeRefreshLayout.setRefreshing(true);
        onRefresh();
    }

    private void autoDeleteNewsWithOld() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        readerProvider.deleteNews(String.format("%s<?", Reader.Newses.COLUMN_NAME_PUB_DATE),
                                new String[]{String.valueOf(System.currentTimeMillis() - THREE_DAY)});
                        return null;
                    }
                }.execute();
            }
        }, ONE_MINUTE);
    }

    public void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(TAG);
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(TAG);
    }
}
