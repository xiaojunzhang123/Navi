package com.mapbar.moo;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbar.map.MapRenderer;
import com.mapbar.map.Overlay;
import com.mapbar.map.PolylineOverlay;
import com.mapbar.moo.resource.Config;
import com.mapbar.moo.resource.DemoMapView;
import com.mapbar.moo.resource.MessageBox;

/**
 * 地图视图
 * 
 * @author malw
 * 
 */
public class MapViewActivity extends Activity implements OnClickListener {

	//地图控制类
	private DemoMapView mDemoMapView;
	//地图渲染
	private MapRenderer mRenderer;
	// 地图放大缩小控件
	public ImageView mZoomInImageView = null;
	private ImageView mZoomOutImageView = null;
	//在线离线切换
	private Button mBtnOnline;
	//在线或离线
	private boolean online = Config.online;
	// For Debugging
	private final static String TAG = "[MapViewActivity]";
	
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_mapview);
		//初始化地图
		initMap();
		//初始化控件
		initView();
	}

	private void initMap () {
		try {
			if (Config.DEBUG) {
				Log.d(TAG, "Before - Initialize the GLMapRenderer Environment");
			}
			// 加载地图
			mDemoMapView = (DemoMapView) findViewById(R.id.glView_mapview);
			mDemoMapView.setZoomHandler(handler);
			if(online&&!mDemoMapView.isOpenNet()){
				Toast.makeText(this, "请连接网络", Toast.LENGTH_SHORT	).show();
			}
		} catch (Exception e) {
			e.printStackTrace();
			new MessageBox(this, false).showDialog(e.getMessage());
		}
	}

	/**
	 * 用于接收DemoMapView的消息
	 */
	private Handler handler = new Handler() {
		@Override
		public void handleMessage (Message msg) {
			super.handleMessage(msg);
			switch(msg.what){
			case 1:
				//地图控件加载完毕
				mRenderer = mDemoMapView.getMapRenderer();
				mRenderer.setDataMode(online?MapRenderer.DataMode.online:MapRenderer.DataMode.offline);
				//加载卫星图
//				mRenderer.setSatellitePicProvider(MapRenderer.SatellitePicProvider.Bing);
//				mRenderer.enableSatelliteMap(true);
//				boolean flag = mRenderer.loadStyleSheet("res/map3ss_satellite.json");
				//取消卫星图
//				mRenderer.enableSatelliteMap(false);
//				boolean flag = mRenderer.loadStyleSheet("res/map3_style_sheet.json");
//				Log.i(TAG, "loadStyleSheet:"+flag);
				//加载影像图
//				mRenderer.setDataUrlPrefix(UrlType.satellite, "http://112.124.12.16/");
//				mRenderer.setSatellitePicProvider(MapRenderer.SatellitePicProvider.Default);
//				mRenderer.enableSatelliteMap(true);
//				boolean flag = mRenderer.loadStyleSheet("res/map3ss_satellite.json");
				
				//hauye 消除出现红线，
//				Point p1 = Config.NAVI_START_POINT;
//				Point p2 = Config.NAVI_END_POINT;
				break;
			case 100:
				//监听地图缩放 修改按钮状态
				Bundle b = msg.getData();
				mZoomInImageView.setEnabled(b.getBoolean("zoomOut"));
				mZoomOutImageView.setEnabled(b.getBoolean("zoomIn"));
				break;
			}
		}
	};
	
	private PolylineOverlay pl1 = null;
	
	/**
	 * 初始化控件
	 */
	private void initView () {
		TextView title = (TextView) findViewById(R.id.tv_title_text);
		title.setText("地图视图");
		mBtnOnline = (Button)findViewById(R.id.iv_title_online);
		mBtnOnline.setText(Config.getOnlineText(online));
		mBtnOnline.setOnClickListener(this);
		findViewById(R.id.btn_2dmap).setOnClickListener(this); // 正北朝上2D视图按钮
		findViewById(R.id.btn_3dmap).setOnClickListener(this); // 正北朝上3D视图按钮
		findViewById(R.id.btn_left2dmap).setOnClickListener(this); // 左转30度2D视图
		findViewById(R.id.btn_right3dmap).setOnClickListener(this); // 右转56度3D视图
		findViewById(R.id.btn_day).setOnClickListener(this); // 白天视图按钮
		findViewById(R.id.btn_night).setOnClickListener(this); // 夜晚视图按钮
		findViewById(R.id.iv_title_back).setOnClickListener(this); // 返回按钮
		mZoomInImageView = (ImageView) findViewById(R.id.btn_zoom_in); // 地图放大按钮
		mZoomOutImageView = (ImageView) findViewById(R.id.btn_zoom_out); // 地图缩小按钮
		mZoomInImageView.setOnClickListener(this);
		mZoomOutImageView.setOnClickListener(this);
	}

	/**
	 * 点击事件监听
	 */
	@Override
	public void onClick (View v) {
		switch (v.getId()) {
		case R.id.iv_title_online:
			if(online){
				online = false;
				mRenderer.setDataMode(MapRenderer.DataMode.offline);
			}else{
				online =  true;
				mRenderer.setDataMode(MapRenderer.DataMode.online);
			}
			mBtnOnline.setText(Config.getOnlineText(online));
			break;
		case R.id.btn_day:
			// 白天视图
			mRenderer.setStyleClass("DEFAULT");
			break;
		case R.id.btn_night:
			// 夜晚视图
			mRenderer.setStyleClass("night");
			break;
		case R.id.btn_2dmap:
			// 正北朝上2D视图
			mRenderer.beginAnimations();
			mRenderer.setHeading(0); // 设置角度 0 ~ 360
			mRenderer.setElevation(90); // 设置2d 3d角度
			mRenderer.commitAnimations(2000, MapRenderer.Animation.linear);
			break;
		case R.id.btn_3dmap:
			// 正北朝上3D视图
			mRenderer.beginAnimations();
			mRenderer.setHeading(0);
			mRenderer.setElevation(27.5f);
			mRenderer.commitAnimations(2000, MapRenderer.Animation.linear);
			break;
		case R.id.btn_left2dmap:
			// 左转35度2D视图
			mRenderer.beginAnimations();
			mRenderer.setHeading(35);
			mRenderer.setElevation(90);
			mRenderer.commitAnimations(2000, MapRenderer.Animation.linear);
			break;
		case R.id.btn_right3dmap:
			// 右转56度3D视图
			mRenderer.beginAnimations();
			mRenderer.setHeading(304);
			mRenderer.setElevation(27.5f);
			mRenderer.commitAnimations(2000, MapRenderer.Animation.linear);
			break;
		case R.id.btn_zoom_in:
			// 放大地图操作
			mDemoMapView.mapZoomIn(mZoomInImageView, mZoomOutImageView);
			// }
			break;
		case R.id.btn_zoom_out:
			// 地图缩小
			mDemoMapView.mapZoomOut(mZoomInImageView, mZoomOutImageView);
			// }
			break;
		case R.id.iv_title_back:
			finish();
			break;
		}
	}

	 @Override
	 public void onPause () {
         super.onPause();
         // 其他操作
         // 暂停地图
         if(mDemoMapView != null) {
        	 mDemoMapView.onPause();
         }
         // 其他操作
	 }
	
	 @Override
	 public void onResume () {
         super.onResume();
         // 其他操作
         // 恢复地图
         if (mDemoMapView != null) {
        	 mDemoMapView.onResume();
         }
         // 其他操作
	 }

	@Override
	public void onConfigurationChanged (Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * 销毁
	 */
	@Override
	public void onDestroy () {
		super.onDestroy();
        if (mDemoMapView != null) {
        	// 此Activity销毁时，销毁地图控件
        	mDemoMapView.onDestroy();
        }
        mDemoMapView = null;
        // 其它资源的清理
        // ...
	}
	
	
}
