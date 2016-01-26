package com.mapbar.moo.resource;

import java.text.DecimalFormat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mapbar.moo.R;
import com.mapbar.offlinednload.OfflineDataManager;
import com.mapbar.offlinednload.OfflineRecordInfo;

public class DownloadItemAdapter extends BaseAdapter {
	
	//下载管理类
	private OfflineDataManager mOfflineDataManager;
	private ListView mListView;
	private LayoutInflater mInflater = null;
	private DownloadViewItem itemViewHolder = null;
	private OfflineRecordInfo[] datas;
	private Context mContext;
	
	public DownloadItemAdapter(Context context,OfflineDataManager mOfflineDataManager,ListView listView,OfflineRecordInfo[] datas){
		this.mContext = context;
		this.mOfflineDataManager = mOfflineDataManager;
		this.mListView = listView;
		this.datas = datas;
		mInflater = LayoutInflater.from(mContext);
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
		final OfflineRecordInfo item = datas[position];
		if (convertView == null) {
			//绑定控件
			convertView = mInflater.inflate(R.layout.item_download_item, null);
			itemViewHolder = new DownloadViewItem();
			itemViewHolder.itemName = (TextView)convertView.findViewById(R.id.tv_item_name);
			itemViewHolder.dataSize = (TextView)convertView.findViewById(R.id.tv_data_size);
			itemViewHolder.downProgress = (TextView)convertView.findViewById(R.id.tv_down_progress);
			itemViewHolder.downloadingProgressBar = (ProgressBar)convertView.findViewById(R.id.pb_manage_downloading_progress_bar);
			itemViewHolder.start = (ImageButton)convertView.findViewById(R.id.ib_start);
			itemViewHolder.pause = (ImageButton)convertView.findViewById(R.id.ib_pause);
			itemViewHolder.remove = (ImageButton)convertView.findViewById(R.id.ib_remove);
			itemViewHolder.update = (ImageButton)convertView.findViewById(R.id.ib_update);
			itemViewHolder.newIcon = (ImageView)convertView.findViewById(R.id.iv_new);
			convertView.setTag(itemViewHolder);
		}else{
			itemViewHolder = (DownloadViewItem) convertView.getTag();
		}
		//获取数据dataId
		itemViewHolder.dataId = item.getDataId();
		//数据类型 1基础数据 2地图数据3导航数据4电子眼数据
		if(item.getType()==OfflineDataManager.DataType.BASE){
			itemViewHolder.itemName.setText(R.string.base_data);
		}else if(item.getType()==OfflineDataManager.DataType.MAP){
			itemViewHolder.itemName.setText(R.string.map_data);
		}else if(item.getType()==OfflineDataManager.DataType.NAVI){
			itemViewHolder.itemName.setText(R.string.navi_data);
		}else if(item.getType()==OfflineDataManager.DataType.CAMERA){
			itemViewHolder.itemName.setText(R.string.camr_data);
		}
		//数据大小
		itemViewHolder.dataSize.setText(downSizeFormat(item.getFileSize()));
		//设置下载百分比和下载进度条
		getDownloadProgress(itemViewHolder.downloadingProgressBar,itemViewHolder.downProgress,item.getFileSize(),item.getDownSize());
		//设置数据状态，根据状态设置相应按钮显示和隐藏信息
		setDownloadStatus(itemViewHolder,item);
		//开始下载
		itemViewHolder.start.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//调用开始下载接口，传入需要下载才数据dataId
				
				mOfflineDataManager.start(item.getDataId());
				
			}
		});
		//暂停下载
		itemViewHolder.pause.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//调用暂停下载接口，传入需要暂停下载的数据dataid
				mOfflineDataManager.pause(item.getDataId());
			}
		});
		//删除下载和数据
		itemViewHolder.remove.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//调用删除接口，传入需要删除的数据dataid
				mOfflineDataManager.remove(item.getDataId());
			}
		});
		//更新数据
		itemViewHolder.update.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//调用更新接口，传入需要更新的数据dataid
				mOfflineDataManager.update(item.getDataId());
			}
		});
		convertView.setTag(itemViewHolder);
		return convertView;
	}
	
	/**
	 * 更改下载数据信息
	 */
	public void updateItem(OfflineRecordInfo item){
		synchronized (mListView){
			int count = mListView.getChildCount();
			for(int i=0; i<count; i++) {
				Object view = mListView.getChildAt(i).getTag();
				if(view instanceof DownloadViewItem) {
					DownloadViewItem viewHolder = (DownloadViewItem)view;
					if(viewHolder.dataId.equals(item.getDataId())) {
						//设置下载百分比和下载进度条
						getDownloadProgress(viewHolder.downloadingProgressBar,viewHolder.downProgress,item.getFileSize(),item.getDownSize());
						//设置下载状态
						setDownloadStatus(viewHolder,item);
						if(mListView.isShown() == false){
							return;
						}
						break;
					}
				}
			}
		}
	}
	
	private void setDownloadStatus(DownloadViewItem item,OfflineRecordInfo info){
		int status = info.getStatus();
		//根据当前状态显示开始，暂停，删除按钮
		if(status==OfflineDataManager.Status.INIT){
			//初始状态
			item.start.setVisibility(View.VISIBLE);
			item.pause.setVisibility(View.GONE);
			item.remove.setVisibility(View.GONE);
		}else if(status==OfflineDataManager.Status.START){
			//下载中……
			item.start.setVisibility(View.GONE);
			item.pause.setVisibility(View.VISIBLE);
			item.remove.setVisibility(View.VISIBLE);
		}else if(status==OfflineDataManager.Status.PAUSE){
			//暂停
			item.start.setVisibility(View.VISIBLE);
			item.pause.setVisibility(View.GONE);
			item.remove.setVisibility(View.VISIBLE);
		}else if(status==OfflineDataManager.Status.FAIL){
			//失败
			item.start.setVisibility(View.VISIBLE);
			item.pause.setVisibility(View.GONE);
			item.remove.setVisibility(View.VISIBLE);
		}else if(status==OfflineDataManager.Status.COMPLETED){
			//下载完毕
			item.start.setVisibility(View.GONE);
			item.pause.setVisibility(View.GONE);
			item.remove.setVisibility(View.VISIBLE);
		}
		//数据是否有更新
		if(info.isUpdate()){
			item.update.setVisibility(View.VISIBLE);
			item.newIcon.setVisibility(View.VISIBLE);
		}else{
			item.update.setVisibility(View.GONE);
			item.newIcon.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 格式化文件大小
	 * @param size
	 * @return
	 */
	private String downSizeFormat(long size){
		double kSize = (double)size /1024;
		if(kSize >= 1000) {
			double dSize = (double)size /1024 / 1024;
			return new DecimalFormat("###,###.##M").format(dSize);
		} else {
			return new DecimalFormat("###K").format(kSize);
		}
	}
	
	/**
	 * 设置进度百分比和进度条进度  最大100，用文件大小和下载大小计算
	 * @param size
	 * @param downsize
	 * @return
	 */
	private void getDownloadProgress(ProgressBar downloadingProgressBar,TextView downProgress,long size,long downsize){
		if(size == 0) {
			downloadingProgressBar.setProgress(0);
			downProgress.setText("0%");
		} else {
			double per = ((double)downsize / size)*100;
			downProgress.setText(new DecimalFormat("##").format(per)+"%");
			int aa = (int)per;
			downloadingProgressBar.setProgress(aa);
		}
	}
	
	public class DownloadViewItem {
		public String dataId;
		public TextView itemName;
		public TextView dataSize ;
		public TextView downProgress;
		public ProgressBar downloadingProgressBar;
		public ImageButton start;
		public ImageButton pause;
		public ImageButton remove;
		public ImageButton update;
		public ImageView newIcon;
	}
}
