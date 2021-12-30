package com.example.practice4_musicplayer;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    private final String TAG="DownloadTaskDebug";

    public static final int TYPE_SUCCESS=0;
    public static final int TYPE_FAILED=1;
    public static final int TYPE_PAUSED=2;
    public static final int TYPE_CANCELED=3;

    private DownloadListener listener;

    private boolean isCanceled=false;

    private boolean isPaused=false;

    private int lastProgress;

    private String fileName;

    public DownloadTask(DownloadListener listener, String fileName){
        this.listener=listener;
        this.fileName=fileName;
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream is=null;
        RandomAccessFile saveFile=null;
        File file=null;
        try{
            long downloadeLength=0;
            String downloadUrl=params[0];
//            String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file=new File(directory+"/"+fileName);
            Log.d(TAG, "doInBackground:    dir: "+directory);
            Log.d(TAG, "doInBackground:   fileName: "+fileName);
            if(file.exists()){
                Log.d(TAG, "doInBackground:  file exists");
                downloadeLength=file.length();
            }
            long contentLength=getContentLength(downloadUrl);
            if(contentLength==0){
                return TYPE_FAILED;
            }else if(contentLength==downloadeLength){
                return TYPE_SUCCESS;
            }
            OkHttpClient client=new OkHttpClient();
            Request request=new Request.Builder()
                    .addHeader("RANGE", "bytes="+downloadeLength+"-")
                    .url(downloadUrl)
                    .build();
            Response response=client.newCall(request).execute();
            if(response!=null){
                Log.d(TAG, "doInBackground: respense is not null...");
                is=response.body().byteStream();
                saveFile=new RandomAccessFile(file, "rw");
                Log.d(TAG, "doInBackground: tttttttt");
                saveFile.seek(downloadeLength);                 //偏移到文件现有长度处，支持断点续传
                byte[] b=new byte[1024];
                int total=0;
                int len;
                /*
                依次读取数据流写入文件
                 */
                while((len=is.read(b))!=-1){
                    Log.d(TAG, "doInBackground:  downloading......");
                    if(isCanceled){
                        return TYPE_CANCELED;
                    }else if(isPaused){
                        return TYPE_PAUSED;
                    }else{
                        total+=len;
                        saveFile.write(b, 0, len);
                        int progress=(int)((total+downloadeLength)*100/contentLength);
                        publishProgress(progress);                                      //更新下载进度
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        }catch (Exception e){
            Log.d(TAG, "doInBackground: "+e.getMessage());
        }finally {
            try{
                if(is!=null){
                    is.close();
                }
                if(saveFile!=null){
                    saveFile.close();
                }
                if(isCanceled&& file!=null){
                    file.delete();
                }
            }catch (Exception e){

            }
        }
        return TYPE_FAILED;
    }


    /**
     * 通过listener更新下载进度显示
     * @param values
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress=values[0];
        if(progress>lastProgress){
            listener.onProgress(progress);
            lastProgress=progress;
        }
    }


    @Override
    protected void onPostExecute(Integer status) {
        switch (status){
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            default:
                break;
        }
    }

    public void pauseDownload(){
        isPaused=true;
    }

    public void cancelDownload(){
        isCanceled=true;
    }

    private long getContentLength(String downloadUrl) throws IOException{
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response=client.newCall(request).execute();
        if(response!=null && response.isSuccessful()){
            long contentLength=response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }
}
