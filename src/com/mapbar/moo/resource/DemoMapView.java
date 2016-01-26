package com.mapbar.moo.resource;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.mapbar.map.Annotation;
import com.mapbar.map.ArrowOverlay;
import com.mapbar.map.CalloutStyle;
import com.mapbar.map.IconOverlay;
import com.mapbar.map.MapRenderer;
import com.mapbar.map.MapState;
import com.mapbar.map.MapView;
import com.mapbar.map.ModelOverlay;
import com.mapbar.map.RouteOverlay;
import com.mapbar.map.Vector2DF;
import com.mapbar.mapdal.NaviCoreUtil;
import com.mapbar.mapdal.NdsPoint;
import com.mapbar.mapdal.PoiFavorite;
import com.mapbar.navi.ArrowInfo;
import com.mapbar.navi.CameraData;
import com.mapbar.navi.ExpandView;
import com.mapbar.navi.HighwayGuide;
import com.mapbar.navi.HighwayGuideItem;
import com.mapbar.navi.NaviSession;
import com.mapbar.navi.NaviSpeaker;
import com.mapbar.navi.RouteBase;
import com.mapbar.navi.RouteCollection;
import com.mapbar.navi.RoutePlan;

public class DemoMapView extends MapView {
	private Context mContext;
	private static final int CAMERAS_MAX = 16;
	private boolean mInited = false;
	public static Handler mHandler;
	private static final int[] mRouteOverlayColors = { 0xffaa0000, 0xff00aa00,
			0xff0000aa, 0xff4578fc };
	// 小车
//	private IconOverlay mCarIcon = null;
	private ModelOverlay mCarOverlay = null;
	// 电子眼
	private Annotation[] mCameraAnnotations = null;
	// 箭头
	private ArrowOverlay mArrowOverlay = null;
	// 小车状态
	private boolean mIsLockedCar = false;
	private float mCarOriented = 0.0f;
	private Point mCarPosition = null;
	// 路线计划
	private RoutePlan mRoutePlan = null;
	// 地图当前的状态
	private MapState mMapState = null;
	// 需要绘制的路线
	private RouteOverlay[] mRouteCollectionOverlays = null;
	private int mRouteOverlayNumber = 0;
	// 用来绘制放大图
	private ImageView mExpandView = null;
	public Bitmap mBitmap = null;
	// 选中的POI
	private Annotation mPoiAnnotation = null;
	private Annotation mPositionAnnotation = null;

	private MapRenderer mRenderer = null;

	private static final float ZOOM_STEP = 0.5f;
	private Vector2DF mZoomLevelRange = null;
	private static final int BITMAP_WIDTH = 480;
	private static final int BITMAP_HEIGHT = 480;
	
	//private Annotation customerAnimation;

	public static Point mClickPoint = null;

	public boolean isInited() {
		return mInited;
	}

	private void init(Context context) {
		mContext = context;
		mIsLockedCar = true;
		mRouteCollectionOverlays = new RouteOverlay[4];
	}

	// TODO: 拋出異常
	
	public DemoMapView(Context context) {
		super(context);
		init(context);
	}

