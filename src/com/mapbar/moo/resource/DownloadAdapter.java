package com.mapbar.moo.resource;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapbar.moo.DownloadItemActivity;
import com.mapbar.moo.R;
import com.mapbar.offlinednload.OfflineRecord;

public class DownloadAdapter extends BaseAdapter {
	private LayoutInflater mInflater = null;
	private DownloadViewItem itemViewHolder = null;
	private OfflineRecord[] datas;
	private Context context;
	
	public DownloadAdapter(Context context, OfflineRecord[] datas){
		this.context = context;
		this.datas = datas;
		mInflater = LayoutInflater.from(context);
	}
	
	public int getCount() {
		return datas.length;
	}

	public Object getItem(int position) {
		return datas[position];
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(final int position, View convertView, ViewGroup parent) {
		final OfflineRecord or = datas[position];
		if (convertView == null) {
			//绑定控件
			convertView = mInflater.inflate(R.layout.item_download, null);
			itemViewHolder = new DownloadViewItem();
			itemViewHolder.itemName = (TextView)convertView.findViewById(R.id.tv_item_name);
			itemViewHolder.itemMain = (RelativeLayout)convertView.findViewById(R.id.layout_item_main);
			convertView.setTag(itemViewHolder);
		}else{
			itemViewHolder = (DownloadViewItem) convertView.getTag();
		}
		itemViewHolder.itemName.setText(datas[position].getName());
		itemViewHolder.itemMain.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context,DownloadItemActivity.class);
				Bundle b = new Bundle();
				b.putSerializable("OfflineRecord", or);
				intent.putExtras(b);
				context.startActivity(intent);
			}
		});
		return convertView;
	}
	
	public class DownloadViewItem {
		public TextView itemName;
		public RelativeLayout itemMain;
	}
}
