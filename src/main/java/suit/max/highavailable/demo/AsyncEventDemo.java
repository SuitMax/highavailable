package suit.max.highavailable.demo;

import suit.max.highavailable.event.AsyncEvent;

public class AsyncEventDemo implements AsyncEvent {

	private String str;

	public AsyncEventDemo(String str) {
		this.str = str;
	}

	public String getString() {
		return str;
	}

}
