package ros.android.activity;

import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import ros.android.util.RobotDescription;

import android.util.Log;

public class RobotNameResolver extends AbstractNodeMain {

	private RobotDescription currentRobot;
	private NameResolver appNameResolver;
	private NameResolver robotNameResolver;
	private GraphName name;
	private GraphName app;
	private ConnectedNode connectedNode;

	public RobotNameResolver() {
	}

	public void setRobot(RobotDescription currentRobot) {
		this.currentRobot = currentRobot;
	}

	@Override
	public GraphName getDefaultNodeName() {
		return null;
	}

	public void setRobotName(String name) {
		this.name = GraphName.of(name);
	}
	
	public void resetRobotName(String name) {
		robotNameResolver = connectedNode.getResolver().newChild(name);
	}


	public NameResolver getAppNameSpace() {
		return appNameResolver;
	}

	public NameResolver getRobotNameSpace() {
		return robotNameResolver;
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
		if (currentRobot != null) {
			name = GraphName.of(currentRobot.getRobotName());
		}
			app = name.join(GraphName.of("application"));
		appNameResolver = connectedNode.getResolver().newChild(app);
		robotNameResolver = connectedNode.getResolver().newChild(name);
	}
}