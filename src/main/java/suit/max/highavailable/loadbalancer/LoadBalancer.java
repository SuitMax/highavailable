package suit.max.highavailable.loadbalancer;

import suit.max.highavailable.event.AsyncEvent;
import suit.max.highavailable.event.SynchronizedEvent;
import suit.max.highavailable.slave.Slave;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface LoadBalancer extends Remote {

	void startup() throws RemoteException, MalformedURLException, AlreadyBoundException;

	void stop() throws RemoteException;

	void registerSlave(String slaveRmi) throws RemoteException, NotBoundException, MalformedURLException;

	void unregisterSlave(String slaveRmi) throws RemoteException;

	void registerCaller(byte[] b) throws RemoteException;

	void unregisterAllCaller() throws RemoteException;

	void callEventListener(AsyncEvent event) throws RemoteException;

	void callEventListener(SynchronizedEvent event) throws RemoteException;

}
