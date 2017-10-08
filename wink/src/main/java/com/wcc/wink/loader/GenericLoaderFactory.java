package com.wcc.wink.loader;

import android.content.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wenbiao.xie on 2016/6/14.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenericLoaderFactory {

    private final Map<Class/*T*/, Map<Class/*Y*/, ResourceLoaderFactory/*T, Y*/>> modelClassToResourceFactories =
            new HashMap<>();

    private final Map<Class/*T*/, Map<Class/*Y*/, ResourceLoader/*T, Y*/>> cachedResourceLoaders =
            new HashMap<>();

    private static final ResourceLoader NULL_RESOURCE_LOADER = new ResourceLoader() {
        @Override
        public Object createResource(Object model) {
            throw new NoSuchMethodError("This should never be called!");
        }

        @Override
        public UrlFetcher getUrlFetcher(Object model) {
            throw new NoSuchMethodError("This should never be called!");
        }

        @Override
        public File getTargetFile(Object model, Object resource) {
            throw new NoSuchMethodError("This should never be called!");
        }

        @Override
        public String toString() {
            return "NULL_RESOURCE_LOADER";
        }
    };

    private final Context context;

    public GenericLoaderFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Registers the given {@link ResourceLoaderFactory} for the given model and resource classes and returns the previous
     * factory registered for the given model and resource classes or null if no such factory existed. Clears all cached
     * model loaders.
     *
     * @param modelClass The model class.
     * @param resourceClass The resource class.
     * @param factory The factory to register.
     * @param <T> The type of the model.
     * @param <Y> The type of the resource.
     */
    public synchronized <T, Y> ResourceLoaderFactory<T, Y> register(Class<T> modelClass, Class<Y> resourceClass,
                                                                    ResourceLoaderFactory<T, Y> factory) {
        cachedResourceLoaders.clear();

        Map<Class/*Y*/, ResourceLoaderFactory/*T, Y*/> resourceToFactories = modelClassToResourceFactories.get(modelClass);
        if (resourceToFactories == null) {
            resourceToFactories = new HashMap<Class/*Y*/, ResourceLoaderFactory/*T, Y*/>();
            modelClassToResourceFactories.put(modelClass, resourceToFactories);
        }

        ResourceLoaderFactory/*T, Y*/ previous = resourceToFactories.put(resourceClass, factory);

        if (previous != null) {
            // This factory may be being used by another model. We don't want to say it has been removed unless we
            // know it has been removed for all models.
            for (Map<Class/*Y*/, ResourceLoaderFactory/*T, Y*/> factories : modelClassToResourceFactories.values()) {
                if (factories.containsValue(previous)) {
                    previous = null;
                    break;
                }
            }
        }

        return previous;
    }

    public <T, Y> boolean supportResourceLoader(Class<T> modelClass, Class<Y> resourceClass) {
        Map<Class/*Y*/, ResourceLoaderFactory/*T, Y*/> resourceToFactories = modelClassToResourceFactories.get(modelClass);
        /*if (resourceToFactories == null) {
            Set<Class> keys = modelClassToResourceFactories.keySet();
            if (keys.isEmpty())
                return false;

            for (Class c: keys) {
                if (c.isAssignableFrom(modelClass)) {
                    resourceToFactories = modelClassToResourceFactories.get(c);
                    break;
                }
            }
        }*/

        return resourceToFactories != null && resourceToFactories.containsKey(resourceClass);
    }

    /**
     * Removes and returns the registered {@link ResourceLoaderFactory} for the given model and resource classes. Returns
     * null if no such factory is registered. Clears all cached model loaders.
     *
     * @param modelClass The model class.
     * @param resourceClass The resource class.
     * @param <T> The type of the model the class.
     * @param <Y> The type of the resource class.
     */
    public synchronized <T, Y> ResourceLoaderFactory<T, Y> unregister(Class<T> modelClass, Class<Y> resourceClass) {
        cachedResourceLoaders.clear();

        ResourceLoaderFactory/*T, Y*/ result = null;
        Map<Class/*Y*/, ResourceLoaderFactory/*T, Y*/> resourceToFactories = modelClassToResourceFactories.get(modelClass);
        if (resourceToFactories != null) {
            result = resourceToFactories.remove(resourceClass);
        }
        return result;
    }

    /**
     * Returns a {@link ResourceLoader} for the given model and resource classes by either returning a cached
     * {@link ResourceLoader} or building a new a new {@link ResourceLoader} using registered {@link ResourceLoaderFactory}s.
     * Returns null if no {@link ResourceLoaderFactory} is registered for the given classes.
     *
     * @param modelClass The model class.
     * @param resourceClass The resource class.
     * @param <T> The type of the model.
     * @param <Y> The type of the resource.
     */

    public synchronized <T, Y> ResourceLoader<T, Y> buildResourceLoader(Class<T> modelClass, Class<Y> resourceClass) {
        ResourceLoader<T, Y> result = getCachedLoader(modelClass, resourceClass);
        if (result != null) {
            // We've already tried to create a model loader and can't with the currently registered set of factories,
            // but we can't use null to demonstrate that failure because model loaders that haven't been requested
            // yet will be null in the cache. To avoid this, we use a special signal model loader.
            if (NULL_RESOURCE_LOADER.equals(result)) {
                return null;
            } else {
                return result;
            }
        }

        final ResourceLoaderFactory<T, Y> factory = getFactory(modelClass, resourceClass);
        if (factory != null) {
            result = factory.build(context, this);
            cacheModelLoader(modelClass, resourceClass, result);
        } else {
            // We can't generate a model loader for the given arguments with the currently registered set of factories.
            cacheNullLoader(modelClass, resourceClass);
        }
        return result;
    }

    private <T, Y> void cacheNullLoader(Class<T> modelClass, Class<Y> resourceClass) {
        cacheModelLoader(modelClass, resourceClass, NULL_RESOURCE_LOADER);
    }

    private <T, Y> void cacheModelLoader(Class<T> modelClass, Class<Y> resourceClass, ResourceLoader<T, Y> modelLoader) {
        Map<Class/*Y*/, ResourceLoader/*T, Y*/> resourceToLoaders = cachedResourceLoaders.get(modelClass);
        if (resourceToLoaders == null) {
            resourceToLoaders = new HashMap<Class/*Y*/, ResourceLoader/*T, Y*/>();
            cachedResourceLoaders.put(modelClass, resourceToLoaders);
        }
        resourceToLoaders.put(resourceClass, modelLoader);
    }

    private <T, Y> ResourceLoader<T, Y> getCachedLoader(Class<T> modelClass, Class<Y> resourceClass) {
        Map<Class/*Y*/, ResourceLoader/*T, Y*/> resourceToLoaders = cachedResourceLoaders.get(modelClass);
        ResourceLoader/*T, Y*/ result = null;
        if (resourceToLoaders != null) {
            result = resourceToLoaders.get(resourceClass);
        }

        return result;
    }

    private <T, Y> ResourceLoaderFactory<T, Y> getFactory(Class<T> modelClass, Class<Y> resourceClass) {
        Map<Class/*Y*/, ResourceLoaderFactory/*T, Y*/> resourceToFactories = modelClassToResourceFactories.get(modelClass);
        ResourceLoaderFactory/*T, Y*/ result = null;
        if (resourceToFactories != null) {
            result = resourceToFactories.get(resourceClass);
        }

        if (result == null) {
            for (Class<? super T> registeredModelClass : modelClassToResourceFactories.keySet()) {
                // This accounts for model subclasses, our map only works for exact matches. We should however still
                // match a subclass of a model with a factory for a super class of that model if if there isn't a
                // factory for that particular subclass. Uris are a great example of when this happens, most uris
                // are actually subclasses for Uri, but we'd generally rather load them all with the same factory rather
                // than trying to register for each subclass individually.
                if (registeredModelClass.isAssignableFrom(modelClass)) {
                    Map<Class/*Y*/, ResourceLoaderFactory/*T, Y*/> currentResourceToFactories =
                            modelClassToResourceFactories.get(registeredModelClass);
                    if (currentResourceToFactories != null) {
                        result = currentResourceToFactories.get(resourceClass);
                        if (result != null) {
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }
}