	public DemoMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public void setZoomHandler (Handler handler) {
		mHandler = handler;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
		// 防止重复创建
		if (!mInited) {
			mRenderer = super.getMapRenderer();
			if(mRenderer == null)
				return;
			mRenderer.setWorldCenter(Config.centerPoint);
			mClickPoint = new Point(mRenderer.getWorldCenter());
			Vector2DF pivot = new Vector2DF(0.5f, 0.82f);
			// 初始化Overlay和Annotation 添加车标
//			mCarIcon = new IconOverlay("res/cars.png", true);
//			mCarIcon.markAsAnimated(4,
//					"a1000;b30;c30;d30;c40;b60;a200;b30;c30;d30;c40;b60;");
//			mCarIcon.setPosition(mClickPoint);
//			mCarIcon.setOrientAngle(0.0f);
//			mRenderer.addOverlay(mCarIcon);

			mCarOverlay = new ModelOverlay(NaviCoreUtil.buildPathInPacket("models/car_model.obj"), true);

			mCarOverlay.setScaleFactor(0.3f);
			mCarOverlay.setPosition(mClickPoint);
			mCarOverlay.setHeading(0);
			mRenderer.addOverlay(mCarOverlay); 

			// 添加点击气泡
			mPoiAnnotation = new Annotation(2, mClickPoint, 1101, pivot);
			mPositionAnnotation = new Annotation(2, mClickPoint, 1101, pivot);
			
			CalloutStyle calloutStyle = mPoiAnnotation.getCalloutStyle();
			calloutStyle.anchor.set(0.5f, 0.5f);
//			calloutStyle.leftIcon = 1002;
			calloutStyle.rightIcon = 1001;
			mPoiAnnotation.setCalloutStyle(calloutStyle);
			mPositionAnnotation.setTitle("选取点");
			mPositionAnnotation.setCalloutStyle(calloutStyle);
			mRenderer.addAnnotation(mPoiAnnotation);
			mRenderer.addAnnotation(mPositionAnnotation);
			showAnnotation(null);
			
			
		/*	customerAnimation=new Annotation(2, mClickPoint, 1004, pivot);
			mRenderer.addAnnotation(customerAnimation);
			customerAnimation.setTag(2);
			customerAnimation.showCallout(true);*/

			// 电子眼
			mCameraAnnotations = new Annotation[CAMERAS_MAX];
			Vector2DF cameraPivot = new Vector2DF(0.5f, 0.9f);
			for (int i = 0; i < mCameraAnnotations.length; i++) {
				mCameraAnnotations[i] = new Annotation(2, mClickPoint, 1300,
						cameraPivot);
				mCameraAnnotations[i].setHidden(true);
				mRenderer.addAnnotation(mCameraAnnotations[i]);
			}

			// 实例化路线计划
			mRoutePlan = new RoutePlan();
			mInited = true;
			//创建完毕通知
			if(mHandler!=null){
				mHandler.sendEmptyMessage(1);
			}
		}
	}

	/**
	 * 初始化放大图绘制使用的view
	 * 
	 * @param view
	 */
	public void setExpandView(ImageView view) {
		mExpandView = view;
		ExpandView.resizeScreen(BITMAP_WIDTH, BITMAP_HEIGHT);
	}

	/**
	 * 开始模拟导航
	 */
	public void startSimulation() {
		backupMapStateBeforeSimulation();
	}

	/**
	 * 结束模拟导航
	 */
	public void endSimulation() {
		resetMapStateAfterSimulation();
		if (mArrowOverlay != null) {
			mRenderer.removeOverlay(mArrowOverlay);
			mArrowOverlay = null;
		}
		mBitmap = null;
		drawCameras(null);
	}

	public void drawHighwayGuide() {
		HighwayGuideItem[] items = HighwayGuide.getItems();
		for (int i = 0; i < items.length; i++) {
			Log.d("[drawHighwayGuid]", items[i].toString());
		}
	}
	
