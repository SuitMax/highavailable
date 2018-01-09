package suit.max.highavailable.demo;

import suit.max.highavailable.event.SynchronizedEvent;

public class SyncEventDemo implements SynchronizedEvent {

	private String str;

	public SyncEventDemo(String str) {
		this.str = str;
	}

	public String getString() {
		return str;
	}

}
