package com.khai.whatappsclone.Models;

public class Status {
    private String ImageUrl;
    private long timeStamp;

    public Status(String imageUrl, long timeStamp) {
        ImageUrl = imageUrl;
        this.timeStamp = timeStamp;
    }

    public Status(){

    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        ImageUrl = imageUrl;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
