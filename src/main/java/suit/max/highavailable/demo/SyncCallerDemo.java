package suit.max.highavailable.demo;

import suit.max.highavailable.loadbalancer.EventCaller;
import suit.max.highavailable.loadbalancer.LoadBalancer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SyncCallerDemo implements EventCaller {

	private LoadBalancer loadBalancer;
	private ServerSocket serverSocket;
	private boolean isRunning = false;

	@Override
	public void setLoadBalancer(LoadBalancer loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	@Override
	public void shutdown() {
		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		isRunning = false;
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(8888);
			isRunning = true;
			Socket socket;
			InputStream inputStream;
			BufferedReader bufferedReader;
			while (isRunning) {
				socket = serverSocket.accept();
				inputStream = socket.getInputStream();
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				String temp;
				temp = bufferedReader.readLine();
				if (temp != null) {
					loadBalancer.callEventListener(new SyncEventDemo(temp));
				}
				try {
					bufferedReader.close();
					inputStream.close();
					if (!socket.isClosed()) {
						socket.shutdownOutput();
						socket.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
