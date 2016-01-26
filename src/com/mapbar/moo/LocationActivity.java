package com.mapbar.moo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbar.android.location.AsyncGeocoder;
import com.mapbar.android.location.AsyncGeocoder.ResultListener;
import com.mapbar.android.location.LocationClient;
import com.mapbar.android.location.LocationClientOption;
import com.mapbar.android.location.LocationClientOption.LocationMode;
import com.mapbar.android.location.QFAuthResultListener;

/**
 * 定位模块
 * 定位模块只有在线，没有离线功能
 * @author malw
 * 
 */
public class LocationActivity extends Activity implements View.OnClickListener, LocationListener, QFAuthResultListener {

	private static int priority = LocationMode.GPS_FIRST;
	private LocationClient mLocationClient;

	private final int LOCA_TYPE_INDEX = 1;
	private final int RES_TYPE_INDEX = 2;
	private final int INV_INFO_INDEX = 3;

	private CharSequence[] loca_type;// 定位选择类型集合
	private CharSequence[] res_type;// 返回结果类型
	private CharSequence[] inv_info;// 是否逆地理选择信息

	private TextView tv_location;// 输出结果
	private EditText cell_interval;// 基站定位间隔
	private EditText gps_interval;// GPS定位间隔
	private TextView local_type;// 定位选择
	private TextView export_type;// 输出结果
	private TextView inverse_type;// 逆地理信息
	private TextView local_count;// 定位次数
	private Button btn_local;// 开始定位按钮
	private ProgressBar loader; //等待框
	
