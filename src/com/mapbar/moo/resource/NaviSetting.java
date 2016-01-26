package com.mapbar.moo.resource;

import com.mapbar.mapdal.WmrObject;
import com.mapbar.navi.NaviSession;
import com.mapbar.navi.RouteBase;
import com.mapbar.poiquery.PoiQuery;

/**
 * 导航、搜索相关参数的基础设置
 */
public class NaviSetting {

	public static final int FROM_NONE = 0;
	public static final int FROM_SEARCH_RESULT = 1;
	public static final int FROM_BUS_SEARCH = 2;

	private static NaviSetting mInstance = null;
	/**
	 * 当前路线详情的生成模式
	 * 
	 * @see RouteBase.DirectionsFlag
	 */
	private int mRouteDirectionsFlag;
	/**
	 * 当前的算路方式
	 * 
	 * @see NaviSession.RouteMethod
	 */
	private int mRouteMethod;
	/**
	 * 当前城市的Id
	 * 
	 * @see WmrObject
	 */
	private int mCurrentCityId;
	/**
	 * 搜索模式
	 * 
	 * @see PoiQuery.Mode
	 */
	private int mPoiQueryMode;
	/**
	 * 是否规避拥堵
	 */
	private boolean mUseTmc;

	public static NaviSetting getInstance () {
		if (mInstance == null) {
			mInstance = new NaviSetting();
		}
		return mInstance;
	}

	private NaviSetting() {
		mRouteDirectionsFlag = RouteBase.DirectionsFlag.origin;
		mRouteMethod = NaviSession.RouteMethod.single;
		mCurrentCityId = WmrObject.INVALID_ID;
		mPoiQueryMode = PoiQuery.Mode.online;
	}

	public void setRouteDirectionsFlag (int routeDirectionsFlag) {
		mRouteDirectionsFlag = routeDirectionsFlag;
	}

	public int getRouteDirectionsFlag () {
		return mRouteDirectionsFlag;
	}

	public void setRouteMethod (int routeMethod) {
		mRouteMethod = routeMethod;
	}

	public int getRouteMethod () {
		return mRouteMethod;
	}

	public void setCurrentCityId (int id) {
		mCurrentCityId = id;
	}

	public int getCurrentCityId () {
		return mCurrentCityId;
	}

	public void setPoiQueryMode (int mode) {
		mPoiQueryMode = mode;
	}

	public int getPoiQueryMode () {
		return mPoiQueryMode;
	}

	public boolean getUseTmc() {
		return mUseTmc;
	}

	public void setUseTmc(boolean mUseTmc) {
		this.mUseTmc = mUseTmc;
	}
}
