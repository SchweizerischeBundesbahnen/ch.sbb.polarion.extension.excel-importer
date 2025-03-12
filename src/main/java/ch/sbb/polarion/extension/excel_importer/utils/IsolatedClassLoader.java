package ch.sbb.polarion.extension.excel_importer.utils;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class IsolatedClassLoader extends URLClassLoader {

    public IsolatedClassLoader(Bundle bundle) throws IOException {
        super(getClassPathURLs(bundle), null);
    }

    private static URL[] getClassPathURLs(Bundle bundle) throws IOException {
        List<URL> urls = new ArrayList<>();

        URL manifestUrl = bundle.getEntry("/META-INF/MANIFEST.MF");
        if (manifestUrl == null) {
            throw new IOException("MANIFEST.MF not found in bundle: " + bundle.getSymbolicName());
        }

        try (InputStream is = manifestUrl.openStream()) {
            Manifest manifest = new Manifest(is);
            Attributes attrs = manifest.getMainAttributes();

            String bundleClassPath = attrs.getValue("Bundle-ClassPath");
            if (bundleClassPath == null || bundleClassPath.isEmpty()) {
                throw new IOException("Bundle-ClassPath not specified in MANIFEST.MF");
            }

            String[] paths = bundleClassPath.split(",");
            for (String path : paths) {
                String trimmedPath = path.trim();

                if (trimmedPath.equals(".")) {
                    URL rootEntry = bundle.getEntry("/");
                    urls.add(rootEntry);
                } else {
                    URL jarEntry = bundle.getEntry(trimmedPath);
                    if (jarEntry == null) {
                        throw new IOException("Element not found in bundle: " + trimmedPath);
                    }

                    URL resolvedUrl = bundle.getResource(trimmedPath);
                    urls.add(resolvedUrl);
                }
            }
        }

        return urls.toArray(new URL[0]);
    }

    public static IsolatedClassLoader createForCurrentBundle() throws IOException {
        Bundle bundle = FrameworkUtil.getBundle(IsolatedClassLoader.class);
        if (bundle == null) {
            throw new IllegalStateException("Bundle not found for " + IsolatedClassLoader.class.getName());
        }

        return new IsolatedClassLoader(bundle);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("java.") || name.startsWith("javax.")) {
            return super.loadClass(name, resolve);
        }

        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null) {
                try {
                    clazz = findClass(name);
                } catch (ClassNotFoundException e) {
                    throw new ClassNotFoundException("Class not found in IsolatedClassLoader : " + name, e);
                }
            }

            if (resolve) {
                resolveClass(clazz);
            }

            return clazz;
        }
    }
}
