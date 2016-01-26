package com.mapbar.moo;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbar.mapdal.Auth;
import com.mapbar.mapdal.NativeEnv;
import com.mapbar.mapdal.NativeEnvParams;
import com.mapbar.mapdal.NaviCore;
import com.mapbar.mapdal.SdkAuth;
import com.mapbar.mapdal.WorldManager;
import com.mapbar.moo.resource.Config;

public class MainActivity extends Activity {
	//激活key
	public static final String KEY = "maliwei-20130205-test-L-A11111";
	//应用跟目录
	private static String mAppPath = null;
	//应用名
	private static String mAppName = null;
	//dpi
	private static int mDensityDpi = 0; 
	// 记录首次按退出键时的时间
	private long mExitTime = 0;
	// 第二次按返回键推出程序的时间间隔，4秒
	final int mInterval = 4000;
	// 主界面GridView
	private GridView mGridView;
	// 显示引擎版本号
	private TextView mVerTextView;
	private ArrayList<HashMap<String, Object>> meumList = null;
	private final String[][] itemText = { 
			{"地图视图","Map"}, {"搜索","Search"},
			{"叠加层/物","Overlay"}, {"逆地理编码","Re-Geo Code"},
			{"定位","Location"}, {"导航","Navi"}, 
			{"公交","Bus"}, {"离线数据","Offline"} ,
			{"退出","Exit"}};
	private final Class<?>[] cname = { 
			MapViewActivity.class, SearchActivity.class, 
			OverlayActivity.class, InverseCodeActivity.class, 
			LocationActivity.class, NaviActivity.class,  
			BusActivity.class, DownloadActivity.class, 
			MainActivity.class};
	// 调试时所使用的输出信息
	private final static String TAG = "[ADemoActivity]";
	
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		setTitle("Mapbar Navigation MOO SDK demo");
		// 初始化圖吧引擎基礎環境
		init();
		initData();
		mGridView = (GridView) findViewById(R.id.GridView);
		// 数据源
		SimpleAdapter saMenuItem = new SimpleAdapter(this, meumList, 
				R.layout.item_main_gridview, 
				new String[] {"ItemText","Leter"}, 
				new int[] { R.id.ItemText,R.id.ItemLetter}); 
		// 添加Item到网格中
		mGridView.setAdapter(saMenuItem);
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick (AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if (cname[arg2].hashCode() == MainActivity.class.hashCode()) {
					new AlertDialog.Builder(MainActivity.this)
							.setTitle("注意")
							.setMessage("确定要退出吗？")
							.setPositiveButton("确定",
									new DialogInterface.OnClickListener() {
										public void onClick (
												DialogInterface dialog,
												int which) {
											finish();
										}
									})
							.setNegativeButton("取消",
									new DialogInterface.OnClickListener() {
										public void onClick (
												DialogInterface dialog,
												int which) {
										}
									}).show();
				} else {
					Intent intent = new Intent(MainActivity.this, cname[arg2]);
					startActivity(intent);
				}
			}
		});
	}

	/**
	 * 初始化引擎所需的基础环境
	 */
	private void init () {
		mVerTextView = (TextView)findViewById(R.id.tv_ver);
		if (Config.DEBUG) {
			Log.d(TAG, "Before - Initialize the Native Environment");
		}
		// 设置应用程序数据根目录
		// 存放包括导航离线数据，资源数据，运行是的临时数据文件等
		mAppPath = Environment.getExternalStorageDirectory().getPath()+"/mapbar/app";
		// 应用程序名称，不要修改
		mAppName = "qyfw";
		// 获取屏幕对应的DPI
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics dm = new DisplayMetrics();
		display.getMetrics(dm);
		mDensityDpi = dm.densityDpi;
		Toast.makeText(this, "mDensityDpi:"+mDensityDpi, Toast.LENGTH_LONG).show();
		NativeEnvironmentInit(mAppName,KEY);
		if (Config.DEBUG) {
			Log.d(TAG, "After - Initialize the Native Environment");
		}
		mVerTextView.setText(NaviCore.getVersion());
	}
	
	public void NativeEnvironmentInit(String appName, String key) {
		NativeEnvParams params = new NativeEnvParams(mAppPath, appName,
				mDensityDpi, key, new NativeEnv.AuthCallback() {
					@Override
					public void onDataAuthComplete(int errorCode) {
						String msg = null;
						switch(errorCode){
						case Auth.Error.deviceIdReaderError:
							msg="设备ID读取错误";
							break;
						case Auth.Error.expired:
							msg="数据文件权限已经过期";
							break;
						case Auth.Error.licenseDeviceIdMismatch:
							msg="授权文件与当前设备不匹配";
							break;
						case Auth.Error.licenseFormatError:
							msg="授权文件格式错误";
							break;
						case Auth.Error.licenseIncompatible:
							msg="授权文件存在且有效，但是不是针对当前应用程序产品的";
							break;
						case Auth.Error.licenseIoError:
							msg="授权文件IO错误";
							break;
						case Auth.Error.licenseMissing:
							msg="授权文件不存在";
							break;
						case Auth.Error.none:
							msg="数据授权成功";
							break;
						case Auth.Error.noPermission:
							msg="数据未授权";
							break;
						case Auth.Error.otherError:
							msg="其他错误";
							break;
						}
						if(msg!=null){
							Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
						}
					}
					
					@Override
					public void onSdkAuthComplete(int errorCode) {
						String msg=null;
						switch(errorCode){
						case SdkAuth.ErrorCode.deviceIdReaderError:
							msg="授权设备ID读取错误";
							break;
						case SdkAuth.ErrorCode.expired:
							msg="授权KEY已经过期";
							break;
						case SdkAuth.ErrorCode.keyIsInvalid:
							msg="授权KEY是无效值，已经被注销";
							break;
						case SdkAuth.ErrorCode.keyIsMismatch:
							msg="授权KEY不匹配";
							break;
						case SdkAuth.ErrorCode.keyUpLimit:
							msg="授权KEY到达激活上线";
							break;
						case SdkAuth.ErrorCode.licenseDeviceIdMismatch:
							msg="设备码不匹配";
							break;
						case SdkAuth.ErrorCode.licenseFormatError:
							msg="SDK授权文件格式错误";
							break;
						case SdkAuth.ErrorCode.licenseIoError:
							msg="SDK授权文件读取错误";
							break;
						case SdkAuth.ErrorCode.licenseMissing:
							msg="SDK授权文件没有准备好";
							break;
						case SdkAuth.ErrorCode.networkContentError:
							msg="网络返回信息格式错误";
							break;
						case SdkAuth.ErrorCode.netWorkIsUnavailable:
							msg="网络不可用，无法请求SDK验证";
							break;
						case SdkAuth.ErrorCode.none:
							msg="SDK验证通过";
							break;
						case SdkAuth.ErrorCode.noPermission:
							msg="模块没有权限";
							break;
						case SdkAuth.ErrorCode.otherError:
							msg="其他错误";
							break;
						}
						if(msg!=null){
							Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
						}
					}
				});
//		params.sdkAuthOfflineOnly=true;
		NativeEnv.init(getApplicationContext(), params);
		WorldManager.getInstance().init();
	}
	
	@Override
	protected void onDestroy() {
		WorldManager.getInstance().cleanup();
		NativeEnv.cleanup();
		super.onDestroy();
	}

	/**
	 * 初始化模块信息
	 */
	public void initData () {
		meumList = new ArrayList<HashMap<String, Object>>();
		for (String[] info : itemText) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("ItemText", info[0]);
			map.put("Leter", info[1]);
			meumList.add(map);
		}
	}

	@Override
	public void onConfigurationChanged (Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event) {
		// 点击返回按钮，退出系统
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			if ((System.currentTimeMillis() - mExitTime) > mInterval) {
				Toast toast = Toast.makeText(getApplicationContext(),
						"再按一次退出程序", Toast.LENGTH_SHORT);
				toast.show();
				mExitTime = System.currentTimeMillis();
			} else {
				finish();
			}
			return true;
		}
		return false;
	}

}