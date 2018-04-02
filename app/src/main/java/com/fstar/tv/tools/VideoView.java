package com.fstar.tv.xinhuatv.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.ProgressBar;

import com.fstar.tv.R;

public class VideoView extends SurfaceView implements MediaPlayerControl {

	private static final String TAG = "VSTVideoView";
	// settable by the client
	private Uri mUri;
	private Map<String, String> mHeaders;
	private int mDuration;

	// all possible internal states
	private static final int STATE_ERROR = -1;
	private static final int STATE_IDLE = 0;
	private static final int STATE_PREPARING = 1;
	private static final int STATE_PREPARED = 2;
	private static final int STATE_PLAYING = 3;
	private static final int STATE_PAUSED = 4;
	private static final int STATE_PLAYBACK_COMPLETED = 5;

	private int mCurrentState = STATE_IDLE;
	private int mTargetState = STATE_IDLE;

	// All the stuff we need for playing and showing a video
	private SurfaceHolder mSurfaceHolder = null;
	private MediaPlayer mMediaPlayer = null;
	private int mVideoWidth;
	private int mVideoHeight;
	private int mSurfaceWidth;
	private int mSurfaceHeight;
	private MediaController mMediaController;

	private OnInfoListener mOnInfoListener = null;
	private OnCompletionListener mOnCompletionListener = null;
	private MediaPlayer.OnPreparedListener mOnPreparedListener = null;
	private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = null;
	private OnSeekCompleteListener mOnSeekCompleteListener = null;
	private OnErrorListener mOnErrorListener = null;

	private int mCurrentBufferPercentage;

	private int mSeekWhenPrepared; // recording the seek position while
									// preparing
	private boolean mCanPause;
	private boolean mCanSeekBack;
	private boolean mCanSeekForward;
	private Context mContext;
	private WindowManager wm;
	private ProgressBar loading;

	public VideoView(Context context) {
		super(context);
		mContext = context;
		initVideoView();

	}

