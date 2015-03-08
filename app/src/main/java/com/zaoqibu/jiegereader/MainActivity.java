package com.zaoqibu.jiegereader;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AbsListView;
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


public class MainActivity extends ActionBarActivity implements HtmlDownloader.HtmlDownloaderListener {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView lvNewses = (ListView)findViewById(R.id.lvNewses);

        newsList = new ArrayList<News>();
        newsArrayAdapter = new NewsArrayAdapter(this, R.layout.news_list_item, newsList);
        lvNewses.setAdapter(newsArrayAdapter);
        lvNewses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = newsArrayAdapter.getItem(position).getLink();

                Intent intent = new Intent(MainActivity.this, ShowOriginalArticleActivity.class);
                intent.putExtra(ShowOriginalArticleActivity.EXTRA_URL, url);
                startActivity(intent);
            }
        });

        readerProvider = new ReaderProvider(this);

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

    @Override
    public void onDownloaded(String html) {
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

                Cursor cursor = readerProvider.query(PROJECTION, null, null, "pub_date desc", String.format("%d, %d", limit, offset));

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
                List<String> rssLinks = new ArrayList<>();
                for (RssFeed rssFeed : rssFeeds) {
                    rssLinks.add(rssFeed.getLink());
                    Log.i(TAG, rssFeed.getLink());
                }

                new HtmlDownloader(rssLinks, MainActivity.this).execute();
            }
        };

        return timerTask;
    }

    @Override
    protected void onDestroy() {
        rssDownloadTimerTask.cancel();
        rssDownloadTimerTask = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_subscription_center) {
            Intent intent = new Intent(this, SubscriptionCenterActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
