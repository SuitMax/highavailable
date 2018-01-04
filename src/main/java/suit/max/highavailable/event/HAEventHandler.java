package suit.max.highavailable.event;

import java.io.Serializable;

public interface HAEventHandler<T extends Serializable> {

    void handleEvent(T event);

}
