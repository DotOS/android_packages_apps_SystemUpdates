package com.dotos.updater.model;

public class ChannelBase {

    String name, summary, url, owner;
    boolean verified, selected;

    public ChannelBase() {

    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public String getSummary() {
        return summary;
    }

    public String getUrl() {
        return url;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
