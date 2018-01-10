package suit.max.highavailable.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.asm.ClassReader;

public class LBClassLoader extends ClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(LBClassLoader.class);

    public Class<?> loadClass(byte[] b) throws Exception {
        ClassReader reader = new ClassReader(b);
        Class<?> clazz;
        boolean flag = true;
        //logger.debug("EventCaller class name {}", EventCaller.class.getName().replaceAll("\\.","/"));
        logger.debug("input class name {}", reader.getClassName());
        for (String intf : reader.getInterfaces()) {
            logger.debug("input class interface name {}", intf);
            if (intf.equals(EventCaller.class.getName().replaceAll("\\.","/"))) {
                flag = false;
                break;
            }
        }
        if (flag) {
            throw new Exception("this class does not implement EventCaller interface.");
        }
        /* check if this class has been loaded, it will throw an exception if not. */
        //clazz = findLoadedClass(reader.getClassName());
        /* this class has not been loaded if clazz == null. **/

        //if (clazz == null) {
            clazz = findClass(reader);
        //}

        resolveClass(clazz);
        return clazz;
    }

    private Class<?> findClass(ClassReader reader) {
        return defineClass(reader.getClassName().replaceAll("/", "."), reader.b, 0, reader.b.length);
    }



}
