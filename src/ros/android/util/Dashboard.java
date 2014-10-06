package ros.android.util;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.parameter.ParameterTree;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class Dashboard implements NodeMain {
	public interface DashboardInterface {
		/**
		 * Set the ROS Node to use to get status data and connect it up.
		 * Disconnects the previous node if there was one.
		 */
		public void onStart(ConnectedNode connectedNode);

		public void onShutdown(Node node);
	}

	private static final String turtlebotDashboardPath = "ros.android.views.TurtlebotDashboard";
	//private static final String pr2DashboardPath = "com.ros.pr2.apps.core_components.Pr2Dashboard";
	private static final String pr2DashboardPath = "ros.android.views.Pr2Dashboard";

	private DashboardInterface dashboard;
	private Activity activity;
	private ViewGroup view;
	private ViewGroup.LayoutParams lparams;
	private static String robotName;
	private static String customDashboardPath;

	public Dashboard(Activity activity) {
		dashboard = null;
		this.activity = activity;
		this.view = null;
		this.lparams = null;
	}

	public void setView(ViewGroup view, ViewGroup.LayoutParams lparams) {
		if (view == null) {
			Log.e("Dashboard", "Null view for dashboard");
		}
		this.view = view;
		this.lparams = lparams;
	}

	public void setRobotName(String name) {
		robotName = name;
	}

	public void setCustomDashboardPath(String path) {
		this.customDashboardPath = path;
	}

	private static DashboardInterface createDashboard(Class dashClass,
			Context context) {
		ClassLoader classLoader = Dashboard.class.getClassLoader();
		Object[] args = new Object[1];
		DashboardInterface result = null;
		args[0] = context;
		try {
			Class contextClass = Class.forName("android.content.Context");
			result = (DashboardInterface) dashClass
					.getConstructor(contextClass).newInstance(args);
		} catch (Exception ex) {
			Log.e("Dashboard", "Error during dashboard instantiation:", ex);
			result = null;
		}
		return result;
	}

	private static DashboardInterface createDashboard(String className,
			Context context) {
		Class dashClass = null;
		try {
			dashClass = Class.forName(className);
		} catch (Exception ex) {
			Log.e("Dashboard", "Error during dashboard class loading:", ex);
			return null;
		}
		return createDashboard(dashClass, context);

	}

	/**
	 * Dynamically locate and create a dashboard.
	 */
	// TODO: deal with the custom robot dashboard
	private static DashboardInterface createDashboard(Context context) {
		if (customDashboardPath != null) {
			return createDashboard(customDashboardPath, context);
		} else if (robotName.equals("turtlebot")) {
			return createDashboard(turtlebotDashboardPath, context);
		} else if (robotName.equals("pr2")) {
			return createDashboard(pr2DashboardPath, context);
		}

		return createDashboard(turtlebotDashboardPath, context); // Default
	}

	@Override
	public void onError(Node arg0, Throwable arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onShutdown(final Node node) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Dashboard.DashboardInterface dash = dashboard;
				if (dash != null) {
					dash.onShutdown(node);
					view.removeView((View) dash);
				}
				dashboard = null;
			}
		});
	}

	@Override
	public void onShutdownComplete(Node node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStart(ConnectedNode connectedNode) {
		if (dashboard != null) {
			// FIXME: should we re-start the dashboard? I think this is really
			// an error.
			return;
		}
		dashboard = Dashboard.createDashboard(activity);
		if (dashboard != null) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {

					Dashboard.DashboardInterface dash = dashboard;
					ViewGroup localView = view;
					if (dash != null && localView != null) {
						localView.addView((View) dash, lparams);
					} else if (dash == null) {
						Log.e("Dashboard",
								"Dashboard could not start: no dashboard");
					} else if (view == null) {
						Log.e("Dashboard", "Dashboard could not start: no view");
					} else {
						Log.e("Dashboard",
								"Dashboard could not start: no view or dashboard");
					}
				}
			});
			dashboard.onStart(connectedNode);
		}
	}

	@Override
	public GraphName getDefaultNodeName() {
		// TODO Auto-generated method stub
		return null;
	}
}
