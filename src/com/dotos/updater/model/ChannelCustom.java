package com.dotos.updater.model;

public class ChannelCustom extends ChannelBase {

    public ChannelCustom(String name, String summary, String url, String owner) {
        this.name = name;
        this.summary = summary;
        this.url = url;
        this.verified = false;
    }
}
