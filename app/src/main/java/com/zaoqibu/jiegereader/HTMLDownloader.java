package com.zaoqibu.jiegereader;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.UnicodeDetector;

/**
 * Created by vwarship on 2015/3/3.
 */
public class HTMLDownloader extends AsyncTask<Void, Void, String> {
    public static interface HTMLDownloaderListener {
        public abstract void onDownloaded(String html);
    }

    private HTMLDownloaderListener listener;
    private String url;

    public HTMLDownloader (String url, HTMLDownloaderListener listener) {
        this.url = url;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(Void... params) {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        String html = "";
        try {
            HttpResponse response = client.execute(request);
            InputStream in;
            in = response.getEntity().getContent();
            String charsetName = getCharsetName(url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charsetName));
            StringBuilder str = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                str.append(line);
            }
            in.close();
            html = str.toString();
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

        Log.i("TEST", charset.name());
        return charset.name();
    }

    @Override
    protected void onPostExecute(String html) {
        super.onPostExecute(html);
        if (!isCancelled()) {
            listener.onDownloaded(html);
        }

        Log.i("TEST", html);
    }

}