	public VideoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public VideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		initVideoView();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
		int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
		System.out.println("onMeasure == " + width + ":" + height);
		setMeasuredDimension(width, height);
	}

	public int resolveAdjustedSize(int desiredSize, int measureSpec) {
		int result = desiredSize;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		switch (specMode) {
		case MeasureSpec.UNSPECIFIED:
			/*
			 * Parent says we can be as big as we want. Just don't be larger
			 * than max size imposed on ourselves.
			 */
			result = desiredSize;
			break;

		case MeasureSpec.AT_MOST:
			/*
			 * Parent says we can be as big as we want, up to specSize. Don't be
			 * larger than specSize, and don't be larger than the max size
			 * imposed on ourselves.
			 */
			result = Math.min(desiredSize, specSize);
			break;

		case MeasureSpec.EXACTLY:
			// No choice. Do what we are told.
			result = specSize;
			break;
		}
		return result;
	}

	private void initVideoView() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		getHolder().addCallback(mSHCallback);
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
		mCurrentState = STATE_IDLE;
		mTargetState = STATE_IDLE;
		loading = (ProgressBar) LayoutInflater.from(mContext).inflate(
				R.layout.player_buf_pro, null);
		wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
	}

	public void setVideoPath(String path) {
		setVideoURI(Uri.parse(path));
	}

	public void setVideoURI(Uri uri) {
		setVideoURI(uri, null);
	}

	public void setVideoURI(Uri uri, Map<String, String> headers) {
		mUri = uri;
		mHeaders = headers;
		mSeekWhenPrepared = 0;
		isList = false;
		openVideo();
		requestLayout();
		invalidate();
	}

	private Uri[] mUris; //
	private int[] mDurations;
	private boolean isList = false;
	private int index;

	/**
	 * 设置播放列表
	 * 
	 * @param uris
	 *            url列表
	 * @param headers
	 *            公用头
	 * @param durations
	 *            每一段时长
	 */
	public void setVideoURI(Uri[] uris, Map<String, String> headers,
			int[] durations) {

		if (uris == null || durations == null
				|| uris.length != durations.length) {
			throw new IllegalArgumentException(
					"uris must not null , durations must nuo null and uris.length must =durations.length");
		}
		isResultSeek = false;
		mUris = uris;
		index = 0;
		isList = true;
		mDurations = durations;
		mUri = uris[index];
		mHeaders = headers;
		mSeekWhenPrepared = 0;
		openVideo();
		requestLayout();
		invalidate();
	}

	public void stopPlayback() {
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mCurrentState = STATE_IDLE;
			mTargetState = STATE_IDLE;
		}
	}

	Handler handler = new Handler(Looper.getMainLooper());

	private void openVideo() {
		if (mUri == null || mSurfaceHolder == null) {
			// not ready for playback just yet, will try again later
			return;
		}
		Log.d(TAG, "current uri = " + mUri.toString());
		// Tell the music playback service to pause
		// TODO: these constants need to be published somewhere in the
		// framework.
		Intent i = new Intent("com.android.music.musicservicecommand");
		i.putExtra("command", "pause");
		mContext.sendBroadcast(i);

		// we shouldn't clear the target state, because somebody might have
		// called start() previously
		release(false);

		final String video_url = mUri.toString();
		new Thread(new Runnable() {
			@Override
			public void run() {
				final String rurl = video_url;
				handler.post(new Runnable() {
					@Override
					public void run() {
						try {
							mUri = Uri.parse(rurl);
							mMediaPlayer = new MediaPlayer();
							mMediaPlayer
									.setOnPreparedListener(mPreparedListener);
							mMediaPlayer
									.setOnVideoSizeChangedListener(mSizeChangedListener);
							mMediaPlayer
									.setOnSeekCompleteListener(mSeekCompleteListener);
							mMediaPlayer.setOnInfoListener(mInfoListener);
							mDuration = -1;
							mMediaPlayer
									.setOnCompletionListener(mCompletionListener);
							mMediaPlayer.setOnErrorListener(mErrorListener);
							mMediaPlayer
									.setOnBufferingUpdateListener(mBufferingUpdateListener);
							mCurrentBufferPercentage = 0;
							Log.d(TAG, "当前视频加载地址  = " + mUri.toString());
							
							mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
							mMediaPlayer.setDisplay(mSurfaceHolder);
							mMediaPlayer
									.setAudioStreamType(AudioManager.STREAM_MUSIC);
							mMediaPlayer.setScreenOnWhilePlaying(true);
							mMediaPlayer.prepareAsync();
							handler.postDelayed(TimeOutError, TIMEOUTDEFAULT);
							mCurrentState = STATE_PREPARING;
							attachMediaController();
						} catch (IOException ex) {
							Log.w(TAG, "Unable to open content: " + mUri, ex);
							mCurrentState = STATE_ERROR;
							mTargetState = STATE_ERROR;
							mErrorListener.onError(mMediaPlayer,
									MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
							return;
						} catch (IllegalArgumentException ex) {
							Log.w(TAG, "Unable to open content: " + mUri, ex);
							mCurrentState = STATE_ERROR;
							mTargetState = STATE_ERROR;
							mErrorListener.onError(mMediaPlayer,
									MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
							return;
						}
					}
				});
			}
		}).start();
	}

	public void setMediaController(MediaController controller) {
		if (mMediaController != null) {
			mMediaController.hide();
		}
		mMediaController = controller;
		attachMediaController();
	}

	private void attachMediaController() {
		if (mMediaPlayer != null && mMediaController != null) {
			mMediaController.setMediaPlayer(this);
			View anchorView = this.getParent() instanceof View ? (View) this
					.getParent() : this;
			mMediaController.setAnchorView(anchorView);
			mMediaController.setEnabled(isInPlaybackState());
		}
	}

	MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
		public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
			mVideoWidth = mp.getVideoWidth();
			mVideoHeight = mp.getVideoHeight();
			if (mVideoWidth != 0 && mVideoHeight != 0) {
				getHolder().setFixedSize(mVideoWidth, mVideoHeight);
			}
		}
	};

	MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
		public void onPrepared(MediaPlayer mp) {
			mCurrentState = STATE_PREPARED;

			// Get the capabilities of the player for this stream
			// Metadata data = mp.getMetadata(MediaPlayer.METADATA_ALL,
			// MediaPlayer.BYPASS_METADATA_FILTER);
			//
			// if (data != null) {
			// mCanPause = !data.has(Metadata.PAUSE_AVAILABLE)
			// || data.getBoolean(Metadata.PAUSE_AVAILABLE);
			// mCanSeekBack = !data.has(Metadata.SEEK_BACKWARD_AVAILABLE)
			// || data.getBoolean(Metadata.SEEK_BACKWARD_AVAILABLE);
			// mCanSeekForward = !data.has(Metadata.SEEK_FORWARD_AVAILABLE)
			// || data.getBoolean(Metadata.SEEK_FORWARD_AVAILABLE);
			// } else {
			mCanPause = mCanSeekBack = mCanSeekForward = true;
			// }

			handler.removeCallbacks(TimeOutError);

			if (mMediaController != null) {
				mMediaController.setEnabled(true);
			}

			mVideoWidth = mp.getVideoWidth();
			mVideoHeight = mp.getVideoHeight();

			if (mOnPreparedListener != null) {
				mOnPreparedListener.onPrepared(mMediaPlayer);
			}

			int seekToPosition = mSeekWhenPrepared; // mSeekWhenPrepared
													// may be
													// changed after seekTo()
													// call
			if (seekToPosition != 0) {
				Log.d(TAG, "seekToPosition =" + seekToPosition);
				seekTo(seekToPosition);
			}

			if (mVideoWidth != 0 && mVideoHeight != 0) {
				// Log.i("@@@@", "video size: " + mVideoWidth +"/"+
				// mVideoHeight);
				getHolder().setFixedSize(mVideoWidth, mVideoHeight);
				if (mSurfaceWidth == mVideoWidth
						&& mSurfaceHeight == mVideoHeight) {
					// We didn't actually change the size (it was already at the
					// size
					// we need), so we won't get a "surface changed" callback,
					// so
					// start the video here instead of in the callback.
					if (mTargetState == STATE_PLAYING) {
						start();
						if (mMediaController != null) {
							mMediaController.show();
						}
					} else if (!isPlaying()
							&& (seekToPosition != 0 || getCurrentPosition() > 0)) {
						if (mMediaController != null) {
							// Show the media controls when we're paused into a
							// video and make 'em stick.
							mMediaController.show(0);
						}
					}
				}
			} else {
				// We don't know the video size yet, but should start anyway.
				// The video size might be reported to us later.
				if (mTargetState == STATE_PLAYING) {
					start();
				}
			}
		}
	};

	private OnCompletionListener mCompletionListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mp) {

			if (isList && index < mUris.length - 1) { // 是列表 但不最后一段 没有这正结束
				index += 1;
				mUri = mUris[index];
				Log.d(TAG, "index = " + index + ",uri = " + mUri);
				openVideo();
				return;
			}
			mCurrentState = STATE_PLAYBACK_COMPLETED;
			mTargetState = STATE_PLAYBACK_COMPLETED;
			if (mMediaController != null) {
				mMediaController.hide();
			}

			if (mOnCompletionListener != null) {
				mOnCompletionListener.onCompletion(mMediaPlayer);
			}
		}
	};

	private OnErrorListener mErrorListener = new OnErrorListener() {
		public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
			Log.d(TAG, "Error: " + framework_err + "," + impl_err);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			if (mMediaController != null) {
				mMediaController.hide();
			}

			/* If an error handler has been supplied, use it and finish. */
			if (mOnErrorListener != null) {
				if (mOnErrorListener.onError(mMediaPlayer, framework_err,
						impl_err)) {
					return true;
				}
			}

			/*
			 * Otherwise, pop up an error dialog so the user knows that
			 * something bad has happened. Only try and pop up the dialog if
			 * we're attached to a window. When we're going away and no longer
			 * have a window, don't bother showing the user an error.
			 */
			if (getWindowToken() != null) {
				Resources r = mContext.getResources();
				String messageId;

				if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
					messageId = "playback error";
				} else {
					messageId = "unknown error ";
				}

				new AlertDialog.Builder(mContext)
						.setTitle("Sorry")
						.setMessage(messageId)
						.setPositiveButton("确认",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										/*
										 * If we get here, there is no onError
										 * listener, so at least inform them
										 * that the video is over.
										 */
										if (mOnCompletionListener != null) {
											mOnCompletionListener
													.onCompletion(mMediaPlayer);
										}
									}
								}).setCancelable(false).show();
			}
			return true;
		}
	};

	private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			mCurrentBufferPercentage = percent;
		}
	};

	private OnSeekCompleteListener mSeekCompleteListener = new OnSeekCompleteListener() {

		@Override
		public void onSeekComplete(MediaPlayer mp) {

			if (isList) {
				isResultSeek = false;
			}
			if (mOnSeekCompleteListener != null) {
				mOnSeekCompleteListener.onSeekComplete(mp);
			}
		}
	};

	/**
	 * Register a callback to be invoked when the media file is loaded and ready
	 * to go.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
		mOnPreparedListener = l;
	}

	/**
	 * Register a callback to be invoked when the end of a media file has been
	 * reached during playback.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnCompletionListener(OnCompletionListener l) {
		mOnCompletionListener = l;
	}

	/**
	 * Register a callback to be invoked when an error occurs during playback or
	 * setup. If no listener is specified, or if the listener returned false,
	 * VideoView will inform the user of any errors.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnErrorListener(OnErrorListener l) {
		mOnErrorListener = l;
	}

	public void setOnInfoListener(OnInfoListener l) {
		mOnInfoListener = l;
	}

	public void setOnBufferingUpdateListener(
			MediaPlayer.OnBufferingUpdateListener l) {
		mOnBufferingUpdateListener = l;
	}

	public void setOnSeekCompleteListener(OnSeekCompleteListener l) {
		mOnSeekCompleteListener = l;
	}

	SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
		public void surfaceChanged(SurfaceHolder holder, int format, int w,
				int h) {
			mSurfaceWidth = w;
			mSurfaceHeight = h;
			boolean isValidState = (mTargetState == STATE_PLAYING);
			boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
			if (mMediaPlayer != null && isValidState && hasValidSize) {
				if (mSeekWhenPrepared != 0) {
					seekTo(mSeekWhenPrepared);
				}
				start();
			}
		}

		public void surfaceCreated(SurfaceHolder holder) {
			mSurfaceHolder = holder;
			openVideo();
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// after we return from this we can't use the surface any more
			mSurfaceHolder = null;
			if (mMediaController != null)
				mMediaController.hide();
			release(true);
		}
	};

	/*
	 * release the media player in any state
	 */
	private void release(boolean cleartargetstate) {
		if (mMediaPlayer != null) {
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mCurrentState = STATE_IDLE;
			if (cleartargetstate) {
				mTargetState = STATE_IDLE;
			}
		}

		handler.removeCallbacks(TimeOutError);

		if (loading.getParent() != null)
			wm.removeView(loading);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (isInPlaybackState() && mMediaController != null) {
			toggleMediaControlsVisiblity();
		}
		return false;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent ev) {
		if (isInPlaybackState() && mMediaController != null) {
			toggleMediaControlsVisiblity();
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK
				&& keyCode != KeyEvent.KEYCODE_VOLUME_UP
				&& keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
				&& keyCode != KeyEvent.KEYCODE_VOLUME_MUTE
				&& keyCode != KeyEvent.KEYCODE_MENU
				&& keyCode != KeyEvent.KEYCODE_CALL
				&& keyCode != KeyEvent.KEYCODE_ENDCALL;
		if (isInPlaybackState() && isKeyCodeSupported
				&& mMediaController != null) {
			if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
					|| keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
				if (mMediaPlayer.isPlaying()) {
					pause();
					mMediaController.show();
				} else {
					start();
					mMediaController.hide();
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
				if (!mMediaPlayer.isPlaying()) {
					start();
					mMediaController.hide();
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
					|| keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
				if (mMediaPlayer.isPlaying()) {
					pause();
					mMediaController.show();
				}
				return true;
			} else if (keyCode == 185) {
				defaultScale = (defaultScale + 1) % 3;
				selectScales(defaultScale);
				if (mOnChangScaleListener != null) {
					mOnChangScaleListener.changeScale(defaultScale);
				}
				return true;
			} else {
				toggleMediaControlsVisiblity();
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private int defaultScale = 0;

	public void setDefaultScale(int defaultScale) {
		this.defaultScale = defaultScale;
	}

	private void toggleMediaControlsVisiblity() {
		if (mMediaController.isShowing()) {
			mMediaController.hide();
		} else {
			mMediaController.show();
		}
	}

	public void start() {
		if (isInPlaybackState()) {
			mMediaPlayer.start();
			mCurrentState = STATE_PLAYING;
		}
		mTargetState = STATE_PLAYING;
	}

	public void pause() {
		if (isInPlaybackState()) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.pause();
				mCurrentState = STATE_PAUSED;
			}
		}
		mTargetState = STATE_PAUSED;
	}

	public void suspend() {
		release(false);
	}

	public void resume() {
		openVideo();
	}

	// cache duration as mDuration for faster access
	public int getDuration() {
		if (isInPlaybackState()) {
			if (mDuration > 0) {
				return mDuration;
			}
			if (isList) {
				for (int i = 0; i < mDurations.length; i++) {
					mDuration += mDurations[i];
				}
				return mDuration;
			} else {
				mDuration = mMediaPlayer.getDuration();
				return mDuration;
			}
		}
		mDuration = -1;
		return mDuration;

	}

	public int getCurrentPosition() {
		if (isInPlaybackState()) {
			if (isList) { // �б�
				int currentPosition = 0;
				for (int i = 0; i < index; i++) {
					currentPosition += mDurations[i];
				}
				return currentPosition += mMediaPlayer.getCurrentPosition();

			} else {
				return mMediaPlayer.getCurrentPosition();
			}
		}
		return 0;
	}

	boolean isResultSeek = true;

	public void seekTo(int msec) {
		if (msec <= 0) {
			return;
		}
		Log.d(TAG, "msec = " + msec);
		if (isResultSeek) {
			if (isInPlaybackState()) {
				mMediaPlayer.seekTo(msec);
				mSeekWhenPrepared = 0;
			} else {
				mSeekWhenPrepared = msec;
				Log.d(TAG, "mSeekWhenPrepared = " + mSeekWhenPrepared);
			}
		} else { // 不是最终的 则需要计算
			for (int i = 0; i < mDurations.length; i++) {
				msec -= mDurations[i];
				if (msec < 0) {
					msec += mDurations[i];
					isResultSeek = true;
					if (index == i) {
						mMediaPlayer.seekTo(msec);
						mSeekWhenPrepared = 0;
					} else {
						index = i;
						mUri = mUris[i];
						mSeekWhenPrepared = msec;
						Log.d(TAG, "mSeekWhenPrepared = " + mSeekWhenPrepared);
						handler.post(new Runnable() {
							@Override
							public void run() {
								openVideo();
							}
						});
					}
					break;
				}
			}
		}
	}

	public boolean isPlaying() {
		return isInPlaybackState() && mMediaPlayer.isPlaying();
	}

	public int getBufferPercentage() {
		if (mMediaPlayer != null) {
			return mCurrentBufferPercentage;
		}
		return 0;
	}

	private boolean isInPlaybackState() {
		return (mMediaPlayer != null && mCurrentState != STATE_ERROR
				&& mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
	}

	public boolean canPause() {
		return mCanPause;
	}

	public boolean canSeekBackward() {
		return mCanSeekBack;
	}

	public boolean canSeekForward() {
		return mCanSeekForward;
	}

	private static final long TIMEOUTDEFAULT = 30000;
	private static final int MEDIA_ERROR_TIMED_OUT = 0xffffff92;
	private Runnable TimeOutError = new Runnable() {

		@Override
		public void run() {
			Log.e(TAG, "open video time out : Uri = " + mUri);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			mErrorListener.onError(mMediaPlayer,
					MediaPlayer.MEDIA_ERROR_UNKNOWN, -100);
			release(false);
		}
	};

	OnInfoListener mInfoListener = new OnInfoListener() {

		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			Log.i(TAG, "OnInfoListener-------->what:" + what + ",  extra :"
					+ extra);

			if (mOnInfoListener != null) {
				mOnInfoListener.onInfo(mp, what, extra);
				return true;
			}
			/*
			 * MEDIA_INFO_VIDEO_TRACK_LAGGING MEDIA_INFO_BUFFERING_START
			 * MEDIA_INFO_BUFFERING_END MEDIA_INFO_NOT_SEEKABLE
			 * MEDIA_INFO_DOWNLOAD_RATE_CHANGED
			 */
			if (getWindowToken() != null) {

				LayoutParams lp = new LayoutParams();
				lp.format = PixelFormat.TRANSPARENT;
				lp.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
				lp.width = LayoutParams.WRAP_CONTENT;
				lp.height = LayoutParams.WRAP_CONTENT;
				// lp.token = getWindowToken();
				lp.gravity = Gravity.CENTER;

				switch (what) {
				case MediaPlayer.MEDIA_INFO_BUFFERING_START:
					if (loading.getParent() == null)
						wm.addView(loading, lp);
					break;
				case MediaPlayer.MEDIA_INFO_BUFFERING_END:
					if (loading.getParent() != null)
						wm.removeView(loading);
					break;
				default:
					break;
				}
			}
			return true;
		}
	};

	public final static int A_4X3 = 1;
	public final static int A_16X9 = 2;
	public final static int A_RAW = 4; // 原始大小
	public final static int A_DEFALT = 0; // 原始比例

	/**
	 * 全屏状态 才可以使用 选择比例
	 * 
	 * @param flg
	 * 
	 */
	public void selectScales(int flg) {
		if (getWindowToken() != null) {
			Rect rect = new Rect();
			getWindowVisibleDisplayFrame(rect);
			Log.d(TAG, "Rect = " + rect.top + ":" + rect.bottom + ":"
					+ rect.left + ":" + rect.right);

			double height = rect.bottom - rect.top;
			double width = rect.right - rect.left;
			Log.d(TAG, "diplay = " + width + ":" + height);

			if (height <= 0.0 || width <= 0.0 || mVideoHeight <= 0.0
					|| mVideoWidth <= 0.0) {
				return;
			}
			ViewGroup.LayoutParams param = getLayoutParams();
			switch (flg) {
			case A_4X3:
				if (width / height >= 4.0 / 3.0) { // 屏幕 宽了 以屏幕高为基�?
					param.height = (int) height;
					param.width = (int) (4 * height / 3);
				} else { // 屏幕 高了 以宽为基�?
					param.width = (int) width;
					param.height = (int) (3 * width / 4);
				}
				System.out.println("A_4X3 === " + param.width + ":"
						+ param.height);
				setLayoutParams(param);
				break;
			case A_16X9:
				if (width / height >= 16.0 / 9.0) { // 屏幕 宽了 以屏幕高为基�?
					param.height = (int) height;
					param.width = (int) (16 * height / 9);
				} else { // 屏幕 高了 以宽为基�?
					param.width = (int) width;
					param.height = (int) (9 * width / 16);
				}
				System.out.println("A_16X9 === " + param.width + ":"
						+ param.height);
				setLayoutParams(param);
				break;
			case A_DEFALT: //
				if (width / height >= mVideoWidth / mVideoHeight) { // 屏幕 宽了
																	// 以屏幕高为基�?
					param.height = (int) height;
					param.width = (int) (mVideoWidth * height / mVideoHeight);
				} else { // 屏幕 高了 以宽为基�?
					param.width = (int) width;
					param.height = (int) (mVideoHeight * width / mVideoWidth);
				}
				System.out.println("A_DEFALT === " + param.width + ":"
						+ param.height);
				setLayoutParams(param);
				break;
			}
		}
	}

	private OnChangScaleListener mOnChangScaleListener;

	public void setOnChangScaleListener(OnChangScaleListener l) {
		mOnChangScaleListener = l;
	}

	public interface OnChangScaleListener {
		public void changeScale(int scalemod);
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}
}
