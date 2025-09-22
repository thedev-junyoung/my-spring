package myspring.core;

import myspring.core.annotation.Component;
import myspring.core.annotation.Inject;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;

public class ApplicationContext {
    private final Map<Class<?>, Object> beans = new HashMap<>();
    private final Set<Class<?>> components;
    private final Set<Class<?>> creating = new HashSet<>(); // 순환 감지용

    private ApplicationContext(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        this.components = reflections.getTypesAnnotatedWith(Component.class);
        System.out.println("Found components: " + components);
        // 즉시 전부 만들 필요는 없지만, 간단히 전부 만들어두자
        for (Class<?> c : components) {
            getOrCreate(c);
        }
    }

    public static ApplicationContext of(String basePackage) {
        return new ApplicationContext(basePackage);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        // 정확 매칭
        Object bean = beans.get(type);
        if (bean != null) return (T) bean;

        // 할당 가능 매칭(인터페이스/상위타입)
        List<Map.Entry<Class<?>, Object>> candidates = beans.entrySet().stream()
                .filter(e -> type.isAssignableFrom(e.getKey()))
                .toList();
        if (candidates.size() == 1) {
            return (T) candidates.get(0).getValue();
        }
        if (candidates.isEmpty()) {
            // 아직 안 만들어졌으면 컴포넌트 중에서 타입 맞는 걸 찾아 생성 시도
            Optional<Class<?>> target = components.stream()
                    .filter(type::isAssignableFrom)
                    .findFirst();
            if (target.isPresent()) {
                return (T) getOrCreate(target.get());
            }
        }
        throw new IllegalArgumentException("No unique bean of type: " + type.getName());
    }

    // ===== 내부 구현 =====

    private Object getOrCreate(Class<?> clazz) {
        Object existing = beans.get(clazz);
        if (existing != null) return existing;

        if (creating.contains(clazz)) {
            throw new IllegalStateException("Circular dependency detected at: " + clazz.getName());
        }
        creating.add(clazz);
        try {
            Constructor<?> ctor = selectConstructor(clazz);
            Object[] args = Arrays.stream(ctor.getParameterTypes())
                    .map(this::resolveDependency)
                    .toArray();
            ctor.setAccessible(true);
            Object instance = newInstance(ctor, args);
            beans.put(clazz, instance);
            return instance;
        } finally {
            creating.remove(clazz);
        }
    }

    private Constructor<?> selectConstructor(Class<?> clazz) {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        List<Constructor<?>> injects = Arrays.stream(ctors)
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .toList();
        if (injects.size() > 1) {
            throw new IllegalStateException("@Inject constructor must be single: " + clazz.getName());
        }
        if (injects.size() == 1) return injects.get(0);

        // @Inject가 없으면 기본 생성자 fallback
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No default constructor: " + clazz.getName(), e);
        }
    }

    private Object resolveDependency(Class<?> depType) {
        // 이미 만들어진 빈
        for (Map.Entry<Class<?>, Object> e : beans.entrySet()) {
            if (depType.isAssignableFrom(e.getKey())) {
                return e.getValue();
            }
        }
        // 컴포넌트 중 타입 일치하는 것 찾아 생성
        Optional<Class<?>> target = components.stream()
                .filter(depType::isAssignableFrom)
                .findFirst();
        if (target.isPresent()) {
            return getOrCreate(target.get());
        }
        throw new IllegalStateException("Unsatisfied dependency: " + depType.getName());
    }

    private static Object newInstance(Constructor<?> ctor, Object[] args) {
        try {
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + ctor.getDeclaringClass().getName(), e);
        }
    }
}
