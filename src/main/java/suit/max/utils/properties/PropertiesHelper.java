package suit.max.utils.properties;

import suit.max.highavailable.exceptions.CanNotFindAnnotationException;
import suit.max.highavailable.exceptions.IllegalValueException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesHelper implements InvocationHandler, Serializable {

	private static final Logger logger = LoggerFactory.getLogger(PropertiesHelper.class);

	//private Set<BufferedReader> propertiesFiles = null;
	private Properties properties = new Properties();
	private Class clazz;

	private PropertiesHelper(Class clazz) throws IOException, IllegalValueException {
		this.clazz = clazz;
		readProperties(getPropertiesFiles());
	}

	public static <T> T getProperties(Class<T> clazz) throws CanNotFindAnnotationException, IOException, IllegalValueException {

		// check the class has been annotated correctly.
		if (!(clazz.isAnnotationPresent(Location.class))) {
			throw new CanNotFindAnnotationException("Can not find annotation @Location in class: " + clazz.toString());
		}
		for (Method method : clazz.getMethods()) {
			// check the method has been annotated correctly.
			if (!(method.isAnnotationPresent(PropertyName.class) && method.isAnnotationPresent(ReturnType.class))) {
				throw new CanNotFindAnnotationException("Can not find annotation @PropertyName or @ReturnType in method: " + method.toString());
			}
		}
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new PropertiesHelper(clazz));
	}

	private Set<BufferedReader> getPropertiesFiles() {
		Set<BufferedReader> propertiesBufferedReader = new HashSet<>();
		Set<String> addresses = new HashSet<>();
		String location = ((Location) clazz.getAnnotation(Location.class)).value();
		String[] folders;
		String name;
		if (location.contains("}")) {
			String[] tmp = location.replaceAll("\\{", "").split("}\\s*:\\s*");
			folders = tmp[0].split("\\s*;\\s*");
			name = tmp[1];
		} else {
			String[] tmp = location.split("\\s*:\\s*");
			folders = new String[]{tmp[0]};
			name = tmp[1];
		}
		for (String folder : folders) {
			logger.debug("finding {} in {}", folder, name);
			String tmp = null;
			if ("$classpath".equals(folder.toLowerCase())) {
				tmp = System.getProperty("java.class.path");
				//Objects.nonNull(tmp);
				for (String str : tmp.split(";")) {
					try {
						boolean flag = true;
						for (String addr : addresses) {
							logger.debug("addr = {}, tmp = {}", addr, str);
							if (addr.equals(str)) {
								flag = false;
								break;
							}
						}
						if (flag) {
							propertiesBufferedReader.add(new BufferedReader(new FileReader(str + "/" + name)));
							addresses.add(str);
							logger.info("Find properties file in {}.", str + "/" + name);
							break;
						}
					} catch (IOException | NullPointerException e) {
						//logger.info("Can't find properties file in {}.", str + "/" + name);
					}
				}
			} else {
				try {
					if (folder.startsWith("$")) {
						logger.debug("Property {} : {}", folder.replaceAll("\\$", ""), System.getProperty(folder.replaceAll("\\$", "")));
						logger.debug("Property {} : {}", folder.replaceAll("\\$", ""), System.getenv(folder.replaceAll("\\$", "")));
						tmp = System.getProperty(folder.replaceAll("\\$", ""));
						if (tmp == null) {
							tmp = System.getenv(folder.replaceAll("\\$", ""));
						}
					}
					boolean flag = true;
					for (String addr : addresses) {
						logger.debug("addr = {}, tmp = {}", addr, tmp);
						if (addr.equals(tmp)) {
							flag = false;
							break;
						}
					}
					if (flag) {
						//logger.debug("Folder : ", tmp);
						propertiesBufferedReader.add(new BufferedReader(new FileReader(tmp + "/" + name)));
						addresses.add(tmp);
						logger.info("Find properties file in {}.", tmp + "/" + name);
					}
				} catch (IOException | NullPointerException e) {
					//logger.info("Can't find properties file in {}.", folder + "/" + name);
				}
			}
		}
		return propertiesBufferedReader;
	}

	private void readProperties(Set<BufferedReader> propertiesFiles) throws IOException, IllegalValueException {
		String line;
		if (propertiesFiles.isEmpty()) {
			throw new IOException("Can not find properties file.");
		}
		for (BufferedReader propertiesReader : propertiesFiles) {
			while ((line = propertiesReader.readLine()) != null) {
				String[] str = line.split("\\s*=\\s*");
				if (str.length > 2) {
					throw new IllegalValueException("Wrong syntax in property : " + str[0]);
				}
				if (str[1] != null && !"".equals(str[1].replaceAll("\\s", ""))) {
					properties.put(str[0], str[1]);
					logger.info("Load property {} : {}", str[0], str[1]);
				} else {
					properties.put(str[0], null);
				}
			}
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Class returnType = method.getAnnotation(ReturnType.class).value();
		String property = (String) properties.get(method.getAnnotation(PropertyName.class).value());
		if (property != null) {
			if (!method.getReturnType().equals(returnType)) {
				return new IllegalValueException("Need return type " + returnType.getName() + ", but found " + method.getReturnType().getName() + ".");
			}
			if (returnType.equals(String.class)) {
				return property;
			}
			return returnType.getMethod("valueOf", String.class).invoke(null, property);
		}
		if (method.isAnnotationPresent(DefaultValue.class)) {
			String value = method.getAnnotation(DefaultValue.class).value();
			logger.info("Loading default value : {}.", value);
			if (returnType.equals(String.class)) {
				return value;
			}
			return returnType.getMethod("valueOf", returnType).invoke(null, value);
		}
		return null;

		/*
		String line;
		for (BufferedReader properties : propertiesFiles) {
			while ((line = properties.readLine()) != null) {
				String[] str = line.split("\\s*=\\s*");
				if (str[0].equals(method.getAnnotation(PropertyName.class).value())) {
					if (str.length != 2) {
						throw new IllegalValueException("Wrong value in property : " + str[0]);
					}
					if (!method.getReturnType().equals(returnType)) {
						return new IllegalValueException("Need return type " + returnType.getName() + ", but found " + method.getReturnType().getName() + ".");
					}
					if (returnType.equals(String.class)) {
						return str[1];
					}
					return returnType.getMethod("valueOf", String.class).invoke(null, str[1]);
				}
			}
		}
		*/
	}
}
