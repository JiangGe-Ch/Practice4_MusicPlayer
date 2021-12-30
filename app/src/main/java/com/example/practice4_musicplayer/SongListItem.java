package com.example.practice4_musicplayer;

import android.os.Environment;

public class SongListItem {
    private String name;
    private String url;
    private boolean downloaded;
    private String dir;
    private String stauts;

    public String getStauts() {
        return stauts;
    }

    public void setStauts(String stauts) {
        this.stauts = stauts;
    }

    public SongListItem(String name, String url, boolean downloaded){
        this.name=name;
        this.url=url;
        this.downloaded=downloaded;
        dir= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+"/"+name;
        this.stauts="";
    }

    public SongListItem(String dir){
        this.dir=dir;
    }

    public SongListItem(String dir, String name){
        this.dir=dir;
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }
}
