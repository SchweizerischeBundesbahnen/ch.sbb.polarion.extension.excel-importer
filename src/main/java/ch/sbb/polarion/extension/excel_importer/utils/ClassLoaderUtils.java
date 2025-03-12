package ch.sbb.polarion.extension.excel_importer.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLClassLoader;
import java.util.concurrent.Callable;

public class ClassLoaderUtils {

    public static <T> T runWithClassLoader(@NotNull URLClassLoader newClassLoader, @NotNull Callable<T> task) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(newClassLoader);
        try {
            return task.call();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public static <T> T runWithClassLoader(@NotNull Callable<T> task) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader newClassLoader = ClassLoaderUtils.class.getClassLoader();

        Thread.currentThread().setContextClassLoader(newClassLoader);
        try {
            return task.call();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public static void runWithClassLoader(@NotNull Runnable task) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader newClassLoader = ClassLoaderUtils.class.getClassLoader();

        Thread.currentThread().setContextClassLoader(newClassLoader);
        try {
            task.run();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> loadClass(Class<T> type, ClassLoader classLoader) throws ClassNotFoundException {
        return (Class<? extends T>) classLoader.loadClass(type.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> T createInstance(Class<T> clazz, ClassLoader classLoader) throws Exception {
        Class<?> loadedClass = classLoader.loadClass(clazz.getName());
        Object instance = loadedClass.getDeclaredConstructor().newInstance();
        return (T) instance;
    }
}
