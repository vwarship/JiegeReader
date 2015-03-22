package com.zaoqibu.jiegereader.async;

import android.os.AsyncTask;

import com.zaoqibu.jiegereader.db.RssFeed;

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
import java.util.concurrent.TimeUnit;

import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.UnicodeDetector;

/**
 * Created by vwarship on 2015/3/3.
 */
public class HtmlDownloader extends AsyncTask<List<RssFeed>, Object, Void> {
    private static String TAG = "HtmlDownloader";

    public static interface HtmlDownloaderListener {
        public abstract void onDownloaded(final RssFeed rssFeed, final String html);
    }

    private HtmlDownloaderListener listener;

    HttpGet request;
    HttpClient httpClient;


    public HtmlDownloader(HtmlDownloaderListener listener) {
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(List<RssFeed>... params) {
        List<RssFeed> rssFeeds = params[0];
        for (RssFeed rssFeed : rssFeeds) {
            String html = downloadHtml(rssFeed);
            publishProgress(rssFeed, html);
        }

        return null;
    }

    private String downloadHtml(RssFeed rssFeed) {
        String html = "";

        String url = rssFeed.getLink();
        request = new HttpGet(url);
        httpClient = new DefaultHttpClient();

        try {
            HttpResponse response = httpClient.execute(request);
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
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);

        if (!isCancelled()) {
            RssFeed rssFeed = (RssFeed)values[0];
            String html = (String)values[1];

            listener.onDownloaded(rssFeed, html);

            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
