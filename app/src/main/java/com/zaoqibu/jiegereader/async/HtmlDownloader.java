package com.zaoqibu.jiegereader.async;

import android.os.AsyncTask;

import com.zaoqibu.jiegereader.db.RssFeed;
import com.zaoqibu.jiegereader.util.DateUtil;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.UnicodeDetector;

/**
 * Created by vwarship on 2015/3/3.
 */
public class HtmlDownloader extends AsyncTask<Void, String, Void> {
    private static String TAG = "HtmlDownloader";

    public static interface HtmlDownloaderListener {
        public abstract void onDownloaded(int rssFeedId, String html);
    }

    private HtmlDownloaderListener listener;
    private List<RssFeed> rssFeeds;

    public HtmlDownloader(List<RssFeed> rssFeeds, HtmlDownloaderListener listener) {
        this.rssFeeds = rssFeeds;
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(Void... params) {
        for (RssFeed rssFeed : rssFeeds) {
            String html = downloadHtml(rssFeed);
            publishProgress(String.valueOf(rssFeed.getId()), html);
        }

        return null;
    }

    private String downloadHtml(RssFeed rssFeed) {
        String html = "";

        String url = rssFeed.getLink();
        HttpGet request = new HttpGet(url);
        HttpClient client = new DefaultHttpClient();

        try {
            HttpResponse response = client.execute(request);
//            long getLastModifiedTime = getLastModifiedTime(response);

            final int responseCode = response.getStatusLine().getStatusCode();
            if(responseCode == HttpStatus.SC_OK) {
                InputStream in = response.getEntity().getContent();

                String charsetName = getCharsetName(url);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, charsetName));

                StringBuilder str = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    str.append(line);
                }
                in.close();

                html = str.toString();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return html;
    }

    private long getLastModifiedTime(HttpResponse response) {
        long lastModified = 0;

        Header header = response.getFirstHeader("Date");
        if (header != null) {
            String lastModifiedStr = header.getValue();

            DateUtil dateUtil = new DateUtil();
            lastModified = dateUtil.dateParse(lastModifiedStr);
        }

        return lastModified;
    }

    private String getCharsetName(String url) {
        CodepageDetectorProxy codepageDetectorProxy = CodepageDetectorProxy.getInstance();

        codepageDetectorProxy.add(JChardetFacade.getInstance());
        codepageDetectorProxy.add(ASCIIDetector.getInstance());
        codepageDetectorProxy.add(UnicodeDetector.getInstance());

        Charset charset = Charset.forName("iso-8859-1");
        try {
            charset = codepageDetectorProxy.detectCodepage(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return charset.name();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        if (!isCancelled()) {
            int rssFeedId = Integer.parseInt(values[0]);
            String html = values[1];
            listener.onDownloaded(rssFeedId, html);
        }
    }

}
