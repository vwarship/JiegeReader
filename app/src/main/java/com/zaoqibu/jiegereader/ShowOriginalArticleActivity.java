package com.zaoqibu.jiegereader;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.zaoqibu.jiegereader.util.Share;


public class ShowOriginalArticleActivity extends ActionBarActivity {
    public static final String EXTRA_TITLE = "TITLE";
    public static final String EXTRA_URL = "URL";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_original_article);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String title = getIntent().getExtras().getString(EXTRA_TITLE);
        setTitle(title);

        webView = (WebView)findViewById(R.id.webView);

        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        //Android 19 has Chromium engine for WebView. I guess it works better with hardware acceleration.
        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(true);

        webSettings.setAppCacheEnabled(false);

        // 网页根据你手机的屏幕自适应。
        webSettings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        String url = getIntent().getExtras().getString(EXTRA_URL);
        webView.loadUrl(url);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK) {
            webView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_original_article, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_share) {
            showShare();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showShare(){
        String title = getIntent().getExtras().getString(EXTRA_TITLE);
        String url = getIntent().getExtras().getString(EXTRA_URL);
        Share.share(this, title, url);
    }
}
