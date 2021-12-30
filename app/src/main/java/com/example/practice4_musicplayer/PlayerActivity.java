package com.example.practice4_musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * 播放器页面
 */

public class PlayerActivity extends AppCompatActivity {

    private final String TAG="PlayerActivityDebug";

    private ArrayList<SongListItem> songList=new ArrayList<>();

    private MusicPlayService.MusicPlayerBinder musicPlayerBinder;

    private int position;

    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicPlayerBinder=(MusicPlayService.MusicPlayerBinder) service;
            musicPlayerBinder.initPlayer(position, songList);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicPlayerBinder.onStop();
        }
    };

    private final Handler handler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            SeekBar seekBar=(SeekBar) findViewById(R.id.player_control_seekBar);
            int progress=msg.what;
            Log.d(TAG, "handleMessage: what:"+progress);
            seekBar.setProgress(progress);
            TextView startPos=(TextView) findViewById(R.id.player_control_startpos);
            int current= musicPlayerBinder.getCurrentPosition();
            current=current/1000;     //总秒数
            int minute=current/60;     //分钟
            int sec=current%60;        //秒数
            String currentStr=String.valueOf(minute)+":"+String.valueOf(sec);
            startPos.setText(currentStr);
            ImageButton playBt=(ImageButton) findViewById(R.id.player_control_play);
            if(musicPlayerBinder.isPlaying()){
                playBt.setImageResource(R.mipmap.pause);
            }else {
                playBt.setImageResource(R.mipmap.play);
            }
        }
    };

    private void initSongList(){
        DataBaseHelper dataBaseHelper=new DataBaseHelper(PlayerActivity.this, "song.db", null, 1);
        SQLiteDatabase db=dataBaseHelper.getReadableDatabase();
        Cursor cursor=db.query("songlist", null, null, null, null, null, null);
        if(cursor.moveToFirst()){
            String dir= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            do{
                @SuppressLint("Range") String name=cursor.getString(cursor.getColumnIndex("name"));
                String fullDir=dir+"/"+name;
                SongListItem item=new SongListItem(fullDir, name);
                songList.add(item);
            }while (cursor.moveToNext());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        Intent intent=getIntent();
        position=intent.getIntExtra("position", 0);
        initSongList();
        setContentView(R.layout.activity_player);
        Intent startServiceIntent=new Intent(this, MusicPlayService.class);
        startServiceIntent.putExtra("position", position);
        startForegroundService(startServiceIntent);
        Intent bindIntent=new Intent(this, MusicPlayService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
        ImageButton playButton=(ImageButton) findViewById(R.id.player_control_play);
        ImageButton preButton=(ImageButton) findViewById(R.id.player_control_pre);
        ImageButton nextButton=(ImageButton) findViewById(R.id.player_control_next);
        SeekBar seekBar=(SeekBar) findViewById(R.id.player_control_seekBar);
        TextView endpos=(TextView) findViewById(R.id.player_control_endpos);

        Thread thread=new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    Message message=new Message();
                    message.what=(int)((double)musicPlayerBinder.getCurrentPosition()/(double) musicPlayerBinder.getDuration()*100);
                    handler.sendMessage(message);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "run: "+e.getMessage());
                    }
                }
            }
        });

        TextView nameView=(TextView) findViewById(R.id.player_name);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(musicPlayerBinder.isPlaying()){
                    musicPlayerBinder.onPause();
                    playButton.setImageResource(R.drawable.play);
//                    thread.stop();
                }else if(!musicPlayerBinder.isPlaying()) {
                    musicPlayerBinder.onPlay();
                    playButton.setImageResource(R.drawable.pause);
                    int duration= musicPlayerBinder.getDuration();
                    duration=duration/1000;     //总秒数
                    int minute=duration/60;     //分钟
                    int sec=duration%60;        //秒数
                    String endPosStr=String.valueOf(minute)+":"+String.valueOf(sec);
                    endpos.setText(endPosStr);
                    if(!thread.isAlive()){
                        thread.start();
                    }
                    nameView.setText(songList.get(musicPlayerBinder.getPosition()).getName());
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int duration= musicPlayerBinder.getDuration();
                int position=seekBar.getProgress();
                int seek=(int)(((double)position/100)*duration);
                musicPlayerBinder.seekTo(seek);
            }
        });

        preButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicPlayerBinder.onPre();
                if(!thread.isAlive()){
                    thread.start();
                }
                nameView.setText(songList.get(musicPlayerBinder.getPosition()).getName());
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicPlayerBinder.onNext();
                if(!thread.isAlive()){
                    thread.start();
                }
                nameView.setText(songList.get(musicPlayerBinder.getPosition()).getName());
            }
        });
    }
}