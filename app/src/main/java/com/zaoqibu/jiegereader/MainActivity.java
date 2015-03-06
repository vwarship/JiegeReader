package com.zaoqibu.jiegereader;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.zaoqibu.jiegereader.db.Reader;
import com.zaoqibu.jiegereader.db.ReaderProvider;
import com.zaoqibu.jiegereader.rss.Item;
import com.zaoqibu.jiegereader.rss.Rss;
import com.zaoqibu.jiegereader.rss.RssParser;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements HTMLDownloader.HTMLDownloaderListener, LoaderManager.LoaderCallbacks<Cursor> {
    private List<Item> newsList;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView lvNewses = (ListView)findViewById(R.id.lvNewses);

        newsList = new ArrayList<Item>();
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
//        getLoaderManager().initLoader(0, null, this);

        final String url = "http://www.36kr.com/feed";
//        final String url = "http://www.zhihu.com/rss";
//        final String url = "http://tech.qq.com/web/webnews/rss_11.xml";
        new HTMLDownloader(url, this).execute();
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
            Log.i("TEST", item.getLink());
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

        int titleColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_TITLE);
        int linkColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_LINK);
        int descriptionColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_DESCRIPTION);
        int pubDateColumnIndex = cursor.getColumnIndex(Reader.Newses.COLUMN_NAME_PUB_DATE);

        while (cursor.moveToNext()) {
            Item item = new Item();
            item.setTitle(cursor.getString(titleColumnIndex));
            item.setLink(cursor.getString(linkColumnIndex));
            item.setDescription(cursor.getString(descriptionColumnIndex));
            item.setPubDate(cursor.getLong(pubDateColumnIndex));

            Log.i("TEST", String.format("%d, %s", item.getPubDate(), item.getTitle()));
            newsList.add(item);
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
