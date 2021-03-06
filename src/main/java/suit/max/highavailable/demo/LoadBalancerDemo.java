package suit.max.highavailable.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import suit.max.highavailable.loadbalancer.LoadBalancerAppConfig;

public class LoadBalancerDemo {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerDemo.class);

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(LoadBalancerAppConfig.class);
    }

}
