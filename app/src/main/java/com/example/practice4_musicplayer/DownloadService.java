package com.example.practice4_musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;

public class DownloadService extends Service {
    private final String TAG="DownloadServiceDebug";

    private DownloadTask downloadTask;

    private String downloadUrl;

    /**
     * DownloadListener用于DownloadService与DownloadTask的关联
     */
    private DownloadListener listener=new DownloadListener() {
        private int progress;
        private boolean success=false;

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onProgress(int progress) {
            success=false;
            getNotificationManager().notify(1, getNotification("正在下载......", progress));
            this.progress=progress;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onSuccess() {
            success=true;
            mBinder.setSuccess(true);
            downloadTask=null;
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("下载成功！", -1));
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onFailed() {
            success=false;
            downloadTask=null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("下载失败！", -1));
        }

        @Override
        public void onPaused() {
            success=false;
            downloadTask=null;
        }

        @Override
        public void onCanceled() {
            success=false;
            downloadTask=null;
            stopForeground(true);
        }

        @Override
        public int getProgress(){
            return this.progress;
        }

        @Override
        public boolean isDownloading(){
            if(downloadTask==null){
                return false;
            }else{
                return true;
            }
        }

        @Override
        public boolean isSuccess(){
            return success;
        }
    };

    private DownloadBinder mBinder=new DownloadBinder();

    /**
     * DownloadBinder用于DownloadService与Activity的绑定
     */
    class DownloadBinder extends Binder{
        private boolean isSuccess=false;

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void startDownload(String url, String fileName){
            isSuccess=false;
            if (downloadTask == null) {
                downloadUrl=url;
                downloadTask=new DownloadTask(listener, fileName);
                downloadTask.execute(downloadUrl);
                Log.d(TAG, "startDownload: url:"+url);
                startForeground(1, getNotification("正在下载......", 0));
            }
        }

        public void pauseDownload(){
            if (downloadTask != null) {
                downloadTask.pauseDownload();
            }   
        }

        public void cancelDownload(){
            if(downloadTask!=null){
                downloadTask.cancelDownload();
            }else{
                if(downloadUrl!=null){
                    String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file=new File(directory+fileName);
                    if(file.exists()){
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                }
            }
        }

        public int getProgress(){
            return listener.getProgress();
        }

        public boolean isDownloading(){
            if(downloadTask==null){
                return false;
            }else {
                return true;
            }
        }

        public boolean isSuccess(){
            return this.isSuccess;
        }

        public void setSuccess(boolean success){
            this.isSuccess=success;
        }
    }

    private NotificationManager getNotificationManager(){
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * 创建DownloadService前台服务的通知
     * @param title
     * @param progress
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification getNotification(String title, int progress){
        NotificationManager manager=getNotificationManager();
        NotificationChannel channel=new NotificationChannel("downloadService", "下载服务", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);
        Intent intent=new Intent(this, MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder=new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        builder.setChannelId("downloadService");
        if(progress>0){
            builder.setContentText(progress+"%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }

    public DownloadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }
}