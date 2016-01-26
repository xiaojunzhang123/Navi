package com.mapbar.moo.resource;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.mapbar.moo.R;

public class MessageBox extends Dialog {

	Activity mContext;
	int dialogResult;
	Handler mHandler;
	boolean mStop = true;

	public MessageBox(Activity context, boolean stop) {
		super(context);
		dialogResult = 0;
		mContext = context;
		setOwnerActivity(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		onCreate();
		mStop = stop;
	}

	public void onCreate () {
		setContentView(R.layout.layout_alter);
		findViewById(R.id.bt_ok).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View paramView) {
				endDialog(1);
			}
		});
	}

	public int getDialogResult () {
		return dialogResult;
	}

	public void setDialogResult (int dialogResult) {
		this.dialogResult = dialogResult;
	}

	public void endDialog (int result) {
		if (mStop) {
			android.os.Process.killProcess(android.os.Process.myPid());
		} else {
			dismiss();
			setDialogResult(result);
			mContext.finish();
			// Message m = mHandler.obtainMessage();
			// mHandler.sendMessage(m);
		}
	}

	public int showDialog (String Msg) {
		TextView TvErrorInfo = (TextView) findViewById(R.id.tv_tip);
		TvErrorInfo.setText(Msg);

		mHandler = new Handler() {
			@Override
			public void handleMessage (Message mesg) {
				throw new RuntimeException();

			}
		};
		super.show();
		try {
			Looper.getMainLooper();
			Looper.loop();
		} catch (RuntimeException e2) {
		}
		return dialogResult;
	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				|| keyCode == KeyEvent.KEYCODE_HOME) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
