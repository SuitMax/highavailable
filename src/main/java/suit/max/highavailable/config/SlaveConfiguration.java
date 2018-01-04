package suit.max.highavailable.config;

import suit.max.utils.properties.DefaultValue;
import suit.max.utils.properties.Location;
import suit.max.utils.properties.PropertyName;
import suit.max.utils.properties.ReturnType;

@Location("{$LB_CONFIG_DIR;$classpath}:config.txt")
public interface SlaveConfiguration {

	@PropertyName("loadBalancerAddress")
	@ReturnType(String.class)
	String loadBalancerAddress();

	@PropertyName("loadBalancerPort")
	@ReturnType(Integer.class)
	@DefaultValue("6600")
	Integer loadBalancerPort();

	@PropertyName("callerClass")
	@ReturnType(String.class)
	String callerClass();

	@PropertyName("handlerClass")
	@ReturnType(String.class)
	String handlerClass();

}
