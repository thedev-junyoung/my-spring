package myspring.core;

import myspring.core.annotation.Component;
import myspring.core.annotation.Inject;
import myspring.core.annotation.Scope;
import myspring.core.annotation.ScopeType;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;

public class ApplicationContext implements AutoCloseable {

    // 싱글톤 캐시: SINGLETON 스코프의 인스턴스만 저장
    private final Map<Class<?>, Object> singletons = new HashMap<>();

    // 빈 정의 메타: 컴포넌트 클래스 → 스코프(SINGLETON/PROTOTYPE)
    private final Map<Class<?>, ScopeType> beanDefinitions = new HashMap<>();

    private final Set<Class<?>> components;
    private final Set<Class<?>> creating = new HashSet<>();

    // 라이프사이클 위임자
    private final LifecycleProcessor lifecycle = new LifecycleProcessor();


    private ApplicationContext(String basePackage) {
        // 1) @Component 스캔
        Reflections reflections = new Reflections(basePackage);
        this.components = reflections.getTypesAnnotatedWith(Component.class);

        // 2) 스코프 메타 등록 (@Scope 없으면 기본 SINGLETON)
        for (Class<?> c : components) {
            ScopeType scope = c.isAnnotationPresent(Scope.class)
                    ? c.getAnnotation(Scope.class).value()
                    : ScopeType.SINGLETON;
            beanDefinitions.put(c, scope);
        }

        // 싱글톤만 eager 생성 + PostConstruct 호출은 lifecycle이 담당
        for (Class<?> c : components) {
            if (beanDefinitions.get(c) == ScopeType.SINGLETON) {
                getOrCreateSingleton(c);
            }
        }
    }

    public static ApplicationContext of(String basePackage) {
        return new ApplicationContext(basePackage);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        // 1) 정확 매칭: 정의에 등록된 클래스면 스코프에 맞게 반환
        if (beanDefinitions.containsKey(type)) {
            return (T) getAccordingToScope(type);
        }

        // 2) 다형성 매칭: 컴포넌트들 중 assignable 후보 수집
        List<Class<?>> candidates = components.stream()
                .filter(type::isAssignableFrom)
                .toList();

        if (candidates.size() == 1) {
            return (T) getAccordingToScope(candidates.get(0));
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No bean of type: " + type.getName());
        }
        throw new IllegalArgumentException("No unique bean of type: " + type.getName() + ", candidates=" + candidates);
    }

    // 스코프에 따른 반환 전략
    private Object getAccordingToScope(Class<?> clazz) {
        ScopeType scope = beanDefinitions.getOrDefault(clazz, ScopeType.SINGLETON);
        return (scope == ScopeType.SINGLETON)
                ? getOrCreateSingleton(clazz)    // 캐시 사용/생성
                : createNewInstanceGraph(clazz); // 매번 새로 생성
    }

    // ===== 생성 로직 =====

    // 싱글톤 전용: 캐시에 있으면 꺼내고, 없으면 만들어서 캐시에 저장
    private Object getOrCreateSingleton(Class<?> clazz) {
        Object existing = singletons.get(clazz);
        if (existing != null) return existing;
        Object created = createNewInstanceGraph(clazz);
        singletons.put(clazz, created);
        return created;
    }



    // 생성 그래프: 생성자 선택 → 의존성 해결 → new → PostConstruct
    private Object createNewInstanceGraph(Class<?> clazz) {
        if (creating.contains(clazz))
            throw new IllegalStateException("Circular dependency detected at: " + clazz.getName());
        creating.add(clazz);
        try {
            Constructor<?> ctor = selectConstructor(clazz);
            Object[] args = Arrays.stream(ctor.getParameterTypes())
                    .map(this::resolveDependency)
                    .toArray();
            ctor.setAccessible(true);
            Object instance = newInstance(ctor, args);

            // 위임: 생성/주입 완료 직후 초기화 훅 실행
            lifecycle.invokePostConstruct(instance);

            return instance;
        } finally {
            creating.remove(clazz);
        }
    }

    // @Inject 1개 우선, 없으면 기본 생성자
    private Constructor<?> selectConstructor(Class<?> clazz) {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        List<Constructor<?>> injects = Arrays.stream(ctors)
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .toList();
        if (injects.size() > 1) {
            throw new IllegalStateException("@Inject constructor must be single: " + clazz.getName());
        }
        if (injects.size() == 1) return injects.get(0);
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No default constructor: " + clazz.getName(), e);
        }
    }

    private Object resolveDependency(Class<?> depType) {
        // 정의된 컴포넌트 중 타입 매칭
        Optional<Class<?>> target = components.stream()
                .filter(depType::isAssignableFrom)
                .findFirst();
        if (target.isEmpty()) {
            throw new IllegalStateException("Unsatisfied dependency: " + depType.getName());
        }
        Class<?> targetClass = target.get();
        ScopeType scope = beanDefinitions.getOrDefault(targetClass, ScopeType.SINGLETON);
        return (scope == ScopeType.SINGLETON)
                ? getOrCreateSingleton(targetClass)    // 싱글톤은 캐시
                : createNewInstanceGraph(targetClass); // 프로토타입은 새로 생성
    }

    private static Object newInstance(Constructor<?> ctor, Object[] args) {
        try {
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + ctor.getDeclaringClass().getName(), e);
        }
    }

    @Override
    public void close() {
        lifecycle.invokePreDestroyAll(singletons.values());
    }
}
