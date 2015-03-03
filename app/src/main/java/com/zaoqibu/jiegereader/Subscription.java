package com.zaoqibu.jiegereader;

/**
 * Created by vwarship on 2015/3/4.
 */
public class Subscription {
    private String name;
    private String url;

    public Subscription(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
