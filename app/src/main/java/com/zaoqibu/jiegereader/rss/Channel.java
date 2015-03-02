package com.zaoqibu.jiegereader.rss;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vwarship on 2015/3/3.
 */
public class Channel {
    private String title;
    private String link;
    private String description;
    private List<Item> items = new ArrayList<Item>();

    public Channel() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Item> getItems() {
        return items;
    }

    public void addItem(Item item) {
        this.items.add(item);
    }
}
