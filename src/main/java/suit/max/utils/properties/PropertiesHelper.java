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

	private Set<BufferedReader> propertiesFiles = null;
	private Class clazz;

	private PropertiesHelper(Class clazz) {
		this.clazz = clazz;
	}

	public static <T> T getProperties(Class<T> clazz) throws CanNotFindAnnotationException {

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
		Set<BufferedReader> properties = new HashSet<>();
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
							properties.add(new BufferedReader(new FileReader(str + "/" + name)));
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
						properties.add(new BufferedReader(new FileReader(tmp + "/" + name)));
						addresses.add(tmp);
						logger.info("Find properties file in {}.", tmp + "/" + name);
					}
				} catch (IOException | NullPointerException e) {
					//logger.info("Can't find properties file in {}.", folder + "/" + name);
				}
			}
		}
		return properties;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String line;
		propertiesFiles = getPropertiesFiles();
		Class returnType = method.getAnnotation(ReturnType.class).value();
		if (propertiesFiles.isEmpty()) {
			throw new IOException("Can not find properties file.");
		}
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
		if (method.isAnnotationPresent(DefaultValue.class)) {
			String value = method.getAnnotation(DefaultValue.class).value();
			logger.info("Loading default value : {}.", value);
			if (returnType.equals(String.class)) {
				return value;
			}
			return returnType.getMethod("valueOf", returnType).invoke(null, value);
		}
		return null;
	}
}
