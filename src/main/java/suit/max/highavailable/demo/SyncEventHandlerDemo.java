package suit.max.highavailable.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import suit.max.highavailable.event.SynchronizedEvent;
import suit.max.highavailable.event.SynchronizedEventHandler;

public class SyncEventHandlerDemo implements SynchronizedEventHandler {

	private static final Logger logger = LoggerFactory.getLogger(SyncEventHandlerDemo.class);

	@Override
	public void handleEvent(SynchronizedEvent event) {
		if (event instanceof SyncEventDemo) {
			SyncEventDemo msg = (SyncEventDemo) event;
			logger.info("TEST MESSAGE : {}", msg.getString());
		} else {
			logger.info("MESSAGE NOT SUPPORT.");
		}
	}
}
