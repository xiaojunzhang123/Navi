package com.mapbar.moo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
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
import android.widget.Toast;

import com.mapbar.map.Annotation;
import com.mapbar.map.CalloutStyle;
import com.mapbar.map.CircleOverlay;
import com.mapbar.map.CustomAnnotation;
import com.mapbar.map.MapRenderer;
import com.mapbar.map.Overlay;
import com.mapbar.map.PolygonOverlay;
import com.mapbar.map.PolylineOverlay;
import com.mapbar.map.Vector2DF;
import com.mapbar.moo.resource.Config;
import com.mapbar.moo.resource.DemoMapView;
import com.mapbar.moo.resource.MessageBox;

/**
 * 叠加层/物
 * 
 * @author malw
 * 
 */
public class OverlayActivity extends Activity implements OnClickListener{

	//地图控制类
	private DemoMapView mDemoMapView;
	//地图渲染
	private MapRenderer mRenderer;
	
	private Point centerPoint = Config.centerPoint;
	
	private ImageView mZoomInImageView = null;
	private ImageView mZoomOutImageView = null;
	private ImageView cb_hotel;
	private ImageView cb_gas;
	private ImageView cb_road;
	private ImageView radio;

	// 输出调试信息
	private final static String TAG = "[OverlayActivity]";
	
