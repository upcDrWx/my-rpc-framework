package github.wx.extension;

import github.wx.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLOutput;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * refer to dubbo spi:
 * https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/reference-manual/spi/description/dubbo-spi/
 *
 * @author wx
 * @date 2023/10/4 21:13
 */
@Slf4j
public final class ExtensionLoader<T> {

    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    private final Class<?> type;

    // Map<扩展点名称，扩展点目标对象>
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    // 所有加载的扩展类
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type must be an interface.");
        }
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }

        // 首先从缓存中获取，如果没有命中，则创建一个
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if (extensionLoader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<S>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }

    public T getExtension(String name) {
        if (StringUtil.isBlank(name)) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }

        // 首先从缓存中获取，如果没有命中，则创建一个
        Holder<Object> holder = cachedInstances.get(name);
        // 这段逻辑保证了只有一个线程能够创建 Holder 对象
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }

        //如果没有实例，创建一个单例
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }

        return (T) instance;
    }

    private T createExtension(String name) {
        // 从配置文件中加载所有的扩展类，并按名称获取特定的扩展类
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new RuntimeException("No such extension of name " + name);
        }
        T instance = (T) EXTENSION_INSTANCES.get(clazz);

        // 这段代码保证了扩展类只会被构造一次，也就是单例的.
        if (instance == null) {
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return instance;
    }

    // 获取扩展类，没有则加载
    private Map<String, Class<?>> getExtensionClasses() {
        // 从缓存中获取加载的扩展类
        Map<String, Class<?>> classes = cachedClasses.get();
        // double check
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    // 从扩展目录加载所有扩展类
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    // 加载指定文件下所有的扩展类
    private void loadDirectory(Map<String, Class<?>> extensionClasses) {

        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        try {
            Enumeration<URL> urls;
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            urls = classLoader.getResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }

    // 加载一个具体的扩展类
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl) {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {

                final int ci = line.indexOf("#");
                if (ci >= 0) {
                    // # 后面是注释，忽略它
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        final int ei = line.indexOf('=');
                        String name = line.substring(0, ei).trim();
                        String clazzName = line.substring(ei + 1).trim();
                        // SPI使用键值对，不能为空
                        if (name.length() > 0 && clazzName.length() > 0) {
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


}
