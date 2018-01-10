package suit.max.highavailable.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class CallerClientDemo {

	public static void main(String[] args) throws IOException {
		Socket socket = new Socket(args[0], Integer.valueOf(args[1]));
		PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
		printWriter.print("Hi Server!");
		printWriter.flush();
		printWriter.close();
		socket.close();
	}

}
