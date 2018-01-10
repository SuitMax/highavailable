package suit.max.highavailable.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import suit.max.highavailable.event.AsyncEvent;
import suit.max.highavailable.event.AsyncEventHandler;

public class AsyncEventHandlerDemo implements AsyncEventHandler {

	private static final Logger logger = LoggerFactory.getLogger(AsyncEventHandlerDemo.class);

	@Override
	public void handleEvent(AsyncEvent event) {
		if (event instanceof AsyncEventDemo) {
			AsyncEventDemo msg = (AsyncEventDemo) event;
			logger.info("TEST MESSAGE : {}", msg.getString());
		} else {
			logger.info("MESSAGE NOT SUPPORT.");
		}
	}
}
