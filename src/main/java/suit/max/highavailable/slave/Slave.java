package suit.max.highavailable.slave;

import suit.max.highavailable.event.AsyncEvent;
import suit.max.highavailable.event.SynchronizedEvent;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Slave extends Remote {

	void init() throws Exception;

	void stop() throws RemoteException;

	void handleEvent(AsyncEvent event) throws RemoteException;

	void handleEvent(SynchronizedEvent event) throws RemoteException;

	int getLoad() throws RemoteException;

}
