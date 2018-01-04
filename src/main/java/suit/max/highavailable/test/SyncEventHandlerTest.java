package suit.max.highavailable.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import suit.max.highavailable.event.SynchronizedEvent;
import suit.max.highavailable.event.SynchronizedEventHandler;

public class SyncEventHandlerTest implements SynchronizedEventHandler {

	private static final Logger logger = LoggerFactory.getLogger(SyncEventHandlerTest.class);

	@Override
	public void handleEvent(SynchronizedEvent event) {
		if (event instanceof SyncEventTest) {
			SyncEventTest msg = (SyncEventTest) event;
			logger.info("TEST MESSAGE : {}", msg.getString());
		} else {
			logger.info("MESSAGE NOT SUPPORT.");
		}
	}
}
