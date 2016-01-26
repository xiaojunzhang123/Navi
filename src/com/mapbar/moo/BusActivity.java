package com.mapbar.moo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbar.bus.BusLine;
import com.mapbar.bus.BusQuery;
import com.mapbar.bus.BusQuery.Listener;
import com.mapbar.bus.BusQueryInitParams;
import com.mapbar.bus.BusRoute;
import com.mapbar.bus.BusRoutePlan;
import com.mapbar.bus.BusSegment;
import com.mapbar.bus.BusSegmentBase;
import com.mapbar.bus.BusStation;
import com.mapbar.bus.BusWalkSegment;
import com.mapbar.bus.SubwayEntrance;
import com.mapbar.mapdal.WmrObject;
import com.mapbar.mapdal.WorldManager;
import com.mapbar.moo.resource.BusLineToMapViewActivity;
import com.mapbar.moo.resource.Config;
import com.mapbar.moo.resource.NaviManager;
import com.mapbar.moo.resource.NaviSetting;
import com.mapbar.moo.resource.SearchBusActivity;

/**
 * 搜索
 * 
 * @author malw
 * 
 */
public class BusActivity extends Activity implements OnClickListener, Listener{
	
	private ListView.LayoutParams lp=new ListView.LayoutParams(ListView.LayoutParams.WRAP_CONTENT,1);
	private String TAG = "[BusActivity]";
	//输入发对象
	private InputMethodManager imm = null;
	
	private int level = 1; //列表级别
	private Object[] mFirstData = null;
	private Object[] mSecendData = null;
	private Object[] mThirdData = null;
	private int mFirstIndex = 0;
	
	private static final int BUS_QUERY_ROUTE_START = 1;				//换乘起点
	private static final int BUS_QUERY_ROUTE_END = 2;					//换乘终点
	
	private int mTabIndex = 0; //0换乘 1线路 2站点
	private String[] mBusLineTypeStrings = {"none", "地铁", "公交", "步行"};
	public static final int busTran = 0; //换乘查询
	public static final int busRoad = 1;//线路查询
	public static final int busStation = 2;//站点查询
	public static final int busLineDetail = 3;//查询单条线路详情
	public static final int subwayEntrance = 4;//查询地铁入口
	private int mQueryType = busTran;    //公交查询类型
	private int mBusRoadQueryOptions = 2147483647  ;  //线路查询
	private int mBusStationQueryOptions = 2147483647  ; //站点查询
	private int mBusQuerySuggest = 1; //查询提示，实际查询
	private BusQuery mBusQuery = null;
	private ListView mTranListView = null;
	private ListView mRoadListView = null;
	private ListView mStationListView = null;
	private TranResultItemAdapter mTranResultItemAdapter = null;
	private OtherResultItemAdapter mOtherResultItemAdapter = null;
	private Object[] mResultObjects = new Object[0];			//查询结果对象
	private Object[] mResultItems = new Object[0];				//结果显示数据
	private Object mTempObject = null;
	private BusRoutePlan mRoutePlan = new BusRoutePlan();
	
	private View headView;
	private View footView;
	
	private RelativeLayout mTransferLayout;
	private RelativeLayout mRoadLayout;
	private RelativeLayout mStationLayout;
	private TextView mTransferBtn;
	private TextView mRoadBtn;
	private TextView mStationBtn;
	//换乘
	private Button mTranChange;
	private EditText mStartEditText;
	private EditText mEndEditText;
	private TextView mTranSearchBtn;
	//路线
	private EditText mRoadNameEditText;
	private TextView mRoadSearchBtn;
	//站点
	private EditText mStationEditText;
	private TextView mStationSearchBtn;
	
	//换乘规则
	private ImageView mRadioTime;
	private ImageView mRadioTransfer;
	private ImageView mRadioWalk;
	private ImageView mCheckSubway;
	//进入地图按钮
	private ImageView mMapViewBtn;
	private Button mBtnOnline;
	//等待提示框
	public ProgressDialog myDialog;
	
