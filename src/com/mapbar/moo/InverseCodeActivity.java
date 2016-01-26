package com.mapbar.moo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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

import com.mapbar.map.Annotation;
import com.mapbar.map.CalloutStyle;
import com.mapbar.map.MapRenderer;
import com.mapbar.map.Vector2DF;
import com.mapbar.moo.resource.Config;
import com.mapbar.moo.resource.DemoMapView;
import com.mapbar.moo.resource.MessageBox;
import com.mapbar.poiquery.PoiQuery;
import com.mapbar.poiquery.PoiQueryInitParams;
import com.mapbar.poiquery.ReverseGeocoder;
import com.mapbar.poiquery.ReverseGeocoder.EventHandler;
import com.mapbar.poiquery.ReverseGeocoderDetail;

/**
 * 逆地理编码
 * 
 * @author malw
 * 
 */
public class InverseCodeActivity extends Activity implements OnClickListener, EventHandler {

	private DemoMapView mDemoMapView;
	private MapRenderer mRenderer;
	private ReverseGeocoder mReverseGeocoder;
	private TextView mSearchTextView;
	private TextView mLatLon;
	private ImageView mZoomInImageView = null;
	private ImageView mZoomOutImageView = null;
	private ProgressDialog mDialog;
	private Point mPoint = Config.inverSePoint;
	//在线离线切换
	private Button mBtnOnline;
	//在线或离线
	private boolean online = Config.online;
	// 调试信息
	private final static String TAG = "[InverseCodeActivity]";

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_inverse_code);
		// 初始化控件
		initView();
		// 初始化引擎
		init();
	}

	/**
	 * 初始化控件
	 */
	public void initView () {
		TextView title = (TextView) findViewById(R.id.tv_title_text);
		title.setText("逆地理编码");
		mBtnOnline = (Button)findViewById(R.id.iv_title_online);
		mBtnOnline.setText(Config.getOnlineText(online));
		mBtnOnline.setOnClickListener(this);
		findViewById(R.id.iv_title_back).setOnClickListener(this);
		mSearchTextView = (TextView) findViewById(R.id.btn_inverse_search); // 搜索按钮
		mLatLon = (TextView)findViewById(R.id.tv_latlon);
		mLatLon.setText(mPoint.x+","+mPoint.y);
		mZoomInImageView = (ImageView) findViewById(R.id.btn_zoom_in); // 地图放大按钮
		mZoomOutImageView = (ImageView) findViewById(R.id.btn_zoom_out); // 地图缩小按钮

		mSearchTextView.setOnClickListener(this); // 搜索按钮
		mZoomInImageView.setOnClickListener(this); // 地图放大按钮
		mZoomOutImageView.setOnClickListener(this); // 地图按钮按钮
	}

	private void init () {
		// 初始化POI搜索引擎
		if (Config.DEBUG) {
			Log.d(TAG, "Before - Initialize the POIQuery Environment");
		}
		try {
			PoiQueryInitParams param = new PoiQueryInitParams();
			PoiQuery.getInstance().init(param);
			mReverseGeocoder = new ReverseGeocoder(this);
			mReverseGeocoder.setMode(online?ReverseGeocoder.Mode.online:ReverseGeocoder.Mode.offline);
		} catch (Exception e) {
			// ！！！此處應該添加保護，如果初始化不成功那麼後續使用PoiQuery相關功能將崩潰！！！
			if (Config.DEBUG) {
				Log.e(TAG,
						"Error on initializing the PoiQuery Environment -> Reason: "
								+ e.getMessage());
			}
			new MessageBox(this, false).showDialog(e.getMessage());
		}
		if (Config.DEBUG) {
			Log.d(TAG, "After - Initialize the POIQuery Environment");
		}
		try {
			if (Config.DEBUG) {
				Log.d(TAG, "Before - Initialize the GLMapRenderer Environment");
			}
			// 加载地图
			mDemoMapView = (DemoMapView) findViewById(R.id.glView_inverse);
			mDemoMapView.setZoomHandler(handler);
		} catch (Exception e) {
			e.printStackTrace();
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
				mRenderer.setDataMode(online?MapRenderer.DataMode.online:MapRenderer.DataMode.offline);
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

	@Override
	public void onClick (View v) {
		switch (v.getId()) {
		case R.id.iv_title_back:
			finish();
			break;
		case R.id.iv_title_online:
			if(online){
				online = false;
				mReverseGeocoder.setMode(ReverseGeocoder.Mode.offline);
				mRenderer.setDataMode(MapRenderer.DataMode.offline);
			}else{
				online =  true;
				mReverseGeocoder.setMode(ReverseGeocoder.Mode.online);
				mRenderer.setDataMode(MapRenderer.DataMode.online);
			}
			mBtnOnline.setText(Config.getOnlineText(online));
			break;
		case R.id.btn_inverse_search:
			if(mReverseGeocoder.getMode() == PoiQuery.Mode.offline || mDemoMapView.isOpenNet()){
				progressDialog("计算中，请稍后……");
				mReverseGeocoder.start(mPoint);
			}else{
				Toast.makeText(this, "请连接网络", Toast.LENGTH_SHORT).show();
			}
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
	 * 逆地理回调函数
	 */
	@Override
	public void onReverseGeoRequest(ReverseGeocoder geocoder, int event, int err, java.lang.Object userData) {
		switch(event){
		case ReverseGeocoder.Event.none:
			break;
		case ReverseGeocoder.Event.failed:
			if (mDialog != null) {
				mDialog.dismiss();
			}
			String msg = null;
			switch(err){
			case ReverseGeocoder.Error.netError:
				msg = "网络错误";
				break;
			case ReverseGeocoder.Error.noData:
				msg = "没有本地离线数据";
				break;
			case ReverseGeocoder.Error.none:
				break;
			case ReverseGeocoder.Error.noResult:
				msg = "无搜索结果";
				break;
			case ReverseGeocoder.Error.noPermission:
				msg = "没有权限";
				break;
			}
			if(msg!=null){
				Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
			}
			break;
		case ReverseGeocoder.Event.started:
			break;
		case ReverseGeocoder.Event.succeeded :
			ReverseGeocoderDetail result = mReverseGeocoder.getResult();
			// 设置气泡
			Vector2DF pivot = new Vector2DF(0.5f, 0.0f);
			Annotation an = new Annotation(1, result.pos, 1101, pivot);
			CalloutStyle calloutStyle = an.getCalloutStyle();
			//calloutStyle.leftIcon = 0;
			calloutStyle.rightIcon = 1001;
			an.setCalloutStyle(calloutStyle);
			an.setTitle(result.poiName);
			mRenderer.addAnnotation(an);
			an.showCallout(true);
			// 设置中心点
			mRenderer.setWorldCenter(result.pos);
			mDemoMapView.setCarPosition(result.pos);
			if (mDialog != null) {
				mDialog.dismiss();
			}
			break;
		}
	}

	/**
	 * 加载等待
	 */
	private void progressDialog (String msg) {
		mDialog = new ProgressDialog(this);
		mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mDialog.setMessage(msg);
		mDialog.setIndeterminate(false);
		mDialog.setCancelable(true);
		mDialog.show();
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
        PoiQuery.getInstance().cleanup();
        // 其它资源的清理
        // ...
	}
}
