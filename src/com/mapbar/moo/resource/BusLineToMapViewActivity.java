package com.mapbar.moo.resource;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbar.bus.BusLine;
import com.mapbar.bus.BusRoute;
import com.mapbar.bus.BusSegment;
import com.mapbar.bus.BusWalkSegment;
import com.mapbar.map.Annotation;
import com.mapbar.map.CalloutStyle;
import com.mapbar.map.CustomAnnotation;
import com.mapbar.map.MapRenderer;
import com.mapbar.map.Overlay;
import com.mapbar.map.PolylineOverlay;
import com.mapbar.map.Vector2DF;
import com.mapbar.moo.BusActivity;
import com.mapbar.moo.R;

/**
 * 在地图上画公交路线
 * 
 * @author malw
 * 
 */
public class BusLineToMapViewActivity extends Activity implements OnClickListener {

	//地图控制类
	private DemoMapView mDemoMapView;
	//地图渲染
	private MapRenderer mRenderer;
	// 地图放大缩小控件
	public ImageView mZoomInImageView = null;
	private ImageView mZoomOutImageView = null;
	//打点
	public static int mAnnotPoint = 4;
	public static int mSegment = 5;
	//管理类
	private NaviManager mNaviManager;
	int xMin = 0,yMin = 0,xMax = 0,yMax = 0,rindex = 0;
	// For Debugging
	private final static String TAG = "[BusLineToMapViewActivity]";
	
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_busline_mapview);
		mNaviManager = NaviManager.getNaviManager();
		//初始化控件
		initView();
		//初始化地图
		initMap();
	}

	private void initMap () {
		try {
			if (Config.DEBUG) {
				Log.d(TAG, "Before - Initialize the GLMapRenderer Environment");
			}
			// 加载地图
			mDemoMapView = (DemoMapView) findViewById(R.id.glView_mapview);
			mDemoMapView.setZoomHandler(handler);
		} catch (Exception e) {
			e.printStackTrace();
			new MessageBox(this, false).showDialog(e.getMessage());
		}
	}
	
	/**
	 * 初始化数据
	 */
	private void initData(){
		Bundle b = getIntent().getExtras();
		int type = b.getInt("SearchBusType");
		if(type == BusActivity.busTran){
			//换乘
			BusRoute br = mNaviManager.getmResultBusRoute();
			showBusTranRoute(br);
		}else if(type == BusActivity.busRoad || type == BusActivity.busStation){
			//站点  线路
			BusLine bl = mNaviManager.getmResultBusLine();
			showOtherRoute(bl);
		}else if(type == mAnnotPoint){
			//打气泡
			int lat = b.getInt("lat");
			int lon = b.getInt("lon");
			String name = b.getString("name");
			showAnnotation(new Point(lat,lon),name);
		}else if(type == mSegment){
			//显示路线片段
			int routeType = b.getInt("routeType");
			showChildSe(routeType,mNaviManager.getmResultPoints());
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
				mRenderer.setDataMode(Config.online?MapRenderer.DataMode.online:MapRenderer.DataMode.offline);
				initData();
				break;
			case 100:
				//监听地图缩放 修改按钮状态
				Bundle b = msg.getData();
				mZoomInImageView.setEnabled(b.getBoolean("zoomIn"));
				mZoomOutImageView.setEnabled(b.getBoolean("zoomOut"));
				break;
			case R.id.btn_zoom_in:
				// 放大地图操作
				mDemoMapView.mapZoomIn(mZoomInImageView, mZoomOutImageView);
				break;
			case R.id.btn_zoom_out:
				// 地图缩小
				mDemoMapView.mapZoomOut(mZoomInImageView, mZoomOutImageView);
				break;
			}
		}
	};

	/**
	 * 初始化控件
	 */
	private void initView () {
		TextView title = (TextView) findViewById(R.id.tv_title_text);
		title.setText("公交路线");
		findViewById(R.id.iv_mapview).setVisibility(View.GONE);
		findViewById(R.id.iv_title_online).setVisibility(View.GONE);
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
		case R.id.iv_title_back:
			finish();
			break;
		case R.id.btn_zoom_in:
			// 地图放大
			mDemoMapView.mapZoomIn(mZoomInImageView, mZoomOutImageView);
			break;
		case R.id.btn_zoom_out:
			// 地图缩小
			mDemoMapView.mapZoomOut(mZoomInImageView, mZoomOutImageView);
			break;
		}
	}
	/**
	 * 打气泡
	 * @param point
	 * @param name
	 */
	private void showAnnotation(Point point,String name){
		Vector2DF pivot = new Vector2DF(0.5f, 0.0f);
		Annotation an = new Annotation(1, point, 1101, pivot);
		CalloutStyle calloutStyle = an.getCalloutStyle();
		//calloutStyle.leftIcon = 0;
		calloutStyle.rightIcon = 1001;
		an.setCalloutStyle(calloutStyle);
		an.setTitle(name);
		mRenderer.addAnnotation(an);
		an.showCallout(true);
		// 设置中心点
		mRenderer.setWorldCenter(point);
	}
	
	/**
	 * 显示路段信息
	 * @param route
	 */
	private void showBusTranRoute(BusRoute route){
		Point[] ps;
		int ptNum = 0;
		for (int i = 0; i < route.segments.length; i++) {
			if(route.segments[i].type == BusLine.Type.bus | route.segments[i].type == BusLine.Type.subway) {
				ptNum += ((BusSegment)route.segments[i]).points.length;
			} else if (route.segments[i].type == BusLine.Type.walk) {
				ptNum += ((BusWalkSegment)route.segments[i]).points.length;
			}
		}
		ps = new Point[ptNum];
		ptNum=0;
		List<BusPoint> bus = new ArrayList<BusPoint>();
		xMin=yMin=xMax=yMax=rindex=0;
		for (int i = 0; i < route.segments.length; i++) {
			if(route.segments[i].type == BusLine.Type.subway) {
				Point[] points = ((BusSegment)route.segments[i]).points;
				for (int j = 0; j < points.length; j++) {
					ps[ptNum] = points[j];
					rectPoint(points[j]);
					ptNum ++;
				}
				bus.add(new BusPoint(1,points));
			} else if(route.segments[i].type == BusLine.Type.bus){
				Point[] points = ((BusSegment)route.segments[i]).points;
				for (int j = 0; j < points.length; j++) {
					ps[ptNum] = points[j];
					rectPoint(points[j]);
					ptNum ++;
				}
				bus.add(new BusPoint(2,points));
			}else if (route.segments[i].type == BusLine.Type.walk) {
				Point[] points = ((BusWalkSegment)route.segments[i]).points;
				for (int j = 0; j < points.length; j++) {
					ps[ptNum] = points[j];
					rectPoint(points[j]);
					ptNum ++;
				}
				bus.add(new BusPoint(3,points));
			}
		}
		
		PolylineOverlay mBusQueryOverlay = null;
		Vector2DF pivot = new Vector2DF(0.5f, 0.82f);
		int index = 1,res=0;
		Point sepoint = null;
		for(BusPoint info : bus){
			if(info.getPoints()==null || info.getPoints().length < 1)
				return;
			if(sepoint != null){
				//将近地铁通道里的路线用步行画出来
				Point[] tmppoint = new Point[2];
				tmppoint[0] = sepoint;
				tmppoint[1] = info.getPoints()[0];
				mBusQueryOverlay = new PolylineOverlay(tmppoint, false);
				mBusQueryOverlay.setStrokeStyle(Overlay.StrokeStyle.solidWithButt);
				mBusQueryOverlay.setColor(0xff7ae295);
				mBusQueryOverlay.setOutlineColor(0xff34964d);
				mBusQueryOverlay.setWidth(5.0f);
				res = R.drawable.img_walk;
				mRenderer.addOverlay(mBusQueryOverlay);
//				mNaviManager.getPolyLineList().add(mBusQueryOverlay);
			}
			mBusQueryOverlay = new PolylineOverlay(info.getPoints(), false);
			sepoint = new Point(info.getPoints()[info.getPoints().length-1]);
			
			if(info.getType() == 1){//地铁
//				boolean flag = mBusQueryOverlay.setStyleClass("subway");
				mBusQueryOverlay.setStrokeStyle(Overlay.StrokeStyle.railway);
				mBusQueryOverlay.setColor(0xff4aa1cf);
				mBusQueryOverlay.setOutlineColor(0xff37579c);
				mBusQueryOverlay.setWidth(9.0f);
				res = R.drawable.img_subway;
			}else if(info.getType() == 2){//公交
//				boolean flag = mBusQueryOverlay.setStyleClass("bus");
				mBusQueryOverlay.setStrokeStyle(Overlay.StrokeStyle.route);
				mBusQueryOverlay.setColor(0xff67b9e4);
				mBusQueryOverlay.setOutlineColor(0xff2d83b0);
				mBusQueryOverlay.setWidth(9.0f);
				res = R.drawable.img_bus;
			}else if(info.getType() == 3){//步行
//				boolean flag = mBusQueryOverlay.setStyleClass("walk");
				mBusQueryOverlay.setStrokeStyle(Overlay.StrokeStyle.solidWithButt);
				mBusQueryOverlay.setColor(0xff7ae295);
				mBusQueryOverlay.setOutlineColor(0xff34964d);
				mBusQueryOverlay.setWidth(5.0f);
				res = R.drawable.img_walk;
			}
			
			mRenderer.addOverlay(mBusQueryOverlay);
			CustomAnnotation mCustomAnnotation = new CustomAnnotation(1,
					info.getPoints()[0],
					index, pivot, BitmapFactory.decodeResource(
							getResources(), res));
			mCustomAnnotation.setClickable(false);
			mCustomAnnotation.setSelected(false);
			mRenderer.addAnnotation(mCustomAnnotation);
//			mNaviManager.getCustomAnnotList().add(mCustomAnnotation);
			index++;
		}
//		delRouteByBus();
		
		Rect area = new Rect(xMin, yMin, xMax, yMax);
//		mNaviManager.setmRect(area);
		mRenderer.fitWorldArea(area);
//		mRenderer.setWorldCenter(bus.get(0).getPoints()[0]);
	}
	
	private void showOtherRoute(BusLine busline){
		xMin=yMin=xMax=yMax=rindex=0;
		PolylineOverlay mBusQueryOverlay = new PolylineOverlay(busline.stationPositions, false);
		Vector2DF pivot = new Vector2DF(0.5f, 0.82f);
		if(busline.type == 1){//地铁
//				boolean flag = mBusQueryOverlay.setStyleClass("subway");
//				System.out.println(flag);
			mBusQueryOverlay.setStrokeStyle(Overlay.StrokeStyle.railway);
			mBusQueryOverlay.setColor(0xff4aa1cf);
			mBusQueryOverlay.setOutlineColor(0xff37579c);
			mBusQueryOverlay.setWidth(9.0f);
		}else if(busline.type == 2){//公交
//				boolean flag = mBusQueryOverlay.setStyleClass("bus");
//				System.out.println(flag);
			mBusQueryOverlay.setStrokeStyle(Overlay.StrokeStyle.route);
			mBusQueryOverlay.setColor(0xff67b9e4);
			mBusQueryOverlay.setOutlineColor(0xff2d83b0);
			mBusQueryOverlay.setWidth(9.0f);
		}
		mRenderer.addOverlay(mBusQueryOverlay);
		
		int res=0;
		for(int i=0;i<busline.stationNames.length;i++){
			String bs = busline.stationNames[i];
			rectPoint(busline.stationPositions[i]);
			if(busline.type == 1){//地铁
				res = R.drawable.img_subway;
			}else if(busline.type == 2){//公交
				res = R.drawable.img_bus;
			}
			CustomAnnotation mCustomAnnotation = new CustomAnnotation(1,
					busline.stationPositions[i],
					i, pivot, BitmapFactory.decodeResource(
							getResources(), res));
			mCustomAnnotation.setTitle(bs);
			mCustomAnnotation.setClickable(true);
			mCustomAnnotation.setSelected(false);
			mRenderer.addAnnotation(mCustomAnnotation);
		}
		Rect area = new Rect(xMin, yMin, xMax, yMax);
		mRenderer.fitWorldArea(area);
	}
	
	/**
	 * 显示路线详情片段
	 * @param index
	 */
	private void showChildSe(int type,Point[] points){
		//重新铺路
		PolylineOverlay mBusQueryOverlay = null;
		Vector2DF pivot = new Vector2DF(0.5f, 0.82f);
		mBusQueryOverlay = new PolylineOverlay(points, false);
		int res=0;
		if(type == 1){//地铁
//			boolean flag = mBusQueryOverlay.setStyleClass("subway");
			mBusQueryOverlay.setStrokeStyle(Overlay.StrokeStyle.railway);
			mBusQueryOverlay.setColor(0xff4aa1cf);
			mBusQueryOverlay.setOutlineColor(0xff37579c);
			mBusQueryOverlay.setWidth(10.0f);
			res = R.drawable.img_subway;
		}else if(type == 2){//公交
//			boolean flag = mBusQueryOverlay.setStyleClass("bus");
			mBusQueryOverlay.setStrokeStyle(Overlay.StrokeStyle.route);
			mBusQueryOverlay.setColor(0xff67b9e4);
			mBusQueryOverlay.setOutlineColor(0xff2d83b0);
			mBusQueryOverlay.setWidth(10.0f);
			res = R.drawable.img_bus;
		}else if(type == 3){//步行
//			boolean flag = mBusQueryOverlay.setStyleClass("walk");
			mBusQueryOverlay.setStrokeStyle(Overlay.StrokeStyle.solidWithButt);
			mBusQueryOverlay.setColor(0xff7ae295);
			mBusQueryOverlay.setOutlineColor(0xff34964d);
			mBusQueryOverlay.setWidth(5.0f);
			res = R.drawable.img_walk;
		}
		mRenderer.addOverlay(mBusQueryOverlay);
//		mNaviManager.getPolyLineList().add(mBusQueryOverlay);
		
		CustomAnnotation mCustomAnnotation = new CustomAnnotation(1,
				points[0],
				1, pivot, BitmapFactory.decodeResource(
						getResources(), res));
		mCustomAnnotation.setClickable(false);
		mCustomAnnotation.setSelected(false);
		mRenderer.addAnnotation(mCustomAnnotation);
		xMin=yMin=xMax=yMax=rindex=0;
		for(Point info : points){
			rectPoint(info);
		}
		Rect area = new Rect(xMin, yMin, xMax, yMax);
		mRenderer.fitWorldArea(area);
	}
	
	/**
	 * 获取路线最大最小坐标
	 * @param point
	 */
	private void rectPoint(Point point){
		if (rindex == 0){
			rindex = 1;
			xMin = point.x;
			xMax = point.x;
			yMin = point.y;
			yMax = point.y;
		} else{
			xMin = Math.min(xMin, point.x);
			xMax = Math.max(xMax, point.x);
			yMin = Math.min(yMin, point.y);
			yMax = Math.max(yMax, point.y);
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
