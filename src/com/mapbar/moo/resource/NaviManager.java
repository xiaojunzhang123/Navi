package com.mapbar.moo.resource;

import com.mapbar.bus.BusLine;
import com.mapbar.bus.BusRoute;

import android.graphics.Point;



public class NaviManager {
	//导航管理类
	private static NaviManager naviManager = new NaviManager();
	//公共保存对象
	private BusRoute mResultBusRoute;
	private BusLine mResultBusLine;
	private Point[] mResultPoints;
	
	public static NaviManager getNaviManager() {
		return naviManager;
	}

	public static void setNaviManager(NaviManager naviManager) {
		NaviManager.naviManager = naviManager;
	}

	public Point[] getmResultPoints() {
		return mResultPoints;
	}

	public void setmResultPoints(Point[] mResultPoints) {
		this.mResultPoints = mResultPoints;
	}

	public BusRoute getmResultBusRoute() {
		return mResultBusRoute;
	}

	public void setmResultBusRoute(BusRoute mResultBusRoute) {
		this.mResultBusRoute = mResultBusRoute;
	}

	public BusLine getmResultBusLine() {
		return mResultBusLine;
	}

	public void setmResultBusLine(BusLine mResultBusLine) {
		this.mResultBusLine = mResultBusLine;
	}
	
}
