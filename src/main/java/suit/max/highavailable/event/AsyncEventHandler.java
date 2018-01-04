package suit.max.highavailable.event;

public interface AsyncEventHandler extends HAEventHandler<AsyncEvent> {

    void handleEvent(AsyncEvent event);

}
