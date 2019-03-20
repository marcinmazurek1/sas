package com.github.marcinmazurek1.sas;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.sas.iom.SAS.IWorkspace;
import com.sas.iom.SAS.IWorkspaceHelper;
import com.sas.iom.SASIOMDefs.GenericError;
import com.sas.meta.SASOMI.IOMI;
import com.sas.meta.SASOMI.IOMIHelper;
import com.sas.metadata.remote.LogicalServer;
import com.sas.metadata.remote.Login;
import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MdFactoryImpl;
import com.sas.metadata.remote.MdOMIUtil;
import com.sas.metadata.remote.MdOMRConnection;
import com.sas.metadata.remote.MdObjectStore;
import com.sas.metadata.remote.MetadataObjects;
import com.sas.metadata.remote.ServerContext;
import com.sas.rio.MVAConnection;
import com.sas.services.connection.BridgeServer;
import com.sas.services.connection.ConnectionFactoryConfiguration;
import com.sas.services.connection.ConnectionFactoryException;
import com.sas.services.connection.ConnectionFactoryInterface;
import com.sas.services.connection.ConnectionFactoryManager;
import com.sas.services.connection.ConnectionInterface;
import com.sas.services.connection.ManualConnectionFactoryConfiguration;
import com.sas.services.connection.Server;
import com.sas.services.connection.omr.OMRConnectionFactoryConfiguration;

/**
 * Connect to SAS server
 * 
 * @author Marcin Mazurek
 * @since 1.0
 *
 */
public class Sas {
	private static MdFactoryImpl factory;
	private static ConnectionInterface cx;
	private static ConnectionInterface cx_omr;
	private static ConnectionFactoryInterface cxf;
	private static ConnectionFactoryInterface cxf_omr;
	private static IWorkspace iWorkspace;
	private static Connection conn;

	public static boolean connect(String host, int port, String username, String password, String contextServer,
			String authenticationDomain)
			throws IllegalArgumentException, RemoteException, MdException, ConnectionFactoryException, SQLException {
		String classID = Server.CLSID_SASOMI;
		String logicalUser = username, logicalPass = password;

		factory = new MdFactoryImpl(false);
		MdOMRConnection connFactory = factory.getConnection();

		connFactory.makeOMRConnection(host, String.valueOf(port), username, password);
		MdOMIUtil omiUtil = factory.getOMIUtil();
		MdObjectStore store = factory.createObjectStore();
		int flags = MdOMIUtil.OMI_GET_METADATA | MdOMIUtil.OMI_ALL_SIMPLE;
		String reposID = omiUtil.getFoundationReposID();

		Server omrServer = new BridgeServer(classID, host, port);
		ConnectionFactoryConfiguration cxfConfig_omr = new ManualConnectionFactoryConfiguration(omrServer);
		ConnectionFactoryManager cxfManager = new ConnectionFactoryManager();
		cxf_omr = cxfManager.getFactory(cxfConfig_omr);

		cx_omr = cxf_omr.getConnection(username, password);
		IOMI iOMI = IOMIHelper.narrow(cx_omr.getObject());
	
		Set<ServerContext> contextServers = new HashSet<ServerContext>();
		Iterator<?> serverContexts = omiUtil
				.getMetadataObjectsSubset(store, reposID, MetadataObjects.SERVERCONTEXT, flags, "").iterator();
		while (serverContexts.hasNext())
			contextServers.add((ServerContext) serverContexts.next());

		String workspaceServer = null;
		if (contextServers.size() == 1)
			workspaceServer = getWorkspaceServer(contextServers.iterator().next());
		else {
			boolean found = false;
			Set<String> contextNames = new HashSet<String>();
			for (ServerContext context : contextServers) {
				String contextName = context.getName();
				contextNames.add(contextName);
				if (contextName.equals(contextServer)) {
					workspaceServer = getWorkspaceServer(context);
					found = true;
					break;
				}
			}
			if (!found)
				throw new IllegalArgumentException("Available ServerContext ".concat(String.valueOf(contextNames)));
		}

		ConnectionFactoryConfiguration cxfConfig = new OMRConnectionFactoryConfiguration(iOMI, reposID, workspaceServer);
		cxf = cxfManager.getFactory(cxfConfig);

		List<?> authenticationDomains = cxf.getDomains();
		if (!authenticationDomains.contains(authenticationDomain)) {
			if (authenticationDomains.size() == 1)
				authenticationDomain = String.valueOf(authenticationDomains.get(0));
			else
				throw new IllegalArgumentException(String.valueOf(authenticationDomains));
		}

		Map<String, String> users = new HashMap<String, String>();
		Iterator<?> logins = omiUtil.getMetadataObjectsSubset(store, reposID, MetadataObjects.LOGIN, flags, "")
				.iterator();
		while (logins.hasNext()) {
			Login info = (Login) logins.next();
			if (info.getDomain().getName().equals(authenticationDomain))
				users.put(info.getUserID(), info.getPassword());
		}

		if (users.size() == 1) {
			logicalUser = users.keySet().iterator().next();
			logicalPass = users.get(logicalUser).isBlank() ? password : users.get(logicalUser);
		} else {
			if (logicalUser != null)
				if (users.containsKey(logicalUser))
					logicalPass = users.get(logicalUser).isBlank() ? logicalPass == null ? password : logicalPass
							: users.get(logicalUser);
				else
					throw new IllegalArgumentException(String.valueOf(users));
			else if (users.containsKey(username))
				logicalPass = users.get(username).isBlank() ? logicalPass == null ? password : logicalPass
						: users.get(username);
			else
				throw new IllegalArgumentException(String.valueOf(users));
		}

		cx = cxf.getConnection(logicalUser, logicalPass, authenticationDomain);
		iWorkspace = IWorkspaceHelper.narrow(cx.getObject());
		conn = new MVAConnection(iWorkspace.DataService(), new Properties());
		return !conn.isClosed();
	}
	
	private static String getWorkspaceServer(ServerContext contextServer) throws RemoteException, MdException {
		Iterator<?> dependentComponents = contextServer.getDependentComponents().iterator();
		while (dependentComponents.hasNext()) {
			Object obj = dependentComponents.next();
			if (obj instanceof LogicalServer) {
				LogicalServer info = (LogicalServer) obj;
				if (info.getPublicType().equals("LogicalServer.Workspace"))
					return info.getName();
			}
		}
		return null;
	}

	public static IWorkspace workspace() {
		return iWorkspace;
	}

	public static Connection connection() {
		return conn;
	}

	public static void close() throws SQLException, GenericError, ConnectionFactoryException, RemoteException {
		if (conn != null)
			conn.close();

		if (iWorkspace != null)
			iWorkspace.Close();
		iWorkspace = null;

		if (cx != null)
			cx.close();

		if (cxf != null)
			cxf.getAdminInterface().shutdown();

		if (cx_omr != null)
			cx_omr.close();

		if (cxf_omr != null)
			cxf_omr.getAdminInterface().shutdown();

		if (factory != null)
			factory.closeOMRConnection();
	}
}
