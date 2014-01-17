package pt.fjss.pulltoupdatelibrary;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author frederico <fredericojssilva@gmail.com>
 * 
 */

public class PullToUpdateListView extends ListView implements OnScrollListener {
	private static final int DOING_NOTHING = 0;
	private static final int LOADING = 1;
	private static final int RELEASE = 2;
	
	private static String PULL_MESSAGE = "Pull to refresh.";
	private static String RELEASE_MESSAGE= "Release to refresh.";
	private static String LOADING_MESSAGE = "Loading...";

	private int pullState;
	private int headerHeigh;
	private float lastY;

	private int scrollState;

	private int headerFixedOriginalHeight;

	private IonRefreshListener refreshListener;

	private RelativeLayout mHeaderView;
	private LayoutInflater mInflater;
	private TextView headerMessage;
	private ImageView headerImage;
	private ProgressBar headerProgressBar;

	private RelativeLayout mFooterView;
	private TextView footerMessage;
	private ProgressBar footerProgressBar;

	private RotateAnimation mImageFlipDownAnimation, mImageFlipUpAnimation;
	private MODE mode;
	private boolean autoLoad;
	private int autoLoadPosition;
	private boolean isFooterLoading;

	public enum MODE {
		UP_ONLY, UP_AND_DOWN;
	}

	public PullToUpdateListView(Context context) {
		super(context);
		init(context);
	}

	public PullToUpdateListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public PullToUpdateListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		//initial states
		pullState = DOING_NOTHING;
		mode = MODE.UP_AND_DOWN;
		isFooterLoading = false;
		/*
		 * Header
		 */
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mHeaderView = (RelativeLayout) mInflater.inflate(R.layout.header, this,
				false);
		headerMessage = (TextView) mHeaderView.findViewById(R.id.headerMessage);
		headerImage = (ImageView) mHeaderView.findViewById(R.id.headerImage);
		headerProgressBar = (ProgressBar) mHeaderView.findViewById(R.id.headerProgressbar);
		headerImage.setVisibility(View.VISIBLE);
		headerProgressBar.setVisibility(View.INVISIBLE);

		addHeaderView(mHeaderView);
		headerHeigh = mHeaderView.getPaddingTop();// getLayoutParams().height;
		headerFixedOriginalHeight = 100;// mHeaderView.getHeight();
		/*
		 * Footer
		 */
		mFooterView = (RelativeLayout) mInflater.inflate(R.layout.footer, this,
				false);
		footerMessage = (TextView) mFooterView
				.findViewById(R.id.footer_message);
		footerProgressBar = (ProgressBar) mFooterView
				.findViewById(R.id.footer_progressbar);

		addFooterView(mFooterView);
		footerMessage.setVisibility(View.GONE);

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

		}

		if (mode == MODE.UP_AND_DOWN
				&& autoLoad
				&& (getLastVisiblePosition() >= getCount()
						- (1 + autoLoadPosition))
				&& (getCount() - (1 + autoLoadPosition) > getFirstVisiblePosition())
				&& !isFooterLoading) {
			isFooterLoading = true;
			refreshDown();

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
		switch (ev.getAction()) {
		case MotionEvent.ACTION_MOVE:
			if ((getFirstVisiblePosition() == 0 || getFirstVisiblePosition() == 1)
					&& (pullState == RELEASE || pullState == DOING_NOTHING)) {
				setHeaderTopPadding(ev);
				if (getFirstVisiblePosition() == 0
						&& mHeaderView.getBottom() >= (headerFixedOriginalHeight + 200)) {
					headerMessage.setText(RELEASE_MESSAGE);

					if (pullState != RELEASE) {
						headerImage.clearAnimation();
						headerImage.startAnimation(mImageFlipDownAnimation);
					}

					pullState = RELEASE;

				} else {
					headerMessage.setText(PULL_MESSAGE);
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
				headerMessage.setText(LOADING_MESSAGE);
				refresh();
			} else if (getFirstVisiblePosition() == 0 && pullState != LOADING) {
				Log.d("FRED", "NHAA");

				setSelection(1);

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

	private void refresh() {
		headerImage.setVisibility(View.INVISIBLE);
		headerProgressBar.setVisibility(View.VISIBLE);
		if (refreshListener != null) {
			refreshListener.onRefreshUp();
		}
	}

	private void refreshDown() {

		if (refreshListener != null) {
			refreshListener.onRefeshDown();
		}
	}

	/********************
	 * PUBLIC METHODSS
	 ******************/

	/*
	 * Reset up header list
	 */
	public void onRefreshUpComplete() {
		resetHeader();
		headerImage.setVisibility(View.VISIBLE);
		headerProgressBar.setVisibility(View.INVISIBLE);
		if (getFirstVisiblePosition() == 0) {
			invalidateViews();
			setSelection(1);
		}
		
		
		pullState = DOING_NOTHING;
	}

	/*
	 * Reset down header list
	 */
	public void onRefreshDownComplete(String message) {
		if (message != null) {
			footerMessage.setText(message);
			footerMessage.setVisibility(View.VISIBLE);
			footerProgressBar.setVisibility(View.GONE);
		}
		isFooterLoading = false;
	}

	/**
	 * Set mode
	 * 
	 * @param mode
	 * 
	 *            MODE.UP_ONLY -> Only up pull active MODE.UP_AND_DOWN -> Up and
	 *            down pull active
	 */
	public void setPullMode(MODE mode) {
		this.mode = mode;
		if (mode == MODE.UP_ONLY) {
			removeFooterView(mFooterView);
		}
	}

	/**
	 * call onRefreshDown() when autoLoadDistanceToEnd Value to end of the list
	 * is reached
	 * 
	 * @param autoload
	 * @param autoLoadDistanceToEnd
	 *            distance to end of list
	 */
	public void setAutoLoad(boolean autoload, int autoLoadDistanceToEnd) {
		this.autoLoad = autoload;
		this.autoLoadPosition = autoLoadDistanceToEnd;
	}
	
	/**
	 * Set pull to refresh message
	 * @param message
	 */
	public void setPullMessage(String message)
	{
		PULL_MESSAGE = message;
	}
	/**
	 * Set release to refresh message
	 * @param message
	 */
	public void setReleaseMessage(String message)
	{
		RELEASE_MESSAGE = message;
	}
	/**
	 * Set loading message
	 * @param message
	 */
	public void setLoadingMessage(String message)
	{
		LOADING_MESSAGE = message;
	}
	
	/**
	 * 
	 * @param color
	 */
	public void setPullMessageColor(int color)
	{
		headerMessage.setTextColor(color);
	}
	
	/**
	 * @param size
	 */
	public void setPullMessageSize(float size)
	{
		headerMessage.setTextSize(size);
		
	}
	public void setPullRotateImage(Drawable image)
	{
		headerImage.setImageDrawable(image);
	}

}
