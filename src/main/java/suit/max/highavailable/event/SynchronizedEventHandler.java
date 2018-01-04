package suit.max.highavailable.event;

public interface SynchronizedEventHandler extends HAEventHandler<SynchronizedEvent> {

    void handleEvent(SynchronizedEvent event);

}
