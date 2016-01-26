package com.mapbar.moo;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbar.map.Annotation;
import com.mapbar.map.CalloutStyle;
import com.mapbar.map.MapRenderer;
import com.mapbar.map.Vector2DF;
import com.mapbar.mapdal.WmrObject;
import com.mapbar.moo.resource.Config;
import com.mapbar.moo.resource.DemoMapView;
import com.mapbar.moo.resource.MessageBox;
import com.mapbar.moo.resource.SelectActivity;
import com.mapbar.poiquery.PoiFavoriteInfo;
import com.mapbar.poiquery.PoiQuery;
import com.mapbar.poiquery.PoiQueryInitParams;
import com.mapbar.poiquery.PoiTypeManager;

/**
 * 搜索
 * 
 * @author malw
 * 
 */
public class SearchActivity extends Activity implements OnClickListener,OnTouchListener,PoiQuery.EventHandler {
	/**
	 * 地图绘制引擎相关
	 */
	private DemoMapView mDemoMapView;
	private MapRenderer mRenderer;
	// POI搜索对象
	private PoiQuery mPoiQuery;
	// POI搜索结果分页时，每页大小
	private int mPageSize;
	// 搜索结果集合
	private ArrayList<PoiFavoriteInfo> mItems = new ArrayList<PoiFavoriteInfo>();
	// 搜索类型id
	private int mTypeIndex = 0;
	// 当前城市中心点
	private Point mPoint;

	/**
	 * UI相关
	 */
	private ListView mListView;
	private EditText mCityEditText;
	private EditText mContentEditText;
	private TextView mSearchKeyTextView;
	private TextView mSearchNearTextView;
	private TextView mSearchNearTypeTextView;
	private Button mPrevPage;
	private Button mNextPage;
	private TextView mPageInfoTextView;
	private ImageView mZoomInImageView = null;
	private ImageView mZoomOutImageView = null;
	private RelativeLayout mSearchSpanLayout;
	private ProgressDialog mDialog;
	//在线离线切换
	private Button mBtnOnline;
	//在线或离线
	private boolean online = Config.online;
	// listview适配器
	private ListViewAdapter mListviewAdapter;
	// 默认搜索城市名称
	private String mCityName = Config.SEARCH_DEFAULT_CITY;
	// 1 关键字搜索 2按类别搜索 3周边搜索
	private int mType = 1;

