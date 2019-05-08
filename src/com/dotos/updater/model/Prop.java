package com.dotos.updater.model;

public class Prop {

    String desc, value;

    public Prop() {

    }

    public Prop(String desc, String value) {
        this.desc = desc;
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public String getValue() {
        return value;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
