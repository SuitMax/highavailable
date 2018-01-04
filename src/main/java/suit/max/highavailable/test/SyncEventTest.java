package suit.max.highavailable.test;

import suit.max.highavailable.event.SynchronizedEvent;

public class SyncEventTest implements SynchronizedEvent {

	private String str;

	SyncEventTest(String str) {
		this.str = str;
	}

	public String getString() {
		return str;
	}

}
