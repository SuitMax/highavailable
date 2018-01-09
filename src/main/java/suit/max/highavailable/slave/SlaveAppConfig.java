package suit.max.highavailable.slave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import suit.max.highavailable.config.SlaveConfiguration;
import suit.max.highavailable.exceptions.CanNotFindAnnotationException;
import suit.max.highavailable.exceptions.IllegalValueException;
import suit.max.utils.properties.PropertiesHelper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.rmi.RemoteException;

@Configuration
public class SlaveAppConfig {

	private static final Logger logger = LoggerFactory.getLogger(SlaveAppConfig.class);

	@Resource
	private Slave slave;

	@Bean
	public Slave getSlave() throws RemoteException {
		return new SlaveImpl();
	}

	@Bean
	public SlaveConfiguration getSlaveConfiguration() throws CanNotFindAnnotationException, IOException, IllegalValueException {
		return PropertiesHelper.getProperties(SlaveConfiguration.class);
	}

	@PostConstruct
	public void init() throws Exception {
		slave.init();
		logger.info("Slave started");
	}

	@PreDestroy
	public void stop() throws RemoteException {
		slave.stop();
		logger.info("Slave stopped.");
	}

}
