package com.example.practice4_musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * 音乐播放器主界面（音乐列表
 */

public class MainActivity extends AppCompatActivity {

    private final String TAG="MainActivityDebug";

    private ArrayList<SongListItem> list=new ArrayList<>();

    private DownloadService.DownloadBinder downloadBinder;

    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder=(DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Music player——Classical music");
        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }else {
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
                Log.d(TAG, "onCreate:    READ PERMISSION");
            }else {
                Log.d(TAG, "onCreate:    PERMISSION ACCESS");
                PermissionAccessOnCreate();
            }
        }
//        DataBaseHelper dbHelper=new DataBaseHelper(MainActivity.this, "song.db", null, 1);
//        dbHelper.getWritableDatabase();

    }

    private void initDataBase(SQLiteDatabase db){
        ContentValues values=new ContentValues();
        values.put("name", "Tea k Pea.mp3");
        values.put("url", "https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/JHH5LjW9KYmd5yJ8XLEL0APJemXw15l1FZTEDYsh.mp3?download=1&name=Tea%20K%20Pea%20-%20lemoncholy.mp3");
        values.put("downloaded", 0);
        db.insert("songlist", null, values);
        values.clear();

        values.put("name", "Maarten Schellekens.mp3");
        values.put("url", "https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/jgW1IkP61IkW1PY3EDmwmfWGfAFa2a612j9diuCg.mp3?download=1&name=Maarten%20Schellekens%20-%20Farewell%20%28Remix%29.mp3");
        values.put("downloaded", 0);
        db.insert("songlist", null, values);
        values.clear();

        values.put("name", "Axletree.mp3");
        values.put("url", "https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/L8IpVq9stMFdl6RYEacK6VGf36HSANjA89Ooq5xV.mp3?download=1&name=Axletree%20-%20Drops%20of%20Melting%20Snow%20%28after%20Holst%2C%20Abroad%20as%20I%20was%20walking%29.mp3");
        values.put("downloaded", 0);
        db.insert("songlist", null, values);
        values.clear();

        values.put("name", "Victoria Darian & Alexei Kalinkin.mp3");
        values.put("url", "https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/jCh9gHZGvM0fwbnAqm4mebNI0CoDJPkH8lP5eaKp.mp3?download=1&name=Victoria%20Darian%20%26%20Alexei%20Kalinkin%20-%20Progulka.mp3");
        values.put("downloaded", 0);
        db.insert("songlist", null, values);
        values.clear();

        values.put("name", "Maarten Schellekens.mp3");
        values.put("url", "https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/JuG1OD1wm6f3XD93haM4qSlwAfyBS4zrOcXstBPb.mp3?download=1&name=Maarten%20Schellekens%20-%20Into%20the%20Unknown.mp3");
        values.put("downloaded", 0);
        db.insert("songlist", null, values);
    }

    private void setDownloaded(String name, SQLiteDatabase db){
        ContentValues values=new ContentValues();
        values.put("downloaded", "1");
        Log.d(TAG, "setDownloaded:   position="+name);
        db.update("songlist", values, "name=?", new String[]{name});
    }

    private void PermissionAccessOnCreate(){
        DataBaseHelper dbHelper=new DataBaseHelper(MainActivity.this, "song.db", null, 1);
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        SharedPreferences pref=getSharedPreferences("ruCount", MODE_PRIVATE);
        boolean first=pref.getBoolean("first", true);
        if(first){
            Log.d(TAG, "PermissionAccessOnCreate:   first run,  init  Database...");
            initDataBase(db);
            SharedPreferences.Editor editor=pref.edit();
            editor.putBoolean("first", false);
            editor.apply();
        }
        Intent bindIntent=new Intent(this, DownloadService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
        initSongList(db);
        Log.d(TAG, "PermissionAccessOnCreate:   list  length:  "+list.size());
        SongListItemAdapter adapter=new SongListItemAdapter(MainActivity.this, R.layout.item_songlist, list);
        ListView listView=(ListView) findViewById(R.id.list_songs);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SongListItem item=list.get(position);
                Intent intent=new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra("position", position);
                if(item.isDownloaded()){
                    startActivity(intent);
                }else if(!downloadBinder.isDownloading()){
                    downloadBinder.startDownload(item.getUrl(), item.getName());
                    item.setStauts("正在下载......");
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this,"正在下载......", Toast.LENGTH_LONG).show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!downloadBinder.isSuccess()){
                                Log.d(TAG, "run:  not yet success!");
                            }
                            item.setDownloaded(true);
                            setDownloaded(item.getName(), db);
                            startActivity(intent);
                        }
                    }).start();
                    Log.d(TAG, "onItemClick:  download:"+item.getName());
                }
            }
        });
    }

    private void initSongList(SQLiteDatabase db){
        list.clear();
        SongListItem item;
        Cursor cursor=db.query("songlist", null, null, null, null, null, null);
        if(cursor.moveToFirst()){
            do{
                @SuppressLint("Range") String name=cursor.getString(cursor.getColumnIndex("name"));
                @SuppressLint("Range") String url=cursor.getString(cursor.getColumnIndex("url"));
                @SuppressLint("Range") int isDownloaded=cursor.getInt(cursor.getColumnIndex("downloaded"));
                if(isDownloaded==1){
                    item=new SongListItem(name, url, true);
                }else{
                    item=new SongListItem(name, url, false);
                }
                Log.d(TAG, "initSongList:   new  item:"+name+"  "+isDownloaded);
                list.add(item);
            }while (cursor.moveToNext());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                Log.d(TAG, "onRequestPermissionsResult:   Code   1");
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
                    Log.d(TAG, "onRequestPermissionsResult:   Code   1   Permission   grant");
                }else {
                    Log.d(TAG, "onRequestPermissionsResult:    Code   1   else");
                    PermissionAccessOnCreate();
                }
            case 2:
                if(grantResults.length>0 && grantResults[0]==PackageManager
                        .PERMISSION_GRANTED){
                    Log.d(TAG, "onRequestPermissionsResult:    Code     2");
                   PermissionAccessOnCreate();
                }else {
                    Toast.makeText(MainActivity.this, "权限被拒绝！", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}