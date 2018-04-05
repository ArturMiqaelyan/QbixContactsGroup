package com.qbix.qbixcontactgrouplib;

public class QbixGroup {

    private long id;
    private String title;

    public QbixGroup() {
    }

    public QbixGroup(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
