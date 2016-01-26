package com.mapbar.moo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbar.moo.resource.DownloadAdapter;
import com.mapbar.offlinednload.OfflineDataListener;
import com.mapbar.offlinednload.OfflineDataManager;
import com.mapbar.offlinednload.OfflineRecord;
import com.mapbar.offlinednload.OfflineRecordInfo;

public class DownloadActivity extends Activity implements OfflineDataListener, OnClickListener{
	//下载管理类
	private OfflineDataManager mOfflineDataManager;
	private ListView mListView;
	private ProgressBar progressBar;
	
	private String appPath = Environment
		.getExternalStorageDirectory().toString() + "/mapbar/app/";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_download);
		//获取下载管理对象
		mOfflineDataManager = OfflineDataManager.getInstance().init(this, this, appPath);
        //初始化控件
		initView();
	}
	
	/**
	 * 初始化基础控件
	 * @return
	 */
	private void initView(){
		TextView title = (TextView) findViewById(R.id.tv_title_text);
		title.setText("离线数据下载");
		findViewById(R.id.iv_title_online).setVisibility(View.GONE);
		findViewById(R.id.iv_title_back).setOnClickListener(this);
		progressBar=(ProgressBar) findViewById(R.id.pb_init);
		mListView = (ListView)this.findViewById(R.id.dataNameListView);
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
			progressBar.setVisibility(View.GONE);
			msg = "初始化完毕";
			//设置下载列表信息
			OfflineRecord[] datas = mOfflineDataManager.getOfflineRecords();
			if(datas!=null){
				//设置listView数据源
				DownloadAdapter downloadAdapter = new DownloadAdapter(this, datas);
				mListView.setAdapter(downloadAdapter);
			}
			break;
		case OfflineDataManager.Event.START:
			item = (OfflineRecordInfo)data;
			msg = item.getDataId()+"开始下载";
			break;
		case OfflineDataManager.Event.UPDATE:
			item = (OfflineRecordInfo)data;
			msg = item.getDataId()+"修改";
			break;
		case OfflineDataManager.Event.PAUSE:
			item = (OfflineRecordInfo)data;
			msg = "暂停"+item.getDataId();
			break;
		case OfflineDataManager.Event.REMOVE:
			item = (OfflineRecordInfo)data;
			msg = "删除"+item.getDataId();
			break;
		case OfflineDataManager.Event.FAIL:
			switch(err){
			case OfflineDataManager.Error.NONE:
				//无错误
				break;
			case OfflineDataManager.Error.NET_ERROR:
				//网络异常
				msg = "网络异常";
				Toast.makeText(DownloadActivity.this, msg, Toast.LENGTH_SHORT).show()	;
				break;
			case OfflineDataManager.Error.SDCARD_ERROR:
				//访问异常
				msg = "没有sdcard";
				Toast.makeText(DownloadActivity.this, msg, Toast.LENGTH_SHORT).show()	;
				break;
			}
			break;
		}
		if(msg!=null){
			Toast.makeText(DownloadActivity.this, msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.iv_title_back:
			// 返回
			finish();
		}
	}
	
	@Override
	protected void onDestroy() {
		//销毁下载管理实例
		mOfflineDataManager.onDestroy();
		super.onDestroy();
	}
}
