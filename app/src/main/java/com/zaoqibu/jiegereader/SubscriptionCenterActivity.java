package com.zaoqibu.jiegereader;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;


public class SubscriptionCenterActivity extends ActionBarActivity {
    private SubscriptionArrayAdapter subscriptionArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_center);

        ListView lvSubscriptionList = (ListView)findViewById(R.id.lvSubscriptionList);

        subscriptionArrayAdapter = new SubscriptionArrayAdapter(this, R.layout.subscription_list_item);

        subscriptionArrayAdapter.add(new Subscription("知乎每日精选", "http://www.zhihu.com/rss"));
        subscriptionArrayAdapter.add(new Subscription("互联网_腾讯科技", "http://tech.qq.com/web/webnews/rss_11.xml"));
        subscriptionArrayAdapter.add(new Subscription("虎嗅网", "http://www.huxiu.com/rss/0.xml"));

        lvSubscriptionList.setAdapter(subscriptionArrayAdapter);
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
        }

        return super.onOptionsItemSelected(item);
    }
}
