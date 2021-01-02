package org.geysermc.connector.skin.resource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.skin.resource.types.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResourceManager {
    private static final Map<Class<?>, LinkedHashMap<Pattern, ResourceLoader<?, ?>>> loaders = new ConcurrentHashMap<>();
    private static final Map<ResourceDescriptor<?, ?>, CompletableFuture<ResourceLoadResult>> requestedResources = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Cache<URI, ResourceContainer>> resources = new ConcurrentHashMap<>();
    private static final Map<Class<? extends ResourceLoader<?, ?>>, ResourceLoader<?, ?>> loaderInstances = new ConcurrentHashMap<>();

    static {

        // LinkedHashMap sorts entries by insertion order (used for pattern matching precedence)

        // skins
        for (PlayerSkinType skinType : PlayerSkinType.values()) {
            registerLoader(PlayerSkin.class, skinType.getUriPattern(), instantiateLoader(skinType.getLoader()));
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
        Constructor ctor = null;

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

    public static <T, P> void registerLoader(@NonNull Class<T> type, @NonNull Pattern pattern, @NonNull ResourceLoader<?, ?> loader) {
        loaders.computeIfAbsent(type, loaders -> new LinkedHashMap<>())
                .put(pattern, loader);
    }

    public static CompletableFuture<Map<? extends ResourceDescriptor<?, ?>, ResourceLoadResult>> loadAsync(@NonNull ResourceDescriptor<?, ?>... descriptors) {
        List<CompletableFuture<ResourceLoadResult>> completableFutures = Arrays.stream(descriptors).map(ResourceManager::loadAsync).collect(Collectors.toList());
        return CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]))
                .thenApply(future -> {
                    return completableFutures.stream().map(completableFuture -> completableFuture.join())
                            .collect(Collectors.toMap(ResourceLoadResult::getDescriptor, Function.identity(), (existing, replacement) -> existing));
                });
    }

    public static <T, P> CompletableFuture<ResourceLoadResult> loadAsync(@NonNull ResourceDescriptor<T, P> descriptor) {
        ResourceLoader<T, P> loader = findLoader(descriptor.getType(), descriptor.getUri());
        if (loader == null) {
            return CompletableFuture.completedFuture(ResourceLoadResult.of(true, descriptor, null, new ResourceLoadFailureException("Unable to find loader for " + descriptor)));
        }

        if (isLoaded(descriptor)) {
            ResourceContainer resourceContainer = resources.get(descriptor.getType()).getIfPresent(descriptor.getUri());
            if (resourceContainer != null) {
                return CompletableFuture.completedFuture(ResourceLoadResult.of(false, descriptor, resourceContainer.getResource(), null));
            }
        }

        if (!isLoading(descriptor)) {
            doLoad(descriptor, loader);
        }

        return requestedResources.get(descriptor);
    }

    public static <T, P> void add(@NonNull ResourceDescriptor<T, P> descriptor, T resource) {
        resources.computeIfAbsent(descriptor.getType(), ResourceManager::createCache)
                .put(descriptor.getUri(), ResourceContainer.of(resource));
    }

    public static <T, P> T get(@NonNull ResourceDescriptor<T, P> descriptor) {
        if (resources.containsKey(descriptor.getType())) {
            ResourceContainer container = resources.get(descriptor.getType()).getIfPresent(descriptor.getUri());
            if (container != null) {
                return (T) container.getResource();
            }
        }
        return null;
    }

    private static <T> Cache<URI, ResourceContainer> createCache(@NonNull Class<T> type) {
        return CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
    }

    private static <T, P> void doLoad(@NonNull ResourceDescriptor<T, P> descriptor, @NonNull ResourceLoader<T, P> loader) {
        CompletableFuture<ResourceLoadResult> clientFuture = new CompletableFuture<>();
        requestedResources.put(descriptor, clientFuture);
        loader.loadAsync(descriptor)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        add(descriptor, result);
                        notifySuccess(descriptor, result, clientFuture);
                    } else {
                        notifyFailure(descriptor, throwable, clientFuture);
                    }
                });
    }

    private static <T, P> void notifySuccess(@NonNull ResourceDescriptor<T, P> descriptor, T resource, CompletableFuture<ResourceLoadResult> clientFuture) {
        requestedResources.remove(descriptor);
        clientFuture.complete(ResourceLoadResult.of(false, descriptor, resource, null));
    }

    private static <T, P> void notifyFailure(@NonNull ResourceDescriptor<T, P> descriptor, Throwable throwable, CompletableFuture<ResourceLoadResult> clientFuture) {
        requestedResources.remove(descriptor);
        if (!(throwable instanceof ResourceLoadFailureException)) {
            throwable = new ResourceLoadFailureException(throwable);
        }
        ResourceLoadResult loadResult = ResourceLoadResult.of(true, descriptor, null, (ResourceLoadFailureException) throwable);
        clientFuture.complete(loadResult);
        GeyserConnector.getInstance().getLogger().error("Resource loading failed: " + loadResult);
    }

    public static <T, P> boolean isLoading(@NonNull ResourceDescriptor<T, P> descriptor) {
        return requestedResources.containsKey(descriptor);

    }

    public static <T, P> boolean isLoaded(@NonNull ResourceDescriptor<T, P> descriptor) {
        return resources.containsKey(descriptor.getClass()) && resources.get(descriptor.getClass()).getIfPresent(descriptor.getUri()) != null;
    }

    private static <T, P> ResourceLoader<T, P> findLoader(Class<T> type, URI uri) {
        Optional<Map.Entry<Pattern, ResourceLoader<?, ?>>> loaderEntry = loaders.getOrDefault(type, new LinkedHashMap<>())
                .entrySet().stream().filter(e -> e.getKey().matcher(uri.toString()).matches()).findFirst();
        return loaderEntry.map(patternResourceLoaderEntry -> (ResourceLoader<T, P>) patternResourceLoaderEntry.getValue()).orElse(null);
    }
}