	//管理类
	private NaviManager mNaviManager;
	
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_bus);
		mNaviManager = NaviManager.getNaviManager();
		imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		//初始化worldManager
		WorldManager.getInstance().init();
		//初始化公交查询
		initBus();
		//初始化控件
		initView();
	}
	
	private void initBus(){
		try{
			mBusQuery = BusQuery.getInstance();
			BusQueryInitParams params = new BusQueryInitParams();
			params.callback = this;
			mBusQuery.init(params);
			mBusQuery.setWmrId(getIdByCityName(Config.SEARCH_DEFAULT_CITY));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void initView(){
		((TextView)findViewById(R.id.tv_title_text)).setText("公交");
		findViewById(R.id.iv_title_back).setOnClickListener(this);
		//换乘layout
		mTransferLayout = (RelativeLayout)findViewById(R.id.tran);
		//路线layout
		mRoadLayout = (RelativeLayout)findViewById(R.id.road);
		//站点layout
		mStationLayout = (RelativeLayout)findViewById(R.id.station);
		//换乘按钮
		mTransferBtn = (TextView)findViewById(R.id.btn_tran);
		mTransferBtn.setOnClickListener(this);
		//路线按钮
		mRoadBtn = (TextView)findViewById(R.id.btn_road);
		mRoadBtn.setOnClickListener(this);
		//站点按钮
		mStationBtn = (TextView)findViewById(R.id.btn_station);
		mStationBtn.setOnClickListener(this);
		//换乘
		mStartEditText = (EditText)findViewById(R.id.et_start_place);
		mStartEditText.setOnClickListener(this);
		mEndEditText = (EditText)findViewById(R.id.et_end_place);
		mEndEditText.setOnClickListener(this);
		mTranSearchBtn = (TextView)findViewById(R.id.btn_search);
		mTranSearchBtn.setOnClickListener(this);
		mTranChange = (Button)findViewById(R.id.btn_tran_change);
		mTranChange.setOnClickListener(this);
		//路线
		mRoadNameEditText = (EditText)findViewById(R.id.et_road);
		mRoadSearchBtn = (TextView)findViewById(R.id.btn_route_search);
		mRoadSearchBtn.setOnClickListener(this);
		//站点
		mStationEditText = (EditText)findViewById(R.id.et_station);
		mStationSearchBtn = (TextView)findViewById(R.id.btn_station_search);
		mStationSearchBtn.setOnClickListener(this);
		//换乘规则
		mRadioTime = (ImageView)findViewById(R.id.btn_time_less);
		mRadioTime.setOnClickListener(this);
		mRadioTransfer = (ImageView)findViewById(R.id.btn_transfer_less);
		mRadioTransfer.setOnClickListener(this);
		mRadioWalk = (ImageView)findViewById(R.id.btn_walk_less);
		mRadioWalk.setOnClickListener(this);
		mCheckSubway = (ImageView)findViewById(R.id.btn_subway_no);
		mCheckSubway.setOnClickListener(this);
		
		//路线 和 listview
		mRoutePlan.userOption = BusRoutePlan.Option.lessWalking;
		mRoutePlan.shift = BusRoutePlan.LineShift.day;
		mTranListView = (ListView)findViewById(R.id.tran_list);
		mRoadListView = (ListView)findViewById(R.id.road_list);
		mStationListView = (ListView)findViewById(R.id.station_list);
		
		//进入地图按钮
		mMapViewBtn = (ImageView)findViewById(R.id.iv_mapview);
		mMapViewBtn.setOnClickListener(this);
		mBtnOnline  = (Button)findViewById(R.id.iv_title_online);
		mBtnOnline.setVisibility(View.GONE);
		
		LayoutInflater mLayoutInflater = LayoutInflater.from(BusActivity.this);
		headView = mLayoutInflater.inflate(R.layout.item_bus_list_foot, null);
		RelativeLayout hlayout = (RelativeLayout)headView.findViewById(R.id.layout_grid);
		ImageView hiv = (ImageView)headView.findViewById(R.id.iv_bus_segment_icon);
		TextView  htv = (TextView)headView.findViewById(R.id.tv_bus_segment_detail);
		hiv.setBackgroundResource(R.drawable.img_start);
		htv.setText("我的位置");
		
		footView = mLayoutInflater.inflate(R.layout.item_bus_list_foot, null);
		RelativeLayout flayout = (RelativeLayout)footView.findViewById(R.id.layout_grid);
		ImageView fiv = (ImageView)footView.findViewById(R.id.iv_bus_segment_icon);
		TextView  ftv = (TextView)footView.findViewById(R.id.tv_bus_segment_detail);
		fiv.setBackgroundResource(R.drawable.img_end);
		ftv.setText("终点");
		
		hlayout.setVisibility(View.GONE);
		flayout.setVisibility(View.GONE);
		
		mTranListView.addHeaderView(headView);
		mTranListView.addFooterView(footView);
	}
	
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.iv_title_back:
			//返回按钮
			if(level==1){
				finish();
			}else{
				goBack();
			}
			break;
		case R.id.iv_mapview:
			//进入地图
			goToMap();
			break;
		case R.id.btn_tran:
			//换乘
			mTabIndex = busTran;
			mQueryType = busTran;
			changeBootomBtn(v);
			showOnlineBtn(true);
			break;
		case R.id.btn_road:
			//路线
			mTabIndex = busRoad;
			mQueryType = busRoad;
			changeBootomBtn(v);
			showOnlineBtn(true);
			break;
		case R.id.btn_station:
			//站点
			mTabIndex = busStation;
			mQueryType = busStation;
			changeBootomBtn(v);
			showOnlineBtn(true);
			break;
		case R.id.et_start_place:
			//换乘起点
			Intent intent = new Intent(BusActivity.this, SearchBusActivity.class);
			intent.putExtra("__from", NaviSetting.FROM_BUS_SEARCH);
			intent.putExtra("wmrId", mBusQuery.getWmrId());
			startActivityForResult(intent, BUS_QUERY_ROUTE_START);
			break;
		case R.id.et_end_place:
			//换乘终点
			Intent intent2 = new Intent(BusActivity.this, SearchBusActivity.class);
			intent2.putExtra("__from", NaviSetting.FROM_BUS_SEARCH);
			intent2.putExtra("wmrId", mBusQuery.getWmrId());
			startActivityForResult(intent2, BUS_QUERY_ROUTE_END);
			break;
		case R.id.btn_tran_change:
			//交互起终点
			if (TextUtils.isEmpty(mStartEditText.getText().toString()) 
					|| TextUtils.isEmpty(mEndEditText.getText().toString())
					|| mRoutePlan.startPoint == null || mRoutePlan.endPoint == null) {
				Toast.makeText(BusActivity.this, "请先输入起终点信息", Toast.LENGTH_SHORT).show();
				break;
			}
			Point tmpPoint = mRoutePlan.startPoint;
			mRoutePlan.startPoint = mRoutePlan.endPoint;
			mRoutePlan.endPoint = tmpPoint;
			String tmpName = mStartEditText.getText().toString();
			mStartEditText.setText(mEndEditText.getText().toString());
			mEndEditText.setText(tmpName);
			break;
		case R.id.btn_search:
			//换乘查询
			mQueryType = busTran;
			startQuery();
			break;
		case R.id.btn_time_less:
			//最快到达
			mQueryType = busTran;
			mRoutePlan.userOption = 2;
			changeBusPlanBackGround();
			startQuery();
			break;
		case R.id.btn_transfer_less:
			//少换乘
			mQueryType = busTran;
			mRoutePlan.userOption = 1;
			changeBusPlanBackGround();
			startQuery();
			break;
		case R.id.btn_walk_less:
			//少步行
			mQueryType = busTran;
			mRoutePlan.userOption = 0;
			changeBusPlanBackGround();
			startQuery();
			break;
		case R.id.btn_subway_no:
			//不乘地铁
			mQueryType = busTran;
			mRoutePlan.userOption = 3;
			changeBusPlanBackGround();
			startQuery();
			break;
		case R.id.btn_route_search:
			//线路查询
			mQueryType = busRoad;
			startQuery();
			break;
		case R.id.btn_station_search:
			//站点查询
			mQueryType = busStation;
			startQuery();
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CANCELED) {
			return;
		}
		switch (requestCode) {
		case BUS_QUERY_ROUTE_START:
			mRoutePlan.startPoint = new Point();
			mRoutePlan.startPoint.x = data.getIntExtra("poiX", 0);
			mRoutePlan.startPoint.y = data.getIntExtra("poiY", 0);
			mStartEditText.setText(data.getStringExtra("name") + "(" + mRoutePlan.startPoint.x + "," + mRoutePlan.startPoint.y + ")");
			break;
		case BUS_QUERY_ROUTE_END:
			mRoutePlan.endPoint = new Point();
			mRoutePlan.endPoint.x = data.getIntExtra("poiX", 0);
			mRoutePlan.endPoint.y = data.getIntExtra("poiY", 0);
			mEndEditText.setText(data.getStringExtra("name") + "(" + mRoutePlan.endPoint.x + "," + mRoutePlan.endPoint.y + ")");
			break;
		default:
			break;
		}
	}
	
	private void startQuery() {
		String string;
		switch (mQueryType) {
		case busTran:
			if (TextUtils.isEmpty(mStartEditText.getText().toString()) 
					|| TextUtils.isEmpty(mEndEditText.getText().toString())
					|| mRoutePlan.startPoint == null || mRoutePlan.endPoint == null) {
				Toast.makeText(BusActivity.this, "查询内容为空", Toast.LENGTH_SHORT).show();
				break;
			}
			if (mRoutePlan.userOption == 4) {
				mBusQuery.queryBusRoutesWalkOnly(mRoutePlan);
			} else {
				mBusQuery.queryBusRoutes(mRoutePlan);
			}
			break;
		case busRoad:
			string = mRoadNameEditText.getText().toString();
			if (TextUtils.isEmpty(string)) {
				Toast.makeText(BusActivity.this, "查询内容为空", Toast.LENGTH_SHORT).show();
				break;
			}
			Pattern pattern = Pattern.compile("\\([0-9]+,[0-9]+\\)");
			Matcher matcher = pattern.matcher(string);
			imm.hideSoftInputFromWindow(mRoadNameEditText.getWindowToken(), 0);
			if (matcher.find()) {
				mBusQuery.queryBusLinesByPosition(Config.centerPoint, mBusRoadQueryOptions);
			} else {
				mBusQuery.queryBusLinesByKeyword(string, mBusRoadQueryOptions, mBusQuerySuggest == 0 ? true : false);
			}
			break;
		case busStation:
			mBusStationQueryOptions = mBusStationQueryOptions != 2 ? mBusStationQueryOptions + 1 : BusQuery.Option.allFields;
			string = mStationEditText.getText().toString();
			if (TextUtils.isEmpty(string)) {
				Toast.makeText(BusActivity.this, "查询内容为空", Toast.LENGTH_SHORT).show();
				break;
			}
			Pattern pattern2 = Pattern.compile("\\([0-9]+,[0-9]+\\)");
			Matcher matcher2 = pattern2.matcher(string);
			imm.hideSoftInputFromWindow(mStationEditText.getWindowToken(), 0);
			if (matcher2.find() && mBusStationQueryOptions != 4) {
				mQueryType = busStation;
				mBusQuery.queryStationsByPosition(Config.centerPoint, mBusStationQueryOptions);
			} else if (mBusStationQueryOptions != 4) {
				mQueryType = busStation;
				mBusQuery.queryStationsByKeyword(string, mBusStationQueryOptions,false);
			} else if (mBusStationQueryOptions == 4) {
				mQueryType = subwayEntrance;
				mBusQuery.querySubwayEntrances(string);
			} else {
				Toast.makeText(BusActivity.this, "查询条件错误", Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
	}
	

	@Override
	public void onBusQuery(int event) {
		switch (event) {
		case BusQuery.Event.none:
			disDialog();
			break;
		case BusQuery.Event.netFailed:
			disDialog();
			Toast.makeText(this, "请连接网络", Toast.LENGTH_SHORT).show();
			break;
		case BusQuery.Event.start:
			waitDialog("计算中，请稍后……", false);
			break;
		case BusQuery.Event.canceled:
			Log.d(TAG, "Failed to query BUS!");
			disDialog();
			break;
		case BusQuery.Event.completed:
			Log.d(TAG, "Query completed!");
			showQueryResult();
			disDialog();
			break;
		case BusQuery.Event.noResult:
			Log.d(TAG, "No result!");
			Toast.makeText(this, "无结果", Toast.LENGTH_SHORT).show();
			disDialog();
			break;
		default:
			break;
		}
	}
	
	/**
	 * 显示查询结果
	 */
	private void showQueryResult() {
		int num = mBusQuery.getResultNumber();
		if(num==0){
			Toast.makeText(this, "0个结果", Toast.LENGTH_SHORT).show();
			return;
		}
		switch(mQueryType){
		case busTran:
			showOnlineBtn(true);
			//恢复到一级
			level = 1;
			mResultObjects = mBusQuery.getResultAsBusRoutes(0, num-1);
			//缓存一级信息
			mFirstData = mResultObjects;
			//显示list
			if(mResultObjects!=null){
				mTranResultItemAdapter = new TranResultItemAdapter(this,mResultObjects,1,0);
				mTranListView.setAdapter(mTranResultItemAdapter);
			}
			headView.findViewById(R.id.layout_grid).setVisibility(View.GONE);
			headView.findViewById(R.id.line).setVisibility(View.GONE);
			footView.findViewById(R.id.layout_grid).setVisibility(View.GONE);
			footView.findViewById(R.id.line).setVisibility(View.GONE);
			break;
		case busRoad:
			showOnlineBtn(true);
			//恢复到一级
			level = 1;
			mResultObjects = mBusQuery.getResultAsBusLines(0, num-1);
			//缓存一级信息
			mFirstData = mResultObjects;
			
			mResultItems = new Object[num-1];
			for (int i = 0; i < num-1; i++) {
				BusLine line = (BusLine)mResultObjects[i];
				if(line!=null){
					mResultItems[i] = line.name;
				}
			}
			mOtherResultItemAdapter = new OtherResultItemAdapter(this);
			mRoadListView.setAdapter(mOtherResultItemAdapter);
			break;
		case busStation:
			showOnlineBtn(true);
			//恢复到一级
			level = 1;
			mResultObjects = mBusQuery.getResultAsStations(0, num-1);
			//缓存一级信息
			mFirstData = mResultObjects;
			mResultItems = new String[num-1];
			for (int i = 0; i < num-1; i++) {
				BusStation station = (BusStation)mResultObjects[i];
				mResultItems[i] = station.name + " ("+mBusLineTypeStrings[station.type]+" "+station.lines.length+")";
			}
			mOtherResultItemAdapter = new OtherResultItemAdapter(this);
			mStationListView.setAdapter(mOtherResultItemAdapter);
			break;
		case busLineDetail:
			showOnlineBtn(false);
			mTempObject = mBusQuery.getResultAsBusLine(0);
			BusLine line = (BusLine)mTempObject;
			String[] bs = line.stationNames;
			mResultItems = new BusLine[bs.length];
			mResultItems = bs;
			//缓存第三级信息
			mOtherResultItemAdapter = new OtherResultItemAdapter(this);
			if(mTabIndex == busRoad){
				mQueryType = busRoad;
				mRoadListView.setAdapter(mOtherResultItemAdapter);
				mSecendData = new Object[1];
				mSecendData[0] = line;
			}else if(mTabIndex == busStation){
				mQueryType = busStation;
				mStationListView.setAdapter(mOtherResultItemAdapter);
				mThirdData = new Object[1];
				mThirdData[0] = line;
			}
			break;
		case subwayEntrance:
			showOnlineBtn(true);
			//回复一级
			level = 1;
			mResultObjects = mBusQuery.getResultAsSubwayEntrances(0, num-1);
			//缓存一级信息
			mFirstData = mResultObjects;
			mResultItems = new String[num];
			for (int i = 0; i < num; i++) {
				SubwayEntrance se = ((SubwayEntrance)mResultObjects[i]);
				mResultItems[i] = se.toString();
			}
			mOtherResultItemAdapter = new OtherResultItemAdapter(this);
			mStationListView.setAdapter(mOtherResultItemAdapter);
			break;
		}
	}
	
	/**
	 * 显示路线详情
	 * @param index
	 */
	private void showChild(int index){
		//显示item
		Message msg = new Message();
		msg.arg1 = index;
		msg.what = 1;
		handler.sendMessage(msg);
		headView.findViewById(R.id.layout_grid).setVisibility(View.VISIBLE);
		footView.findViewById(R.id.layout_grid).setVisibility(View.VISIBLE);
		footView.findViewById(R.id.line).setVisibility(View.VISIBLE);
	}
	
	/**
	 * 显示路线详情信息
	 */
	Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
			case 1:
				showOnlineBtn(false);
				//换乘
				level = 2;
				if(mResultObjects!=null){
					mTranResultItemAdapter = new TranResultItemAdapter(BusActivity.this,mResultObjects,2,msg.arg1);
					mTranListView.setAdapter(mTranResultItemAdapter);
				}
				break;
			case 2:
				//站点查询     当前站点下的路线信息
				BusStation station = (BusStation)mFirstData[mFirstIndex];
				//缓存二级信息
				mSecendData = station.lines;
				mResultItems = new Object[station.lines.length];
				for(int i=0;i<station.lines.length;i++){
					BusLine bl = station.lines[i];
					mResultItems[i] = bl.name;
				}
				level = 2;
				mOtherResultItemAdapter = new OtherResultItemAdapter(BusActivity.this);
				mStationListView.setAdapter(mOtherResultItemAdapter);
				break;
			}
		}
	};
	
	//进入地图显示路线
	private void goToMap(){
		Intent intent = new Intent(this,BusLineToMapViewActivity.class);
		if(mTabIndex == busTran){
			BusRoute br = (BusRoute)mFirstData[mFirstIndex];
			mNaviManager.setmResultBusRoute(br);
			intent.putExtra("SearchBusType", busTran);
		}else if(mTabIndex == busRoad){
			BusLine bl = (BusLine)mSecendData[0];
			mNaviManager.setmResultBusLine(bl);
			intent.putExtra("SearchBusType", busRoad);
		}else if(mTabIndex == busStation){
			BusLine bl = (BusLine)mThirdData[0];
			mNaviManager.setmResultBusLine(bl);
			intent.putExtra("SearchBusType", busStation);
		}
		startActivity(intent);
	}
	
	
	//显示在线按钮 或 进入地图按钮
	private void showOnlineBtn(boolean isShowOnlineBtn){
		if(isShowOnlineBtn){
			mMapViewBtn.setVisibility(View.GONE);
		}else{
			mMapViewBtn.setVisibility(View.VISIBLE);
		}
	}
	
	/**
	 * 修改公交导航路线规划方式背景 - 最快到达，最少换乘，最少步行，不乘地铁
	 */
	private void changeBusPlanBackGround(){
		mRadioTime.setBackgroundResource(R.drawable.uncheck);
		mRadioTransfer.setBackgroundResource(R.drawable.uncheck);
		mRadioWalk.setBackgroundResource(R.drawable.uncheck);
		mCheckSubway.setBackgroundResource(R.drawable.uncheck);
		switch (mRoutePlan.userOption)
		{
			case 2:
				mRadioTime.setBackgroundResource(R.drawable.check);
				break;
			case 1:
				mRadioTransfer.setBackgroundResource(R.drawable.check);
				break;
			case 0:
				mRadioWalk.setBackgroundResource(R.drawable.check);
				break;
			case 3:
				mCheckSubway.setBackgroundResource(R.drawable.check);
				break;
		}
	}
	
	//根据查询类型切换按钮样式
	private void changeBootomBtn(View v){
		switch(v.getId()){
		case R.id.btn_tran:
			//换乘
			mTranResultItemAdapter = new TranResultItemAdapter(this,new BusRoute[0],1,0);
			mTranListView.setAdapter(mTranResultItemAdapter);
			headView.findViewById(R.id.layout_grid).setVisibility(View.GONE);
			headView.findViewById(R.id.line).setVisibility(View.GONE);
			footView.findViewById(R.id.layout_grid).setVisibility(View.GONE);
			footView.findViewById(R.id.line).setVisibility(View.GONE);
			
			mTransferLayout.setVisibility(View.VISIBLE);
			mRoadLayout.setVisibility(View.GONE);
			mStationLayout.setVisibility(View.GONE);
			mTransferBtn.setBackgroundResource(R.drawable.btn_bottom_bg_press);
			mRoadBtn.setBackgroundResource(R.drawable.btn_bottom_bg_normal);
			mStationBtn.setBackgroundResource(R.drawable.btn_bottom_bg_normal);
			mTransferBtn.setTextColor(this.getResources().getColor(R.color.press_text));
			mRoadBtn.setTextColor(this.getResources().getColor(R.color.black));
			mStationBtn.setTextColor(this.getResources().getColor(R.color.black));
			break;
		case R.id.btn_road:
			//路线
			mResultItems = new Object[0];
			mOtherResultItemAdapter = new OtherResultItemAdapter(this);
			mRoadListView.setAdapter(mOtherResultItemAdapter);
			
			mTransferLayout.setVisibility(View.GONE);
			mRoadLayout.setVisibility(View.VISIBLE);
			mStationLayout.setVisibility(View.GONE);
			mTransferBtn.setBackgroundResource(R.drawable.btn_bottom_bg_normal);
			mRoadBtn.setBackgroundResource(R.drawable.btn_bottom_bg_press);
			mStationBtn.setBackgroundResource(R.drawable.btn_bottom_bg_normal);
			mTransferBtn.setTextColor(this.getResources().getColor(R.color.black));
			mRoadBtn.setTextColor(this.getResources().getColor(R.color.press_text));
			mStationBtn.setTextColor(this.getResources().getColor(R.color.black));
			break;
		case R.id.btn_station:
			//站点
			mResultItems = new Object[0];
			mOtherResultItemAdapter = new OtherResultItemAdapter(this);
			mStationListView.setAdapter(mOtherResultItemAdapter);
			
			mTransferLayout.setVisibility(View.GONE);
			mRoadLayout.setVisibility(View.GONE);
			mStationLayout.setVisibility(View.VISIBLE);
			mTransferBtn.setBackgroundResource(R.drawable.btn_bottom_bg_normal);
			mRoadBtn.setBackgroundResource(R.drawable.btn_bottom_bg_normal);
			mStationBtn.setBackgroundResource(R.drawable.btn_bottom_bg_press);
			mTransferBtn.setTextColor(this.getResources().getColor(R.color.black));
			mRoadBtn.setTextColor(this.getResources().getColor(R.color.black));
			mStationBtn.setTextColor(this.getResources().getColor(R.color.press_text));
			break;
		}
	}
	
	/**
	 * 根据城市名称获取城市id
	 * 
	 * @param cname
	 * @return
	 */
	private int getIdByCityName(String cname) {
		// 根据城市名称获取城市中心点
		WmrObject wb = new WmrObject(0);
		int id = wb.getChildIdByName(cname);
		if (id == WmrObject.INVALID_ID) {
			int zzid = wb.getChildIdByName(Config.SEARCH_DEFAULT_PROVINCE);
			WmrObject wb2 = new WmrObject(zzid);
			id = wb2.getChildIdByName(cname);
		}
		return id;
	}
	
	/**
	 * 加载等待
	 */
	public void waitDialog(String msg,boolean flag) {
		myDialog = new ProgressDialog(BusActivity.this);
		myDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		myDialog.setMessage(msg);
		myDialog.setIndeterminate(false);
		myDialog.setCancelable(flag);
		myDialog.show();
	}
	
	/**
	 * 关闭等等
	 */
	public void disDialog(){
		if(myDialog!=null){
			myDialog.dismiss();
		}
	}
	//返回
	private void goBack(){
		if(mTabIndex == 0){
			if(level == 1){
				finish();
			}else if(level == 2){
				mResultObjects = mFirstData;
				//显示list
				if(mResultObjects!=null){
					mTranResultItemAdapter = new TranResultItemAdapter(this,mResultObjects,1,0);
					mTranListView.setAdapter(mTranResultItemAdapter);
				}
				headView.findViewById(R.id.layout_grid).setVisibility(View.GONE);
				headView.findViewById(R.id.line).setVisibility(View.GONE);
				footView.findViewById(R.id.layout_grid).setVisibility(View.GONE);
				footView.findViewById(R.id.line).setVisibility(View.GONE);
				level = 1;
				showOnlineBtn(true);
			}
		}else if(mTabIndex == 1){
			if(level == 1){
				finish();
			}else if(level == 2){
				mResultObjects = mFirstData;
				mResultItems = new Object[mResultObjects.length];
				for (int i = 0; i < mResultObjects.length; i++) {
					if(mResultObjects[i] instanceof BusLine){
						BusLine line = (BusLine)mResultObjects[i];
						mResultItems[i] = line.getId();
					}else{
						finish();
					}
				
				}
				mOtherResultItemAdapter = new OtherResultItemAdapter(this);
				mRoadListView.setAdapter(mOtherResultItemAdapter);
				level = 1;
				showOnlineBtn(true);
			}
		}else if(mTabIndex == 2){
			if(level == 1){
				finish();
			}else if(level == 2){
				mResultObjects = mFirstData;
				mResultItems = new String[mResultObjects.length];
				for (int i = 0; i < mResultObjects.length; i++) {
					if(mResultObjects[i] instanceof BusStation){
						BusStation station = (BusStation)mResultObjects[i];
						mResultItems[i] = station.name + " ("+mBusLineTypeStrings[station.type]+" "+station.lines.length+")";
					}else{
						finish();
					}
						
						
				}
				mOtherResultItemAdapter = new OtherResultItemAdapter(this);
				mStationListView.setAdapter(mOtherResultItemAdapter);
				level = 1;
				showOnlineBtn(true);
			}else if(level == 3){
				handler.sendEmptyMessage(2);
				showOnlineBtn(true);
			}
		}
	}
	
	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event) {
		// 点击返回按钮，退出系统
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			goBack();
			return true;
		}
		return false;
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
		//销毁worldManager
		WorldManager.getInstance().cleanup();
		BusQuery.getInstance().cleanup();
	}
	
	/**
	 * ListView数据适配器
	 * @author malw
	 *
	 */
	private class TranResultItemAdapter extends BaseAdapter {
		
		private BusRoute[] mResultObjects;
		private LayoutInflater mLayoutInflater = null;
		private int mType = 1; //1父类 2子类
		private int mIndex = 0;
		
		public TranResultItemAdapter(Context mContext,Object[] mResultObjects,int type,int index) {
			super();
			this.mResultObjects = (BusRoute[])mResultObjects;
			mLayoutInflater = LayoutInflater.from(mContext);
			this.mType = type;
			this.mIndex = index;
		}

		@Override
		public int getCount() {
			if(mType == 1){
				return mResultObjects.length;
			}else if(mType == 2){
				return mResultObjects[mIndex].segments.length;
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View view, ViewGroup parent) {
			if(mType == 1){
				BusRouteItem bri;
				if (view == null) {
					bri = new BusRouteItem();
					view = mLayoutInflater.inflate(R.layout.item_bus_route, null);
					view.setTag(bri);
				} else{
					bri = (BusRouteItem) view.getTag();
				}
				bri.layout = (RelativeLayout)view.findViewById(R.id.bus_item);
				bri.num = (TextView)view.findViewById(R.id.tv_bus_route_num);
				bri.brief = (TextView)view.findViewById(R.id.tv_bus_route_brief);
				bri.other = (TextView)view.findViewById(R.id.tv_bus_route_other);
				
				BusRoute busRoute = mResultObjects[position];
				bri.num.setText(String.valueOf(position+1));
				String brief = busRoute.transferBrief.replaceAll(":", "<br/><font color='red'><b>换乘: </b></font>");
				bri.brief.setText(Html.fromHtml(brief));
				bri.other.setText(busRoute.travelTime+"分钟 / "+busRoute.distance+"米");
				bri.layout.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						mFirstIndex = position;
						showChild(mFirstIndex);
					}
				});
			}else if(mType == 2){
				Segment se;
				if (view == null) {
					se = new Segment();
					view = mLayoutInflater.inflate(R.layout.item_bus_segment, null);
					view.setTag(se);
				} else{
					se = (Segment) view.getTag();
				}
				se.layout = (RelativeLayout)view.findViewById(R.id.bus_item);
				se.icon = (ImageView)view.findViewById(R.id.iv_bus_segment_icon);
				se.detail = (TextView)view.findViewById(R.id.tv_bus_segment_detail);
				
				final BusSegmentBase sebase = (BusSegmentBase)mResultObjects[mIndex].segments[position];
				StringBuffer stringBuilder = new StringBuffer();
				if (sebase.type == BusLine.Type.bus
						|| sebase.type == BusLine.Type.subway) {
					final BusSegment segment = (BusSegment)sebase;
					if (segment.stationIds.length > 0) {
						//type  "none", "地铁", "公交", "步行" 
						switch(segment.type){
						case 0:
//							se.icon.setBackgroundResource(R.drawable.btn_poi_normal);
							break;
						case 1:
							se.icon.setBackgroundResource(R.drawable.subway);
							break;
						case 2:
							se.icon.setBackgroundResource(R.drawable.bus);
							break;
						}
						String html = "乘坐<font color='green'><b>" + segment.lineName + "</b></font>, 从" + segment.stationNames[0] + "站到" + segment.stationNames[segment.stationNames.length-1] + "站";
						se.detail.setText(Html.fromHtml(html));
						se.layout.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								mNaviManager.setmResultPoints(segment.points);
								Intent intent = new Intent(BusActivity.this,BusLineToMapViewActivity.class);
								intent.putExtra("SearchBusType", BusLineToMapViewActivity.mSegment);
								intent.putExtra("routeType", sebase.type);
								startActivity(intent);
							}
						});
					}
					else{
						se.icon.setBackgroundResource(R.drawable.subway);
						String html = "乘坐<font color='green'><b>" + segment.lineName + "</b></font>";
						se.detail.setText(Html.fromHtml(html));
					}
				} else if (sebase.type == BusLine.Type.walk) {
					if(position == 0){
						view.setLayoutParams(lp);
					}else{
						se.icon.setBackgroundResource(R.drawable.walk);
						final BusWalkSegment wsegment = (BusWalkSegment)sebase;
						String des = wsegment.desc;
						if("".equals(des)){
							view.setLayoutParams(lp);
							se.detail.setText("2");
						}else{
							stringBuilder.append(wsegment.desc);
							se.detail.setText(stringBuilder.toString());
							se.layout.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									mNaviManager.setmResultPoints(wsegment.points);
									Intent intent = new Intent(BusActivity.this,BusLineToMapViewActivity.class);
									intent.putExtra("SearchBusType", BusLineToMapViewActivity.mSegment);
									intent.putExtra("routeType", sebase.type);
									startActivity(intent);
								}
							});
						}
					}
				}else{
					view.setLayoutParams(lp);
				}
			}
			return view;
		}
	}
	
	private class OtherResultItemAdapter extends BaseAdapter {
		private Context mContext;
		public OtherResultItemAdapter(Context context) {
			mContext = context;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mResultItems.length;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return arg0;
		}

		@Override
		public View getView(final int postion, View view, ViewGroup viewGroup) {
			// TODO Auto-generated method stub
			OtherItem oi;
			if (view == null) {
				oi = new OtherItem();
				view = LayoutInflater.from(mContext).inflate(R.layout.item_bus_other_result, null);
				view.setTag(oi);
			} else {
				oi = (OtherItem)view.getTag();
			}
			oi.layout = (RelativeLayout)view.findViewById(R.id.other_layout);
			oi.tv = (TextView)view.findViewById(R.id.other_text);
			oi.de = (TextView)view.findViewById(R.id.other_detail);
			if(mQueryType == busRoad){
				oi.tv.setText(((String)mResultItems[postion]));
			}else if(mQueryType == busLineDetail){
				oi.tv.setText(((String)mResultItems[postion]));
			}else if(mQueryType == busStation){
				oi.tv.setText(((String)mResultItems[postion]));
			}else if(mQueryType == subwayEntrance){
				oi.tv.setText(((String)mResultItems[postion]));
			}
			oi.de.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(mQueryType == busRoad){
						if(level == 1){
							mQueryType = busLineDetail;
							mFirstIndex = postion;
							level = 2;
							mBusQuery.queryBusLineDetail(((BusLine)mFirstData[mFirstIndex]));
						}else if(level == 2){
							Intent intent = new Intent(BusActivity.this,BusLineToMapViewActivity.class);
							intent.putExtra("SearchBusType", BusLineToMapViewActivity.mAnnotPoint);
							BusLine bl = (BusLine)mSecendData[0];
							Point point = bl.stationPositions[postion];
							intent.putExtra("lat", point.x);
							intent.putExtra("lon", point.y);
							intent.putExtra("name", (String)mResultItems[postion]);
							startActivity(intent);
						}
					}else if(mQueryType == busStation){
						if(level == 1){
							mFirstIndex = postion;
							handler.sendEmptyMessage(2);
						}else if(level == 2){
							mQueryType = busLineDetail;
							level = 3;
							mBusQuery.queryBusLineDetail((BusLine)mSecendData[postion]);
						}else if(level == 3){
							Intent intent = new Intent(BusActivity.this,BusLineToMapViewActivity.class);
							intent.putExtra("SearchBusType", BusLineToMapViewActivity.mAnnotPoint);
							BusLine bl = (BusLine)mThirdData[0];
							Point point = bl.stationPositions[postion];
							intent.putExtra("lat", point.x);
							intent.putExtra("lon", point.y);
							intent.putExtra("name", (String)mResultItems[postion]);
							startActivity(intent);
						}
					}
				}
			});
			
			return view;
		}
	}
	
	class BusRouteItem{
		RelativeLayout layout;
		TextView num;
		TextView brief;
		TextView other;
	}
	
	class Segment{
		RelativeLayout layout;
		ImageView icon;
		TextView detail;
	}
	
	class OtherItem{
		RelativeLayout layout;
		TextView tv;
		TextView de;
	}
}
