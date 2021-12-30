package com.example.practice4_musicplayer;

public interface DownloadListener {
    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();

    int getProgress();

    boolean isDownloading();

    boolean isSuccess();
}
