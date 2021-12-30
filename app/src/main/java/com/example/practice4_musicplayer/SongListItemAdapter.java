package com.example.practice4_musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class SongListItemAdapter extends ArrayAdapter<SongListItem> {
    private int resourceId;

    public SongListItemAdapter(@NonNull Context context, int textViewResourceId, @NonNull List<SongListItem> objects) {
        super(context, textViewResourceId, objects);
        resourceId=textViewResourceId;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        SongListItem item= getItem(position);
        View view;

        ViewHolder holder;
        if(convertView==null){
            view= LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            holder=new ViewHolder();
            holder.name=(TextView)view.findViewById(R.id.item_songlist_name);
            holder.status=(TextView) view.findViewById(R.id.item_songlist_stauts);
            holder.downloaded=(TextView) view.findViewById(R.id.item_songlist_isdownloaded);
            view.setTag(holder);
        }else {
            view=convertView;
            holder=(ViewHolder)view.getTag();
        }
        holder.name.setText(item.getName());
        if(item.getStauts()!=""){
            holder.status.setText(item.getStauts());
        }else {
            holder.status.setText("暂无");
        }
        boolean isDownloaded=item.isDownloaded();
        if(isDownloaded){
            holder.downloaded.setText("已下载");
        }else{
            holder.downloaded.setText("未下载");
        }
        return view;
    }

    class ViewHolder{
        TextView name;
        TextView status;
        TextView downloaded;
    }

}
