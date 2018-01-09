package suit.max.highavailable.demo;

import suit.max.highavailable.loadbalancer.EventCaller;
import suit.max.highavailable.loadbalancer.LoadBalancer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class CallerDemo implements EventCaller {

	private LoadBalancer loadBalancer;
	private ServerSocket serverSocket;
	private Socket socket;
	private InputStream inputStream;
	private BufferedReader bufferedReader;

	@Override
	public void setLoadBalancer(LoadBalancer loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	@Override
	public void shutdown() {
		try {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
			if (socket != null) {
				socket.shutdownOutput();
				socket.close();
			}
			if (serverSocket != null) {
				serverSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(8888);
			socket = serverSocket.accept();
			inputStream = socket.getInputStream();
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			String temp;
			while (true) {
				temp = bufferedReader.readLine();
				if (temp != null) {
					loadBalancer.callEventListener(new SyncEventDemo(temp));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
