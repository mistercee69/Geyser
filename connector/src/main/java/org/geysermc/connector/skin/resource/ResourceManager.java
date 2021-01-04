package org.geysermc.connector.skin.resource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserLogger;
import org.geysermc.connector.skin.resource.types.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResourceManager {
    private static final Map<Class<?>, LinkedHashMap<Pattern, ResourceLoader<?, ?>>> loaders = new ConcurrentHashMap<>();
    private static final Map<ResourceDescriptor<?, ?>, CompletableFuture<ResourceLoadResult>> requestedResources = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Cache<URI, ResourceContainer>> resources = new ConcurrentHashMap<>();
    private static final Map<Class<? extends ResourceLoader<?, ?>>, ResourceLoader<?, ?>> loaderInstances = new ConcurrentHashMap<>();
    private static final Cache<ResourceDescriptor<?, ?>, ReentrantLock> resourceLoadLockCache = CacheBuilder.newBuilder().softValues().build();

    static {

        // LinkedHashMap sorts entries by insertion order (used for pattern matching precedence)

        // skins
        for (SkinType skinType : SkinType.values()) {
            registerLoader(Skin.class, skinType.getUriPattern(), instantiateLoader(skinType.getLoader()));
        }

        // geometry
        for (SkinGeometryType geometryType : SkinGeometryType.values()) {
            registerLoader(SkinGeometry.class, geometryType.getUriPattern(), instantiateLoader(geometryType.getLoader()));
        }

        // capes
        for (CapeType capeType : CapeType.values()) {
            registerLoader(Cape.class, capeType.getUriPattern(), instantiateLoader(capeType.getLoader()));
        }

        // ears
        for (EarsType earsType : EarsType.values()) {
            registerLoader(Ears.class, earsType.getUriPattern(), instantiateLoader(earsType.getLoader()));
        }

        // game profiles
        for (GameProfileDataType profileDataType : GameProfileDataType.values()) {
            registerLoader(GameProfileData.class, profileDataType.getUriPattern(), instantiateLoader(profileDataType.getLoader()));
        }

        // skulls
        for (SkullType skullType : SkullType.values()) {
            registerLoader(Skull.class, skullType.getUriPattern(), instantiateLoader(skullType.getLoader()));
        }
    }

    private static ResourceLoader<?, ?> instantiateLoader(@NonNull Class<? extends ResourceLoader<?, ?>> loaderClass) {
        return loaderInstances.computeIfAbsent(loaderClass, c -> createReflectively(loaderClass));
    }

    private static ResourceLoader<?, ?> createReflectively(@NonNull Class<? extends ResourceLoader<?, ?>> loaderClass) {
        Constructor<?>[] ctors = loaderClass.getDeclaredConstructors();
        Constructor<?> ctor = null;

        for (Constructor<?> constructor : ctors) {
            if (constructor.getGenericParameterTypes().length == 0) {
                ctor = constructor;
                break;
            }
        }

        if (ctor != null) {
            try {
                ctor.setAccessible(true);
                return (ResourceLoader<?, ?>) ctor.newInstance();
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException x) {
                x.printStackTrace();
            }
        }

        return null;
    }

    public static <T> void registerLoader(@NonNull Class<T> type, @NonNull Pattern pattern, @NonNull ResourceLoader<?, ?> loader) {
        loaders.computeIfAbsent(type, loaders -> new LinkedHashMap<>())
                .put(pattern, loader);
    }

    public static CompletableFuture<Map<? extends ResourceDescriptor<?, ?>, ResourceLoadResult>> loadAsyncForced(@NonNull ResourceDescriptor<?, ?>... descriptors) {
        return loadAsync(true, descriptors);
    }

    public static CompletableFuture<Map<? extends ResourceDescriptor<?, ?>, ResourceLoadResult>> loadAsync(@NonNull ResourceDescriptor<?, ?>... descriptors) {
        return loadAsync(false, descriptors);
    }

    public static CompletableFuture<Map<? extends ResourceDescriptor<?, ?>, ResourceLoadResult>> loadAsync(boolean force, @NonNull ResourceDescriptor<?, ?>... descriptors) {
        List<CompletableFuture<ResourceLoadResult>> completableFutures = Arrays.stream(descriptors).map(force ? ResourceManager::loadAsyncForced : ResourceManager::loadAsync).collect(Collectors.toList());
        if (completableFutures.size() > 0) {
            CompletableFuture<?>[] completableFuturesArray = completableFutures.toArray(new CompletableFuture[0]);
            return CompletableFuture.allOf(completableFuturesArray)
                    .thenApply(future -> completableFutures.stream().map(CompletableFuture::join)
                            .collect(Collectors.toMap(ResourceLoadResult::getDescriptor, Function.identity(), (existing, replacement) -> existing)));
        }

        return CompletableFuture.completedFuture(Collections.emptyMap());
    }

    public static <T, P> CompletableFuture<ResourceLoadResult> loadAsyncForced(@NonNull ResourceDescriptor<T, P> descriptor) {
        return loadAsync(true, descriptor);
    }

    public static <T, P> CompletableFuture<ResourceLoadResult> loadAsync(@NonNull ResourceDescriptor<T, P> descriptor) {
        return loadAsync(false, descriptor);
    }

    @SneakyThrows
    public static <T, P> CompletableFuture<ResourceLoadResult> loadAsync(boolean force, @NonNull ResourceDescriptor<T, P> descriptor) {
        ReentrantLock reentrantLock = resourceLoadLockCache.get(descriptor, ReentrantLock::new);
        reentrantLock.lock();
        try {
            if (!force) {
                // do this first to avoid race conditions (with completions happening)
                CompletableFuture<ResourceLoadResult> waitingFuture = getWaitingFuture(descriptor);
                if (waitingFuture != null) {
                    logger().debug("Descriptor: " + descriptor + " waitingFuture");
                    return waitingFuture;
                }

                // check if it's already loaded
                Cache<URI, ResourceContainer> uriResourceContainerCache = resources.get(descriptor.getType());
                if (uriResourceContainerCache != null) {
                    ResourceContainer resourceContainer = uriResourceContainerCache.getIfPresent(descriptor.getUri());
                    if (resourceContainer != null) {
                        if (!resourceContainer.isFailed) {
                            logger().debug("Descriptor: " + descriptor + " alreadyLoaded");
                            return CompletableFuture.completedFuture(ResourceLoadResult.of(false, descriptor, resourceContainer.getResource(), null));
                        }
                        logger().debug("Descriptor: " + descriptor + " alreadyFailed");
                        return CompletableFuture.completedFuture(ResourceLoadResult.of(true, descriptor, null, resourceContainer.getException()));
                    }
                }
            }

            // gotta load it
            ResourceLoader<T, P> loader = findLoader(descriptor.getType(), descriptor.getUri());
            if (loader == null) {
                logger().debug("Descriptor: " + descriptor + " noLoader");
                return CompletableFuture.completedFuture(ResourceLoadResult.of(true, descriptor, null, new ResourceLoadFailureException("Unable to find loader for " + descriptor)));
            }

            logger().debug("Descriptor: " + descriptor + " doLoad");
            return doLoad(descriptor, loader);
        } finally {
            reentrantLock.unlock();
        }
    }

    public static <T, P> void add(@NonNull ResourceDescriptor<T, P> descriptor, T resource) {
        resources.computeIfAbsent(descriptor.getType(), ResourceManager::createCache)
                .put(descriptor.getUri(), ResourceContainer.of(resource));
    }

    public static <T, P> void add(@NonNull ResourceDescriptor<T, P> descriptor, Throwable throwable) {
        resources.computeIfAbsent(descriptor.getType(), ResourceManager::createCache)
                .put(descriptor.getUri(), ResourceContainer.of(throwable));
    }

    public static <T, P> T get(@NonNull ResourceDescriptor<T, P> descriptor) {
        return get(descriptor, false, true);
    }

    @SuppressWarnings("unchecked")
    public static <T, P> T get(@NonNull ResourceDescriptor<T, P> descriptor, boolean throwFailure, boolean loadSync) {
        if (resources.containsKey(descriptor.getType())) {
            ResourceContainer container = resources.get(descriptor.getType()).getIfPresent(descriptor.getUri());
            if (container != null) {
                if (!container.isFailed) {
                    return (T) container.getResource();
                }
                if (throwFailure && !loadSync) {
                    throw container.getException();
                }
            }
        }

        if (loadSync) {
            ResourceLoadResult loadResult = loadAsyncForced(descriptor).join();
            if (!loadResult.isFailed()) {
                return (T) loadResult.getResource();
            }
            if (throwFailure) {
                throw loadResult.getException();
            }
        }

        return null;
    }

    private static <T> Cache<URI, ResourceContainer> createCache(@NonNull Class<T> type) {
        return CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
    }

    private static <T, P> CompletableFuture<ResourceLoadResult> doLoad(@NonNull ResourceDescriptor<T, P> descriptor, @NonNull ResourceLoader<T, P> loader) {
        CompletableFuture<ResourceLoadResult> clientFuture = new CompletableFuture<>();
        requestedResources.put(descriptor, clientFuture);
        loader.loadAsync(descriptor)
                .whenComplete((result, throwable) -> {
                    logger().debug("whenComplete r: " + result + " t: " + throwable);
                    if (throwable == null) {
                        add(descriptor, result);
                        notifySuccess(descriptor, result, clientFuture);
                    } else {
                        add(descriptor, throwable);
                        notifyFailure(descriptor, throwable, clientFuture);
                    }
                });
        return clientFuture;
    }

    private static <T, P> void notifySuccess(@NonNull ResourceDescriptor<T, P> descriptor, T resource, CompletableFuture<ResourceLoadResult> clientFuture) {
        requestedResources.remove(descriptor);
        clientFuture.complete(ResourceLoadResult.of(false, descriptor, resource, null));
    }

    private static <T, P> void notifyFailure(@NonNull ResourceDescriptor<T, P> descriptor, Throwable throwable, CompletableFuture<ResourceLoadResult> clientFuture) {
        requestedResources.remove(descriptor);
        ResourceLoadResult loadResult = ResourceLoadResult.of(true, descriptor, null, ResourceLoadFailureException.getOrWrapException(throwable));
        clientFuture.complete(loadResult);
        if (logger().isDebug()) {
            logger().error("Resource loading failed: " + descriptor, loadResult.getException());
        }
    }

    private static <T, P> CompletableFuture<ResourceLoadResult> getWaitingFuture(@NonNull ResourceDescriptor<T, P> descriptor) {
        return requestedResources.get(descriptor);
    }

    @SuppressWarnings("unchecked")
    private static <T, P> ResourceLoader<T, P> findLoader(Class<T> type, URI uri) {
        Optional<Map.Entry<Pattern, ResourceLoader<?, ?>>> loaderEntry = loaders.getOrDefault(type, new LinkedHashMap<>())
                .entrySet().stream().filter(e -> e.getKey().matcher(uri.toString()).matches()).findFirst();
        return loaderEntry.map(patternResourceLoaderEntry -> (ResourceLoader<T, P>) patternResourceLoaderEntry.getValue()).orElse(null);
    }

    private static GeyserLogger logger() {
        return GeyserConnector.getInstance().getLogger();
    }
}