	private boolean isInverse = true;
	private long gpsExpire = 1500;//gps失效时间 毫秒
	private int resultType = 0;
	private int count = 0;//定位次数

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_location);
		initView();
		initLocation();
	}

	/**
	 * 初始化控件
	 */
	public void initView () {
		TextView title = (TextView) findViewById(R.id.tv_title_text);
		title.setText("定位");
		findViewById(R.id.iv_title_online).setVisibility(View.GONE);
		findViewById(R.id.iv_title_back).setOnClickListener(this); // 返回按钮
		// 获取定位类型集合
		loca_type = getResources().getTextArray(R.array.loca_type);
		// 返回结果类型
		res_type = getResources().getTextArray(R.array.res_type);
		// 是否逆地理选择信息
		inv_info = getResources().getTextArray(R.array.inv_info);
		tv_location = (TextView) findViewById(R.id.tv_location);
		cell_interval = (EditText) findViewById(R.id.cell_interval);
		gps_interval = (EditText) findViewById(R.id.gps_interval);
		local_type = (TextView) findViewById(R.id.local_type);
		local_type.setOnClickListener(this);
		export_type = (TextView) findViewById(R.id.export_type);
		export_type.setOnClickListener(this);
		inverse_type = (TextView) findViewById(R.id.inverse_type);
		inverse_type.setOnClickListener(this);
		local_count = (TextView)findViewById(R.id.local_count);
		btn_local = (Button) findViewById(R.id.btn_local);
		btn_local.setOnClickListener(this);
		tv_location = (TextView) findViewById(R.id.tv_location);
		loader = (ProgressBar)findViewById(R.id.loader);
	}
	
	/**
	 * 初始化定位
	 */
	private void initLocation() {
		try {
			mLocationClient = new LocationClient(this,MainActivity.KEY,this);
			mLocationClient.enableDebug(true, "/mnt/sdcard/mapbar/log/");
			LocationClientOption option = new LocationClientOption();
			option.setPriority(priority);
			option.setScanSpanGPS(Long.parseLong(gps_interval.getText().toString()));//设置GPS定位最小间隔时间
			option.setGpsExpire(gpsExpire);//设置GPS定位失效时间
			option.setScanSpanNetWork(Long.parseLong(cell_interval.getText().toString()));//设置基站定位最小间隔时间
			option.setResultType(resultType);//默认返回逆地理信息
			mLocationClient.setOption(option);
			mLocationClient.addListener(this);
			System.out.println("-------开始定位---");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onClick (View v) {
		switch (v.getId()) {
		case R.id.btn_local:
			if(TextUtils.isEmpty(cell_interval.getText())){
				Toast.makeText(this, "请输入基站定位间隔", Toast.LENGTH_SHORT).show();
				return;
			}else if(TextUtils.isEmpty(gps_interval.getText())){
				Toast.makeText(this, "请输入GPS定位间隔", Toast.LENGTH_SHORT).show();
				return;
			}
			LocationClientOption option = new LocationClientOption();
			option.setPriority(priority);
			option.setScanSpanGPS(Long.parseLong(gps_interval.getText().toString()));//设置GPS定位最小间隔时间
			option.setScanSpanNetWork(Long.parseLong(cell_interval.getText().toString()));//设置基站定位最小间隔时间
			option.setResultType(resultType);
			mLocationClient.setOption(option);
			if("开始定位".equals(btn_local.getText().toString())){
				mLocationClient.start();
				loader.setVisibility(View.VISIBLE);
				btn_local.setText("停止定位");
			}else{
				if("重启定位".equals(btn_local.getText().toString())){
					mLocationClient.reStart();
					loader.setVisibility(View.VISIBLE);
					btn_local.setText("停止定位");
				}else{
					mLocationClient.stop();
					loader.setVisibility(View.GONE);
					btn_local.setText("重启定位");
				}
			}
			tv_location.setText("");
			break;
		case R.id.local_type:
			infoDialog(LOCA_TYPE_INDEX);
			break;
		case R.id.export_type:
			infoDialog(RES_TYPE_INDEX);
			break;
		case R.id.inverse_type:
			infoDialog(INV_INFO_INDEX);
			break;
		case R.id.iv_title_back:
			finish();
			break;
		}
	}
	
	@Override
	public void onLocationChanged(Location location) {
		System.out.println("收到定位反馈" + location.getProvider());
		if (location != null) {
			count++;
			local_count.setText(String.valueOf(count));
			Message msg = mLocationHandler.obtainMessage(1);
			msg.obj=location;
			mLocationHandler.sendMessage(msg);
		}
	}

	private Handler mLocationHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case 1:
				try {
					Location location = (Location) msg.obj;
					if (location != null) {
						double dLat = location.getLatitude();
						double dLon = location.getLongitude();
						final StringBuilder sb = new StringBuilder();
						boolean bGps = LocationManager.GPS_PROVIDER.equals(location.getProvider());
						sb.append("provider：")
						.append(bGps ? "GPS" : "Cell").append("\n");
						if(location.getExtras()!=null){
							if(!TextUtils.isEmpty(location.getExtras().getString("city"))){
								sb.append("city：")
								.append(location.getExtras().getString("city")).append("\n");
							}
							if(!TextUtils.isEmpty(location.getExtras().getString("address"))){
								sb.append("address：")
								.append(location.getExtras().getString("address")).append("\n");
							}
						}
						sb.append("LatLon：")
						.append(dLat).append(",").append(dLon).append("\n")
						.append("accuracy：")
						.append(location.getAccuracy()).append("\n")
						.append("time：")
						.append(printTimeStamp(location.getTime())).append("\n");
						tv_location.setText(sb.toString());
						if(isInverse){
							//是否逆地理
							getInverse(dLat, dLon);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					
				}
				break;
			}
		}
	};
	/**
	 * 逆地理 （新定位中带逆地理功能呢）
	 *  @Enclosing_Method  : getInverse
	 *  @Written by        : maliwei
	 *  @Creation Date     : 2014-8-18 下午02:37:07 
	 *  @version           : v1.00
	 *  @Description       :  
	 *  
	 *  @param dLat
	 *  @param dLon
	 *
	 */
	private void getInverse(double dLat,double dLon){
		try {
			AsyncGeocoder gc = new AsyncGeocoder(LocationActivity.this,
					Locale.getDefault());
			gc.setResultListener(new ResultListener() {
				@Override
				public void onResult(Object obj,List<Address> lstAddress) {
					StringBuilder sbGeo = new StringBuilder();
					int flag = Integer.parseInt(String.valueOf(obj));
					// 判断地址是否为多行
					if (lstAddress.size() > 0 && count==flag){
						for (int i = 0; i < lstAddress.size(); i++) {
							Address adsLocation = lstAddress.get(i);
							for (int j = 0; j <= adsLocation.getMaxAddressLineIndex(); j++) {
								sbGeo.append("address:"+ adsLocation.getAddressLine(j)).append("\n");
							}
							sbGeo.append("FeatureName:"+ adsLocation.getFeatureName()).append("\n");
							sbGeo.append("AdminArea:"+ adsLocation.getAdminArea()).append("\n");
							sbGeo.append("Phone:" + adsLocation.getPhone()).append("\n");
							sbGeo.append("Thoroughfare:"+ adsLocation.getThoroughfare()).append("\n");
							sbGeo.append("Locality:"+ adsLocation.getLocality()).append("\n");
							sbGeo.append("Country:"+ adsLocation.getCountryName()).append("\n");
							sbGeo.append("CountryCode:"+ adsLocation.getCountryCode()).append("\n");
							sbGeo.append("Latitude:"+ adsLocation.getLatitude()).append("\n");
							sbGeo.append("Longitude:"+ adsLocation.getLongitude()).append("\n");
						}
						Message msg = mHandler.obtainMessage();
						msg.obj = sbGeo.toString();
						mHandler.sendMessage(msg);
					}
				}
			});
			gc.setFlagObject(count);
			gc.getFromLocation(dLat, dLon, 1);
		} catch (Exception ex) {
			ex.printStackTrace();
			Toast.makeText(LocationActivity.this, "有异常", Toast.LENGTH_SHORT).show();
		}
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			tv_location.setText(tv_location.getText()+"\n"+msg.obj.toString());
		}
	};

	@Override
	public void onProviderDisabled(String provider) {
		System.out.println("----onProviderDisabled----" + provider);
	}

	@Override
	public void onProviderEnabled(String provider) {
		System.out.println("----onProviderEnabled----" + provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		System.out.println("----onStatusChanged----" + provider + "," + status
				+ "," + extras);
	}
	/**
	 * 根据类型选择提示框
	 * 
	 * @param type
	 */
	public void infoDialog(int type) {
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
		dialog.setContentView(R.layout.layout_location_setting_dialog);
		ListView content = (ListView) dialog
				.findViewById(R.id.option_dialog_content_listview);
		switch (type) {
		case LOCA_TYPE_INDEX:
			handlerSetDialog(type, loca_type, dialog, content);
			break;
		case RES_TYPE_INDEX:
			handlerSetDialog(type, res_type, dialog, content);
			break;
		case INV_INFO_INDEX:
			handlerSetDialog(type, inv_info, dialog, content);
			break;
		}
	}

	/**
	 * 提示框
	 * 
	 * @param type
	 * @param res
	 * @param dialog
	 * @param content
	 */
	private void handlerSetDialog(final int type, CharSequence[] res,
			final AlertDialog dialog, ListView content) {
		String[] ress = new String[res.length];
		for (int i = 0; i < res.length; i++) {
			ress[i] = res[i].toString();
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.layout_location_spinner_item, ress);
		content.setAdapter(adapter);
		content.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				LocationClientOption option = mLocationClient.getOption();
				switch (type) {
				case LOCA_TYPE_INDEX:
					// 定位模式选择
					switch (arg2) {
					case 0:
						// 混合定位
						priority = LocationClientOption.LocationMode.GPS_FIRST;
						break;
					case 1:
						// gps定位
						priority = LocationClientOption.LocationMode.GPS_ONLY;
						break;
					case 2:
						// 基站定位
						priority = LocationClientOption.LocationMode.NETWORK_ONLY;
						break;
					}
					option.setPriority(priority);
					mLocationClient.setOption(option);
					if(mLocationClient.isStarted()){
						Toast.makeText(LocationActivity.this, "重启", Toast.LENGTH_SHORT).show();
						mLocationClient.reStart();
					}
					local_type.setText(loca_type[arg2]);
					tv_location.setText("");
					break;
				case RES_TYPE_INDEX:
					switch (arg2) {
					case 0:
						// 0：默认值，只显示经纬度
						resultType = 0;
						break;
					case 1:
						// 1：显示经纬度对应的城市
						resultType = 1;
						break;
					case 2:
						// 2：显示经纬度对应的城市和用户当前地址，返回字段结构增加地址段
						resultType = 2;
						break;
					}
					option.setResultType(resultType);
					mLocationClient.setOption(option);
					if(mLocationClient.isStarted()){
						Toast.makeText(LocationActivity.this, "重启", Toast.LENGTH_SHORT).show();
						mLocationClient.reStart();
					}
					export_type.setText(res_type[arg2]);
					tv_location.setText("");
					break;
				case INV_INFO_INDEX:
					if(arg2==0){
						isInverse=true;
					}else{
						isInverse=false;
					}
					if(mLocationClient.isStarted()){
						Toast.makeText(LocationActivity.this, "重启", Toast.LENGTH_SHORT).show();
						mLocationClient.reStart();
					}
					inverse_type.setText(inv_info[arg2]);
					tv_location.setText("");
					break;
				}
				dialog.cancel();
			}
		});
	}
	

	
	/**
	 * 授权是否成功
	 */
	@Override
	public void onAuthResult(boolean arg0, String arg1) {
		if(!arg0){
			Toast.makeText(LocationActivity.this, arg1, Toast.LENGTH_SHORT).show();
		}
	}
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private String printTimeStamp(long time) {
		if(time > 0){
			Date date = new Date(time);
			return sdf.format(date);
		}
		return "error Time";
	}
	
    @Override
    protected void onStart() {
    	// TODO Auto-generated method stub
    	super.onStart();
    	Log.i("MapSDK", "---------回到前台-------");
    	mLocationClient.onForeground();
    }
    
    @Override
    protected void onStop() {
    	// TODO Auto-generated method stub
    	super.onStop();
    	Log.i("MapSDK", "---------切到后台-------");
    	mLocationClient.onBackground();
    }
    
    @Override
    public void onConfigurationChanged (Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }
	
	@Override
	protected void onDestroy() {
		mLocationClient.stop();
		mLocationClient.removeAllListener();
		super.onDestroy();
	}
}