	/**
	 * 调试相关
	 */
	private final static String TAG = "[SearchActivity]";

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_search);
		// 初始化UI
		initView();
		// 初始化引擎
		init();
		
	}

	// 用于接收GLMapRenderer加载完的消息
	private Handler mHandler = new Handler() {
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
				mZoomInImageView.setEnabled(b.getBoolean("zoomOut"));
				mZoomOutImageView.setEnabled(b.getBoolean("zoomIn"));
				break;
			}
		}
	};
	
	/**
	 * 初始化需要使用的引擎 这里主要使用了POI搜索引擎和绘图引擎
	 */
	private void init () {
		// 初始化地图引擎
		try {
			if (Config.DEBUG) {
				Log.d(TAG, "Before - Initialize the GLMapRenderer Environment");
			}
			// 加载地图
			mDemoMapView = (DemoMapView) findViewById(R.id.glView_search);
			mDemoMapView.setZoomHandler(mHandler);
		} catch (Exception e) {
			if (Config.DEBUG) {
				e.printStackTrace();
			}
			new MessageBox(this, false).showDialog(e.getMessage());
		}

		// 初始化POI搜索引擎
		if (Config.DEBUG) {
			Log.d(TAG, "Before - Initialize the POIQuery Environment");
		}
		PoiQueryInitParams param = new PoiQueryInitParams();
		param.searchRange=1000;
		try {
			// 如果授權不通過那麼將拋出異常
			PoiQuery.getInstance().init(param); // 初始化搜索
			mPageSize = param.pageSize;
			mPoiQuery = PoiQuery.getInstance();
			mPoiQuery.setQueryParams(PoiQuery.PoiQueryParamsType.onlineSs, 0);
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

		// 设置搜索模式：online在线搜索 offline离线搜索
		mPoiQuery.setMode(online?PoiQuery.Mode.online:PoiQuery.Mode.offline);
		// 设置搜索城市id
		mPoiQuery.setWmrId(getIdByCityName(mCityName));
		// 注册搜索监听回调方法
		mPoiQuery.setCallback(this);
		
		mPoiQuery.sortByDistance();
	}

	/**
	 * 初始化控件
	 */
	public void initView () {
		TextView title = (TextView) findViewById(R.id.tv_title_text);
		title.setText("搜索");
		//在线离线切换按钮
		mBtnOnline = (Button)findViewById(R.id.iv_title_online);
		mBtnOnline.setText(Config.getOnlineText(online));
		// 显示查询结果的listview
		mListView = (ListView) findViewById(R.id.search_ls);
		// 城市选择
		mCityEditText = (EditText) findViewById(R.id.btn_search_editcity);
		// 搜索内容
		mContentEditText = (EditText) findViewById(R.id.btn_search_edit);
		// 地图放大按钮
		mZoomInImageView = (ImageView) findViewById(R.id.btn_zoom_in);
		// 地图缩小按钮
		mZoomOutImageView = (ImageView) findViewById(R.id.btn_zoom_out);
		// 上一页按钮
		mPrevPage = (Button) findViewById(R.id.prevPage);
		// 下一页按钮
		mNextPage = (Button) findViewById(R.id.nextPage);
		// 显示页数信息
		mPageInfoTextView = (TextView) findViewById(R.id.page_info);
		// 搜索内容的显示弹框
		mSearchSpanLayout = (RelativeLayout) findViewById(R.id.search_span);
		// 关键字搜索
		mSearchKeyTextView = (TextView)findViewById(R.id.btn_searchkey);
		// 周边搜索
		mSearchNearTextView = (TextView)findViewById(R.id.btn_searchnear);
		// 周边类型搜索
		mSearchNearTypeTextView = (TextView)findViewById(R.id.btn_searchtype);

		// 选择城市
		mCityEditText.setOnClickListener(this);
		//搜索条件
		mContentEditText.setOnClickListener(this);
		// 地图放大按钮
		mZoomInImageView.setOnClickListener(this);
		// 地图缩小按钮
		mZoomOutImageView.setOnClickListener(this);
		// 上一页按钮
		mPrevPage.setOnClickListener(this);
		// 下一页按钮
		mNextPage.setOnClickListener(this);
		// 搜索按钮
		findViewById(R.id.btn_search).setOnClickListener(this);
		// 返回按钮
		findViewById(R.id.iv_title_back).setOnClickListener(this);
		// 关键字搜索按钮
		mSearchKeyTextView.setOnClickListener(this);
		// 按类别搜索
		mSearchNearTypeTextView.setOnClickListener(this);
		// 周边搜索按钮
		mSearchNearTextView.setOnClickListener(this);
		// 关键字搜索按钮
		mSearchKeyTextView.setOnTouchListener(this);
		// 按类别搜索
		mSearchNearTypeTextView.setOnTouchListener(this);
		// 周边搜索按钮
		mSearchNearTextView.setOnTouchListener(this);
		//在线离线按钮
		mBtnOnline.setOnClickListener(this);
		//适配器
		mListviewAdapter = new ListViewAdapter(this, mItems);
		// ListView添加
		mListView.setAdapter(mListviewAdapter);
		mCityEditText.setInputType(InputType.TYPE_NULL);
		mCityEditText.setText(Config.SEARCH_DEFAULT_CITY);
	}

	/**
	 * 点击事件
	 */
	@Override
	public void onClick (View v) {
		switch (v.getId()) {
		case R.id.iv_title_back:
			// 返回按钮
			goBack();
			break;
		case R.id.iv_title_online:
			//在线离线切换按钮
			if(online){
				online = false;
				mPoiQuery.setMode(PoiQuery.Mode.offline);
				mRenderer.setDataMode(MapRenderer.DataMode.offline);
			}else{
				online =  true;
				mPoiQuery.setMode(PoiQuery.Mode.online);
				mRenderer.setDataMode(MapRenderer.DataMode.online);
			}
			//检查是否还存在上下页信息
			setPageBtn();
			mBtnOnline.setText(Config.getOnlineText(online));
			break;
		case R.id.btn_search_editcity:
			//选择搜索城市  需要进入SelectActivity页面
			Intent intent = new Intent(this,SelectActivity.class);
			intent.putExtra("type", 1);
			startActivityForResult(intent, 1);
			break;
		case R.id.btn_search_edit:
			//选择搜索类型  需要进入SelectActivity页面
			if(mType == 2){
				intent = new Intent(this,SelectActivity.class);
				intent.putExtra("type", 2);
				startActivityForResult(intent, 2);
			}
			break;
		case R.id.btn_search:
			// 搜索按钮
			progressDialog("正在搜索，请稍后……");

			
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.hideSoftInputFromWindow(getWindow().getDecorView()
						.getWindowToken(), 0);

//				imm.hideSoftInputFromWindow(getCurrentFocus()
//						.getApplicationWindowToken(),
//						InputMethodManager.HIDE_NOT_ALWAYS);
			}
			
			// 根据搜索条件和point查询
			search();
			break;
		case R.id.prevPage:
			// 上一页
			progressDialog("正在搜索，请稍后……");
			mPoiQuery.loadPreviousPage(null);
			refereshResult();
			break;
		case R.id.nextPage:
			// 下一页
			progressDialog("正在搜索，请稍后……");
			mPoiQuery.loadNextPage(null);
			refereshResult();
			break;
		case R.id.btn_searchkey:
			// 关键字搜索按钮
			mType = 1;
			mSearchSpanLayout.setVisibility(View.GONE);
			mContentEditText.setText(Config.SEARCH_BY_KEY_TEXT);
			mContentEditText.setInputType(InputType.TYPE_CLASS_TEXT); 
			break;
		case R.id.btn_searchtype:
			// 按类别搜索按钮
			mType = 2;
			mSearchSpanLayout.setVisibility(View.GONE);
			mContentEditText.setText(Config.SEARCH_BY_TYPE_TEXT);
			mContentEditText.setInputType(InputType.TYPE_NULL); 
			mTypeIndex = 2;
			break;
		case R.id.btn_searchnear:
			// 周边搜索按钮
			mType = 3;
			mSearchSpanLayout.setVisibility(View.GONE);
			mContentEditText.setText(Config.SEARCH_BY_NEAR_TEXT);
			mContentEditText.setInputType(InputType.TYPE_CLASS_TEXT); 
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

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch(v.getId()){
		case R.id.btn_searchkey:
			// 关键字搜索按钮
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				mSearchKeyTextView.setBackgroundResource(R.drawable.btn_bottom_bg_press);
				mSearchKeyTextView.setTextColor(this.getResources().getColor(R.color.press_text));
			}else{
				mSearchNearTypeTextView.setBackgroundResource(R.drawable.btn_bottom_bg);
				mSearchNearTypeTextView.setTextColor(this.getResources().getColor(R.color.black));
				mSearchNearTextView.setBackgroundResource(R.drawable.btn_bottom_bg);
				mSearchNearTextView.setTextColor(this.getResources().getColor(R.color.black));
			}
			break;
		case R.id.btn_searchtype:
			// 按类别搜索按钮
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				mSearchNearTypeTextView.setBackgroundResource(R.drawable.btn_bottom_bg_press);
				mSearchNearTypeTextView.setTextColor(this.getResources().getColor(R.color.press_text));
			}else{
				mSearchKeyTextView.setBackgroundResource(R.drawable.btn_bottom_bg);
				mSearchKeyTextView.setTextColor(this.getResources().getColor(R.color.black));
				mSearchNearTextView.setBackgroundResource(R.drawable.btn_bottom_bg);
				mSearchNearTextView.setTextColor(this.getResources().getColor(R.color.black));
			}
			break;
		case R.id.btn_searchnear:
			// 周边搜索按钮
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				mSearchNearTextView.setBackgroundResource(R.drawable.btn_bottom_bg_press);
				mSearchNearTextView.setTextColor(this.getResources().getColor(R.color.press_text));
			}else{
				mSearchKeyTextView.setBackgroundResource(R.drawable.btn_bottom_bg);
				mSearchKeyTextView.setTextColor(this.getResources().getColor(R.color.black));
				mSearchNearTypeTextView.setBackgroundResource(R.drawable.btn_bottom_bg);
				mSearchNearTypeTextView.setTextColor(this.getResources().getColor(R.color.black));
			}
			break;
		}
		return false;
	}
	
	/**
	 * 接收选择信息
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK){
			String  name = data.getStringExtra("name");
			Integer ids  = data.getIntExtra("id", 0);
			if(requestCode == 1){
				//获取返回的城市名称和城市id
				mCityEditText.setText(name);
				if (ids != WmrObject.INVALID_ID) {
					// 设置搜索城市id
					mPoiQuery.setWmrId(ids);
					mPoint = new WmrObject(ids).pos;
					// 设置地图中心点 把小车放在中心点坐标上
					mRenderer.setWorldCenter(mPoint);
					mDemoMapView.setCarPosition(mPoint);
				}
			}else if(requestCode == 2){
				//获取返回的搜索类型信息 和 类型id
				mContentEditText.setText(name);
				mTypeIndex = ids;
			}
		}
		
	}
	
	/**
	 * 根据当前搜索类型 搜索
	 */
	public void search () {
		if(online&&!mDemoMapView.isOpenNet()){
			if (mDialog != null) {
				mDialog.dismiss();
			}
			Toast.makeText(this, "请连接网络", Toast.LENGTH_SHORT	).show();
			return;
		}
		mPoint = mRenderer.getWorldCenter();
		switch (mType) {
		case 1:
			// 关键字搜索
			String content = mContentEditText.getText().toString();
			Log.i(TAG, content);
			Log.i(TAG, mPoint.x+","+mPoint.y);
			mPoiQuery.queryByKeyword(mContentEditText.getText().toString(),
					mPoint,null);
			break;
		case 2:
			// 按类别搜索
			mPoiQuery.queryNearby(mPoint, mTypeIndex,null);
			break;
		case 3:
			// 周边搜索
			mPoiQuery.queryNearbyKeyword(mContentEditText.getText().toString(),
					mPoint,null);
			break;
		}
	}
	
	private void goBack(){
		if(mPoiQuery!=null){
			mPoiQuery.cancel();
		}
		if (mSearchSpanLayout.getVisibility() == View.VISIBLE) {
			// 如果搜索结果框显示，先隐藏
			mSearchSpanLayout.setVisibility(View.GONE);
		}else{
			finish();
		}
	}
	
	public boolean onKeyDown (int keyCode, KeyEvent event) {
		// 返回键
		if (keyCode == KeyEvent.KEYCODE_BACK
				|| keyCode == KeyEvent.KEYCODE_HOME) {
			goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * POI搜索回调函数<br>
	 * 事件类型:<br>
	 * @param event 所产生的查询事件PoiQuery.Event
	 * @param err 搜索结果返回的错误类型PoiQuery.Error
	 * @param data 用户自定义的数据参数
	 * 0 无<br>
	 * 1 开始搜索<br>
	 * 2 搜索失败<br>
	 * 3 搜索不到结果<br>
	 * 4 查询结束<br>
	 * 5 页面加载成功<br>
	 * 6 逆地理编码成功<br>
	 * 7 逆地理编码失败
	 */
	@Override
	public void onPoiQuery (int event,int err, Object data) {
		try {
			String msg = null;
			switch (event) {
			case PoiQuery.Event.start:
				break;
			case PoiQuery.Event.failed:
				if (mDialog != null) {
					mDialog.dismiss();
				}
				switch(err){
				case PoiQuery.Error.canceled:
					msg = "取消搜索操作";
					break;
				case PoiQuery.Error.netError:
					msg = "请连接网络";
					break;
				case PoiQuery.Error.noData:
					msg = "查询无结果";
					break;
				case PoiQuery.Error.none:
					msg = "无错误";
					break;
				case PoiQuery.Error.noResult:
					msg = "无搜索结果";
					mSearchSpanLayout.setVisibility(View.VISIBLE);
					break;
				case PoiQuery.Error.notSupport:
					msg = "不支持的功能操作";
					break;
				}
				if(msg!=null){
					Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
				}
				mItems.clear();
				mListviewAdapter.notifyDataSetChanged();
				mPageInfoTextView.setText("0/0");
				break;
			case PoiQuery.Event.succ:
				if (mDialog != null) {
					mDialog.dismiss();
				}
				break;
			case PoiQuery.Event.pageLoaded:
				refereshResult();
				mSearchSpanLayout.setVisibility(View.VISIBLE);
				if (mDialog != null) {
					mDialog.dismiss();
				}
				break;
			case PoiQuery.Event.pageFailed:
				if (mDialog != null) {
					mDialog.dismiss();
				}
				switch(err){
				case PoiQuery.Error.canceled:
					msg = "取消搜索操作";
					break;
				case PoiQuery.Error.netError:
					msg = "请连接网络";
					break;
				case PoiQuery.Error.noData:
					msg = "查询无结果";
					break;
				case PoiQuery.Error.none:
					msg = "无错误";
					break;
				case PoiQuery.Error.noResult:
					msg = "无搜索结果";
					mSearchSpanLayout.setVisibility(View.VISIBLE);
					break;
				case PoiQuery.Error.notSupport:
					msg = "不支持的功能操作";
					break;
				}
				if(msg!=null){
					Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
				}
				mItems.clear();
				mListviewAdapter.notifyDataSetChanged();
				mPageInfoTextView.setText("0/0");
				break;
			default:
				if (mDialog != null) {
					mDialog.dismiss();
				}
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 刷新并显示搜索结果数据到ListView中
	 */
	private void refereshResult () {
		if (mPoiQuery.getResultNumber() != 0) {
			mItems.clear();
			int first = mPoiQuery.getCurrentPageFirstResultIndex();
			int last = mPoiQuery.getCurrentPageLastResultIndex();
			for (int i = first; i <= last; i++) {
				mItems.add(mPoiQuery.getResultAsPoiFavoriteInfo(i));
			}
			mListviewAdapter = new ListViewAdapter(this, mItems);
			mListView.setAdapter(mListviewAdapter); // listview添加适配器

			mNextPage.setEnabled(mPoiQuery.hasNextPage());
			mPrevPage.setEnabled(mPoiQuery.getCurrentPageIndex() != 0);

			int pnum = (mPoiQuery.getTotalResultNumber() + mPageSize - 1)
					/ mPageSize;
			mPageInfoTextView.setText((mPoiQuery.getCurrentPageIndex() + 1)
					+ "/" + pnum);
		} else {
			mItems.clear();
			mListviewAdapter = new ListViewAdapter(this, mItems);
			mListView.setAdapter(mListviewAdapter);

			mPageInfoTextView.setText("0/0");
			mNextPage.setEnabled(false);
			mPrevPage.setEnabled(false);
		}
	}
	
	/**
	 * 检查是否还存在上下页信息，防止在线搜索后切换到离线模式翻页出现问题
	 */
	public void setPageBtn(){
		if(mPoiQuery==null)return;
		if(mPoiQuery.hasNextPage()){
			mNextPage.setEnabled(true);
		}else{
			mNextPage.setEnabled(false);
		}
		mPrevPage.setEnabled(mPoiQuery.getCurrentPageIndex() != 0);
	}
	
	/**
	 * 根据城市名称获取城市id
	 * 
	 * @param cname
	 * @return
	 */
	private int getIdByCityName (String cname) {
		//根据经纬度获取当前城市名称
//		WorldManager wm = WorldManager.getInstance();
//		int cid = wm.getPedIdByPosition(Config.centerPoint);
//		int cid = wm.getPedIdByPosition(new Point(11633465,4005672));
//		WmrObject wob = new WmrObject(cid);
//		String cityName = wob.chsName;
//		System.out.println(cityName);
		// 根据城市名称获取城市中心点
		WmrObject wb = new WmrObject(0);
		int id = wb.getChildIdByName(cname);
		if (id == WmrObject.INVALID_ID) {
			int zzid = wb.getChildIdByName(Config.SEARCH_DEFAULT_PROVINCE);
			WmrObject wb2 = new WmrObject(zzid);
			id = wb2.getChildIdByName(cname);
		}
		if (id != WmrObject.INVALID_ID) {
			mPoint = new WmrObject(id).pos;
		}
		return id;
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

	/**
	 * 搜索结果ListView的Adapter
	 */
	private class ListViewAdapter extends BaseAdapter {

		private LayoutInflater mLayoutInflater = null;
		private ArrayList<PoiFavoriteInfo> items;

		public ListViewAdapter(Context context, ArrayList<PoiFavoriteInfo> items) {
			super();
			mLayoutInflater = LayoutInflater.from(context);
			this.items = items;
		}

		@Override
		public int getCount () {
			if (mItems != null) {
				return mItems.size();
			}
			return 0;
		}

		@Override
		public Object getItem (int position) {
			if (mItems != null) {
				return mItems.get(position);
			}
			return null;
		}

		@Override
		public long getItemId (int position) {
			return position;
		}

		@Override
		public View getView (final int position, View convertView,
				ViewGroup parent) {
			if (mItems == null) {
				return null;
			}
			TextView title = null;
			TextView address = null;
			TextView range = null;
			TextView type = null;
			Button btn_map = null;

			if (convertView == null) {
				convertView = mLayoutInflater.inflate(
						R.layout.item_search_list, null);
			}
			title = (TextView) convertView.findViewById(R.id.tv_title);
			address = (TextView) convertView.findViewById(R.id.tv_address);
			range = (TextView) convertView.findViewById(R.id.tv_range);
			type = (TextView) convertView.findViewById(R.id.tv_type);
			btn_map = (Button) convertView.findViewById(R.id.btn_inmap);

			// item赋值
			title.setText(mItems.get(position).fav.name);
			range.setText("(" + mItems.get(position).distance + "米)");
			address.setText("地址：" + mItems.get(position).fav.address);
			// 获取搜索类型信息
			PoiTypeManager ptm = PoiTypeManager.getInstance();
			String tname = ptm.getTypeName(mItems.get(position).fav.type);
			type.setText("类型：" + tname);

			// item按钮的点击事件
			btn_map.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick (View v) {
					mSearchSpanLayout.setVisibility(View.GONE);
					if (items != null && items.size() > 0) {
						PoiFavoriteInfo poi = items.get(position);
						Vector2DF pivot = new Vector2DF(0.5f, 0.0f);
						Annotation an = new Annotation(1, poi.fav.pos, 1101, pivot);
						CalloutStyle calloutStyle = an.getCalloutStyle();
						//calloutStyle.leftIcon = 0;
						calloutStyle.rightIcon = 1001;
						an.setCalloutStyle(calloutStyle);
						an.setTitle(poi.fav.name);
						mRenderer.addAnnotation(an);
						an.showCallout(true);
						// 设置中心点
						mRenderer.setWorldCenter(poi.fav.pos);
					}
				}
			});

			return convertView;
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
        PoiQuery.getInstance().cleanup();
        // 其它资源的清理
        // ...
	}
}
