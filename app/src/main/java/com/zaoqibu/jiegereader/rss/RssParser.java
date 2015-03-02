package com.zaoqibu.jiegereader.rss;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by vwarship on 2015/3/3.
 */
public class RssParser {
    public Rss parse(String rssText) {
        Channel channel = new Channel();

        Rss rss = new Rss();
        rss.setChannel(channel);

        XmlPullParserFactory factory = null;
        try {
            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser xmlPullParser = factory.newPullParser();
            xmlPullParser.setInput(new StringReader(rssText));

            int eventType = xmlPullParser.getEventType();
            while (eventType != xmlPullParser.END_DOCUMENT) {
                if (eventType == xmlPullParser.START_TAG && xmlPullParser.getName().equals("title")) {
                    String title = xmlPullParser.nextText();
                    channel.setTitle(title);
                } else if (eventType == xmlPullParser.START_TAG && xmlPullParser.getName().equals("link")) {
                    String link = xmlPullParser.nextText();
                    channel.setLink(link);
                } else if (eventType == xmlPullParser.START_TAG && xmlPullParser.getName().equals("description")) {
                    String description = xmlPullParser.nextText();
                    channel.setDescription(description);
                } else if (eventType == xmlPullParser.START_TAG && xmlPullParser.getName().equals("image")) {
                    while (!(eventType == xmlPullParser.END_TAG && xmlPullParser.getName().equals("image"))) {
                        eventType = xmlPullParser.next();
                        // 不处理
                    }
                } else if (eventType == xmlPullParser.START_TAG && xmlPullParser.getName().equals("item")) {
                    Item item = new Item();
                    parseChannelItem(xmlPullParser, eventType, item);
                    channel.addItem(item);

                    Log.i("TEST", item.getTitle());
                }

                eventType = xmlPullParser.next();
            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rss;
    }

    private void parseChannelItem(XmlPullParser xmlPullParser, int eventType, Item item) throws XmlPullParserException, IOException {
        while (!(eventType == xmlPullParser.END_TAG && xmlPullParser.getName().equals("item"))) {
            if (eventType == xmlPullParser.START_TAG) {
                if (xmlPullParser.getName().equals("title")) {
                    String title = xmlPullParser.nextText();
                    item.setTitle(title);
                } if (xmlPullParser.getName().equals("link")) {
                    String link = xmlPullParser.nextText();
                    item.setLink(link);
                } if (xmlPullParser.getName().equals("pubDate")) {
                    String pubDate = xmlPullParser.nextText();
                    item.setPubDate(pubDate);
                } if (xmlPullParser.getName().equals("description")) {
                    String description = xmlPullParser.nextText();
                    item.setDescription(description);
                }
            }

            eventType = xmlPullParser.next();
        }
    }
}
