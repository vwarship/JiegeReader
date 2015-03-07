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

    private List<News> newsList;
    private NewsArrayAdapter newsArrayAdapter;
    private ReaderProvider readerProvider;

    private static final String[] PROJECTION =
            new String[] {
                    Reader.Newses._ID,
                    Reader.Newses.COLUMN_NAME_TITLE,
                    Reader.Newses.COLUMN_NAME_LINK,
                    Reader.Newses.COLUMN_NAME_DESCRIPTION,
                    Reader.Newses.COLUMN_NAME_PUB_DATE
            };

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
        readNewses();

        new AsyncTask<Void, Void, List<RssFeed>>() {
            private /*static*/ final String[] PROJECTION =
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

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                List<String> rssLinks = new ArrayList<>();
                for (RssFeed rssFeed : rssFeeds) {
                    Log.i(TAG, "timerTask().........." + rssFeed.getLink());
                    rssLinks.add(rssFeed.getLink());
                }

                new HtmlDownloader(rssLinks, MainActivity.this).execute();
            }
        };
        new Timer().schedule(timerTask, 3000, 1000*60*10);
    }

    @Override
    public void onDownloaded(String html) {
        RssParser rssParser = new RssParser();
        Rss rss = rssParser.parse(html);

        Log.i("TEST", "channel item count: "+rss.getChannel().getItems().size());

//        for (Item item : rss.getChannel().getItems()) {
//            newsArrayAdapter.add(item);
//        }
//        newsArrayAdapter.notifyDataSetChanged();

        for (Item item : rss.getChannel().getItems()) {
            if (isNewsExist(item.getLink()))
                continue;

            ContentValues values = new ContentValues();
            values.put(Reader.Newses.COLUMN_NAME_TITLE, item.getTitle());
            values.put(Reader.Newses.COLUMN_NAME_LINK, item.getLink());
            values.put(Reader.Newses.COLUMN_NAME_DESCRIPTION, item.getDescription());
            values.put(Reader.Newses.COLUMN_NAME_PUB_DATE, item.getPubDate());

            readerProvider.insert(values);
        }

        readNewses();
    }

    private boolean isNewsExist(String link) {
        Cursor cursor = readerProvider.query(new String[]{Reader.Newses._ID},
                String.format("%s=?", Reader.Newses.COLUMN_NAME_LINK),
                new String[]{link}, null);

        boolean exist = cursor.getCount() > 0;
        cursor.close();

        return exist;
    }

    private void readNewses() {
        newsList.clear();

        Cursor cursor = readerProvider.query(PROJECTION, null, null, "pub_date desc");

        int idColumnIndex = cursor.getColumnIndex(Reader.Newses._ID);
        int titleColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_TITLE);
        int linkColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_LINK);
        int descriptionColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_DESCRIPTION);
        int pubDateColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_PUB_DATE);

        while (cursor.moveToNext()) {
            News news = new News();
            news.setId(cursor.getInt(idColumnIndex));
            news.setTitle(cursor.getString(titleColumnIndex));
            news.setLink(cursor.getString(linkColumnIndex));
            news.setDescription(cursor.getString(descriptionColumnIndex));
            news.setPubDate(cursor.getLong(pubDateColumnIndex));

            newsList.add(news);
        }
        cursor.close();

        newsArrayAdapter.notifyDataSetChanged();
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