	// 存储酒店叠加层的集合
	private List<CustomAnnotation> hlist = new ArrayList<CustomAnnotation>();
	// 存储加油站叠加层结合
	private List<CustomAnnotation> glist = new ArrayList<CustomAnnotation>();
	// 点线面圈
	private List<Overlay> olist = new ArrayList<Overlay>();
	// 气泡
	private List<Annotation> alist = new ArrayList<Annotation>();
	
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_overlay);
		// 初始化控件
		initView();
		init();
	}

	private void init () {
		try {
			if (Config.DEBUG) {
				Log.d(TAG, "Before - Initialize the GLMapRenderer Environment");
			}
			// 加载地图
			mDemoMapView = (DemoMapView) findViewById(R.id.glView_overlay);
			mDemoMapView.setZoomHandler(handler);
		} catch (Exception e) {
			if (Config.DEBUG) {
				e.printStackTrace();
			}
			new MessageBox(this, false).showDialog(e.getMessage());
		}
	}

	/**
	 * 用于接收GLMapRenderer加载完的消息
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
				// 设置地图显示级别
				mRenderer.setZoomLevel(10);
				break;
			case 100:
				//监听地图缩放 修改按钮状态
				Bundle b = msg.getData();
				mZoomInImageView.setEnabled(b.getBoolean("zoomIn"));
				mZoomOutImageView.setEnabled(b.getBoolean("zoomOut"));
				break;
			}
		}
	};

	/**
	 * 初始化控件
	 */
	private void initView () {
		// 绑定控件
		TextView title = (TextView) findViewById(R.id.tv_title_text);
		title.setText("叠加层/物");
		findViewById(R.id.iv_title_online).setVisibility(View.GONE);
		cb_hotel = (ImageView) findViewById(R.id.cb_hotel); // 酒店
		cb_gas = (ImageView) findViewById(R.id.cb_gas); // 加油站
		cb_road = (ImageView) findViewById(R.id.cb_road); // 实时路况
		radio = (ImageView) findViewById(R.id.radio_overlay); // 点线面
		mZoomInImageView = (ImageView) findViewById(R.id.btn_zoom_in); // 地图放大按钮
		mZoomOutImageView = (ImageView) findViewById(R.id.btn_zoom_out); // 地图缩小按钮
		// 注册点击事件监听
		cb_hotel.setOnClickListener(this);
		cb_gas.setOnClickListener(this);
		cb_road.setOnClickListener(this);
		radio.setOnClickListener(this);
		mZoomInImageView.setOnClickListener(this);
		mZoomOutImageView.setOnClickListener(this);
		findViewById(R.id.iv_title_back).setOnClickListener(this);
	}

	/**
	 * 点击事件监听
	 */
	@Override
	public void onClick (View v) {
		switch (v.getId()) {
		case R.id.cb_hotel:
			// 酒店
			if (cb_hotel.getTag() != null && Integer.parseInt(cb_hotel.getTag().toString()) == 1) {
				// 显示叠加层
				chechBg(true);
				radio.setTag(1);
				radio.setBackgroundResource(R.drawable.unradio);
				cb_hotel.setTag(0);
				cb_hotel.setBackgroundResource(R.drawable.check);
				addPoint(hlist, R.drawable.hotel, 7, 10000);
			} else {
				// 隐藏叠加层
				cb_hotel.setTag(1);
				cb_hotel.setBackgroundResource(R.drawable.uncheck);
				for (CustomAnnotation info : hlist) {
					info.setHidden(true);
				}
				hlist.clear();
			}
			break;
		case R.id.cb_gas:
			// 加油站
			if (cb_gas.getTag() != null && Integer.parseInt(cb_gas.getTag().toString()) == 1) {
				// 显示叠加层
				chechBg(true);
				radio.setTag(1);
				radio.setBackgroundResource(R.drawable.unradio);
				cb_gas.setTag(0);
				cb_gas.setBackgroundResource(R.drawable.check);
				addPoint(glist, R.drawable.gas, 7, 20000);
			} else {
				// 隐藏叠加层
				cb_gas.setTag(1);
				cb_gas.setBackgroundResource(R.drawable.uncheck);
				for (CustomAnnotation info : glist) {
					info.setHidden(true);
				}
				glist.clear();
			}
			break;
		case R.id.cb_road:
			// 实时路况
			if (cb_road.getTag() != null && Integer.parseInt(cb_road.getTag().toString()) == 1) {
				if(!mDemoMapView.isOpenNet()){
					Toast.makeText(this, "实时路况需要连接网络", Toast.LENGTH_SHORT).show();
				}else{
					chechBg(true);
					radio.setTag(1);
					radio.setBackgroundResource(R.drawable.unradio);
					cb_road.setTag(0);
					cb_road.setBackgroundResource(R.drawable.check);
					mRenderer.enableTmc(true);
				}
			} else {
				cb_road.setTag(1);
				cb_road.setBackgroundResource(R.drawable.uncheck);
				mRenderer.enableTmc(false);
			}
			break;
		case R.id.radio_overlay:
			// 点线面
			chechBg(false);
			cb_hotel.setTag(1);
			cb_hotel.setBackgroundResource(R.drawable.uncheck);
			cb_gas.setTag(1);
			cb_gas.setBackgroundResource(R.drawable.uncheck);
			cb_road.setTag(1);
			cb_road.setBackgroundResource(R.drawable.uncheck);
			if (radio.getTag() != null && Integer.parseInt(radio.getTag().toString()) == 1) {
				radio.setTag(0);
				radio.setBackgroundResource(R.drawable.radio);
				Point point = new Point(centerPoint.x-300, centerPoint.y+1000);//创建点对象
				// 点
				CircleOverlay circle = new CircleOverlay(point, 0f);//创建以point为中心的一个点
				mRenderer.addOverlay(circle);//将点绘制在地图上
				olist.add(circle);
				// 气泡
				Vector2DF pivot = new Vector2DF(0.5f, 0.0f);//设置气泡在点上的偏移量
				Annotation an = new Annotation(1, point, 1101, pivot);//在点point上创建一个偏移量为pivot的气泡
				CalloutStyle calloutStyle = an.getCalloutStyle();//获取气泡样式
				//calloutStyle.leftIcon = 0;//气泡左侧的图标为空
				calloutStyle.rightIcon = 1001;//气泡右侧的图标为空
				an.setCalloutStyle(calloutStyle);//为气泡设置显示样式
				an.setTitle("自定义POI点");//为气泡设置显示标题内容
				mRenderer.addAnnotation(an);//添加气泡an到MapRenderer对象
				an.showCallout(true);//设置气泡可显示
				alist.add(an);
				// 圈
				CircleOverlay circle2 = new CircleOverlay(new Point(centerPoint.x-1000,
						centerPoint.y), 400f);//创建一个圆，半径为400毫米
				circle2.setColor(0x00FFFFFF); // 颜色
				circle2.setBorderColor(Color.BLUE);
				circle2.setLayer(1); // 显示层次
				mRenderer.addOverlay(circle2);//
				olist.add(circle2);
				// 线
				PolylineOverlay pl1 = new PolylineOverlay(new Point[] {
						new Point(centerPoint.x+1000, centerPoint.y),
						new Point(centerPoint.x-1000, centerPoint.y-800) }, false);
				pl1.setColor(0xffaa0000);
				pl1.setStrokeStyle(Overlay.StrokeStyle.solid);
				pl1.setWidth(5.0f);
				mRenderer.addOverlay(pl1);
				olist.add(pl1);
				// 面
				PolylineOverlay pl2 = new PolylineOverlay(new Point[] {
						new Point(centerPoint.x, centerPoint.y),
						new Point(centerPoint.x-800, centerPoint.y-1700),
						new Point(centerPoint.x+500, centerPoint.y-1120) }, true);
				pl2.setColor(0xffaa0000);
				pl2.setStrokeStyle(Overlay.StrokeStyle.l63);
				pl2.setWidth(10.0f);
				mRenderer.addOverlay(pl2);
				olist.add(pl2);
				// 多边形 透明
				Point[] points = new Point[] {
						new Point(centerPoint.x, 3998500),
						new Point(centerPoint.x+20, centerPoint.y+100),
						new Point(centerPoint.x+40, centerPoint.y),
						new Point(centerPoint.x+60, centerPoint.y+100),
						new Point(centerPoint.x+80, centerPoint.y),
						new Point(centerPoint.x+100, centerPoint.y),
						new Point(centerPoint.x+60, centerPoint.y+150),
						new Point(centerPoint.x+20, centerPoint.y+150) };
				PolygonOverlay polygonOverlay = new PolygonOverlay(points);
				polygonOverlay.setColor(0xaaffff00);
				mRenderer.addOverlay(polygonOverlay);
				olist.add(polygonOverlay);
			} else {
				radio.setTag(1);
				radio.setBackgroundResource(R.drawable.unradio);
				mRenderer.enableTmc(false);
				for (Overlay info : olist) {
					info.setHidden(true);
				}
				for (Annotation info : alist) {
					info.showCallout(false);
				}
			}
			break;
		case R.id.iv_title_back:
			// 返回
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
	 * 根据参数随机打点
	 */
	public void addPoint (List<CustomAnnotation> list, int vid, int count,
			int start) {
		Vector2DF pivot = new Vector2DF(0.5f, 0.82f);
		// 随机添加7个点
		for (int i = 0; i < count; i++) {
			Random rand = new Random();
			int num1 = (rand.nextInt(200) + 1) * 10;
			int num2 = (rand.nextInt(200) + 1) * 10;
			CustomAnnotation mCustomAnnotation = new CustomAnnotation(2,
					new Point(centerPoint.x - num1, centerPoint.y - num2),
					// 此参数为气泡id 不能重复
					start + i, pivot, BitmapFactory.decodeResource(
							getResources(), vid));
			mCustomAnnotation.setClickable(true);
			mCustomAnnotation.setSelected(true);
			mCustomAnnotation.setTitle("--(" + i + ")--");
			mRenderer.addAnnotation(mCustomAnnotation);
			list.add(mCustomAnnotation);
		}
	}

	private void chechBg(boolean ischeck){
		if(ischeck){
			for (Overlay info : olist) {
				info.setHidden(true);
			}
			olist.clear();
			for (Annotation info : alist) {
				info.showCallout(false);
			}
			alist.clear();
		}else{
			for (CustomAnnotation info : hlist) {
				info.setHidden(true);
			}
			hlist.clear();
			for (CustomAnnotation info : glist) {
				info.setHidden(true);
			}
			glist.clear();
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
