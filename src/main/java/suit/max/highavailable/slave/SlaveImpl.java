package suit.max.highavailable.slave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.asm.ClassReader;
import suit.max.highavailable.config.SlaveConfiguration;
import suit.max.highavailable.event.*;
import suit.max.highavailable.loadbalancer.EventCaller;
import suit.max.highavailable.loadbalancer.LoadBalancer;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SlaveImpl extends UnicastRemoteObject implements Slave {

	private static final Logger logger = LoggerFactory.getLogger(Slave.class);

	@Resource
	private SlaveConfiguration config;
	private LoadBalancer loadBalancer;
	private List<AsyncEventHandler> asyncEventHandlers;
	private List<SynchronizedEventHandler> synchronizedEventHandlers;
	private List<EventCaller> callers;
	private String rmiAddr;
	private int load;

	SlaveImpl() throws RemoteException {
		super();
		asyncEventHandlers = new ArrayList<>();
		synchronizedEventHandlers = new ArrayList<>();
		callers = new ArrayList<>();
		load = 0;
	}

	private void addHandler(String handlers) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		for (String clazz : handlers.split("[\\s]*;[\\s]*")) {
			HAEventHandler handler = (HAEventHandler) Class.forName(clazz).getConstructor().newInstance();
			if (handler instanceof AsyncEventHandler) {
				asyncEventHandlers.add((AsyncEventHandler) handler);
			} else {
				synchronizedEventHandlers.add((SynchronizedEventHandler) handler);
			}
		}
	}

	private void addCaller(String classes) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		for (String clazz : classes.split("[\\s]*;[\\s]*")) {
			EventCaller caller = (EventCaller) Class.forName(clazz).getConstructor().newInstance();
			callers.add(caller);
		}
	}

	private void registerCallers() {
		for (EventCaller caller : callers) {
			try {
				loadBalancer.registerCaller(new ClassReader(caller.getClass().getName()).b);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void reRegisterCallers() throws RemoteException {
		loadBalancer.unregisterAllCaller();
		registerCallers();
	}

	void unbind() throws RemoteException {
		loadBalancer.unregisterSlave(rmiAddr);
	}

	@Override
	public void init() throws Exception {
		LocateRegistry.createRegistry(config.loadBalancerPort());
		rmiAddr = "rmi://0.0.0.0:" + String.valueOf(config.loadBalancerPort()) + "/suit.max.highavailable.slave.Slave";
		Naming.bind(rmiAddr, this);
		loadBalancer = (LoadBalancer) Naming.lookup("rmi://" + config.loadBalancerAddress() + ":" + String.valueOf(config.loadBalancerPort()) + "/suit.max.highavailable.loadbalancer.LoadBalancer");
		logger.debug("binding slave.");
		loadBalancer.registerSlave("rmi://" + getIP() + ":" + String.valueOf(config.loadBalancerPort()) + "/suit.max.highavailable.slave.Slave");
		addCaller(config.callerClass());
		reRegisterCallers();
		addHandler(config.handlerClass());
	}

	@Override
	public void stop() {
		try {
			unbind();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handleEvent(AsyncEvent event) {
		logger.info("Calling event handlers.");
		for (AsyncEventHandler handler : asyncEventHandlers) {
			++load;
			handler.handleEvent(event);
			--load;
		}
	}

	@Override
	public void handleEvent(SynchronizedEvent event) {
		logger.info("Calling event handlers.");
		for (SynchronizedEventHandler handler : synchronizedEventHandlers) {
			++load;
			handler.handleEvent(event);
			--load;
		}
	}

	@Override
	public int getLoad() {
		return load;
	}

	private String getIP() {
		Enumeration allNetInterfaces = null;
		try {
			allNetInterfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		InetAddress ip = null;
		while (allNetInterfaces.hasMoreElements())
		{
			NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
			System.out.println(netInterface.getName());
			Enumeration addresses = netInterface.getInetAddresses();
			while (addresses.hasMoreElements())
			{
				ip = (InetAddress) addresses.nextElement();
				if (ip != null && ip instanceof Inet4Address)
				{
					return ip.getHostAddress();
				}
			}
		}
		return ip.getHostAddress();
	}

}
