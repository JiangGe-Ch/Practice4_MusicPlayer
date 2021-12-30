package com.example.practice4_musicplayer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.os.Environment;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import androidx.annotation.RequiresApi;
import androidx.media.MediaBrowserServiceCompat;

import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app"s UI, which gives a seamless playback
 * experience to the user.
 * <p>
 * To implement a MediaBrowserService, you need to:
 *
 * <ul>
 *
 * <li> Extend {@link MediaBrowserServiceCompat}, implementing the media browsing
 *      related methods {@link MediaBrowserServiceCompat#onGetRoot} and
 *      {@link MediaBrowserServiceCompat#onLoadChildren};
 * <li> In onCreate, start a new {@link MediaSessionCompat} and notify its parent
 *      with the session"s token {@link MediaBrowserServiceCompat#setSessionToken};
 *
 * <li> Set a callback on the {@link MediaSessionCompat#setCallback(MediaSessionCompat.Callback)}.
 *      The callback will receive all the user"s actions, like play, pause, etc;
 *
 * <li> Handle all the actual music playing using any method your app prefers (for example,
 *      {@link android.media.MediaPlayer})
 *
 * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 *      {@link MediaSessionCompat#setPlaybackState(android.support.v4.media.session.PlaybackStateCompat)}
 *      {@link MediaSessionCompat#setMetadata(android.support.v4.media.MediaMetadataCompat)} and
 *      {@link MediaSessionCompat#setQueue(java.util.List)})
 *
 * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
 *      android.media.browse.MediaBrowserService
 *
 * </ul>
 * <p>
 * To make your app compatible with Android Auto, you also need to:
 *
 * <ul>
 *
 * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 *      with a &lt;automotiveApp&gt; root element. For a media app, this must include
 *      an &lt;uses name="media"/&gt; element as a child.
 *      For example, in AndroidManifest.xml:
 *          &lt;meta-data android:name="com.google.android.gms.car.application"
 *              android:resource="@xml/automotive_app_desc"/&gt;
 *      And in res/values/automotive_app_desc.xml:
 *          &lt;automotiveApp&gt;
 *              &lt;uses name="media"/&gt;
 *          &lt;/automotiveApp&gt;
 *
 * </ul>
 */

/**
 * 音乐播放服务
 */
public class MusicPlayService extends MediaBrowserServiceCompat {

    private final String TAG="MusicServiceDebug";

    private final String ACTION_PRE="com.example.practice4_musicplayer.CONTROL.PRE";
    private final String ACTION_PLAY="com.example.practice4_musicplayer.CONTROL.PLAY";
    private final String ACTION_NEXT="com.example.practice4_musicplayer.CONTROL.NEXT";
    private final String ACTION_PAUSE="com.example.practice4_musicplayer.CONTROL.PAUSE";

    private ArrayList<SongListItem> songListItems=new ArrayList<>();

    private int position;

    private MediaSession mSession;
    private PlaybackState mPlaybackState;

    private MyMusicPlayer musicPlayer=new MyMusicPlayer();

    private MusicPlayerBinder musicPlayerBinder=new MusicPlayerBinder();
    
    class MusicPlayerBinder extends Binder {
        public void initPlayer(int position, ArrayList<SongListItem> songList) {
            initMusicPlayer(songList.get(position));
        }

        public int getPosition(){
            return position;
        }

        public boolean isPlaying(){
            if(musicPlayer!=null){
                return musicPlayer.isPlaying();
            }else {
                return false;
            }
        }

        public void onPlay() {
            if (!musicPlayer.isPlaying()) {
                musicPlayer.start();
                mPlaybackState = new PlaybackState.Builder()
                        .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                        .build();
                mSession.setPlaybackState(mPlaybackState);
                Log.d(TAG, "onPlay:  duration:"+musicPlayer.getDuration());
                Log.d(TAG, "onPlay: currentPosition:"+musicPlayer.getCurrentPosition());
            }
        }

        public void onPause() {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
                mPlaybackState = new PlaybackState.Builder()
                        .setState(PlaybackState.STATE_PAUSED, 0, 1.0f)
                        .build();
                mSession.setPlaybackState(mPlaybackState);
            }
        }

        public void onStop() {
            musicPlayer.stop();
            mPlaybackState = new PlaybackState.Builder()
                    .setState(PlaybackState.STATE_STOPPED, 0, 1.0f)
                    .build();
            mSession.setPlaybackState(mPlaybackState);
        }
        
        public void onPre(){
            Log.d(TAG, "onPre: ");
            musicPlayer.reset();
            if(position>0){
                position--;
                initMusicPlayer(songListItems.get(position));
                musicPlayer.start();
            }else {
                position=4;
                initMusicPlayer(songListItems.get(position));
                musicPlayer.start();
            }
        }
        
        public void onNext(){
            Log.d(TAG, "onNext: ");
            musicPlayer.reset();
            if(position<4){
                position++;
                initMusicPlayer(songListItems.get(position));
                musicPlayer.start();
            }else {
                position=0;
                initMusicPlayer(songListItems.get(position));
                musicPlayer.start();
            }
        }

        public void seekTo(int msec){
            musicPlayer.seekTo(msec);
            Log.d(TAG, "seekTo: "+msec);
        }

        public int getCurrentPosition(){
            return musicPlayer.getCurrentPosition();
        }

        public int getDuration(){
            return musicPlayer.getDuration();
        }

    }


    private void initMusicPlayer(SongListItem item){
        try {
//            File file=new File(Environment.getExternalStorageDirectory(), "music.mp3");
            File file=new File(item.getDir());
            Log.d(TAG, "initMusicPlayer:  dir:  "+file.getPath());
            musicPlayer.setDataSource(file.getPath());
            musicPlayer.prepare();
        }catch (Exception e){
            Log.d(TAG, "initMusicPlayer: "+e.getMessage());
        }
        mPlaybackState=new PlaybackState.Builder()
                .setState(PlaybackState.STATE_NONE, 0, 1.0f)
                .build();
        mSession = new MediaSession(this, "MusicService");
        mSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                if(!musicPlayer.isPlaying()){
                    musicPlayer.start();
                    mPlaybackState=new PlaybackState.Builder()
                            .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                            .build();
                    mSession.setPlaybackState(mPlaybackState);
                    Log.d(TAG, "onPlay: playerState:"+musicPlayer.isPlaying());
                }
            }

            @Override
            public void onPause() {
                if(musicPlayer.isPlaying()){
                    musicPlayer.pause();
                    mPlaybackState=new PlaybackState.Builder()
                            .setState(PlaybackState.STATE_PAUSED, 0, 1.0f)
                            .build();
                    mSession.setPlaybackState(mPlaybackState);
                }
            }

            @Override
            public void onStop() {
                musicPlayer.stop();
                mPlaybackState=new PlaybackState.Builder()
                        .setState(PlaybackState.STATE_STOPPED, 0,1.0f)
                        .build();
                mSession.setPlaybackState(mPlaybackState);
            }
        });
        Log.d(TAG, "initMusicPlayer: ");
//        Log.d(TAG, "initMusicPlayer: playerState:"+musicPlayer.isPlaying());
    }

    private void initSonglist(){
        DataBaseHelper dataBaseHelper=new DataBaseHelper(this, "song.db", null, 1);
        SQLiteDatabase db=dataBaseHelper.getReadableDatabase();
        Cursor cursor=db.query("songlist", null, null, null, null, null, null);
        String dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        if(cursor.moveToFirst()){
            do{
                @SuppressLint("Range") String fullDir=dir+"/"+cursor.getString(cursor.getColumnIndex("name"));
                SongListItem item=new SongListItem(fullDir);
                songListItems.add(item);
            }while (cursor.moveToNext());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        String controlAction="";
        controlAction=intent.getAction();
        Log.d(TAG, "onStartCommand:  controlAction:"+controlAction);
        if(controlAction!=null){
            Log.d(TAG, "onStartCommand:  controlAction is not null");
            if(controlAction.equals(ACTION_PLAY)){
                musicPlayer.start();
                Log.d(TAG, "onStartCommand:  play");
                return super.onStartCommand(intent, flags, startId);
            }else if(controlAction.equals(ACTION_PAUSE)){
                musicPlayer.pause();
                Log.d(TAG, "onStartCommand:  pause");
                return super.onStartCommand(intent, flags, startId);
            }else if(controlAction.equals(ACTION_PRE)){
                Log.d(TAG, "onStartCommand:  pre");
                return super.onStartCommand(intent, flags, startId);
            }else if(controlAction.equals(ACTION_NEXT)){
                Log.d(TAG, "onStartCommand:  next");
                return super.onStartCommand(intent, flags, startId);
            }
        }
        position=intent.getIntExtra("position", 0);
        initSonglist();
        musicPlayer.reset();
        initMusicPlayer(songListItems.get(position));
        NotificationManager manager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel=new NotificationChannel("forgroundService", "MusicPlayService", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(notificationChannel);

        Intent startServiceIntent=new Intent(this, MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,startServiceIntent, 0);

        Intent preIntent=new Intent(MusicPlayService.this, MusicPlayService.class);
        preIntent.setAction(ACTION_PRE);
        preIntent.setComponent(new ComponentName("com.example.practice4_musicplayer", "com.example.practice4_musicplayer.MusicPlayService"));
        PendingIntent prePiIntent=PendingIntent.getForegroundService(this, 0, preIntent, 0);

        Intent playIntent=new Intent(MusicPlayService.this, MusicPlayService.class);
        playIntent.setAction(ACTION_PLAY);
        playIntent.setComponent(new ComponentName("com.example.practice4_musicplayer", "com.example.practice4_musicplayer.MusicPlayService"));
        PendingIntent playPiIntent=PendingIntent.getService(this, 0, playIntent, 0);

        Intent pauseIntent=new Intent(MusicPlayService.this, MusicPlayService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        pauseIntent.setComponent(new ComponentName("com.example.practice4_musicplayer", "com.example.practice4_musicplayer.MusicPlayService"));
        PendingIntent pausePiIntent=PendingIntent.getForegroundService(this, 0, pauseIntent, 0);

        Intent nextIntent=new Intent(MusicPlayService.this, MusicPlayService.class);
        nextIntent.setAction(ACTION_NEXT);
        nextIntent.setComponent(new ComponentName("com.example.practice4_musicplayer", "com.example.practice4_musicplayer.MusicPlayService"));
        PendingIntent nextPiIntent=PendingIntent.getForegroundService(this, 0, nextIntent, 0);

        Notification.Action action=new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.pre), "2", null).build();

        Notification notification=new Notification.Builder(this, "forgroundServiceNotifacation")
                .setContentTitle("歌曲播放服务")
                .setContentText("前台服务正在播放歌曲")
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.mipmap.pre), "上一曲", prePiIntent).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.mipmap.play), "播放", playPiIntent).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.mipmap.pause), "暂停", pausePiIntent).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.mipmap.next), "下一曲", nextPiIntent).build())
//                .addAction(R.drawable.pre, "上一曲", prePiIntent) // #0
//                .addAction(R.drawable.play, "播放", playPiIntent) // #1
//                .addAction(R.drawable.pause, "暂停", pausePiIntent) // #2
//                .addAction(R.drawable.next, "下一曲", nextPiIntent) // #3
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new Notification.MediaStyle()
                    .setShowActionsInCompactView(1 /* #1: pause button */)
                    .setMediaSession(mSession.getSessionToken()))
                .setContentIntent(pi)
                .setWhen(System.currentTimeMillis())
                .setChannelId("forgroundService")
                .build();

        try{
            startForeground(1, notification);
        }catch (Exception e){
            Log.d(TAG, "onStartCommand: "+e.getMessage());
        }
//        musicPlayer.start();
        Log.d(TAG, "onStartCommand:   successful!");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: ");
        return musicPlayerBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        musicPlayer.release();

//        initMusicPlayer();

        Log.d(TAG, "onCreate:  MusicService");
    }

    @Override
    public void onDestroy() {
        mSession.release();
        Log.d(TAG, "onDestroy: ");
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
                                 int clientUid,
                                 Bundle rootHints) {
        Log.d(TAG, "onGetRoot: ");
        return new BrowserRoot("root", null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId,
                               @NonNull final Result<List<MediaItem>> result) {
        result.sendResult(new ArrayList<MediaItem>());
    }
}