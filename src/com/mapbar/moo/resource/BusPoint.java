package com.mapbar.moo.resource;

import android.graphics.Point;

public class BusPoint {
	private int type;  // 1地铁 2公交 3步行
	private Point[] points;
	
	public BusPoint(int type,Point[] points){
		this.type = type;
		this.points = points;
	}
	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}

	public Point[] getPoints() {
		return points;
	}



	public void setPoints(Point[] points) {
		this.points = points;
	}
	
	
}
