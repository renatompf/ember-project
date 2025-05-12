package io.github.renatompf.ember.core.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * ClassScanner is a utility class that scans the classpath for classes annotated with a specific annotation.
 * It can be used to find classes that are marked with custom annotations, such as @Component or @Service.
 */
public class ClassScanner {
    private static final Logger logger = LoggerFactory.getLogger(ClassScanner.class);

    /**
     * Scans the classpath for classes annotated with the specified annotation.
     *
     * @param basePackage   The base package to scan.
     * @param annotation    The annotation to look for.
     * @return A list of classes annotated with the specified annotation.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If a class cannot be loaded.
     */
    public List<Class<?>> findAnnotatedClasses(String basePackage, Class<? extends Annotation> annotation)
            throws IOException, ClassNotFoundException {
        List<Class<?>> annotatedClasses = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                File directory = new File(resource.getFile());
                if (directory.exists() && directory.isDirectory()) {
                    annotatedClasses.addAll(findClassesInDirectory(directory, basePackage, annotation));
                }
            } else if (resource.getProtocol().equals("jar")) {
                String jarFilePath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                try (var jarFile = new java.util.jar.JarFile(jarFilePath)) {
                    var entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        if (entry.getName().startsWith(path) && entry.getName().endsWith(".class")) {
                            String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                            try {
                                var clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                                if (clazz.isAnnotationPresent(annotation) && !clazz.isLocalClass() && !clazz.isAnonymousClass()) {
                                    logger.debug("Found class with annotation {}: {}", annotation.getSimpleName(), clazz.getName());
                                    annotatedClasses.add(clazz);
                                }
                            } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
                                logger.error("Failed to load class: {}", className);
                            }
                        }
                    }
                }
            }
        }

        return annotatedClasses;
    }

    /**
     * Recursively scans a directory for classes annotated with the specified annotation.
     *
     * @param directory     The directory to scan.
     * @param packageName   The package name corresponding to the directory.
     * @param annotation    The annotation to look for.
     * @return A list of classes annotated with the specified annotation.
     * @throws ClassNotFoundException If a class cannot be loaded.
     */
    private List<Class<?>> findClassesInDirectory(File directory, String packageName, Class<? extends Annotation> annotation)
            throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classes.addAll(findClassesInDirectory(
                            file,
                            packageName + "." + file.getName(),
                            annotation));
                } else if (file.getName().endsWith(".class")) {
                    String simpleClassName = file.getName().substring(0, file.getName().length() - 6);
                    String className = (packageName.isEmpty() ? simpleClassName : packageName + "." + simpleClassName);
                    if (className.startsWith(".")) {
                        className = className.substring(1);
                    }


                    try {
                        var clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                        if (clazz.isAnnotationPresent(annotation) && !clazz.isLocalClass() && !clazz.isAnonymousClass()) {
                            logger.debug("Found class with annotation {}: {}", annotation.getSimpleName(), clazz.getName());
                            classes.add(clazz);
                        }
                    } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
                        logger.error("Failed to load class: {}", className);
                    }
                }
            }
        }

        return classes;
    }
}