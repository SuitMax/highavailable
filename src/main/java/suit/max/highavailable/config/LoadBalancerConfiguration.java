package suit.max.highavailable.config;

import suit.max.utils.properties.DefaultValue;
import suit.max.utils.properties.Location;
import suit.max.utils.properties.PropertyName;
import suit.max.utils.properties.ReturnType;

@Location("{$LB_CONFIG_DIR;$classpath}:config.txt")
public interface LoadBalancerConfiguration {

    @PropertyName("listenAddress")
    @ReturnType(String.class)
    String listenAddress();

    @PropertyName("listenPort")
    @ReturnType(Integer.class)
    @DefaultValue("6600")
    Integer listenPort();

}
