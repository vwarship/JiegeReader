package com.zaoqibu.jiegereader.db;

/**
 * Created by vwarship on 2015/3/6.
 */
public class RssFeed {
    private int id;
    private String title;
    private String link;
    private boolean isFeed;
    private long createDate;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isFeed() {
        return isFeed;
    }

    public void setFeed(boolean isFeed) {
        this.isFeed = isFeed;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }
}
