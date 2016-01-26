package com.mapbar.moo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbar.moo.resource.DownloadItemAdapter;
import com.mapbar.offlinednload.OfflineDataListener;
import com.mapbar.offlinednload.OfflineDataManager;
import com.mapbar.offlinednload.OfflineRecord;
import com.mapbar.offlinednload.OfflineRecordInfo;

public class DownloadItemActivity extends Activity implements OfflineDataListener,OnClickListener{
	//下载管理类
	private OfflineDataManager mOfflineDataManager;
	private DownloadItemAdapter downloadItemAdapter;
	private ListView mListView;
	private TextView mTitle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_download_item);
		//获取下载管理对象
		mOfflineDataManager = OfflineDataManager.getInstance();//.init(this, this);
		mOfflineDataManager.addOfflineDataListener(this);
		//初始化控件
		initView();
		//获取下载信息对象
		OfflineRecord or = (OfflineRecord)getIntent().getExtras().getSerializable("OfflineRecord");
		if(or!=null){
			//设置标题
			mTitle.setText(or.getName());
			//设置listView数据源
			OfflineRecordInfo[] infos = mOfflineDataManager.getOfflineRecordInfos(or.getRecordId());
			downloadItemAdapter = new DownloadItemAdapter(this,mOfflineDataManager,mListView,infos);
			mListView.setAdapter(downloadItemAdapter);
		}
	}
	
	/**
	 * 初始化控件
	 */
	private void initView(){
		mTitle = (TextView)findViewById(R.id.tv_title_text);
		findViewById(R.id.iv_title_back).setOnClickListener(this);
		mListView = (ListView)findViewById(R.id.dataItemListView);
		findViewById(R.id.iv_title_online).setVisibility(View.GONE);
		
	}
	
	/**
	 * 下载监听回调方法
	 */
	@Override
	public void onOfflineDataEvent(int event, int err, Object data) {
		String msg = null;
		OfflineRecordInfo item = null;
		switch(event){
		case OfflineDataManager.Event.INIT:
			item = (OfflineRecordInfo)data;
			msg = "初始化完毕";
			break;
		case OfflineDataManager.Event.START:
			item = (OfflineRecordInfo)data;
			msg = item.getDataId()+"开始下载";
			downloadItemAdapter.updateItem(item);
			break;
		case OfflineDataManager.Event.UPDATE:
			item = (OfflineRecordInfo)data;
			msg = item.getDataId()+"更新完毕";
			downloadItemAdapter.updateItem(item);
			break;
		case OfflineDataManager.Event.PAUSE:
			item = (OfflineRecordInfo)data;
			msg = "暂停"+item.getDataId();
			downloadItemAdapter.updateItem(item);
			break;
		case OfflineDataManager.Event.REMOVE:
			item = (OfflineRecordInfo)data;
			msg = "删除"+item.getDataId();
			downloadItemAdapter.updateItem(item);
			break;
		case OfflineDataManager.Event.TRACKING:
			//实时跟踪下载进度,修改下载进度条和下载比例等信息
			item = (OfflineRecordInfo)data;
			downloadItemAdapter.updateItem(item);
			System.out.println("+++");
			switch(err){
			case OfflineDataManager.Error.NONE:
				//跟踪正常信息，获取数据变化
				if(item.getStatus()==OfflineDataManager.Status.COMPLETED){
					msg = item.getDataId()+"下载完毕";
				}
				break;
			case OfflineDataManager.Error.NET_ERROR:
				//网络异常
				msg = "下载"+item.getDataId()+"数据时，出现网络异常";
				break;
			case OfflineDataManager.Error.VALIDATA_ERROR:
				//数据验证失败
				msg = "数据"+item.getDataId()+"验证失败";
				break;
			case OfflineDataManager.Error.SDCARD_ERROR:
				//访问异常
				msg = "没有sdcard";
				break;
			}
			break;
		case OfflineDataManager.Event.FAIL:
			switch(err){
			case OfflineDataManager.Error.NONE:
				//无错误
				break;
			case OfflineDataManager.Error.NET_ERROR:
				//网络异常
				msg = "网络异常";
				break;
			case OfflineDataManager.Error.SDCARD_ERROR:
				//访问异常
				msg = "没有sdcard";
				break;
			}
			break;
		}
		if(msg!=null){
			Toast.makeText(DownloadItemActivity.this, msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.iv_title_back:
			finish();
			break;
		}
	}
}
