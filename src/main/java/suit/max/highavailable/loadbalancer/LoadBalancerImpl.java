package suit.max.highavailable.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import suit.max.highavailable.config.LoadBalancerConfiguration;
import suit.max.highavailable.event.AsyncEvent;
import suit.max.highavailable.event.SynchronizedEvent;
import suit.max.highavailable.event.TransactionalEvent;
import suit.max.highavailable.slave.Slave;

import javax.annotation.Resource;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class LoadBalancerImpl extends UnicastRemoteObject implements LoadBalancer {

	private static final Logger logger = LoggerFactory.getLogger(LoadBalancerImpl.class);
	private static final int WAIT_TIME = 100;

	@Resource
	private LoadBalancerConfiguration config;
	@Resource(name = "callerExecutor")
	private ThreadPoolTaskExecutor callerExecutor;
	@Resource(name = "taskExecutor")
	private ThreadPoolTaskExecutor taskExecutor;
	private LBClassLoader loader;
	private Set<Slave> slaves;
	private Set<EventCaller> eventCallers;
	private Map<Integer, Slave> eventMap;
	private Map<String, Slave> slaveRmiMap;
	private Map<Slave, String> slaveAddresses;
	private Slave lightestSlave = null;
	private boolean stopFlag = false;

	LoadBalancerImpl() throws RemoteException {
		super();
		slaves = new HashSet<>();
		eventCallers = new HashSet<>();
		eventMap = new HashMap<>();
		slaveRmiMap = new HashMap<>();
		slaveAddresses = new HashMap<>();
		loader = new LBClassLoader();
	}

	@Override
	public void startup() throws RemoteException, MalformedURLException, AlreadyBoundException {
		LocateRegistry.createRegistry(config.listenPort());
		Naming.bind("rmi://" + config.listenAddress() + ":" + config.listenPort() + "/suit.max.highavailable.loadbalancer.LoadBalancer", this);
		logger.info("LoadBalancer started.");
		taskExecutor.execute(() -> {
			while (!stopFlag) {
				updateHandler();
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void stop() {
		for (Slave slave : slaves) {
			try {
				slave.stop();
				logger.info("Slave {} unregistered.", slaveAddresses.get(slave));
			} catch (RemoteException e) {
				logger.warn("Slave {} disconnected.", slaveAddresses.get(slave));
			}
		}
		stopFlag = true;
		taskExecutor.shutdown();
		logger.info("LoadBalancer stopped.");
	}

	@Override
	public void registerSlave(String slaveRmi) {
		try {
			Slave slave = (Slave) Naming.lookup(slaveRmi);
			slaveRmiMap.put(slaveRmi, slave);
			slaveAddresses.put(slave, slaveRmi.replace("rmi://", "").replace("/suit.max.highavailable.loadbalancer.LoadBalancer", ""));
			slaves.add(slave);
			logger.info("Slave registered.");
		} catch (NotBoundException | RemoteException | MalformedURLException e) {
			logger.warn("Exception when register slave {}, {}", slaveRmi, e.toString());
		}
	}

	@Override
	public void unregisterSlave(String slaveRmi) {
		Slave slave = slaveRmiMap.get(slaveRmi);
		slaveAddresses.remove(slave);
		for (Integer key : eventMap.keySet()) {
			if (eventMap.get(key).equals(slave)) {
				eventMap.remove(key);
			}
		}
		lightestSlave = null;
		slaves.remove(slave);
		slaveRmiMap.remove(slaveRmi);
		if (slaves.isEmpty()) {
			logger.warn("All the slave disconnected, please run a slave to continue.");
			unregisterAllCaller();
		}
	}

	private void removeSlave(Slave slave) {
		try {
			slave.stop();
			logger.info("Slave {} unregistered.", slaveAddresses.get(slave));
		} catch (RemoteException e) {
			logger.warn("Slave {} disconnected.", slaveAddresses.get(slave));
		}
		slaveRmiMap.remove("rmi://" + slaveAddresses.get(slave) + "/suit.max.highavailable.loadbalancer.LoadBalancer");
		slaveAddresses.remove(slave);
		for (Integer key : eventMap.keySet()) {
			if (eventMap.get(key).equals(slave)) {
				eventMap.remove(key);
			}
		}
		lightestSlave = null;
		slaves.remove(slave);
		if (slaves.isEmpty()) {
			logger.warn("All the slave has been disconnected, please run a slave to continue.");
			unregisterAllCaller();
		}
	}

	@Override
	public synchronized void registerCaller(byte[] b) {
		try {
			Class<EventCaller> c = (Class<EventCaller>) loader.loadClass(b);
			EventCaller caller = c.getConstructor().newInstance();
			caller.setLoadBalancer(this);
			eventCallers.add(caller);
			callerExecutor.execute(caller);
			logger.info("Caller {} registered.", caller.getClass().getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unregisterAllCaller() {
		for (EventCaller caller : eventCallers) {
			caller.shutdown();
		}
		int times = 0;
		while (callerExecutor.getActiveCount() != 0 && times < 5) {
			++times;
			try {
				Thread.sleep(WAIT_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		loader = new LBClassLoader();
		eventCallers.clear();
		logger.info("All callers unregistered.");
	}

	@Override
	public synchronized void callEventListener(AsyncEvent event) {
		waitForSlave();
		logger.debug("Calling slave for message {}.", event);
		boolean flag = true;
		if (event instanceof TransactionalEvent) {
			while (flag) {
				TransactionalEvent transactionalEvent = (TransactionalEvent) event;
				if (eventMap.get(transactionalEvent.getId()) != null) {
					try {
						eventMap.get(transactionalEvent.getId()).handleEvent(event);
						flag = false;
					} catch (RemoteException e) {
						Slave timeoutSlave = eventMap.get(transactionalEvent.getId());
						removeSlave(timeoutSlave);
						logger.warn("Slave {} timeout, redirect to other slave.", slaveAddresses.get(timeoutSlave));
						if (slaves.isEmpty()) {
							return;
						}
					}
				} else {
					Slave slave;
					while ((slave = lightestSlave) == null) {
						updateHandler();
					}
					eventMap.put(transactionalEvent.getId(), slave);
					try {
						slave.handleEvent(event);
						flag = false;
					} catch (RemoteException e) {
						Slave timeoutSlave = eventMap.get(transactionalEvent.getId());
						removeSlave(timeoutSlave);
						logger.warn("Slave {} timeout, redirect to other slave.", slaveAddresses.get(timeoutSlave));
						if (slaves.isEmpty()) {
							return;
						}
					}
				}
			}
		} else {
			while (flag) {
				Slave slave;
				while ((slave = lightestSlave) == null) {
					updateHandler();
				}
				try {
					slave.handleEvent(event);
					flag = false;
				} catch (RemoteException e) {
					removeSlave(slave);
					logger.warn("Slave {} timeout, redirect to other slave.", slaveAddresses.get(slave));
					if (slaves.isEmpty()) {
						return;
					}
				}

			}
		}
	}

	@Override
	public synchronized void callEventListener(SynchronizedEvent event) {
		waitForSlave();
		Iterator<Slave> it = slaves.iterator();
		while (it.hasNext()) {
			Slave slave = it.next();
			logger.debug("Calling slave {} for message {}.", slave, event);
			try {
				slave.handleEvent(event);
			} catch (RemoteException e) {
				removeSlave(slave);
				logger.warn("Slave {} timeout, redirect to other slave.", slaveAddresses.get(slave));
			}
		}
	}

	private void waitForSlave() {
		while (slaves.isEmpty()) {
			sleep();
		}
	}

	private void sleep() {
		while (slaves.isEmpty()) {
			try {
				Thread.sleep(WAIT_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private synchronized void updateHandler() {
		if (slaves.isEmpty()) {
			logger.warn("There is no slave available, please register slaves to start service.");
			sleep();
			return;
		}
		int tmp = Integer.MAX_VALUE;
		for (Slave slave : new HashSet<>(slaves)) {
			if (!slaves.contains(slave)) {
				break;
			}
			try {
				if (tmp > slave.getLoad()) {
					lightestSlave = slave;
				}
			} catch (RemoteException e) {
				logger.warn("Slave {} timeout, remove.", slaveAddresses.get(slave));
				removeSlave(slave);
				lightestSlave = null;
				tmp = Integer.MAX_VALUE;
			}
		}
	}

}
