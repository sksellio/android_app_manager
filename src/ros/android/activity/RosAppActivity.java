/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ros.android.activity;

//package ros.android.activity;

import java.net.URI;
import java.net.URISyntaxException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseListener;

import app_manager.StartAppResponse;
import app_manager.StopAppResponse;

import ros.android.util.Dashboard;
import ros.android.activity.AppManager;
import ros.android.util.RobotDescription;
import ros.android.activity.RobotNameResolver;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public abstract class RosAppActivity extends RosActivity {

	public static final String ROBOT_DESCRIPTION_EXTRA = "ros.android.util.RobotDescription";
	private String robotAppName = null;
	private String defaultAppName = null;
	private String defaultRobotName = null;
	private boolean startApplication = true;
	private boolean fromAppChooser = false;
	private boolean keyBackTouched = false;
	private int dashboardResourceId = 0;
	private int mainWindowId = 0;
	private Dashboard dashboard = null;
	private NodeConfiguration nodeConfiguration;
	private NodeMainExecutor nodeMainExecutor;
	private URI uri;
	private ProgressDialog startingDialog;
	protected RobotNameResolver robotNameResolver;
	protected RobotDescription robotDescription;
	protected boolean fromApplication = false;

	protected void setDashboardResource(int resource) {
		dashboardResourceId = resource;
	}

	protected void setMainWindowResource(int resource) {
		mainWindowId = resource;
	}

	protected void setDefaultRobotName(String name) {
		defaultRobotName = name;
	}

	protected void setDefaultAppName(String name) {
		if (name == null) {
			startApplication = false;
		}
		defaultAppName = name;
	}

	protected void setCustomDashboardPath(String path) {
		dashboard.setCustomDashboardPath(path);
	}

	protected RosAppActivity(String notificationTicker, String notificationTitle) {
		super(notificationTicker, notificationTitle);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (mainWindowId == 0) {
			Log.e("RosAndroid",
					"You must set the dashboard resource ID in your RosAppActivity");
			return;
		}
		if (dashboardResourceId == 0) {
			Log.e("RosAndroid",
					"You must set the dashboard resource ID in your RosAppActivity");
			return;
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(mainWindowId);

		robotNameResolver = new RobotNameResolver();

		if (defaultRobotName != null) {
			robotNameResolver.setRobotName(defaultRobotName);
		}

		robotAppName = getIntent().getStringExtra(
				AppManager.PACKAGE + ".robot_app_name");
		if (robotAppName == null) {
			robotAppName = defaultAppName;
		} else if (robotAppName.equals("AppChooser")) {
			fromApplication = true;
		} else {
			fromAppChooser = true;
			startingDialog = ProgressDialog.show(this, "Starting Robot",
					"starting robot...", true, false);
			startingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}

		if (dashboard == null) {
			dashboard = new Dashboard(this);
			dashboard.setView((LinearLayout) findViewById(dashboardResourceId),
					new LinearLayout.LayoutParams(
							LinearLayout.LayoutParams.WRAP_CONTENT,
							LinearLayout.LayoutParams.WRAP_CONTENT));
		}

	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		this.nodeMainExecutor = nodeMainExecutor;
		nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
				.newNonLoopback().getHostAddress(), getMasterUri());

		if (getIntent().hasExtra(ROBOT_DESCRIPTION_EXTRA)) {
			robotDescription = (RobotDescription) getIntent()
					.getSerializableExtra(ROBOT_DESCRIPTION_EXTRA);
		}
		if (robotDescription != null) {
			if (fromAppChooser) {
				robotNameResolver.setRobot(robotDescription);
			}
			dashboard.setRobotName(robotDescription.getRobotType());
		} else {
			if (getRobotNameSpace().getNamespace()
						.toString() != null) {
				dashboard.setRobotName(getRobotNameSpace().getNamespace()
						.toString());
			} else {
			dashboard.setRobotName("Pr2");
			}
		}		
		nodeMainExecutor.execute(robotNameResolver,
				nodeConfiguration.setNodeName("robotNameResolver"));
		while (getAppNameSpace() == null) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}
		}
		
		nodeMainExecutor.execute(dashboard,
				nodeConfiguration.setNodeName("dashboard"));

	
		if (fromAppChooser && startApplication) {
			if (getIntent().getBooleanExtra("runningNodes", false)) {
				restartApp();
			} else
				startApp();
		} else if (startApplication) {
			startApp();
		}
	}
	

	protected NameResolver getAppNameSpace() {
		return robotNameResolver.getAppNameSpace();
	}

	protected NameResolver getRobotNameSpace() {
		return robotNameResolver.getRobotNameSpace();
	}

	protected void onAppTerminate() {
		RosAppActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(RosAppActivity.this)
						.setTitle("App Termination")
						.setMessage(
								"The application has terminated on the server, so the client is exiting.")
						.setCancelable(false)
						.setNeutralButton("Exit",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										RosAppActivity.this.finish();
									}
								}).create().show();
			}
		});
	}

	@Override
	public void startMasterChooser() {
		if (!fromAppChooser && !fromApplication) {
			super.startMasterChooser();
		} else {
			Intent intent = new Intent();
			intent.putExtra(AppManager.PACKAGE + ".robot_app_name",
					"AppChooser");
			try {
				uri = new URI(getIntent().getStringExtra("ChooserURI"));
			} catch (URISyntaxException e) {
				throw new RosRuntimeException(e);
			}

			nodeMainExecutorService.setMasterUri(uri);
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					RosAppActivity.this.init(nodeMainExecutorService);
					return null;
				}
			}.execute();
		}

	}

	private void restartApp() {
		Log.i("RosAndroid", "Restarting application");
		AppManager appManager = new AppManager("*", getRobotNameSpace());
		appManager.setFunction("stop");

		appManager
				.setStopService(new ServiceResponseListener<StopAppResponse>() {
					@Override
					public void onSuccess(StopAppResponse message) {
						Log.i("RosAndroid", "App stopped successfully");
						try {
							Thread.sleep(1000);
						} catch (Exception e) {

						}
						startApp();
					}

					@Override
					public void onFailure(RemoteException e) {
						Log.e("RosAndroid", "App failed to stop!");
					}
				});
		nodeMainExecutor.execute(appManager,
				nodeConfiguration.setNodeName("start_app"));

	}

	private void startApp() {
		Log.i("RosAndroid", "Starting application");

		AppManager appManager = new AppManager(robotAppName,
				getRobotNameSpace());
		appManager.setFunction("start");

		appManager
				.setStartService(new ServiceResponseListener<StartAppResponse>() {
					@Override
					public void onSuccess(StartAppResponse message) {
						if (message.getStarted()) {
							if (fromAppChooser == true) {
								if(startingDialog != null){
    								startingDialog.dismiss();
								}
							}
							Log.i("RosAndroid", "App started successfully");
						} else
							Log.e("RosAndroid", "App failed to start!");
					}

					@Override
					public void onFailure(RemoteException e) {
						Log.e("RosAndroid", "App failed to start!");
					}
				});

		nodeMainExecutor.execute(appManager,
				nodeConfiguration.setNodeName("start_app"));
	}

	protected void stopApp() {
		Log.i("RosAndroid", "Stopping application");
		AppManager appManager = new AppManager(robotAppName,
				getRobotNameSpace());
		appManager.setFunction("stop");

		appManager
				.setStopService(new ServiceResponseListener<StopAppResponse>() {
					@Override
					public void onSuccess(StopAppResponse message) {
						Log.i("RosAndroid", "App stopped successfully");
					}

					@Override
					public void onFailure(RemoteException e) {
						Log.e("RosAndroid", "App failed to stop!");
					}
				});
		nodeMainExecutor.execute(appManager,
				nodeConfiguration.setNodeName("start_app"));
	}

	protected void releaseRobotNameResolver() {
		nodeMainExecutor.shutdownNodeMain(robotNameResolver);
	}

	protected void releaseDashboardNode() {
		nodeMainExecutor.shutdownNodeMain(dashboard);
	}

	@Override
	protected void onDestroy() {
		if (startApplication && !keyBackTouched) {
			stopApp();
		}
		if (startingDialog != null) {
        		startingDialog.dismiss();
        		startingDialog = null;
        }
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (fromAppChooser) {
			keyBackTouched = true;
			Intent intent = new Intent();
			intent.putExtra(AppManager.PACKAGE + ".robot_app_name",
					"AppChooser");
			intent.putExtra("ChooserURI", uri.toString());
			intent.setAction("org.ros.android.android_app_chooser.AppChooser");
			intent.addCategory("android.intent.category.DEFAULT");
			startActivity(intent);
			onDestroy();
		}
		super.onBackPressed();
	}
}