	/**
	 * 绘制放大图
	 */
	public void drawExpandView () {
		if (ExpandView.shouldDisplay()) {
			if (mBitmap == null)
				mBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT,Bitmap.Config.ARGB_8888);
			ExpandView.render(mBitmap);
			mExpandView.setImageBitmap(mBitmap);
		} else {
			mBitmap = null;
		}
		if (ExpandView.shouldDisplayOpenButton()) {

		}
	}
	
	/**
	 * 删除箭头
	 */
	public void delArrow(){
		if (mArrowOverlay != null) {
			mRenderer.removeOverlay(mArrowOverlay);
			mArrowOverlay = null;
		}
	}
	
	/**
	 * 绘制指定Maneuver的箭头
	 * 
	 * @param routeBase
	 *            路线
	 */
	public void drawArrow(ArrowInfo arrowInfo) {
		//ArrowInfo arrowInfo = ArrowRenderer.getArrowInfo();
		if (arrowInfo.valid()) {
			if (mArrowOverlay != null) {
				mRenderer.removeOverlay(mArrowOverlay);
				mArrowOverlay = null;
			}
			mArrowOverlay = new ArrowOverlay(arrowInfo);
			mRenderer.addOverlay(mArrowOverlay);
		}
		arrowInfo = null;
	}
	
	/**
	 * 绘制摄像头
	 * 
	 * @param cameras
	 *            需要绘制的摄像头数组
	 */
	public void drawCameras(final CameraData[] cameras) {
		for (int i = 0; i < mCameraAnnotations.length; i++) {
			mCameraAnnotations[i].setHidden(true);
		}
		if (cameras != null) {
			for (int i = 0; i < cameras.length; i++) {
				mCameraAnnotations[i].setPosition(cameras[i].position);
				mCameraAnnotations[i].setHidden(false);
			}
		} else {
			Log.e("[GLMapRenderer]",
					"===================================Camera is null====================================");
		}
	}

	/**
	 * 绘制出多条路线
	 * 
	 * @param routeCollection
	 */
	public void drawRoutes(RouteCollection routeCollection, int routeIndex) {
		// 绘制前先清空路线
		removeRouteOverlay(true);
		mRouteOverlayNumber = routeCollection.num;
		// 把所有的路线先绘制出来
		for (int i = 0; i < routeCollection.num; i++) {
			mRouteCollectionOverlays[i] = new RouteOverlay(
					routeCollection.routes[i]);
			 mRouteCollectionOverlays[i].setColor(mRouteOverlayColors[i]);
//			mRouteCollectionOverlays[i].enableTmcColors(true);
			// 显示默认选中的路线 其它路线先隐藏
			mRouteCollectionOverlays[i].setHidden(i != routeIndex);
			// 默认将一条路线画到地图上
			mRenderer.addOverlay(mRouteCollectionOverlays[i]);
		}
	}

	/**
	 * 绘制单挑条路线
	 * 
	 * @param routeCollection
	 */
	public void drawRoutes(RouteBase routeBase,boolean isTmc) {
		//绘制前先清空路线
		removeRouteOverlay(true);
		mRouteCollectionOverlays[0] = new RouteOverlay(routeBase);
		if(isTmc){
			mRouteCollectionOverlays[0].enableTmcColors(isTmc);
		}else{
			mRouteCollectionOverlays[0].setColor(mRouteOverlayColors[0]);
		}
//		mRouteCollectionOverlays[0].setOutlineColor(0xffff0000);
		mRouteCollectionOverlays[0].setOutlineColor(0xff4578fc);
		// 显示
		mRouteCollectionOverlays[0].setHidden(false);
		mRouteOverlayNumber = 1;
		// 默认将一条路线画到地图上
		mRenderer.addOverlay(mRouteCollectionOverlays[0]);
	}
	
	/**
	 * 设置路线是否开启Tmc模式
	 */
	public void setRouteTmc(boolean isTmc){
		if(mRouteCollectionOverlays[0] != null){
			if(isTmc){
				mRouteCollectionOverlays[0].enableTmcColors(isTmc);
			}else{
				mRouteCollectionOverlays[0].enableTmcColors(isTmc);
				mRouteCollectionOverlays[0].setColor(mRouteOverlayColors[0]);
			}
		}
	}

	/**
	 * 将路线显示在地图上
	 * 
	 * @param index
	 */
	public void drawRouteToMap(int index) {
		if (mRouteCollectionOverlays != null && index < mRouteOverlayNumber) {
			mRouteCollectionOverlays[index].setHidden(false);
		}
	}

	/**
	 * 点击气泡
	 */
	@Override
	public void onAnnotationClicked(Annotation annot, int area) {
		super.onAnnotationClicked(annot, area);
		Point point = new Point(mClickPoint);
		annot.showCallout(true);
		Message msg = new Message();
		PoiFavorite fav = new PoiFavorite();
		switch (area) {
		case Annotation.Area.leftButton:
			// 气泡左侧搜索周边
			annot.showCallout(false);
			break;
		case Annotation.Area.rightButton:
			// 气泡右边导航按钮
			msg.what = 102;
			fav.name = mPoiAnnotation.getTitle();
			fav.pos = point;
			msg.obj = fav;
			if(mHandler!=null){
				mHandler.sendMessage(msg);
			}
			annot.showCallout(false);
			break;
		case Annotation.Area.middleButton:
			// 气泡中间
			msg.what = 101;
			fav.name = mPoiAnnotation.getTitle();
			fav.pos = point;
			msg.obj = fav;
			if(mHandler!=null){
				mHandler.sendMessage(msg);
			}
			annot.showCallout(false);
			break;
		case Annotation.Area.none:
		case Annotation.Area.icon:
		default:
			break;
		}
	}

	/**
	 * 选择icon
	 */
	@Override
	public void onAnnotationSelected(Annotation arg0) {
		super.onAnnotationSelected(arg0);
		arg0.showCallout(true);
		/*if(arg0.getTag()==2){
			isSelected=true;
		}*/
		
		
	}

	/**
	 * 取消icon
	 */
	@Override
	public void onAnnotationDeselected(Annotation annot) {
		super.onAnnotationDeselected(annot);
		annot.showCallout(false);
	}

	private void showAnnotation(Annotation annot) {
		mPoiAnnotation.showCallout(true);
		mPositionAnnotation.showCallout(true);
		if (annot != null) {
			annot.showCallout(true);
			// mIsAnnotationDisplaying = true;
		}
	}
	
	
	/**
	 * 点击poi
	 */
	@Override
	public void onPoiSelected(String name, Point point) {
		super.onPoiSelected(name, point);
		// TODO: 替換mRenderer方法
		mClickPoint.set(point.x,point.y);
		mPoiAnnotation.setTitle(name);
		mPoiAnnotation.setPosition(mClickPoint);
		showAnnotation(mPoiAnnotation);
		mRenderer.beginAnimations();
		mRenderer.setWorldCenter(mClickPoint);
		mRenderer.commitAnimations(500, MapRenderer.Animation.linear);
	}

	@Override
	public void onPoiDeselected(String name, Point point) {
		super.onPoiDeselected(name, point);
		mPoiAnnotation.showCallout(false);
	}

	/**
	 * 备份地图和小车的状态以便模拟导航之后可以恢复
	 */
	private void backupMapStateBeforeSimulation() {
		mMapState = mRenderer.getMapState();
		mCarPosition = mCarOverlay.getPosition();
		mCarOriented = mCarOverlay.getHeading();
	}

	/**
	 * 模拟导航结束之后恢复地图和小车之前的状态
	 */
	private void resetMapStateAfterSimulation() {
		mRenderer.setMapState(mMapState);
		mCarOverlay.setPosition(mCarPosition);
		mCarOverlay.setHeading((int)mCarOriented);
	}

	/**
	 * 设置路线起点
	 * 
	 * @param point
	 *            路线起点
	 */
	// TODO: 添加註釋修改邏輯
	public void setStartPoint(Point point) {
		PoiFavorite poi = new PoiFavorite(point);
		mRoutePlan.setStartPoint(poi);
		setCarPosition(point);
	}

	/**
	 * 设置途经点
	 * 
	 * @param point
	 *            路线起点
	 */
	public void setWayPoint(Point point){
		PoiFavorite poi = new PoiFavorite(point);
		mRoutePlan.addWayPoint(poi);
	}
	
	/**
	 * 设置目的地
	 * 
	 * @param point
	 *            目的地
	 */
	public void setDestination(Point point) {
		PoiFavorite poi = new PoiFavorite(point);
		// 如果没有起点，或者已经同时存在起点和终点，那么都应该设置车的位置为新的起点
		int m = mRoutePlan.getDestinationNum();
		if (m != 1) {
			setStartPoint(getCarPosition());
		}
		if (!mRoutePlan.setEndPoint(poi)) {
			NaviSpeaker.enqueue("终点设置失败，请先设置起点");
		}
	}

	/**
	 * 开始算路
	 * 
	 * @return
	 */
	public boolean startRoute() {
		if (mRoutePlan.getDestinationNum() > 1) {
			mRoutePlan.setUseTmc(NaviSetting.getInstance().getUseTmc());
			NaviSetting ns = NaviSetting.getInstance();
			NaviSession.getInstance().startRoute(mRoutePlan,
					ns.getRouteMethod());
			return true;
		}
		return false;
	}

	/**
	 * 控制是否锁车
	 * 
	 * @param lock
	 *            是否锁车，如果为true则锁车，否则不锁车
	 */
	public void lockCar(boolean lock) {
		if (lock != mIsLockedCar) {
			mIsLockedCar = lock;
			if (mIsLockedCar) {
				mRenderer.setWorldCenter(mCarOverlay.getPosition());
				mRenderer.setHeading(360.0f - mCarOverlay.getHeading());
				mRenderer.setViewShift(0.3f);
			}
		}
	}

	/**
	 * 判断是否为锁车状态
	 * 
	 * @return 如果锁车则返回true，否则返回false
	 */
	public boolean carIsLocked() {
		return mIsLockedCar;
	}

	/**
	 * 设置车的位置，用于在模拟导航时更新车的位置使用
	 * 
	 * @param point
	 *            车所在位置
	 */
	public void setCarPosition(Point point) {
		if (mCarOverlay != null) {
			mCarOverlay.setPosition(point);
		}
		if (mIsLockedCar && mRenderer != null) {
			mRenderer.setWorldCenter(point);
		}
	}

	/**
	 * 获取车当前的位置
	 * 
	 * @return 车当前的位置坐标
	 */
	public Point getCarPosition() {
		return mCarOverlay.getPosition();
	}

	/**
	 * 设置当前车的角度，用于导航时更新车的角度
	 * 
	 * @param ori
	 *            车的角度
	 */
	public void setCarOriented(float ori) {
		mCarOverlay.setHeading((int)ori);
		if (mIsLockedCar) {
			mRenderer.setHeading(360.0f - ori);
		}
	}

	/**
	 * 在地图指定位置显示一个POI的信息
	 * 
	 * @param point
	 *            POI所在位置
	 * @param name
	 *            POI名称
	 */
	public void showPoiAnnotation(Point point, String name) {
		mRenderer.setWorldCenter(point);
		mClickPoint.set(point.x, point.y);
		mPoiAnnotation.setTitle(name);
		mPoiAnnotation.setPosition(point);
		showAnnotation(mPoiAnnotation);
	}

	/**
	 * 获取当前车的角度
	 * 
	 * @return 车的较粗
	 */
	public float getCarOriented() {
		return (float)mCarOverlay.getHeading();
	}

	/**
	 * 删除路线
	 */
	public void removeRoute() {
		removeRouteOverlay(true);
		mRoutePlan.clearDestinations();
	}

	/**
	 * 绘制路线
	 * 
	 * @param routeBase
	 *            路线对应的RouteBase
	 * @param isTmc
	 *            是否开启tmc
	 */
	public void setRoute(RouteBase routeBase, boolean isTmc) {
		removeRouteOverlay(true);
		drawRoutes(routeBase, isTmc);
	}

	/**
	 * 将指定的路线隐藏
	 * 
	 * @param index
	 */
	public void removeRouteOverlay(int index) {
		if (mRouteCollectionOverlays[index] != null) {
			mRenderer.removeOverlay(mRouteCollectionOverlays[index]);
			mRouteCollectionOverlays[index] = null;
		}
	}

	/**
	 * 删除所有路线
	 * 
	 * @param removeAll
	 */
	private void removeRouteOverlay(boolean removeAll) {
		for (int i = 0; i < mRouteOverlayNumber; i++) {
			if (removeAll) {
				removeRouteOverlay(i);
			}
		}
		if (removeAll) {
			mRouteOverlayNumber = 0;
		} else {
			mRouteOverlayNumber = 1;
		}
		if (mArrowOverlay != null) {
			mRenderer.removeOverlay(mArrowOverlay);
			mArrowOverlay = null;
		}
	}

	/**
	 * 地图放大操作
	 * 
	 * @param zoomIn
	 *            放大按钮
	 * @param zoomOut
	 *            缩小按钮
	 */
	public void mapZoomIn(ImageView zoomIn, ImageView zoomOut) {
		float zoomLevel = mRenderer.getZoomLevel();
		if (mZoomLevelRange == null) {
			mZoomLevelRange = mRenderer.getZoomLevelRange();
		}
		zoomLevel = zoomLevel + ZOOM_STEP;
		if (zoomLevel >= mZoomLevelRange.getY()) {
			zoomLevel = mZoomLevelRange.getY();
			zoomIn.setEnabled(false);
		}
		zoomOut.setEnabled(true);
		mRenderer.beginAnimations();
		mRenderer.setZoomLevel(zoomLevel);
		mRenderer.commitAnimations(300, MapRenderer.Animation.linear);
	}

	/**
	 * 地图缩小操作
	 * 
	 * @param zoomIn
	 *            放大按钮
	 * @param zoomOut
	 *            缩小按钮
	 */
	public void mapZoomOut(ImageView zoomIn, ImageView zoomOut) {
		float zoomLevel = mRenderer.getZoomLevel();
		if (mZoomLevelRange == null) {
			mZoomLevelRange = mRenderer.getZoomLevelRange();
		}
		zoomLevel = zoomLevel - ZOOM_STEP;
		if (zoomLevel <= mZoomLevelRange.getX()) {
			zoomLevel = mZoomLevelRange.getX();
			zoomOut.setEnabled(false);
		}
		zoomIn.setEnabled(true);
		mRenderer.beginAnimations();
		mRenderer.setZoomLevel(zoomLevel);
		mRenderer.commitAnimations(300, MapRenderer.Animation.linear);
	}

	/**
	 * 检查网络wifi 2G 3G网络
	 * 
	 * @return TODO
	 */
	public boolean isOpenNet() {
		ConnectivityManager connManager = (ConnectivityManager) mContext
				.getApplicationContext().getSystemService(
						Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
		if (networkInfo != null) {
			return networkInfo.isAvailable();
		}
		return false;
	}
	
	/**
	 * 检查gps是否开启
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public boolean isOpenGps(){
		return Settings.Secure.isLocationProviderEnabled(mContext.getContentResolver(), LocationManager.GPS_PROVIDER);
	}
	

	@Override
	public void onCameraChanged(int changeTye) {
		super.onCameraChanged(changeTye);
		MapRenderer render = getMapRenderer();
		if(render != null){
			if(changeTye == MapRenderer.CameraSetting.zoomLevel+MapRenderer.CameraSetting.scale){
				//地图缩放
				zoomChange();
			}
		}
	}
	
	// ////////////////////////////////////////////////
	// OnTouchListener
	// ////////////////////////////////////////////////
//	private boolean isSelected=false;

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		if (mGestureDetector.onTouchEvent(event)) {
			return true;
		}
		int actionAndIndex = event.getAction();
		int action = actionAndIndex & MotionEvent.ACTION_MASK;
		NdsPoint point = mRenderer.screen2WorldNds(new PointF(event.getX(), event.getY()));
		switch(action){
		case MotionEvent.ACTION_MOVE:
			//手动无级缩放时 要注意改变放大缩小是否可用 '
			
				zoomChange();
			
		}
		return super.onTouch(v, event);
	}
	
	/**
	 * 缩放级别改变
	 */
	public void zoomChange(){
		if(mRenderer==null)
			return;
		float zoomLevel = mRenderer.getZoomLevel();
		Message msg = new Message();
		msg.what=100;
		Bundle b = msg.getData();
		// 默认都可用
		b.putBoolean("zoomIn", true);
		b.putBoolean("zoomOut", true);
		if (mZoomLevelRange == null) {
			mZoomLevelRange = mRenderer.getZoomLevelRange();
		}
		//判断放大缩小是否可用
		if (zoomLevel <= mZoomLevelRange.getX()) {
			b.putBoolean("zoomIn", false);
		}
		if (zoomLevel >= mZoomLevelRange.getY()) {
			b.putBoolean("zoomOut", false);
		}
		// 发送消息
		if(mHandler!=null){
			mHandler.sendMessage(msg);
		}
	}

	@SuppressWarnings("deprecation")
	private GestureDetector mGestureDetector = new GestureDetector(
			new OnGestureListener() {

				@Override
				public boolean onSingleTapUp(MotionEvent arg0) {
					return false;
				}

				@Override
				public void onShowPress(MotionEvent arg0) {

				}

				@Override
				public boolean onScroll(MotionEvent arg0, MotionEvent arg1,
						float arg2, float arg3) {
					return false;
				}

				@Override
				public void onLongPress(MotionEvent event) {
					int pointerCount = event.getPointerCount();
					if (pointerCount == 1) {
						MapRenderer mr = mRenderer;
						Point point = mRenderer.screen2World(new PointF(event
								.getX(), event.getY()));
						mClickPoint.set(point.x, point.y);
						mPositionAnnotation.setPosition(mClickPoint);
						showAnnotation(mPositionAnnotation);
						mr.beginAnimations();
						mr.setWorldCenter(mClickPoint);
						mr.commitAnimations(500, MapRenderer.Animation.linear);
					}
				}

				@Override
				public boolean onFling(MotionEvent arg0, MotionEvent arg1,
						float arg2, float arg3) {
					return false;
				}

				@Override
				public boolean onDown(MotionEvent arg0) {
					return false;
				}
			});
	
}
