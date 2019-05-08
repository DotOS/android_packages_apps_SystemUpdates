package com.dotos.updater.model;

public class ChannelVerified extends ChannelBase {

    public ChannelVerified(String name, String summary, String url) {
        this.name = name;
        this.summary = summary;
        this.url = url;
        this.verified = true;
    }
}
