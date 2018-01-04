package suit.max.highavailable.loadbalancer;

import java.io.Serializable;

public interface EventCaller extends Serializable, Runnable {

    void setLoadBalancer(LoadBalancer loadBalancer);

    void shutdown();

}
