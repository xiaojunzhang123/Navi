package com.mapbar.moo.resource;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbar.mapdal.WmrObject;
import com.mapbar.mapdal.WorldManager;
import com.mapbar.moo.R;
import com.mapbar.poiquery.PoiType;
import com.mapbar.poiquery.PoiTypeManager;

public class SelectActivity extends Activity implements OnChildClickListener,OnGroupExpandListener,OnGroupClickListener {
	
	private static String TAG = "[SelectActivity]";
	//一级信息
	private ArrayList<Integer> mParent = null;
	//二级信息
	private ArrayList<ArrayList<Integer>> mChildren = null;
	//分级控件
	private ExpandableListView mExpandableListView = null;
	//获取信息类型 1获取城市 2 获取搜索类型
	private static Integer mType = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select);
		mType = getIntent().getIntExtra("type", 1);
		if(mType == 1){
			//初始化搜索城市信息
			initSearchCityData();
		}else{
			//初始化搜索类型信息
			initSearchTypeData();
		}
		//绑定控件
		mExpandableListView = (ExpandableListView)findViewById(R.id.expand_list);
		mExpandableListView.setAdapter(new SelectAdapter(this));
		mExpandableListView.setOnChildClickListener(this);
		mExpandableListView.setOnGroupExpandListener(this);
		mExpandableListView.setOnGroupClickListener(this);
	}
	
	/**
	 * 初始化搜索城市信息
	 */
	public void initSearchCityData(){
		WorldManager wm = WorldManager.getInstance();
		WmrObject wb = new WmrObject(wm.getRoot());
		mParent = new ArrayList<Integer>();
		mChildren = new ArrayList<ArrayList<Integer>>();
		int fid = wb.getFirstChildId();
		//Log.i(TAG, wb.chsName);
		while (fid != WmrObject.INVALID_ID) {
			mParent.add(fid);
			ArrayList<Integer> list = new ArrayList<Integer>();
			WmrObject cwb = new WmrObject(fid);
			//Log.i(TAG, cwb.chsName);
			int cpt = cwb.getFirstChildId();
			while (cpt != WmrObject.INVALID_ID) {
				WmrObject gsonObj = new WmrObject(cpt);
				list.add(cpt);
				cpt = gsonObj.getNextSiblingId();
			}
			mChildren.add(list);
			fid = cwb.getNextSiblingId();
		}
		
	}
	
	/**
	 * 初始化搜索类型信息
	 */
	public void initSearchTypeData(){
		try {
			//获取一级分类和二级信息
			PoiTypeManager ptm = PoiTypeManager.getInstance();
			mParent = new ArrayList<Integer>();
			mChildren = new ArrayList<ArrayList<Integer>>();
			int pt = ptm.getFirstChild(ptm.getRoot());
			while (pt != PoiType.INVALID_TYPE_ID) {
				mParent.add(pt);
				ArrayList<Integer> list = new ArrayList<Integer>();
				PoiTypeManager cptm = PoiTypeManager.getInstance();
				int cpt = cptm.getFirstChild(pt);
				while (cpt != PoiType.INVALID_TYPE_ID) {
					list.add(cpt);
					cpt = cptm.getNextSibling(cpt);
				}
				mChildren.add(list);
				pt = ptm.getNextSibling(pt);
			}
		} catch (Exception e) {
			// ！！！此處應該添加保護，如果初始化不成功那麼後續使用PoiQuery相關功能將崩潰！！！
			if (Config.DEBUG) {
				//Log.e(TAG,"Error on initializing the PoiQuery Environment -> Reason: "+ e.getMessage());
			}
			new MessageBox(this, false).showDialog(e.getMessage());
		}
	}
	
	Handler handler = new Handler(){
		
		@Override
		public void handleMessage(Message msg) {
			Intent intent = getIntent();
			intent.putExtra("name", (String)msg.obj);
			intent.putExtra("id", msg.what);
			setResult(RESULT_OK, intent);
			SelectActivity.this.finish();
			super.handleMessage(msg);
		}
	};
	
	/**
	 * 点击子类
	 */
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		Integer ids = mChildren.get(groupPosition).get(childPosition);
		Message msg = new Message();
		if(mType ==1 ){
			WmrObject wb = new WmrObject(ids);
			String  name =wb.chsName;
			msg.obj  = name;
			msg.what = ids;
		}else{
			//获取搜索类型信息
			PoiTypeManager ptm = PoiTypeManager.getInstance();
			PoiType poiType = ptm.getObjectById(ids);
			String  name =poiType.name;
			msg.obj  = name;
			msg.what = ids;
		}
		handler.sendMessage(msg);
		return false;
	}
	
	/**
	 * 监听父类展开
	 */
	@Override
	public void onGroupExpand(int groupPosition) {
        for (int i = 0; i < mParent.size(); i++) {
            if (groupPosition != i) {
            	mExpandableListView.collapseGroup(i);
            }
        }
   }
	
	/**
	 * ExpandableList数据适配器
	 * @author malw
	 *
	 */
	private class SelectAdapter implements ExpandableListAdapter {

		private LayoutInflater mLayoutInflater = null;

		public SelectAdapter(Context context) {
			super();
			mLayoutInflater = LayoutInflater.from(context);
		}
		
		@Override
		public boolean areAllItemsEnabled () {
			return true;
		}
		
		@Override
		public Object getChild (int groupPosition, int childPosition) {
			setVolumeControlStream(groupPosition);
			return mChildren.get(groupPosition).get(childPosition);
		}
		
		@Override
		public long getChildId (int groupPosition, int childPosition) {
			return childPosition;
		}
		
		@Override
		public View getChildView (int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			PoiTypeManager ptm = PoiTypeManager.getInstance();
			if (convertView == null) {
				convertView = mLayoutInflater.inflate(R.layout.item_select_list_2, null);
			}
			Integer ids = mChildren.get(groupPosition).get(childPosition);
			TextView tv = (TextView) convertView.findViewById(R.id.select_item2_text);
			if(mType == 1){
				WmrObject wb = new WmrObject(ids);
				tv.setText(wb.chsName);
			}else{
				PoiType poiType = ptm.getObjectById(ids);
				tv.setText(poiType.name);
			}
			return convertView;
		}
		
		@Override
		public int getChildrenCount (int groupPosition) {
			return mChildren.get(groupPosition).size();
		}
		
		@Override
		public long getCombinedChildId (long groupId, long childId) {
			return 0;
		}
		
		@Override
		public long getCombinedGroupId (long groupId) {
			return 0;
		}
		
		@Override
		public Object getGroup (int groupPosition) {
			return mChildren.get(groupPosition);
		}
		
		@Override
		public int getGroupCount () {
			return mChildren.size();
		}
		
		@Override
		public long getGroupId (int groupPosition) {
			return groupPosition;
		}
		
		@Override
		public View getGroupView (int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mLayoutInflater.inflate(R.layout.item_select_list_1, null);
			}
			ImageView iv = (ImageView) convertView.findViewById(R.id.select_item1_image);
			if (isExpanded) {
				iv.setBackgroundResource(R.drawable.btn_more_on);
			} else {
				iv.setBackgroundResource(R.drawable.btn_more_off);
			}
			Integer ids = mParent.get(groupPosition);
			TextView tv = (TextView) convertView.findViewById(R.id.select_item1_text);
			if(mType == 1){
				WmrObject wb = new WmrObject(ids);
				tv.setText(wb.chsName);
			}else{
				PoiType poiType = PoiTypeManager.getInstance().getObjectById(ids);
				tv.setText(poiType.name);
			}
			return convertView;
		}
		
		@Override
		public boolean hasStableIds () {
			return true;
		}
		
		@Override
		public boolean isChildSelectable (int groupPosition, int childPosition) {
			return true;
		}
		
		@Override
		public boolean isEmpty () {
			return false;
		}
		
		@Override
		public void onGroupCollapsed (int groupPosition) {
		}
		
		@Override
		public void onGroupExpanded (int groupPosition) {
		}
		
		@Override
		public void registerDataSetObserver (DataSetObserver observer) {
		}
		
		@Override
		public void unregisterDataSetObserver (DataSetObserver observer) {
		}
	}
	
	@Override
	public boolean onGroupClick(ExpandableListView parent, View v,
			int groupPosition, long id) {
		int count = mChildren.get(groupPosition).size();
		if (count == 0){
			Integer ids = mParent.get(groupPosition);
			Message msg = new Message();
			if(mType ==1 ){
				WmrObject wb = new WmrObject(ids);
				String  name =wb.chsName;
				msg.obj  = name;
				msg.what = ids;
			}
			handler.sendMessage(msg);
		}
		return false;
	}
}
