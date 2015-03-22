package com.zaoqibu.jiegereader;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.umeng.analytics.MobclickAgent;
import com.zaoqibu.jiegereader.db.Reader;
import com.zaoqibu.jiegereader.db.ReaderProvider;


public class SubscriptionCenterActivity extends ActionBarActivity {
    private static final String TAG = "SubscriptionCenterActivity";

    private ListView lvSubscriptionList;
    private ReaderProvider readerProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_center);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        lvSubscriptionList = (ListView) findViewById(R.id.lvSubscriptionList);

        readRssFeedsAsyncTask();
    }

    private void readRssFeedsAsyncTask() {
        new AsyncTask<Void, Void, Cursor>() {
            private final String[] PROJECTION =
                    new String[]{
                            Reader.Rsses._ID,
                            Reader.Rsses.COLUMN_NAME_TITLE,
                            Reader.Rsses.COLUMN_NAME_LINK,
                            Reader.Rsses.COLUMN_NAME_IS_FEED,
                            Reader.Rsses.COLUMN_NAME_CREATE_DATE
                    };

            @Override
            protected Cursor doInBackground(Void... params) {
                if (readerProvider == null)
                    readerProvider = new ReaderProvider(SubscriptionCenterActivity.this);

                Cursor cursor = readerProvider.queryRsses(PROJECTION, null, null, null);

                return cursor;
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                super.onPostExecute(cursor);

                SubscriptionCenterAdapter subscriptionCenterAdapter =
                        new SubscriptionCenterAdapter(SubscriptionCenterActivity.this, cursor, false, readerProvider);

                lvSubscriptionList.setAdapter(subscriptionCenterAdapter);
            }

            private long t;
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onDestroy() {
        lvSubscriptionList.setAdapter(null);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_subscription_center, menu);
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
        } else if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    public void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(TAG);
        MobclickAgent.onResume(this);
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(TAG);
        MobclickAgent.onPause(this);
    }
}
