package suit.max.highavailable.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import suit.max.highavailable.config.LoadBalancerConfiguration;
import suit.max.highavailable.exceptions.CanNotFindAnnotationException;
import suit.max.utils.properties.PropertiesHelper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;

@Configuration
public class LoadBalancerAppConfig {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerAppConfig.class);

    @Resource
    private LoadBalancer loadBalancer;

    @Bean
    public LoadBalancer getLoadBalancer() throws RemoteException {
        return new LoadBalancerImpl();
    }

    @Bean
    public LoadBalancerConfiguration getLoadBalancerConfiguration() throws CanNotFindAnnotationException {
        return PropertiesHelper.getProperties(LoadBalancerConfiguration.class);
    }

    @PostConstruct
    public void start() throws AlreadyBoundException, RemoteException, MalformedURLException {
        logger.info("Starting LoadBalancer.");
        loadBalancer.startup();
        logger.info("LoadBalancer Started.");
    }

    @PreDestroy
    public void stop() throws RemoteException {
        logger.info("Stopping LoadBalancer.");
        loadBalancer.stop();
        logger.info("LoadBalancer Stopped.");
    }

    @Bean(name = "callerExecutor")
    public ThreadPoolTaskExecutor getCallerExecutor() {
        return getThreadPoolTaskExecutor();
    }

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor getTaskExecutor() {
        return getThreadPoolTaskExecutor();
    }

    private ThreadPoolTaskExecutor getThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setKeepAliveSeconds(300);
        executor.setQueueCapacity(20);
        return executor;
    }

}
