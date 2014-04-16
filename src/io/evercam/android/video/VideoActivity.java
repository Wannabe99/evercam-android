package io.evercam.android.video;

import io.evercam.android.ParentActivity;
import io.evercam.android.custom.ProgressView;
import io.evercam.android.dto.CameraStatus;
import io.evercam.android.dto.EvercamCamera;
import io.evercam.android.slidemenu.SlideMenuInterface;
import io.evercam.android.utils.AppData;
import io.evercam.android.utils.Commons;
import io.evercam.android.utils.Constants;
import io.evercam.android.utils.UIUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaList;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import io.evercam.android.R;
import com.google.analytics.tracking.android.EasyTracker;

public class VideoActivity extends ParentActivity implements
		SlideMenuInterface.OnSlideMenuItemClickListener,SurfaceHolder.Callback,IVideoPlayer
{
	public static EvercamCamera camera = new EvercamCamera();

	private final static String TAG = "evercamapp-VideoActivity";
	private final static String LOCATION = "com.compdigitec.libvlcandroidsample.VideoActivity.location";

	private static List<MRLCamba> mrls = null;
	private static int mrlIndex = -1;
	private String mrlPlaying = null;
	private boolean showImagesVideo = false;

	// display surface
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private ProgressView progressView = null;

	// media player
	private LibVLC libvlc;
	private int mVideoWidth;
	private int mVideoHeight;
	private final static int videoSizeChanged = -1;

	// Screen view change vraibales
	private int screen_width, screen_height;
	private int media_width = 0, media_height = 0;
	private boolean landscape;

	// video playing controls and variables
	private RelativeLayout imageViewLayout;
	private ImageView imageView;
	private ImageView mediaPlayerView;

	private long downloadStartCount = 0;
	private long downloadEndCount = 0;
	private BrowseImages imageThread;
	private boolean isProgressShowing = true;
	static boolean enableLogs = true;

	// image tasks and thread variables
	private int sleepIntervalMinTime = 201; // interval between two requests of
											// images
	private int intervalAdjustment = 1; // how much milli seconds to increment
										// or decrement on image failure or
										// success
	private int sleepInterval = sleepIntervalMinTime + 290; // starting image
															// interval
	private boolean startDownloading = false; // start making requests soon
												// after the image is received
												// first time. Until first image
												// is not received, do not make
												// requests
	private static long latestStartImageTime = 0; // time of the latest request
													// that has been made
	private boolean isFirstImageLiveReceived = false;
	private boolean isFirstImageLocalReceived = false;
	private boolean isFirstImageLiveEnded = false;
	private boolean isFirstImageLocalEnded = false;
	private int successiveFailureCount = 0; // how much successive image
											// requests have failed
	private Boolean isShowingFailureMessage = false;
	private Boolean optionsActivityStarted = false; // whether preference
													// activity is showing or
													// not

	// if camera uses cookies authentication, then use these cookies to pass to
	// camera
	// public static EvercamCamera camera = new EvercamCamera(0, 0,
	// "info@camba.tv", "a", "http://url.com/abc",
	// "http://url.com/abc", "http://url.com/abc", "http://url.com/abc",
	// "camera make",
	// "access method", "password", "time zone", "camera username", "code",
	// "http://url.com/abc", "5", "127.0.0.1:99", "http://url.com/abc",
	// "http://url.com/abc",
	// "http://url.com/abc", "http://url.com/abc", "my camera", "0", "554",
	// "http://url.com/abc", "Status", false, false, "0", "cam Group", 1, false,
	// "offser");
	private static String startingCameraID;
	private int defaultCameraIndex;
	// preferences options
	private String localnetworkSettings = "0";
	private boolean isLocalNetwork = false;

	private static String imageLiveCameraURL = "http://www.camba.tv/noimage.png";
	private static String imageLiveLocalURL = "http://www.camba.tv/noimage.png";

	private boolean paused = false;

	private Animation fadeInAnimation = null; // animation that shows the
												// playing icon
	// of media player fading and
	// disappearing

	boolean end = false; // whether to end this activity or not

	public void addCamerasToDropdownActionBar()
	{
		new LoadActiveCamerasTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static boolean startPlayingVIdeoForCamera(Context context, String cameraId)
	{
		startingCameraID = cameraId;
		Intent intent = new Intent(context, VideoActivity.class);
		context.startActivity(intent);

		return false;
	}

	private void setCameraForPlaying(Context context, EvercamCamera cam)
	{
		try
		{
			camera = cam;

			// ***Setting Defaults
			readSetPreferences();
			showImagesVideo = false;

			downloadStartCount = 0;
			downloadEndCount = 0;
			isProgressShowing = false;

			startDownloading = false;
			latestStartImageTime = 0;
			isFirstImageLiveReceived = false;
			isFirstImageLocalReceived = false;
			isFirstImageLiveEnded = false;
			isFirstImageLocalEnded = false;
			successiveFailureCount = 0;
			isShowingFailureMessage = false;

			optionsActivityStarted = false;

			isLocalNetwork = false;

			mediaPlayerView.setVisibility(View.GONE);

			paused = false;
			end = false;

			surfaceView.setVisibility(View.GONE);
			imageView.setVisibility(View.VISIBLE);
			showProgressView();

			loadImageFromCache();
			showProgressView();
			// ###Setting Defaults

			// String ImageUrl = ((camera.getLowResolutionSnapshotUrl() != null
			// && URLUtil
			// .isValidUrl(camera.getLowResolutionSnapshotUrl())) ? camera
			// .getLowResolutionSnapshotUrl() : camera.getCameraImageUrl());

			imageLiveCameraURL = camera.getExternalSnapshotUrl();

			// if (cam.getLocalIpPort() != null && cam.getLocalIpPort().length()
			// > 10)
			// {
			// String Prefix = (ImageUrl.startsWith("https://") ? "https://" :
			// "https://");
			//
			// if (ImageUrl.startsWith("https://")) // Extracting information
			// // from the camera image
			// // url
			// ImageUrl = ImageUrl.replace("http://", "");
			//
			// VideoActivity.imageLiveLocalURL = Prefix +
			// cam.getLocalIpPort().trim()
			// + ImageUrl.substring(ImageUrl.indexOf("/", Prefix.length() + 1));
			// }
			// else
			// {
			// VideoActivity.imageLiveLocalURL = null;
			// }

			VideoActivity.imageLiveLocalURL = null;

			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			mrlPlaying = sharedPrefs.getString("pref_mrlplaying" + camera.getCameraId(), null);

			mrls = new ArrayList<VideoActivity.MRLCamba>();
			mrlIndex = -1;

			if (mrlPlaying != null)
			{
				addUrlIfValid(mrlPlaying, cam);
				mrlIndex = 0;
				mrlPlaying = null;
			}

			// addUrlIfValid(cam.getMpeg4Url(), cam);
			// addUrlIfValid(cam.getMjpgUrl(), cam);
			// addUrlIfValid(cam.getH264Url(), cam);
			// addUrlIfValid(cam.getRtspUrl(), cam);
			// addUrlIfValid(cam.getMobileUrl(), cam);

		}
		catch (Exception e)
		{
			if (enableLogs) Log.e(TAG, e.toString() + "::" + Log.getStackTraceString(e));
			UIUtils.GetAlertDialog(context, "Exception", e.toString()).show();// +
																				// "::cam.getCameraImageUrl() ["
																				// +
																				// cam.getCameraImageUrl()
																				// +
																				// "], cam.getLowResolutionSnapshotUrl() ["+cam.getLowResolutionSnapshotUrl()+"]").show();
		}

	}

	private void addUrlIfValid(String url, EvercamCamera cam)
	{
		try
		{
			if (url == null
					|| url.trim().length() < 10
					|| !(url.startsWith("http://") || url.startsWith("https://")
							|| url.startsWith("rtsp://") || url.startsWith("tcp://"))) return;

			String username = cam.getUsername();
			String password = cam.getPassword();
			String credentialsPart = "";

			if (username != null && password != null && username.trim().length() > 0
					&& password.trim().length() > 0 && !username.equalsIgnoreCase("null")
					&& !password.equals("null")) credentialsPart = username + ":" + password + "@";

			String prefix = url.substring(0, url.indexOf("//") + 2);
			String relativeUrlString = url.substring(url.indexOf("/", prefix.length()));
			String hostPort = url.substring(prefix.length(),
					url.length() - relativeUrlString.length());
			if (hostPort.contains("@")) hostPort = hostPort.substring(hostPort.indexOf("@") + 1);
			if (hostPort.startsWith("www.")) hostPort.substring(4);

			// String localIpPort = "";
			// if (cam.getLocalIpPort() != null)
			// {
			// localIpPort = cam.getLocalIpPort().substring(
			// 0,
			// (cam.getLocalIpPort().contains(":") ?
			// cam.getLocalIpPort().indexOf(":")
			// : cam.getLocalIpPort().length()));
			// if (cam.getRtspPort() != null && prefix.startsWith("rtsp://"))
			// localIpPort += ":"
			// + cam.getRtspPort();
			// else if (hostPort.contains(":")) localIpPort +=
			// hostPort.substring(hostPort
			// .indexOf(":"));
			//
			// }

			// String liveURLString = prefix + credentialsPart + hostPort +
			// relativeUrlString;
			// String localURLString = prefix + credentialsPart + localIpPort +
			// relativeUrlString;
			String liveURLString = "rtsp://admin:12345@89.101.225.158:8300/h264/ch1/main/av_stream";
			String localURLString = "rtsp://admin:12345@192.168.1.101:8300/h264/ch1/main/av_stream";

			if (!localnetworkSettings.equalsIgnoreCase("1"))
			{
				MRLCamba liveMRL = new MRLCamba(liveURLString, false);
				if (!mrls.contains(liveMRL)) mrls.add(liveMRL);
			}
			//
			// if (localIpPort != null && localIpPort.length() > 0
			// && !localnetworkSettings.equalsIgnoreCase("2"))
			// {
			// MRLCamba localMRL = new MRLCamba(localURLString, true);
			// if (!mrls.contains(localMRL)) mrls.add(localMRL);
			// }

			mrlIndex = 0;
		}
		catch (Exception e)
		{
		}

	}

	// Loads image from cache. First image gets loaded correctly and hence we
	// can start making requests concurrently as well
	public boolean loadImageFromCache()
	{
		try
		{

			imageView.setImageDrawable(null);
			if (camera == null) return false;
			String path = this.getCacheDir() + "/" + camera.getCameraId() + ".jpg";
			if (new File(path).exists())
			{
				Drawable result = Drawable.createFromPath(path);
				if (result != null)
				{
					startDownloading = true;
					imageView.setImageDrawable(result);

					if (enableLogs) Log.i(TAG, "Loaded first image from Cache: " + media_width
							+ ":" + media_height);
					return true;
				}
				else
				{
					if (enableLogs) Log.e(
							TAG,
							"laodimagefromcache drawable d1 is null. Camera Object is ["
									+ camera.toString() + "]");
				}
			}
		}
		catch (OutOfMemoryError e)
		{
			if (enableLogs) Log.e(TAG, e.toString() + "-::OOM::-" + Log.getStackTraceString(e));
			return false;
		}
		catch (Exception e)
		{
			if (enableLogs) Log.e(TAG, e.toString() + "::" + Log.getStackTraceString(e));
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
		}

		return false;
	}

	// preferences options for this screen
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		try
		{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.videomenulayout, menu);

			return true;
		}
		catch (Exception ex)
		{
			if (enableLogs) Log.e(TAG, ex.toString());
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(ex);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		menu.clear();

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.ivideomenulayout, menu);

		return true;
	}

	// Tells that what item has been selected from options. We need to call the
	// relevent code for that item.
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		try
		{
			// Handle item selection
			switch (item.getItemId())
			{
			case R.id.menusettings_video:
				optionsActivityStarted = true;
				paused = true;
				startActivity(new Intent(this, VideoPrefsActivity.class));
				mediaPlayerView.setVisibility(View.GONE);

				showProgressView();
				if (enableLogs) Log
						.i(TAG, "Options Activity Started in onPrepareOptionsMenu event");
				return true;
			case android.R.id.home:
				this.finish();
				return true;
			default:
				// return super.onOptionsItemSelected(item);
				optionsActivityStarted = true;
				paused = true;
				startActivity(new Intent(this, VideoPrefsActivity.class));
				mediaPlayerView.setVisibility(View.GONE);

				showProgressView();
				if (enableLogs) Log
						.i(TAG, "Options Activity Started in onPrepareOptionsMenu event");
				return true;

			}
		}
		catch (OutOfMemoryError e)
		{
			if (enableLogs) Log.e(TAG, e.toString() + "-::OOM::-" + Log.getStackTraceString(e));
			return true;
		}
		catch (Exception e)
		{
			if (enableLogs) Log.e(TAG, e.toString() + "::" + Log.getStackTraceString(e));
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
			return true;
		}
	}

	// Read preferences for playing options audio and Video(images)
	public void readSetPreferences()
	{
		try
		{

			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

			isLocalNetwork = false;
			localnetworkSettings = sharedPrefs.getString(
					"pref_enablocalnetwork" + camera.getCameraId(), "0");// ("chkenablocalnetwork",
																			// false);
			if (localnetworkSettings.equalsIgnoreCase("1")) isLocalNetwork = true;
			else isLocalNetwork = false;

		}
		catch (OutOfMemoryError e)
		{
			if (enableLogs) Log.e(TAG, e.toString() + "-::OOM::-" + Log.getStackTraceString(e));
		}
		catch (Exception ex)
		{
			if (enableLogs) Log.e(TAG, ex.toString());
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(ex);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		try
		{
			super.onCreate(savedInstanceState);

			if (Constants.isAppTrackingEnabled) if (Constants.isAppTrackingEnabled) BugSenseHandler
					.initAndStartSession(this, Constants.bugsense_ApiKey);

			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

			int orientation = this.getResources().getConfiguration().orientation;
			if (orientation == Configuration.ORIENTATION_PORTRAIT)
			{
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
						WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			}
			else
			{
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
						WindowManager.LayoutParams.FLAG_FULLSCREEN);
			}

			if (this.getActionBar() != null)
			{

				this.getActionBar().setHomeButtonEnabled(true);
				this.getActionBar().setTitle("");
				this.getActionBar().setIcon(R.drawable.ic_navigation_back);
			}

			setContentView(R.layout.videolayoutwithslide);

			imageViewLayout = (RelativeLayout) this.findViewById(R.id.camimage1);
			imageView = (ImageView) this.findViewById(R.id.img_camera1);
			mediaPlayerView = (ImageView) this.findViewById(R.id.ivmediaplayer1);

			surfaceView = (SurfaceView) findViewById(R.id.surface1);
			surfaceHolder = surfaceView.getHolder();
			surfaceHolder.addCallback(this);

			progressView = ((ProgressView) imageViewLayout.findViewById(R.id.ivprogressspinner1));

			addCamerasToDropdownActionBar();

			if (!Commons.isOnline(this))
			{
				try
				{
					UIUtils.GetAlertDialog(VideoActivity.this, "Network not available",
							"Please connect to internat and try again",
							new DialogInterface.OnClickListener(){

								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									try
									{
										// TODO Auto-generated method stub
										// IVideoActivity.this.onStop();
										paused = true;
										// end = true; // do not finish activity
										// but
										dialog.dismiss();
										hideProgressView();
									}
									catch (Exception e)
									{
										if (Constants.isAppTrackingEnabled) BugSenseHandler
												.sendException(e);
									}
								}
							}).show();
					return;
				}
				catch (Exception e)
				{
					if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
				}
			}

			readSetPreferences();

			progressView.CanvasColor = Color.TRANSPARENT; // transparent color
															// because image
															// loaded in cache
															// should be
															// displayed as well

			isProgressShowing = true;
			progressView.setVisibility(View.VISIBLE);

			mediaPlayerView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v)
				{
					// TODO Auto-generated method stub
					if (end)
					{
						Toast.makeText(VideoActivity.this, "Please close and try again.",
								Toast.LENGTH_SHORT).show();
						return;
					}
					if (isProgressShowing) return;
					if (paused) // video is currently paused. Now we need to
								// resume it.
					{
						showProgressView();

						mediaPlayerView.setImageBitmap(null);
						mediaPlayerView.setVisibility(View.VISIBLE);
						mediaPlayerView.setImageResource(android.R.drawable.ic_media_pause);

						startMediaPlayerAnimation();

						RestartPlay(mrlPlaying);
						paused = false;
					}
					else
					// video is currently playing. Now we need to pause video
					{

						mediaPlayerView.clearAnimation();
						if (fadeInAnimation != null && fadeInAnimation.hasStarted()
								&& !fadeInAnimation.hasEnded())
						{
							fadeInAnimation.cancel();
							fadeInAnimation.reset();
						}
						mediaPlayerView.setVisibility(View.VISIBLE);
						mediaPlayerView.setImageBitmap(null);
						mediaPlayerView.setImageResource(android.R.drawable.ic_media_play);

						stopPlayer();

						paused = true; // mark the images as paused. Do not stop
										// threads, but do not show the images
										// showing up
					}
				}
			});

			imageViewLayout.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v)
				{

					if (end)
					{
						Toast.makeText(VideoActivity.this, "Please close and try again.",
								Toast.LENGTH_SHORT).show();
						return;
					}
					if (isProgressShowing) return;

					if (!paused && !end) // video is currently playing. Now we
											// need to pause video
					{
						VideoActivity.this.getActionBar().show();
						mediaPlayerView.setImageResource(android.R.drawable.ic_media_pause);

						mediaPlayerView.setVisibility(View.VISIBLE);

						startMediaPlayerAnimation();
					}

				}
			});
			if (enableLogs) Log.i(TAG, "Got image view " + imageViewLayout.toString());

			// Get the size of the device, will be our maximum.
			Display display = getWindowManager().getDefaultDisplay();
			screen_width = display.getWidth();
			screen_height = display.getHeight();
			if (enableLogs) Log.i(TAG, "Got Display specs");

			// Keep the screen on

			if (enableLogs) Log.i(TAG, "acquired the power lock");

		}
		catch (OutOfMemoryError e)
		{
			if (enableLogs) Log.e(TAG, e.toString() + "-::OOM::-" + Log.getStackTraceString(e));
		}
		catch (Exception ex)
		{
			if (enableLogs) Log.e(TAG, ex.toString(), ex);
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(ex);
		}
	}

	private void startMediaPlayerAnimation()
	{
		if (fadeInAnimation != null)
		{
			fadeInAnimation.cancel();
			fadeInAnimation.reset();

			mediaPlayerView.clearAnimation();
		}

		fadeInAnimation = AnimationUtils.loadAnimation(VideoActivity.this, R.layout.fadein);

		fadeInAnimation.setAnimationListener(new Animation.AnimationListener(){
			@Override
			public void onAnimationStart(Animation animation)
			{
				// TODO Auto-generated method stub
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
				// TODO Auto-generated method stub
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{

				if (!paused) mediaPlayerView.setVisibility(View.GONE);
				else mediaPlayerView.setVisibility(View.VISIBLE);

				int orientation = VideoActivity.this.getResources().getConfiguration().orientation;
				if (!paused && orientation == Configuration.ORIENTATION_LANDSCAPE)
				{
					VideoActivity.this.getActionBar().hide();
				}
			}
		});

		mediaPlayerView.startAnimation(fadeInAnimation);
	}

	private boolean isCurrentMRLValid()
	{
		if (mrlIndex < 0 || mrlIndex >= mrls.size() || mrls.size() == 0) return false;
		return true;
	}

	private boolean isNextMRLValid()
	{
		if (mrlIndex + 1 >= mrls.size() || mrls.size() == 0) return false;
		return true;
	}

	private String getCurrentMRL()
	{
		if (isCurrentMRLValid()) return mrls.get(mrlIndex).MRL;
		return null;
	}

	private String getNextMRL()
	{
		if (isNextMRLValid()) return mrls.get(++mrlIndex).MRL;
		return null;
	}

	/*************
	 * Surface
	 *************/

	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder)
	{
	}

	@Override
	public void surfaceChanged(SurfaceHolder surfaceholder, int format, int width, int height)
	{
		if (libvlc != null) libvlc.attachSurface(surfaceHolder.getSurface(), this);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceholder)
	{
	}

	private void setSize(int width, int height)
	{
		mVideoWidth = width;
		mVideoHeight = height;
		if (mVideoWidth * mVideoHeight <= 1) return;

		// get screen size
		int w = getWindow().getDecorView().getWidth();
		int h = getWindow().getDecorView().getHeight();

		// getWindow().getDecorView() doesn't always take orientation into
		// account, we have to correct the values
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		if (w > h && isPortrait || w < h && !isPortrait)
		{
			int i = w;
			w = h;
			h = i;
		}

		float videoAR = (float) mVideoWidth / (float) mVideoHeight;
		float screenAR = (float) w / (float) h;

		if (screenAR < videoAR) h = (int) (w / videoAR);
		else w = (int) (h * videoAR);

		// force surface buffer size
		surfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);

		// set display size
		LayoutParams lp = surfaceView.getLayoutParams();
		lp.width = w;
		lp.height = h;
		surfaceView.setLayoutParams(lp);
		surfaceView.invalidate();
	}

	@Override
	public void setSurfaceSize(int width, int height, int visible_width, int visible_height,
			int sar_num, int sar_den)
	{
		Message msg = Message.obtain(mHandler, videoSizeChanged, width, height);
		msg.sendToTarget();
	}

	/*************
	 * Player
	 *************/

	private void createPlayer(String media)
	{
		releasePlayer();
		try
		{

			if (media != null && media.length() > 0)
			{
				// Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
				// //sajjad
				showToast("Connecting... " + media);
			}
			else
			{
				media = "http://127.0.0.1:554/NoVideo-CambaTv";
			}

			// Create a new media player
			libvlc = LibVLC.getInstance();
			// sajjad libvlc.setIomx(false);
			libvlc.setSubtitlesEncoding("");
			libvlc.setAout(LibVLC.AOUT_OPENSLES);
			libvlc.setTimeStretching(false);
			libvlc.setChroma("RV32");
			libvlc.setVerboseMode(true);
			LibVLC.restart(this);
			EventHandler.getInstance().addHandler(mHandler);
			surfaceHolder.setFormat(PixelFormat.RGBX_8888);
			surfaceHolder.setKeepScreenOn(true);
			MediaList list = libvlc.getMediaList();
			list.clear();
			list.add(new Media(libvlc, LibVLC.PathToURI(media)), false);
			libvlc.playIndex(0);
		}
		catch (Exception e)
		{
			Toast.makeText(this, "Error connecting! " + media + " ::::: " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
		catch (Error e)
		{
			Log.e(TAG, e.getMessage());
		}
	}

	private void releasePlayer()
	{
		try
		{

			if (libvlc == null) return;
			EventHandler.getInstance().removeHandler(mHandler);
			libvlc.stop();
			libvlc.detachSurface();
			libvlc.closeAout();
			libvlc.destroy();
			libvlc = null;

			mVideoWidth = 0;
			mVideoHeight = 0;
		}
		catch (Exception e)
		{
			Log.e("sajjad", e.getMessage());
		}
	}

	private void RestartPlay(String media)
	{

		if (libvlc == null) return;

		try
		{

			libvlc.stop();

			if (media.length() > 0)
			{
				// Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
				// sajjad
				showToast("Reconnecting... " + media);
			}

			libvlc.getMediaList().clear();
			libvlc.playMRL(media);

		}
		catch (Exception e)
		{
			Toast.makeText(this, "Error reconnecting! " + media + " ::::: " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	private void pausePlayer()
	{
		if (libvlc == null) return;
		libvlc.pause();
	}

	private void stopPlayer()
	{
		if (libvlc == null) return;
		libvlc.stop();
	}

	private void playPlayer()
	{
		if (libvlc == null) return;
		libvlc.play();
	}

	/*************
	 * Events
	 *************/

	private Handler mHandler = new MyHandler(this);

	private static class MyHandler extends Handler
	{
		private WeakReference<VideoActivity> mOwner;

		public MyHandler(VideoActivity owner)
		{
			mOwner = new WeakReference<VideoActivity>(owner);
		}

		@Override
		public void handleMessage(Message msg)
		{

			try
			{

				VideoActivity player = mOwner.get();

				// SamplePlayer events
				if (msg.what == videoSizeChanged)
				{
					player.setSize(msg.arg1, msg.arg2);
					return;
				}

				// Libvlc events
				Bundle b = msg.getData();
				int event = b.getInt("event");
				// if(event == EventHandler.MediaPlayerPositionChanged || event
				// == EventHandler.MediaPlayerVout)
				// return;
				switch (event)
				{
				case EventHandler.MediaPlayerEndReached:
					Log.e("sajjad", "EventHandler.MediaPlayerEndReached");

					player.RestartPlay(player.mrlPlaying);
					// sajjad player.releasePlayer();
					break;
				case EventHandler.MediaPlayerPlaying:

					player.surfaceView.setVisibility(View.VISIBLE);
					player.imageView.setVisibility(View.GONE);
					player.mrlPlaying = player.getCurrentMRL();

					Log.e("sajjad", "EventHandler.MediaPlayerPlaying");
					break;

				case EventHandler.MediaPlayerPaused:
					Log.e("sajjad", "EventHandler.MediaPlayerPaused");
					break;

				case EventHandler.MediaPlayerStopped:
					Log.e("sajjad", "EventHandler.MediaPlayerStopped");
					break;

				case EventHandler.MediaPlayerEncounteredError:

					player.loadImageFromCache();

					if (player.mrlPlaying == null && player.isNextMRLValid()) player
							.RestartPlay(player.getNextMRL());
					else if (player.mrlPlaying != null) player.RestartPlay(player.mrlPlaying);
					else
					{

						// player.showMediaFailureDialog();
						player.showToast("Switching to jpeg view.");
						player.showImagesVideo = true;
						player.createNewImageThread();
						// player.StartResumeDownloading();

					}

					break;

				case EventHandler.MediaPlayerVout:
					player.hideProgressView();
					Log.e("sajjad", "EventHandler.MediaPlayerVout");
					try
					{
						if (VideoActivity.mrls.get(mrlIndex).isLocalNetwork == false)
						{
							SharedPreferences sharedPrefs = PreferenceManager
									.getDefaultSharedPreferences(player);
							SharedPreferences.Editor editor = sharedPrefs.edit();
							editor.putString("pref_mrlplaying" + camera.getCameraId(),
									player.mrlPlaying);
							editor.commit();
						}
					}
					catch (Exception ex)
					{
					}

					break;
				default:
					break;
				}

			}
			catch (Exception e)
			{
				Log.e("sajjad", e.getMessage());
			}
		}
	}

	public void SetImageAttributesAndLoadImage()
	{
		try
		{
			isFirstImageLiveReceived = false;
			isFirstImageLocalReceived = false;
			isFirstImageLiveEnded = false;
			isFirstImageLocalEnded = false;

			mediaPlayerView.setVisibility(View.GONE); // hide the media player
														// play icon when
														// animation
														// ends

			readSetPreferences();

			startDownloading = false;
			this.paused = false;
			this.end = false;
			this.isShowingFailureMessage = false;
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage(), e);
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
		}
	}

	public class BrowseImages extends AsyncTask<String, String, String>
	{

		@Override
		protected String doInBackground(String... params)
		{
			// TODO Auto-generated method stub
			while (!end && !isCancelled() && showImagesVideo) // keep on sending
																// requests
																// until the
																// activity ends
			{
				try
				{
					// wait for starting
					try
					{
						while (!startDownloading) // if downloading has not
													// started, keep on waiting
													// until it starts
						{
							if (enableLogs) Log.i(TAG, "going to sleep for half second.");
							;
							Thread.sleep(500);
						}
					}
					catch (Exception e)
					{
						if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
					}

					if (!paused) // if application is paused, do not send the
									// requests. Rather wait for the play
									// command
					{

						DownloadImage tasklive = new DownloadImage();

						if (downloadStartCount - downloadEndCount < 9) tasklive.executeOnExecutor(
								AsyncTask.THREAD_POOL_EXECUTOR,
								new String[] { getImageUrlToPost() });

						if (downloadStartCount - downloadEndCount > 9 && sleepInterval < 2000)
						{
							sleepInterval += intervalAdjustment;
						}
						else if (sleepInterval >= sleepIntervalMinTime)
						{
							sleepInterval -= intervalAdjustment;
						}

					}

				}
				catch (RejectedExecutionException ree)
				{
					Log.e(TAG, ree.toString() + "-::REE::-" + Log.getStackTraceString(ree));

				}
				catch (OutOfMemoryError e)
				{
					Log.e(TAG, e.toString() + "-::OOM::-" + Log.getStackTraceString(e));
				}
				catch (Exception ex)
				{
					downloadStartCount--;
					Log.e(TAG, ex.toString() + "-::::-" + Log.getStackTraceString(ex));
					if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(ex);
				}
				try
				{
					Thread.currentThread();
					Thread.sleep(sleepInterval, 50);
					Log.d(TAG, "sleepInterval" + sleepInterval);
				}
				catch (Exception e)
				{
					if (enableLogs) Log
							.e(TAG, e.toString() + "-::::-" + Log.getStackTraceString(e));
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String s)
		{
		}
	}

	private String getImageUrlToPost()
	{
		if (localnetworkSettings.equals("1")) return imageLiveLocalURL;
		else if (localnetworkSettings.equals("2")) return imageLiveCameraURL;
		else if (isLocalNetwork) return imageLiveLocalURL;
		else return imageLiveCameraURL;
	}

	@Override
	public void onResume()
	{
		try
		{
			super.onResume();
			this.paused = false;

			if (optionsActivityStarted)
			{
				optionsActivityStarted = false;

				if (enableLogs) Log.i(TAG, "onResume in block executed");

				showProgressView();

				readSetPreferences();

				startDownloading = false;
				this.paused = false;
				this.end = false;
				this.isShowingFailureMessage = false;

				latestStartImageTime = SystemClock.uptimeMillis();

				if (imageThread == null) // ignore if image thread is null
				{

				}
				else if (imageThread.getStatus() != AsyncTask.Status.RUNNING)
				{
					imageThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
				}
				else if (imageThread.getStatus() == AsyncTask.Status.FINISHED)
				{
					imageThread = new BrowseImages();
					imageThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
				}
			}
			if (enableLogs) Log.i(TAG, "onResume ended");
		}
		catch (OutOfMemoryError e)
		{
			if (enableLogs) Log.e(TAG, e.toString() + "-::OOM::-" + Log.getStackTraceString(e));
		}
		catch (Exception e)
		{
			if (enableLogs) Log.e(TAG, e.toString());
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
		}
	}

	// Hide progress view
	void hideProgressView()
	{
		imageViewLayout.findViewById(R.id.ivprogressspinner1).setVisibility(View.GONE);
		isProgressShowing = false;
		isProgressShowing = false;
	}

	void showProgressView()
	{
		progressView.CanvasColor = Color.TRANSPARENT;
		progressView.setVisibility(View.VISIBLE);
		isProgressShowing = true;
	}

	// When activity gets focused again
	@Override
	public void onRestart()
	{
		try
		{
			super.onRestart();
			paused = false;
			end = false;

			if (optionsActivityStarted)
			{
				mrlPlaying = null;
				setCameraForPlaying(this, camera);

				createPlayer(getCurrentMRL());

			}
		}
		catch (OutOfMemoryError e)
		{
			if (enableLogs) Log.e(TAG, e.toString() + "-::OOM::-" + Log.getStackTraceString(e));
		}
		catch (Exception e)
		{
			if (enableLogs) Log.e(TAG, e.toString());
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
		}
	}

	private void createNewImageThread()
	{
		imageThread = new BrowseImages();
		imageThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
	}

	// when activity loses focus. we need to end this activity
	@Override
	public void onPause()
	{
		try
		{
			super.onPause();

			if (!optionsActivityStarted)
			{
				this.paused = true;
			}
		}
		catch (Exception ex)
		{
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(ex);
		}
	}

	// When activity loses focus and some other activity gets activated. We need
	// to end this activity

	@Override
	public void onStart()
	{
		super.onStart();
		if (Constants.isAppTrackingEnabled)
		{
			EasyTracker.getInstance().activityStart(this); // Add this method.
			if (Constants.isAppTrackingEnabled) BugSenseHandler.startSession(this);
		}
	}

	@Override
	public void onStop()
	{
		try
		{
			super.onStop();
			releasePlayer();
			if (!optionsActivityStarted)
			{
				if (imageThread != null)
				{
					this.paused = true;
				}
				if (enableLogs) Log.i(TAG, "onStop in block executed");
				this.finish();
			}
		}
		catch (Exception ex)
		{
		}

		if (Constants.isAppTrackingEnabled)
		{
			EasyTracker.getInstance().activityStop(this);
			if (Constants.isAppTrackingEnabled) BugSenseHandler.closeSession(this);
		}
	}

	// when screen gets rotated
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		try
		{
			super.onConfigurationChanged(newConfig);

			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			int orientation = newConfig.orientation;
			if (orientation == Configuration.ORIENTATION_PORTRAIT)
			{
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
						WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
				landscape = false;
				this.getActionBar().show();
			}
			else
			{
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
						WindowManager.LayoutParams.FLAG_FULLSCREEN);
				landscape = true;

				if (!paused && !end && !isProgressShowing) this.getActionBar().hide();
				else this.getActionBar().show();
			}

			this.invalidateOptionsMenu();

			mVideoWidth = surfaceView.getWidth();
			mVideoHeight = surfaceView.getHeight() - this.getActionBar().getHeight();
			setSize(mVideoWidth, mVideoHeight);

		}
		catch (Exception e)
		{
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
		}
	}

	// resize the activity if screen gets rotated
	public void resize(int imageHieght, int imageWidth)
	{
		int w = landscape ? screen_height : screen_width;
		int h = landscape ? screen_width : screen_height;

		// sajjad h -= actionBarHeight;

		// If we have the media, calculate best scaling inside bounds.
		if (imageWidth > 0 && imageHieght > 0)
		{
			final float max_w = w;
			final float max_h = h;
			float temp_w = imageWidth;
			float temp_h = imageHieght;
			float factor = max_w / temp_w;
			temp_w *= factor;
			temp_h *= factor;

			// If we went above the height limit, scale down.
			if (temp_h > max_h)
			{
				factor = max_h / temp_h;
				temp_w *= factor;
				temp_h *= factor;
			}

			w = (int) temp_w;
			h = (int) temp_h;
		}
		media_height = h;
		media_width = w;
		if (enableLogs) Log.i(TAG, "resize method called: " + w + ":" + h);
	}

	// download the image from the camera
	private class DownloadImage extends AsyncTask<String, Void, Drawable>
	{
		private long myStartImageTime;
		private boolean isLocalNetworkRequest = false;

		@Override
		protected Drawable doInBackground(String... urls)
		{
			if (!showImagesVideo) return null;
			Drawable response = null;
			for (String url1 : urls)
			{
				try
				{
					downloadStartCount++;
					if (url1 == null) url1 = "http://www.camba.tv/no-image.jpg";
					myStartImageTime = SystemClock.uptimeMillis();

					// if (camera.getUseCredentials())
					// {
					response = Commons.getDrawablefromUrlAuthenticated1(url1, camera.getUsername(),
							camera.getPassword(), camera.cookies, 15000);

					// }
					// else
					// {
					// URL url = new URL(url1);
					// response = Commons.DownlaodDrawableSync(url, 15000);
					// }
					if (response != null) successiveFailureCount = 0;
				}
				catch (OutOfMemoryError e)
				{
					if (enableLogs) Log.e(TAG,
							e.toString() + "-::OOM::-" + Log.getStackTraceString(e));
					successiveFailureCount++;
					return null;
				}
				catch (Exception e)
				{
					if (enableLogs) Log.e(TAG, "Exception: " + e.toString() + "\r\n" + "ImageURl=["
							+ url1 + "]");
					if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
					successiveFailureCount++;
				} finally
				{
					downloadEndCount++;
				}
			}

			return response;
		}

		@Override
		protected void onPostExecute(Drawable result)
		{// DownloadImage Live
			try
			{
				if (!showImagesVideo) return;

				try
				{
					if (isLocalNetworkRequest) isFirstImageLocalEnded = true;
					else isFirstImageLiveEnded = true;
				}
				catch (Exception ex)
				{
				}
				if (result != null && result.getIntrinsicWidth() > 0
						&& result.getIntrinsicHeight() > 0
						&& myStartImageTime >= latestStartImageTime && !paused && !end)
				{
					if (isLocalNetworkRequest) isFirstImageLocalReceived = true;
					else isFirstImageLiveReceived = true;
					if (isLocalNetworkRequest && localnetworkSettings.equalsIgnoreCase("0")) isLocalNetwork = true;

					latestStartImageTime = myStartImageTime;

					if (mediaPlayerView.getVisibility() != View.VISIBLE
							&& VideoActivity.this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) VideoActivity.this
							.getActionBar().hide();

					if (showImagesVideo) imageView.setImageDrawable(result);

					if (enableLogs) Log.i(TAG, "image loaded in ivideo");

					hideProgressView();

				}
				// do not show message on local network failure request.
				else if (((!isFirstImageLocalEnded && !isFirstImageLiveEnded
						&& !isFirstImageLocalReceived && !isFirstImageLiveReceived && localnetworkSettings
							.equalsIgnoreCase("0")) // loclal task ended. Now
													// this is live image
													// request
				|| successiveFailureCount > 10

				// ( ( !isFirstImageLocalReceived &&
				// localnetworkSettings.equalsIgnoreCase("1") )
				// || (!isFirstImageLiveReceived &&
				// localnetworkSettings.equalsIgnoreCase("2") )
				// || (isFirstImageLocalEnded && !isLocalNetworkRequest ) //
				// loclal task ended. Now this is live image request
				// || (isFirstImageLiveEnded && isLocalNetworkRequest ) // Image
				// Live task ended. Now this is local image request
				// || successiveFailureCount > 10
				//
				)
						&& !isShowingFailureMessage
						&& myStartImageTime >= latestStartImageTime
						&& !paused && !end) // end endif condition
				{
					showMediaFailureDialog();
					imageThread.cancel(true);
				}
				else
				{
					if (enableLogs) Log.i(TAG, "downloaded image discarded. ");
				}
			}
			catch (OutOfMemoryError e)
			{
				if (enableLogs) Log.e(TAG, e.toString() + "-::OOM::-" + Log.getStackTraceString(e));
			}
			catch (Exception e)
			{
				if (enableLogs) Log.e(TAG, e.toString());
				if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
			}
			try
			{
				startDownloading = true;
			}
			catch (Exception ex)
			{
			}
		}
	}

	private void showToast(String text)
	{
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
		toast.show();
	}

	void showMediaFailureDialog()
	{
		UIUtils.GetAlertDialog(VideoActivity.this, "Unable to play",
				"Please check camera and try again.", new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						try
						{
							// IVideoActivity.this.onStop();
							VideoActivity.this.getActionBar().show();
							paused = true;
							// end = true; // do not finish activity but
							isShowingFailureMessage = false;
							dialog.dismiss();
							hideProgressView();
						}
						catch (Exception e)
						{
							if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
						}
					}
				}).show();
		isShowingFailureMessage = true;
		showImagesVideo = false;
	}

	static public class MRLCamba
	{
		public String MRL = "";
		public boolean isLocalNetwork = false;

		public MRLCamba(String _MRL, boolean _isLocalNetwork)
		{
			MRL = _MRL;
			isLocalNetwork = _isLocalNetwork;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null) return false;
			if (obj == this) return true;
			if (!(obj instanceof MRLCamba)) return false;

			MRLCamba rhs = (MRLCamba) obj;
			return this.MRL.equalsIgnoreCase(rhs.MRL);
		}
	}

	@Override
	public void onSlideMenuItemClick(int itemId)
	{
		// VideoActivity.startPlayingVIdeoForCamera(VideoActivity.this, itemId);
	}

	private class LoadActiveCamerasTask extends AsyncTask<String, String, String[]>
	{
		final ArrayList<EvercamCamera> activeCameras = new ArrayList<EvercamCamera>();
		int defaultCameraIndex = 0;

		@Override
		protected String[] doInBackground(String... params)
		{
			ArrayList<String> cameraNames = new ArrayList<String>();

			for (int count = 0; count < AppData.evercamCameraList.size(); count++)
			{
				if (!AppData.evercamCameraList.get(count).getStatus()
						.equalsIgnoreCase(CameraStatus.OFFLINE))
				{
					activeCameras.add(AppData.evercamCameraList.get(count));
					cameraNames.add(AppData.evercamCameraList.get(count).getName());
					if (AppData.evercamCameraList.get(count).getCameraId() == startingCameraID)
					{
						defaultCameraIndex = cameraNames.size() - 1;
					}
				}
			}

			String[] cameraArray = new String[cameraNames.size()];
			cameraNames.toArray(cameraArray);

			return cameraArray;
		}

		@Override
		protected void onPostExecute(final String[] cameraNames)
		{
			try
			{
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(VideoActivity.this,
						android.R.layout.simple_spinner_dropdown_item, cameraNames);
				VideoActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
				OnNavigationListener navigationListener = new OnNavigationListener(){
					@Override
					public boolean onNavigationItemSelected(int itemPosition, long itemId)
					{
						try
						{
							showImagesVideo = false;
							if (imageThread != null
									&& imageThread.getStatus() != AsyncTask.Status.RUNNING) imageThread
									.cancel(true);
							imageThread = null;

							mrlPlaying = null;
							setCameraForPlaying(VideoActivity.this, activeCameras.get(itemPosition));

							createPlayer(getCurrentMRL());

						}
						catch (Exception e)
						{
							Log.e(TAG, e.getMessage(), e);
							if (Constants.isAppTrackingEnabled)
							{
								BugSenseHandler.sendException(e);
							}
						}
						return false;
					}
				};

				getActionBar().setListNavigationCallbacks(adapter, navigationListener);
				getActionBar().setSelectedNavigationItem(defaultCameraIndex);

			}
			catch (Exception e)
			{
				Log.e(TAG, e.getMessage(), e);
				if (Constants.isAppTrackingEnabled)
				{
					BugSenseHandler.sendException(e);
				}
			}

		}
	}
}
