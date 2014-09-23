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

import java.util.ArrayList;

import android.util.Log;

import org.ros.exception.RosException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Subscriber;

import app_manager.AppList;
import app_manager.ListApps;
import app_manager.ListAppsRequest;
import app_manager.ListAppsResponse;
import app_manager.StartApp;
import app_manager.StartAppRequest;
import app_manager.StartAppResponse;
import app_manager.StopApp;
import app_manager.StopAppRequest;
import app_manager.StopAppResponse;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class AppManager extends AbstractNodeMain {

	static public final String PACKAGE = "org.ros.android";
	private static final String startTopic = "start_app";
	private static final String stopTopic = "stop_app";
	private static final String listTopic = "list_apps";

	private String appName;
	private NameResolver resolver;
	private ServiceResponseListener<StartAppResponse> startServiceResponseListener;
	private ServiceResponseListener<StopAppResponse> stopServiceResponseListener;
	private ServiceResponseListener<ListAppsResponse> listServiceResponseListener;
	private ArrayList<Subscriber<AppList>> subscriptions;
	private Subscriber<AppList> subscriber;
	
	private ConnectedNode connectedNode;
	private String function = null;

	public AppManager(final String appName, NameResolver resolver) {
		this.appName = appName;
		this.resolver = resolver;
	}

	public AppManager(final String appName) {
		this.appName = appName;
	}

	public AppManager() {

	}

	public void addAppListCallback(MessageListener<AppList> callback)
			throws RosException {
		subscriber = connectedNode.newSubscriber(resolver.resolve("app_list"),"app_manager/AppList");
		subscriber.addMessageListener(callback);
	}

	public void setFunction(String function) {
		this.function = function;
	}
	
	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setStartService(
			ServiceResponseListener<StartAppResponse> startServiceResponseListener) {
		this.startServiceResponseListener = startServiceResponseListener;
	}

	public void setStopService(
			ServiceResponseListener<StopAppResponse> stopServiceResponseListener) {
		this.stopServiceResponseListener = stopServiceResponseListener;
	}

	public void setListService(
			ServiceResponseListener<ListAppsResponse> listServiceResponseListener) {
		this.listServiceResponseListener = listServiceResponseListener;
	}

	public void startApp() {
		String startTopic = resolver.resolve(this.startTopic).toString();

		ServiceClient<StartAppRequest, StartAppResponse> startAppClient;
		try {
			Log.i("RosAndroid", "Start app service client created");
			startAppClient = connectedNode.newServiceClient(startTopic,
					StartApp._TYPE);
		} catch (ServiceNotFoundException e) {
			throw new RosRuntimeException(e);
		}
		final StartAppRequest request = startAppClient.newMessage();
		request.setName(appName);
		startAppClient.call(request, startServiceResponseListener);
		Log.i("RosAndroid", "Done call");
	}

	public void stopApp() {
		String stopTopic = resolver.resolve(this.stopTopic).toString();

		ServiceClient<StopAppRequest, StopAppResponse> stopAppClient;
		try {
			Log.i("RosAndroid", "Stop app service client created");
			stopAppClient = connectedNode.newServiceClient(stopTopic,
					StopApp._TYPE);
		} catch (ServiceNotFoundException e) {
			throw new RosRuntimeException(e);
		}
		final StopAppRequest request = stopAppClient.newMessage();
		request.setName(appName);
		stopAppClient.call(request, stopServiceResponseListener);
		Log.i("RosAndroid", "Done call");
	}

	public void listApps() {
		String listTopic = resolver.resolve(this.listTopic).toString();
		
		ServiceClient<ListAppsRequest, ListAppsResponse> listAppsClient;
		try {
			Log.i("RosAndroid", "List app service client created" + listTopic);
			listAppsClient = connectedNode.newServiceClient(listTopic,
					ListApps._TYPE);
		} catch (ServiceNotFoundException e) {
			throw new RosRuntimeException(e);
		}
		final ListAppsRequest request = listAppsClient.newMessage();
		listAppsClient.call(request, listServiceResponseListener);
		Log.i("RosAndroid", "Done call");
	}

	@Override
	public GraphName getDefaultNodeName() {
		return null;
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
		if (function.equals("start")) {
			startApp();
		} else if (function.equals("stop")) {
			stopApp();
		} else if (function.equals("list")) {
			listApps();
		}
	}
}