package pt.fjss.pulltoupdatelibrary;


import android.content.Context;

import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;

import android.view.animation.LinearInterpolator;

import android.view.animation.RotateAnimation;

import android.widget.AbsListView;
import android.widget.ImageView;

import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author frederico
 * 
 */

public class Pulltoupdate extends ListView implements OnScrollListener {
	private static final int DOING_NOTHING = 0;
	private static final int LOADING = 1;
	private static final int RELEASE = 2;

	private int pullState;
	private int headerHeigh;
	private float lastY;
	private boolean shakeTrick;
	private int scrollState;

	private int headerFixedOriginalHeight;

	private IonRefreshListener refreshListener;

	private RelativeLayout mHeaderView;
	private LayoutInflater mInflater;
	private TextView headerMessage;
	private ImageView headerImage;

	private RotateAnimation mImageFlipDownAnimation, mImageFlipUpAnimation;

	public Pulltoupdate(Context context) {
		super(context);
		init(context);
	}

	public Pulltoupdate(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public Pulltoupdate(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {

		pullState = DOING_NOTHING;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mHeaderView = (RelativeLayout) mInflater.inflate(R.layout.header, this,
				false);
		headerMessage = (TextView) mHeaderView.findViewById(R.id.headerMessage);
		headerImage = (ImageView) mHeaderView.findViewById(R.id.headerImage);

		addHeaderView(mHeaderView);
		headerHeigh = mHeaderView.getPaddingTop();// getLayoutParams().height;
		headerFixedOriginalHeight = 100;// mHeaderView.getHeight();

		/*
		 * Animation
		 */
		// Down
		mImageFlipDownAnimation = new RotateAnimation(0, -180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mImageFlipDownAnimation.setInterpolator(new LinearInterpolator());
		mImageFlipDownAnimation.setDuration(250);
		mImageFlipDownAnimation.setFillAfter(true);
		// Up
		mImageFlipUpAnimation = new RotateAnimation(-180, 0,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mImageFlipUpAnimation.setInterpolator(new LinearInterpolator());
		mImageFlipUpAnimation.setDuration(250);
		mImageFlipUpAnimation.setFillAfter(true);
		/*
		 * 
		 */

		shakeTrick = false;
		setVerticalFadingEdgeEnabled(false);
		setSmoothScrollbarEnabled(true);

		super.setOnScrollListener(this);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

		if (firstVisibleItem == 0 && pullState == DOING_NOTHING
				&& scrollState == SCROLL_STATE_FLING) {
			setSelection(1);

			shakeTrick = true;

		} else if (shakeTrick && scrollState == SCROLL_STATE_FLING) {
			setSelection(1);
		} else if (firstVisibleItem == 0) {
			// scrolledUp = false;
		}

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		this.scrollState = scrollState;

	}

	public void setOnRefreshListener(IonRefreshListener onRefreshListener) {
		refreshListener = onRefreshListener;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int y = (int) ev.getY();
		shakeTrick = false;
		switch (ev.getAction()) {
		case MotionEvent.ACTION_MOVE:
			if ((getFirstVisiblePosition() == 0 || getFirstVisiblePosition() == 1)
					&& (pullState == RELEASE || pullState == DOING_NOTHING)) {
				setHeaderTopPadding(ev);
				if (getFirstVisiblePosition() == 0
						&& mHeaderView.getBottom() >= (headerFixedOriginalHeight + 200)) {
					headerMessage.setText("Release to refresh");

					if (pullState != RELEASE) {
						headerImage.clearAnimation();
						headerImage.startAnimation(mImageFlipDownAnimation);
					}

					pullState = RELEASE;

				} else {
					headerMessage.setText("Pull to refresh");
					if (pullState != DOING_NOTHING) {
						headerImage.clearAnimation();
						headerImage.startAnimation(mImageFlipUpAnimation);
					}
					pullState = DOING_NOTHING;
				}

			}

			break;

		case MotionEvent.ACTION_UP:
			setVerticalScrollBarEnabled(true);
			if (getFirstVisiblePosition() == 0
					&& mHeaderView.getBottom() >= (headerFixedOriginalHeight + 200)) {
				Log.d("FRED", "LOADING");
				pullState = LOADING;

				resetHeader();
				setSelection(0);
				headerMessage.setText("Loading...");
				refresh();
			} else if (getFirstVisiblePosition() == 0 && pullState != LOADING) {
				Log.d("FRED", "NHAA");

				setSelection(1);

			} else if (getLastVisiblePosition() == getCount() - 1) {
				refreshDown();
			}

			resetHeader();

			break;
		case MotionEvent.ACTION_DOWN:
			lastY = y;
			break;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		super.setAdapter(adapter);
		setSelection(1);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		setSelection(1);
	}

	/**
	 * Increase header top padding
	 */
	private void setHeaderTopPadding(MotionEvent me) {
		setVerticalFadingEdgeEnabled(false);

		for (int p = 0; p < me.getHistorySize(); p++) {
			setVerticalScrollBarEnabled(false);

			int topPadding = (int) (((me.getHistoricalY(p) - lastY) - headerHeigh) / 2);
			mHeaderView.setPadding(mHeaderView.getPaddingLeft(), topPadding,
					mHeaderView.getPaddingRight(),
					mHeaderView.getPaddingBottom());

		}

	}

	private void resetHeader() {
		headerImage.clearAnimation();
		mHeaderView.setPadding(mHeaderView.getPaddingLeft(), headerHeigh,
				mHeaderView.getPaddingRight(), mHeaderView.getPaddingBottom());

	}

	public void refresh() {
		if (refreshListener != null) {
			refreshListener.onRefreshUp();
		}
	}

	/*
	 * Reset up header list
	 */
	public void onRefreshUpComplete() {
		resetHeader();
		if (getFirstVisiblePosition() == 0) {
			invalidateViews();
			setSelection(1);
		}
		pullState = DOING_NOTHING;
	}

	/*
	 * Reset down header list
	 */
	public void onRefreshDownComplete() {
		// removeFooterView(v);
	}

	public void refreshDown() {

		if (refreshListener != null) {
			refreshListener.onRefeshDown();
		}
	}

}
