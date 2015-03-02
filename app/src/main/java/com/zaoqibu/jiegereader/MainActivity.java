package com.zaoqibu.jiegereader;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.zaoqibu.jiegereader.rss.Item;
import com.zaoqibu.jiegereader.rss.Rss;
import com.zaoqibu.jiegereader.rss.RssParser;


public class MainActivity extends ActionBarActivity implements HTMLDownloader.HTMLDownloaderListener {
    private NewsArrayAdapter newsArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView lvNewses = (ListView)findViewById(R.id.lvNewses);

        newsArrayAdapter = new NewsArrayAdapter(this, R.layout.news_list_item);
        lvNewses.setAdapter(newsArrayAdapter);
        lvNewses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
            }
        });

        final String url = "http://tech.qq.com/web/webnews/rss_11.xml";
        new HTMLDownloader(url, this).execute();
    }

    @Override
    public void onDownloaded(String html) {
        RssParser rssParser = new RssParser();
        Rss rss = rssParser.parse(html);

        Log.i("TEST", "channel item count: "+rss.getChannel().getItems().size());

        for (Item item : rss.getChannel().getItems()) {
            newsArrayAdapter.add(item);
        }
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
